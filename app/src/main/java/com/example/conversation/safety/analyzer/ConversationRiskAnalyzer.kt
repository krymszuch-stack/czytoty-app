package com.example.conversation.safety.analyzer

import com.example.conversation.safety.model.ConversationRiskResult
import com.example.conversation.safety.model.DetectedSignal
import com.example.conversation.safety.model.SpeakerEstimate
import com.example.conversation.safety.model.TranscriptResult
import com.example.conversation.safety.model.TranscriptStatus
import com.example.engine.ConfidenceAssessment
import com.example.engine.EngineConfig
import com.example.engine.RiskLevel
import com.example.model.ConfidenceLevel
import kotlin.math.roundToInt

/**
 * Deterministyczny analizator ryzyka rozmowy na podstawie sygnałów z transkrypcji (Filar: CONVERSATION RISK).
 *
 * REGULY BIZNESOWE:
 * 1. Safe case: Jeśli brak sygnałów oszustwa, generuje komunikat:
 *    "Nie wykryliśmy typowych sygnałów oszustwa w analizowanym fragmencie."
 *    NIGDY: "Rozmowa jest bezpieczna." (brak wykrycia != bezpieczeństwo).
 * 2. High risk case: Wskazuje konkretne sygnały (prośba o kod BLIK, konto techniczne itp.).
 *    NIGDY: "ROZMAWIASZ Z OSZUSTEM".
 */
object ConversationRiskAnalyzer {

    const val SAFE_CASE_REASON = "Nie wykryliśmy typowych sygnałów oszustwa w analizowanym fragmencie."

    fun analyze(
        transcriptResult: TranscriptResult,
        speakerEstimate: SpeakerEstimate,
        signals: List<DetectedSignal>,
        processingTimeMs: Long = 0
    ): ConversationRiskResult {

        // 1. Obsługa hałasu / braku mowy / błędu transkrypcji (Test Case F)
        if (transcriptResult.status != TranscriptStatus.SUCCESS ||
            transcriptResult.confidence < 0.35f ||
            transcriptResult.text.isBlank()
        ) {
            val confidence = ConfidenceAssessment(
                level = ConfidenceLevel.NISKA,
                internalScore = transcriptResult.confidence.coerceIn(0f, 0.3f),
                independentSourcesCount = 0,
                uniqueReportersCount = 0,
                consensusAgreementRate = 0f,
                summary = "Niezrozumiały sygnał audio lub niska pewność transkrypcji."
            )

            return ConversationRiskResult(
                riskScore = 0,
                riskLevel = RiskLevel.LOW,
                confidence = confidence,
                signals = emptyList(),
                reasons = listOf("Niezrozumiałe audio lub zakłócenia w analizowanym fragmencie."),
                engineVersion = EngineConfig.ENGINE_VERSION,
                processingTimeMs = processingTimeMs
            )
        }

        // 2. Obsługa przypadku bezpiecznego (Brak podejrzanych sygnałów) (Test Case A, C)
        if (signals.isEmpty()) {
            val confidence = ConfidenceAssessment(
                level = if (transcriptResult.confidence >= 0.8f) ConfidenceLevel.WYSOKA else ConfidenceLevel.SREDNIA,
                internalScore = transcriptResult.confidence,
                independentSourcesCount = 1,
                uniqueReportersCount = 0,
                consensusAgreementRate = 1.0f,
                summary = "Czysta transkrypcja fragmentu (brak sygnałów oszustwa)."
            )

            return ConversationRiskResult(
                riskScore = 8, // Niskie bazowe ryzyko
                riskLevel = RiskLevel.LOW,
                confidence = confidence,
                signals = emptyList(),
                reasons = listOf(SAFE_CASE_REASON),
                engineVersion = EngineConfig.ENGINE_VERSION,
                processingTimeMs = processingTimeMs
            )
        }

        // 3. Obliczenie wyniku ryzyka na podstawie wykrytych sygnałów
        val maxWeight = signals.maxOf { it.weight }
        val additionalBoost = ((signals.size - 1) * 4).coerceAtMost(10)
        val calculatedScore = (maxWeight + additionalBoost).coerceIn(0, 100)

        val riskLevel = when {
            calculatedScore > EngineConfig.ThresholdConfig.RISK_ELEVATED_MAX -> RiskLevel.HIGH
            calculatedScore > EngineConfig.ThresholdConfig.RISK_MODERATE_MAX -> RiskLevel.ELEVATED
            calculatedScore > EngineConfig.ThresholdConfig.RISK_LOW_MAX -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        val avgSignalConfidence = signals.map { it.confidence }.average().toFloat()
        val combinedConfidenceScore = (transcriptResult.confidence * 0.4f + avgSignalConfidence * 0.6f)

        val confidenceLevel = when {
            combinedConfidenceScore >= 0.80f -> ConfidenceLevel.WYSOKA
            combinedConfidenceScore >= 0.50f -> ConfidenceLevel.SREDNIA
            else -> ConfidenceLevel.NISKA
        }

        val reasons = signals.map { it.explanation }.distinct()

        val confidence = ConfidenceAssessment(
            level = confidenceLevel,
            internalScore = combinedConfidenceScore,
            independentSourcesCount = signals.map { it.code }.distinct().size,
            uniqueReportersCount = 0,
            consensusAgreementRate = 1.0f,
            summary = "Wykryto ${signals.size} sygnał(y/ów) podwyższonego ryzyka w treści rozmowy."
        )

        return ConversationRiskResult(
            riskScore = calculatedScore,
            riskLevel = riskLevel,
            confidence = confidence,
            signals = signals,
            reasons = reasons,
            engineVersion = EngineConfig.ENGINE_VERSION,
            processingTimeMs = processingTimeMs
        )
    }
}
