package com.example.engine

import com.example.model.ConfidenceLevel
import kotlin.math.min

/**
 * Wynik oceny pewności danych (Filar 4: CONFIDENCE).
 * Odpowiada na pytanie: "Jak dobre i wiarygodne są dane, na których oparto wynik?"
 *
 * Ważne: Confidence NIE oznacza poziomu bezpieczeństwa!
 * Oznacza wyłącznie jakość, świeżość i spójność dostępnych dowodów.
 */
data class ConfidenceAssessment(
    val level: ConfidenceLevel,
    val internalScore: Float, // 0.0 .. 1.0 (do użytku wewnętrznego)
    val independentSourcesCount: Int,
    val uniqueReportersCount: Int,
    val consensusAgreementRate: Float,
    val summary: String
)

object ConfidenceEngineV1 {

    fun evaluateConfidence(
        signals: List<CommunicationSignal>,
        identity: IdentityAssessment,
        isAnomalyDetected: Boolean
    ): ConfidenceAssessment {
        if (signals.isEmpty()) {
            // Jeśli numer figuruje w oficjalnym rejestrze lub SAP, mamy wysoką pewność tożsamości
            if (identity.sapPartner != null || identity.isPublicDirectoryListed) {
                return ConfidenceAssessment(
                    level = ConfidenceLevel.WYSOKA,
                    internalScore = 0.85f,
                    independentSourcesCount = 1,
                    uniqueReportersCount = 0,
                    consensusAgreementRate = 1.0f,
                    summary = "Oficjalny wpis w rejestrze publicznym (brak zgłoszeń społeczności)."
                )
            }

            return ConfidenceAssessment(
                level = ConfidenceLevel.NISKA,
                internalScore = 0.10f,
                independentSourcesCount = 0,
                uniqueReportersCount = 0,
                consensusAgreementRate = 0.0f,
                summary = "Brak wystarczającej liczby danych do rzetelnej oceny."
            )
        }

        // 1. Liczba niezależnych typów źródeł (SourceType diversity)
        val distinctSources = signals.map { it.provenance.sourceType }.distinct().size
        val sourceScore = min(1.0f, distinctSources / 2.0f)

        // 2. Liczba unikalnych raportujących
        val uniqueReporters = signals.map { it.provenance.sourceId }.distinct().size
        val reporterScore = min(1.0f, uniqueReporters / EngineConfig.ConfidenceWeights.MAX_REPORTERS_CEILING)

        // 3. Średni Trust Score raportujących
        val avgTrust = if (signals.isNotEmpty()) {
            signals.map { it.provenance.reporterTrustScore }.average().toFloat().coerceIn(0.1f, 1.0f)
        } else 0.5f

        // 4. Świeżość danych (średni decay sygnałów)
        val avgFreshness = if (signals.isNotEmpty()) {
            signals.map { TimeDecayEngine.calculateExponentialDecay(it.provenance.timestampEpoch) }.average().toFloat()
        } else 0.5f

        // 5. Zgodność między źródłami (Consensus)
        val totalSignals = signals.size.toFloat()
        val dominantCategoryCount = signals.groupBy { it.category }.maxOfOrNull { it.value.size } ?: 0
        val consensusRate = if (totalSignals > 0) dominantCategoryCount / totalSignals else 0.5f

        // 6. Obecność oficjalnego źródła
        val hasOfficial = if (identity.sapPartner != null || identity.isPublicDirectoryListed) 1.0f else 0.0f

        // Suma ważona
        var rawConfidence = (
                sourceScore * EngineConfig.ConfidenceWeights.INDEPENDENT_SOURCES_WEIGHT +
                reporterScore * EngineConfig.ConfidenceWeights.REPORTER_COUNT_WEIGHT +
                avgTrust * EngineConfig.ConfidenceWeights.REPORTER_TRUST_WEIGHT +
                avgFreshness * EngineConfig.ConfidenceWeights.DATA_FRESHNESS_WEIGHT +
                consensusRate * EngineConfig.ConfidenceWeights.INTER_SOURCE_CONSENSUS_WEIGHT +
                hasOfficial * EngineConfig.ConfidenceWeights.OFFICIAL_SOURCE_PRESENCE_WEIGHT
        )

        // Kara za małą próbę: jeśli jest mniej niż 4 unikalnych zgłaszających i brak oficjalnego wpisu,
        // pewność danych z samej społeczności musi pozostać NISKA (LOW)
        if (uniqueReporters < 4 && !identity.isPublicDirectoryListed && identity.sapPartner == null) {
            rawConfidence *= (uniqueReporters / 4.0f)
        }

        // KRYTYCZNA REGUŁA ANTY-ABUSE:
        // Nagły skok zgłoszeń (anomalia) NIE MOŻE dawać wysokiej pewności!
        // Maksymalny poziom pewności przy anomalii to ŚREDNIA (cap na 0.55).
        if (isAnomalyDetected && rawConfidence > EngineConfig.ThresholdConfig.CONFIDENCE_MEDIUM_MAX) {
            rawConfidence = 0.55f
        }

        val internalScore = rawConfidence.coerceIn(0.0f, 1.0f)

        val level = when {
            internalScore > EngineConfig.ThresholdConfig.CONFIDENCE_MEDIUM_MAX -> ConfidenceLevel.WYSOKA
            internalScore >= EngineConfig.ThresholdConfig.CONFIDENCE_LOW_MAX -> ConfidenceLevel.SREDNIA
            else -> ConfidenceLevel.NISKA
        }

        val summary = when (level) {
            ConfidenceLevel.WYSOKA -> "Wysoka pewność: spójne dane z wielu niezależnych źródeł."
            ConfidenceLevel.SREDNIA -> "Umiarkowana pewność: dane cząstkowe wymagające ostrożności."
            ConfidenceLevel.NISKA -> "Niska pewność: pojedyncze lub niejednoznaczne sygnały."
        }

        return ConfidenceAssessment(
            level = level,
            internalScore = internalScore,
            independentSourcesCount = distinctSources,
            uniqueReportersCount = uniqueReporters,
            consensusAgreementRate = consensusRate,
            summary = summary
        )
    }
}
