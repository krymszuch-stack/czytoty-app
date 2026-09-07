package com.example.engine

import kotlin.math.roundToInt

/**
 * Wynik analizy reputacji społecznościowej (Filar 2: REPUTATION).
 * Odpowiada na pytanie: "Jak społeczność i źródła oceniają zachowanie tego numeru?"
 */
data class ReputationAssessment(
    val positivePercent: Int,
    val negativePercent: Int,
    val dominantCategory: ReputationCategory,
    val totalSignalsCount: Int,
    val freshSignalsCount: Int, // Ostatnie 30 dni
    val signalsLast24h: Int,
    val isAnomalyDetected: Boolean,
    val anomalyReason: String?,
    val categoryDistribution: Map<ReputationCategory, Int>,
    val reputationLabel: String,
    val reasons: List<EngineReason>
)

object ReputationEngineV1 {

    fun evaluateReputation(
        signals: List<CommunicationSignal>,
        currentEpoch: Long = System.currentTimeMillis()
    ): ReputationAssessment {
        if (signals.isEmpty()) {
            return ReputationAssessment(
                positivePercent = 50,
                negativePercent = 50,
                dominantCategory = ReputationCategory.OK,
                totalSignalsCount = 0,
                freshSignalsCount = 0,
                signalsLast24h = 0,
                isAnomalyDetected = false,
                anomalyReason = null,
                categoryDistribution = emptyMap(),
                reputationLabel = "Brak opinii społeczności",
                reasons = listOf(EngineReason.create(EngineReasonCode.NO_COMMUNITY_SIGNALS))
            )
        }

        // 1. Ochrona anty-abuse: deduplikacja (jeden aktywny głos na użytkownika)
        val deduplicated = AntiAbuseEngine.deduplicateUserSignals(signals)

        // 2. Detekcja anomalii (fale botów / review bombing)
        val anomalyCheck = AntiAbuseEngine.checkReportingAnomaly(deduplicated, currentEpoch)

        val thirtyDaysAgo = currentEpoch - (30L * 24L * 60L * 60L * 1000L)
        val twentyFourHoursAgo = currentEpoch - (24L * 60L * 60L * 1000L)

        val freshCount = deduplicated.count { it.provenance.timestampEpoch >= thirtyDaysAgo }
        val last24hCount = deduplicated.count { it.provenance.timestampEpoch >= twentyFourHoursAgo }

        var weightedPositive = 0f
        var weightedNegative = 0f

        val categoryCounts = mutableMapOf<ReputationCategory, Int>()
        val reasons = mutableListOf<EngineReason>()

        for (signal in deduplicated) {
            val decay = TimeDecayEngine.calculateExponentialDecay(signal.provenance.timestampEpoch, currentEpoch)
            val trust = signal.provenance.reporterTrustScore.coerceIn(0.1f, 1.0f)
            val effectiveWeight = decay * trust

            categoryCounts[signal.category] = (categoryCounts[signal.category] ?: 0) + 1

            when (signal.category) {
                ReputationCategory.OK -> {
                    weightedPositive += effectiveWeight
                }
                ReputationCategory.ANNOYING -> {
                    weightedNegative += (effectiveWeight * 0.7f)
                }
                ReputationCategory.TELEMARKETING -> {
                    weightedNegative += (effectiveWeight * 0.85f)
                }
                ReputationCategory.BOT -> {
                    weightedNegative += (effectiveWeight * 1.0f)
                }
                ReputationCategory.SUSPECTED_SCAM -> {
                    weightedNegative += (effectiveWeight * 1.6f)
                }
                ReputationCategory.IMPERSONATION -> {
                    weightedNegative += (effectiveWeight * 2.0f)
                }
                ReputationCategory.OTHER -> {
                    weightedNegative += (effectiveWeight * 0.5f)
                }
            }
        }

        val totalWeight = weightedPositive + weightedNegative
        val positivePercent = if (totalWeight > 0f) {
            ((weightedPositive / totalWeight) * 100f).roundToInt().coerceIn(0, 100)
        } else 50
        val negativePercent = 100 - positivePercent

        // Wyznaczenie dominującej kategorii (nie sprowadzając do "dobry/zły")
        val dominantCategory = categoryCounts.maxByOrNull { (cat, count) ->
            count * cat.baseWeight
        }?.key ?: ReputationCategory.OK

        // Generowanie stabilnych przyczyn reputacji
        val scamAndImpersonationCount = (categoryCounts[ReputationCategory.SUSPECTED_SCAM] ?: 0) +
                (categoryCounts[ReputationCategory.IMPERSONATION] ?: 0)

        if (scamAndImpersonationCount > 0) {
            reasons.add(EngineReason.create(EngineReasonCode.SCAM_VISHING_REPORTS, scamAndImpersonationCount.toString()))
        } else if ((categoryCounts[ReputationCategory.TELEMARKETING] ?: 0) > 0) {
            reasons.add(EngineReason.create(EngineReasonCode.TELEMARKETING_MASS_CALLS))
        } else if ((categoryCounts[ReputationCategory.BOT] ?: 0) > 0) {
            reasons.add(EngineReason.create(EngineReasonCode.ROBOCALL_AUTOMATED))
        } else if (positivePercent >= 80 && deduplicated.size >= 3) {
            reasons.add(EngineReason.create(EngineReasonCode.COMMUNITY_HIGHLY_POSITIVE))
        }

        if (last24hCount >= 5) {
            reasons.add(EngineReason.create(EngineReasonCode.HIGH_ACTIVITY_24H, last24hCount.toString()))
        }

        if (anomalyCheck.isAnomalyDetected) {
            reasons.add(EngineReason.create(EngineReasonCode.ANOMALOUS_REPORTING_SURGE))
        }

        val label = when {
            anomalyCheck.isAnomalyDetected -> "Weryfikacja zgłoszeń w toku"
            dominantCategory == ReputationCategory.IMPERSONATION -> "Zgłoszenia podszywania się pod markę"
            dominantCategory == ReputationCategory.SUSPECTED_SCAM -> "Zgłoszenia prób oszustwa"
            dominantCategory == ReputationCategory.TELEMARKETING -> "Oferty handlowe i telemarketing"
            dominantCategory == ReputationCategory.BOT -> "Automatyczne połączenia (bot)"
            dominantCategory == ReputationCategory.ANNOYING -> "Uciążliwy kontakt"
            positivePercent >= 75 -> "Pozytywna ocena społeczności"
            else -> "Umiarkowana reputacja"
        }

        return ReputationAssessment(
            positivePercent = positivePercent,
            negativePercent = negativePercent,
            dominantCategory = dominantCategory,
            totalSignalsCount = deduplicated.size,
            freshSignalsCount = freshCount,
            signalsLast24h = last24hCount,
            isAnomalyDetected = anomalyCheck.isAnomalyDetected,
            anomalyReason = anomalyCheck.anomalyReason,
            categoryDistribution = categoryCounts,
            reputationLabel = label,
            reasons = reasons
        )
    }
}
