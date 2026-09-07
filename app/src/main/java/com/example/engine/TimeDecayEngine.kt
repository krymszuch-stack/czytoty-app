package com.example.engine

import kotlin.math.exp
import kotlin.math.max

/**
 * Silnik wygaszania danych w czasie (Time Decay Engine).
 * Starsze zgłoszenia mają coraz mniejszą wagę w bieżącej ocenie ryzyka,
 * zapobiegając karaniu numerów, które zmieniły właściciela lub charakter użytkowania.
 */
object TimeDecayEngine {

    /**
     * Ciągłe wyliczanie wagi z wykorzystaniem funkcji wykładniczej połowicznego rozpadu (Half-Life).
     * Waga = e^(-ln(2) * (t / halfLifeDays))
     */
    fun calculateExponentialDecay(
        timestampEpoch: Long,
        currentEpoch: Long = System.currentTimeMillis(),
        halfLifeDays: Double = EngineConfig.DecayConfig.HALF_LIFE_DAYS
    ): Float {
        val diffMs = max(0L, currentEpoch - timestampEpoch)
        val days = diffMs / (1000.0 * 60.0 * 60.0 * 24.0)

        val lambda = 0.69314718056 / halfLifeDays // ln(2) / T_1/2
        val decay = exp(-lambda * days).toFloat()

        return decay.coerceIn(EngineConfig.DecayConfig.HISTORICAL_FLOOR_WEIGHT, 1.0f)
    }

    /**
     * Dyskretne wyliczanie progowe (Tiered Decay) dla zachowania intuicyjności reguł biznesowych.
     */
    fun calculateTieredDecay(
        timestampEpoch: Long,
        currentEpoch: Long = System.currentTimeMillis()
    ): Float {
        val diffMs = max(0L, currentEpoch - timestampEpoch)
        val days = (diffMs / (1000L * 60L * 60L * 24L)).toInt()

        return when {
            days <= EngineConfig.DecayConfig.FRESH_DAYS_LIMIT -> 1.0f
            days <= EngineConfig.DecayConfig.RECENT_DAYS_LIMIT -> 0.75f
            days <= EngineConfig.DecayConfig.MEDIUM_DAYS_LIMIT -> 0.45f
            days <= EngineConfig.DecayConfig.OLD_DAYS_LIMIT -> 0.20f
            else -> EngineConfig.DecayConfig.HISTORICAL_FLOOR_WEIGHT
        }
    }
}
