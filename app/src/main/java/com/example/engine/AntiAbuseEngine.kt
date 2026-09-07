package com.example.engine

/**
 * Silnik ochrony przed nadużyciami (AntiAbuseEngine).
 * Chroni system przed review-bombingiem, botami, farmami kont i wielokrotnym głosowaniem.
 */
object AntiAbuseEngine {

    data class AnomalyCheckResult(
        val isAnomalyDetected: Boolean,
        val anomalyReason: String? = null,
        val surgeFactor: Float = 1.0f
    )

    /**
     * Sprawdza czy zgłoszenie mieści się w limitach częstotliwości (Rate Limiting).
     */
    fun isRateLimitExceeded(
        recentSubmissionsFromDeviceInHour: Int,
        recentSubmissionsFromDeviceInDay: Int
    ): Boolean {
        if (recentSubmissionsFromDeviceInHour >= EngineConfig.ThresholdConfig.MAX_REPORTS_PER_HOUR_PER_DEVICE) {
            return true
        }
        if (recentSubmissionsFromDeviceInDay >= EngineConfig.ThresholdConfig.MAX_REPORTS_PER_DAY_PER_DEVICE) {
            return true
        }
        return false
    }

    /**
     * Deduplikacja: Zwraca przefiltrowaną listę sygnałów, gdzie każdy użytkownik (sourceId)
     * ma dokładnie JEDEN aktywny głos dla danego numeru (najświeższy nadpisuje poprzednie).
     */
    fun deduplicateUserSignals(signals: List<CommunicationSignal>): List<CommunicationSignal> {
        val userLatestSignal = mutableMapOf<String, CommunicationSignal>()
        val nonUserSignals = mutableListOf<CommunicationSignal>()

        for (signal in signals) {
            if (signal.provenance.sourceType == SourceType.COMMUNITY) {
                val existing = userLatestSignal[signal.provenance.sourceId]
                if (existing == null || signal.provenance.timestampEpoch > existing.provenance.timestampEpoch) {
                    userLatestSignal[signal.provenance.sourceId] = signal
                }
            } else {
                nonUserSignals.add(signal)
            }
        }

        return nonUserSignals + userLatestSignal.values
    }

    /**
     * Sprawdza czy wystąpił nienaturalny skok aktywności (Anti-Review-Bombing).
     * Nagły wzrost zgłoszeń NIE MOŻE automatycznie oznaczać wysokiej pewności (Confidence).
     */
    fun checkReportingAnomaly(
        signals: List<CommunicationSignal>,
        currentEpoch: Long = System.currentTimeMillis()
    ): AnomalyCheckResult {
        if (signals.size < EngineConfig.ThresholdConfig.ANOMALY_MIN_REPORTS_FOR_SPIKE) {
            return AnomalyCheckResult(isAnomalyDetected = false)
        }

        val oneHourAgo = currentEpoch - (60L * 60L * 1000L)
        val twentyFourHoursAgo = currentEpoch - (24L * 60L * 60L * 1000L)

        val reportsLastHour = signals.count { it.provenance.timestampEpoch >= oneHourAgo }
        val reportsLast24h = signals.count { it.provenance.timestampEpoch >= twentyFourHoursAgo }
        val historicalReports = signals.count { it.provenance.timestampEpoch < twentyFourHoursAgo }

        // Sprawdzenie 1: Nagły skok w ciągu 1 godziny stanowiący większość dobowej aktywności
        if (reportsLastHour >= 6 && reportsLastHour > (reportsLast24h - reportsLastHour) * 2f) {
            val surge = reportsLastHour.toFloat() / ((reportsLast24h - reportsLastHour).coerceAtLeast(1))
            return AnomalyCheckResult(
                isAnomalyDetected = true,
                anomalyReason = "Wykryto nagły skok zgłoszeń w ciągu ostatniej godziny ($reportsLastHour zgłoszeń).",
                surgeFactor = surge
            )
        }

        // Sprawdzenie 2: 80%+ wszystkich raportów w historii numeru pojawiło się w ostatnich 24h przy braku wcześniejszej aktywności
        if (reportsLast24h >= EngineConfig.ThresholdConfig.ANOMALY_MIN_REPORTS_FOR_SPIKE &&
            historicalReports <= 1
        ) {
            return AnomalyCheckResult(
                isAnomalyDetected = true,
                anomalyReason = "Nietypowa fala zgłoszeń bez wcześniejszej historii aktywności numeru.",
                surgeFactor = 2.5f
            )
        }

        // Sprawdzenie 3: Niska dywersyfikacja źródeł (np. 15 raportów pochodzi tylko od 2 unikalnych ID)
        val uniqueReporters = signals.map { it.provenance.sourceId }.distinct().size
        if (signals.size >= 10 && uniqueReporters < signals.size * 0.4f) {
            return AnomalyCheckResult(
                isAnomalyDetected = true,
                anomalyReason = "Zgłoszenia pochodzą ze zbyt małej liczby niezależnych urządzeń.",
                surgeFactor = 1.8f
            )
        }

        return AnomalyCheckResult(isAnomalyDetected = false)
    }
}
