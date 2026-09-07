package com.example.engine

import kotlin.math.min
import kotlin.math.roundToInt

enum class RiskLevel(val displayName: String, val shortLabel: String) {
    LOW("Niskie ryzyko", "Niskie"),
    MODERATE("Umiarkowane ryzyko", "Umiarkowane"),
    ELEVATED("Podwyższone ryzyko", "Podwyższone"),
    HIGH("Wysokie ryzyko oszustwa", "Wysokie")
}

data class RiskAssessment(
    val riskScore: Int, // 0 .. 100
    val riskLevel: RiskLevel,
    val reasons: List<EngineReason>,
    val confidence: ConfidenceAssessment,
    val engineVersion: String = EngineConfig.ENGINE_VERSION
)

/**
 * Główny deterministyczny silnik oceny ryzyka interakcji (Filar 3: RISK).
 * Odpowiada na pytanie: "Jak ryzykowna jest aktualna interakcja?"
 *
 * Wersja: 1.0.0 (INITIAL_CALIBRATION).
 * W pełni deterministyczny, transparentny, bez "czarnych skrzynek".
 */
object RiskEngineV1 {

    fun assessRisk(
        identity: IdentityAssessment,
        reputation: ReputationAssessment,
        activeSignal: CommunicationSignal? = null,
        historicalSignals: List<CommunicationSignal> = emptyList(),
        currentEpoch: Long = System.currentTimeMillis(),
        conversationSignal: com.example.conversation.safety.model.ConversationSafetySignal? = null
    ): RiskAssessment {
        val reasons = mutableListOf<EngineReason>()
        var baseRisk = 0f

        // 1. KATEGORIA REPUTACJI SPOŁECZNOŚCIOWEJ (Baza ryzyka)
        val categoryBase = when (reputation.dominantCategory) {
            ReputationCategory.OK -> 0f
            ReputationCategory.ANNOYING -> 22f
            // Telemarketing to uciążliwość, a NIE oszustwo: mieści się ściśle w przedziale 35..49
            ReputationCategory.TELEMARKETING -> 36f
            ReputationCategory.BOT -> 45f
            ReputationCategory.SUSPECTED_SCAM -> 75f
            ReputationCategory.IMPERSONATION -> 85f
            ReputationCategory.OTHER -> 15f
        }
        baseRisk = categoryBase

        // 2. SKŁADOWA WOLUMENU RAPORTÓW (Do +10 punktów przy dużej próbie)
        val volumeRatio = min(1.0f, reputation.totalSignalsCount / 25f)
        baseRisk += (volumeRatio * 10f)

        // 3. SKŁADOWA ŚWIEŻOŚCI 24H (Do +10 punktów przy aktywnej fali)
        val recencyRatio = min(1.0f, reputation.signalsLast24h / 10f)
        baseRisk += (recencyRatio * 10f)

        // BEZWZGLĘDNY NIEZMIENNIK: Czysty telemarketing nigdy nie przekracza 49 punktów
        if (reputation.dominantCategory == ReputationCategory.TELEMARKETING) {
            baseRisk = baseRisk.coerceAtMost(49f)
        }

        // 4. BEZPOŚREDNIE ZAGROŻENIA W AKTYWNYM SYGNALE (SMS / LINK / TREŚĆ)
        if (activeSignal != null && activeSignal.channel == SignalChannel.SMS) {
            var smsThreatScore = 0

            if (activeSignal.hasBlikKeywords) {
                smsThreatScore += 35
                reasons.add(EngineReason.create(EngineReasonCode.SMS_BLIK_OR_CREDENTIALS))
            }
            if (activeSignal.hasBrandMismatch) {
                smsThreatScore += 35
                reasons.add(EngineReason.create(EngineReasonCode.SMS_BRAND_DOMAIN_MISMATCH))
            }
            if (activeSignal.hasShortenerUrl) {
                smsThreatScore += 25
                reasons.add(EngineReason.create(EngineReasonCode.SMS_SUSPICIOUS_SHORTENER))
            }
            if (activeSignal.hasUrgentPaymentKeywords) {
                smsThreatScore += 25
                reasons.add(EngineReason.create(EngineReasonCode.SMS_URGENT_PAYMENT_DEMAND))
            }
            if (activeSignal.urlThreatScore > 0) {
                smsThreatScore += (activeSignal.urlThreatScore * 0.4f).roundToInt()
            }

            val finalSmsThreat = smsThreatScore.coerceIn(0, 100).toFloat()
            // Złośliwy link / phishing w treści SMS natychmiast wyznacza wysokie ryzyko interakcji
            baseRisk = maxOf(baseRisk, finalSmsThreat)
        }

        // 4b. BEZPOŚREDNIE SYGNAŁY Z ANALIZY ROZMOWY (Conversation Safety Snapshot)
        if (conversationSignal != null) {
            val convResult = conversationSignal.riskResult
            if (convResult.signals.isNotEmpty()) {
                // Wykryto krytyczne żądanie w aktywnej rozmowie: modulacja ryzyka aktywnej sesji
                baseRisk = maxOf(baseRisk, convResult.riskScore.toFloat())
                val criticalSignalsList = convResult.signals.joinToString(", ") { it.code.label }
                reasons.add(0, EngineReason.create(EngineReasonCode.CONVERSATION_HIGH_RISK_SIGNALS, criticalSignalsList))
            } else if (convResult.reasons.isNotEmpty() && reasons.none { it.code == EngineReasonCode.CONVERSATION_SAFE_NO_SIGNALS }) {
                reasons.add(EngineReason.create(EngineReasonCode.CONVERSATION_SAFE_NO_SIGNALS))
            }
        }

        // 5. PARADOKS SPOOFINGU INFOLINII BANKOWEJ (CLI Spoofing)
        // Jeśli numer to oficjalna infolinia przychodząca, a my odbieramy połączenie przychodzące
        if (identity.isInboundOnlyHotline && (activeSignal == null || activeSignal.channel == SignalChannel.CALL)) {
            val scamReports = reputation.categoryDistribution[ReputationCategory.SUSPECTED_SCAM] ?: 0
            val impReports = reputation.categoryDistribution[ReputationCategory.IMPERSONATION] ?: 0

            if (scamReports > 0 || impReports > 0 || reputation.totalSignalsCount > 0) {
                baseRisk += EngineConfig.RiskWeights.INBOUND_HOTLINE_SPOOF_BOOST
                reasons.add(EngineReason.create(EngineReasonCode.HOTLINE_CLI_SPOOF_RISK))
            }
        }

        // 5b. REJESTR PUBLICZNY UKE / KRS
        if (identity.isPublicDirectoryListed) {
            val entityName = identity.organizationName ?: identity.displayName ?: "Rejestr podmiotów"
            reasons.add(EngineReason.create(EngineReasonCode.PUBLIC_REGISTER_LISTED, entityName))
        }

        // 6. WYGASZANIE DANYCH HISTORYCZNYCH (Time Decay Mitigation)
        val allHistoricalSignals = (if (activeSignal != null) historicalSignals + activeSignal else historicalSignals)
        if (allHistoricalSignals.isNotEmpty() && reputation.freshSignalsCount == 0) {
            val avgAgeDays = allHistoricalSignals.map { it.provenance.ageDays(currentEpoch) }.average()
            if (avgAgeDays > 60) {
                baseRisk *= 0.50f // Zmniejszenie wpływu starych danych o 50%
                reasons.add(EngineReason.create(EngineReasonCode.HISTORICAL_DATA_DECAYED))
            }
        }

        // 7. ZWERYFIKOWANY PARTNER SAP (Mitigacja ryzyka dla kartoteki Enterprise)
        if (identity.sapPartner != null && identity.isCallerIdentityAuthenticated) {
            // Jeśli nie ma aktywnego złośliwego linku w SMS, numer SAP traktujemy jako zaufany
            if (activeSignal == null || !activeSignal.hasBrandMismatch) {
                baseRisk = 0f
                reasons.add(EngineReason.create(EngineReasonCode.SAP_VERIFIED_ENTERPRISE, identity.sapPartner.companyName))
            }
        }

        // Przepisz przyczyny z reputacji jeśli jeszcze ich nie ma
        for (repReason in reputation.reasons) {
            if (reasons.none { it.code == repReason.code }) {
                reasons.add(repReason)
            }
        }

        if (reasons.isEmpty()) {
            reasons.add(EngineReason.create(EngineReasonCode.NO_COMMUNITY_SIGNALS))
        }

        val finalScore = min(100, baseRisk.roundToInt().coerceAtLeast(0))

        val riskLevel = when {
            finalScore > EngineConfig.ThresholdConfig.RISK_ELEVATED_MAX -> RiskLevel.HIGH
            finalScore > EngineConfig.ThresholdConfig.RISK_MODERATE_MAX -> RiskLevel.ELEVATED
            finalScore > EngineConfig.ThresholdConfig.RISK_LOW_MAX -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        // Obliczenie pewności danych przez ConfidenceEngine
        val confidence = ConfidenceEngineV1.evaluateConfidence(
            signals = allHistoricalSignals,
            identity = identity,
            isAnomalyDetected = reputation.isAnomalyDetected
        )

        return RiskAssessment(
            riskScore = finalScore,
            riskLevel = riskLevel,
            reasons = reasons,
            confidence = confidence,
            engineVersion = EngineConfig.ENGINE_VERSION
        )
    }
}
