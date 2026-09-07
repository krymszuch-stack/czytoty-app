package com.example.engine

/**
 * Globalna konfiguracja silnika reputacji i oceny ryzyka CzyToTy?.
 *
 * Oznaczenie: INITIAL_CALIBRATION (Wersja v1.0.0).
 * Wszystkie wagi i progi są zebrane w jednym miejscu, w pełni konfigurowalne
 * i przygotowane do przyszłej kalibracji na danych empirycznych.
 */
object EngineConfig {

    const val ENGINE_VERSION = "1.0.0"

    // ==========================================
    // 1. WAGI OCENY RYZYKA (RiskWeights)
    // ==========================================
    object RiskWeights {
        const val CATEGORY_SEVERITY_WEIGHT = 0.40f
        const val REPORT_VOLUME_WEIGHT = 0.15f
        const val RECENCY_24H_WEIGHT = 0.15f
        const val VELOCITY_1H_WEIGHT = 0.10f
        const val SMS_THREAT_WEIGHT = 0.20f

        // Boosty ryzyka w sytuacjach szczególnych
        const val INBOUND_HOTLINE_SPOOF_BOOST = 35
        const val RECENT_BLIK_OR_CREDENTIALS_BOOST = 40
        const val BRAND_DOMAIN_MISMATCH_BOOST = 30
        const val ANOMALY_UNCERTAINTY_PENALTY = 15 // kara/modyfikator niepewności przy anomalii
    }

    // ==========================================
    // 2. WAGI PEWNOŚCI DANYCH (ConfidenceWeights)
    // ==========================================
    object ConfidenceWeights {
        const val INDEPENDENT_SOURCES_WEIGHT = 0.25f
        const val REPORTER_COUNT_WEIGHT = 0.20f
        const val REPORTER_TRUST_WEIGHT = 0.20f
        const val DATA_FRESHNESS_WEIGHT = 0.15f
        const val INTER_SOURCE_CONSENSUS_WEIGHT = 0.10f
        const val OFFICIAL_SOURCE_PRESENCE_WEIGHT = 0.10f

        // Maksymalne sufity do normalizacji pewności
        const val MAX_INDEPENDENT_SOURCES_CEILING = 5f
        const val MAX_REPORTERS_CEILING = 25f
    }

    // ==========================================
    // 3. MODEL WYGASZANIA W CZASIE (DecayConfig)
    // ==========================================
    object DecayConfig {
        const val HALF_LIFE_DAYS = 45.0 // Czas połowicznego zaniku wpływu raportu
        const val FRESH_DAYS_LIMIT = 7   // Waga 1.0f
        const val RECENT_DAYS_LIMIT = 30 // Waga 0.75f
        const val MEDIUM_DAYS_LIMIT = 90 // Waga 0.45f
        const val OLD_DAYS_LIMIT = 180   // Waga 0.20f
        const val HISTORICAL_FLOOR_WEIGHT = 0.05f // Nigdy nie spada do zera dla oszustw
    }

    // ==========================================
    // 4. WAGI KATEGORII REPUTACJI
    // ==========================================
    object ReputationWeights {
        const val OK_WEIGHT = 0.0f
        const val ANNOYING_WEIGHT = 0.25f
        const val TELEMARKETING_WEIGHT = 0.40f
        const val BOT_WEIGHT = 0.50f
        const val SUSPECTED_SCAM_WEIGHT = 0.85f
        const val IMPERSONATION_WEIGHT = 0.95f
        const val OTHER_WEIGHT = 0.20f
    }

    // ==========================================
    // 5. PROGI DECYZYJNE (ThresholdConfig)
    // ==========================================
    object ThresholdConfig {
        // Progi poziomu ryzyka (Risk Level)
        const val RISK_LOW_MAX = 24
        const val RISK_MODERATE_MAX = 49
        const val RISK_ELEVATED_MAX = 74
        // >= 75 to HIGH RISK

        // Progi pewności danych (Confidence Level)
        const val CONFIDENCE_LOW_MAX = 0.35f
        const val CONFIDENCE_MEDIUM_MAX = 0.70f
        // > 0.70f to HIGH CONFIDENCE

        // Limity anty-abuse dla jednego urządzenia
        const val MAX_REPORTS_PER_HOUR_PER_DEVICE = 5
        const val MAX_REPORTS_PER_DAY_PER_DEVICE = 15
        const val ANOMALY_SURGE_HOURLY_FACTOR = 3.0f // 3x więcej zgłoszeń niż średnia dobowa
        const val ANOMALY_MIN_REPORTS_FOR_SPIKE = 8
    }
}
