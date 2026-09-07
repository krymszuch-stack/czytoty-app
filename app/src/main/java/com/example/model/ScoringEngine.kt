package com.example.model

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Transparentny, deterministyczny silnik oceny ryzyka i pewności danych (MVP).
 * Wszystkie wagi i progi są zebrane w jednym miejscu bez udawania sztucznej inteligencji (AI).
 */
object ScoringConfig {
  // Wagi dla składowych oceny ryzyka (0.0 .. 1.0)
  const val WEIGHT_CATEGORY_SEVERITY = 0.45f
  const val WEIGHT_REPORT_VOLUME = 0.20f
  const val WEIGHT_RECENCY_24H = 0.20f
  const val WEIGHT_VELOCITY_1H = 0.15f

  // Progi wolumenu zgłoszeń
  const val MAX_REPORTS_CEILING = 50f
  const val MAX_REPORTS_24H_CEILING = 20f
  const val MAX_REPORTS_1H_CEILING = 10f

  // Mnożnik podwyższonego ryzyka dla podszywania się pod instytucję finansową na infolinii przychodzącej
  const val INBOUND_HOTLINE_SPOOF_RISK_BOOST = 35

  // Progi dla wyznaczania poziomu pewności (Confidence)
  const val CONFIDENCE_HIGH_MIN_REPORTS = 15
  const val CONFIDENCE_MEDIUM_MIN_REPORTS = 5
}

data class ScoreResult(
  val riskScore: Int,
  val confidence: ConfidenceLevel,
  val explanation: String
)

object ScoringEngine {

  /**
   * Deterministyczne wyliczanie Risk Score i Confidence na podstawie obiektywnych parametrów.
   */
  fun calculateScore(
    category: CallCategory,
    totalReports: Int,
    reportsLast24h: Int,
    reportsLastHour: Int,
    numberIdentity: NumberIdentity?,
    reporterTrustMultiplier: Float = 1.0f
  ): ScoreResult {
    // 1. Składowa kategorii
    val categoryScore = category.baseRiskWeight.toFloat()

    // 2. Składowa wolumenu raportów (normalizowana do 0..100)
    val volumeScore = min(100f, (totalReports / ScoringConfig.MAX_REPORTS_CEILING) * 100f)

    // 3. Składowa świeżości (ostatnie 24h)
    val recencyScore = min(100f, (reportsLast24h / ScoringConfig.MAX_REPORTS_24H_CEILING) * 100f)

    // 4. Składowa prędkości (ostatnia godzina - wskaźnik nagłej kampanii)
    val velocityScore = min(100f, (reportsLastHour / ScoringConfig.MAX_REPORTS_1H_CEILING) * 100f)

    // Suma ważona
    var rawRisk = (
        categoryScore * ScoringConfig.WEIGHT_CATEGORY_SEVERITY +
        volumeScore * ScoringConfig.WEIGHT_REPORT_VOLUME +
        recencyScore * ScoringConfig.WEIGHT_RECENCY_24H +
        velocityScore * ScoringConfig.WEIGHT_VELOCITY_1H
    ) * reporterTrustMultiplier

    // Specjalna reguła: jeśli numer to oficjalna infolinia przychodząca banku,
    // ale otrzymano połączenie przychodzące i są zgłoszenia oszustw -> bardzo wysokie ryzyko spoofingu!
    if (numberIdentity?.isFinancialInstitution == true && numberIdentity.isInboundOnlyHotline && totalReports > 0) {
      rawRisk += ScoringConfig.INBOUND_HOTLINE_SPOOF_RISK_BOOST
    }

    val finalRiskScore = min(100, rawRisk.roundToInt().coerceAtLeast(0))

    // Wyliczanie Confidence (pewność danych)
    val confidence = when {
      numberIdentity?.isPublicDirectoryListed == true && totalReports == 0 -> ConfidenceLevel.WYSOKA
      totalReports >= ScoringConfig.CONFIDENCE_HIGH_MIN_REPORTS -> ConfidenceLevel.WYSOKA
      totalReports >= ScoringConfig.CONFIDENCE_MEDIUM_MIN_REPORTS -> ConfidenceLevel.SREDNIA
      else -> ConfidenceLevel.NISKA
    }

    // Transparentne uzasadnienie ("Dlaczego?")
    val explanation = generateExplanation(
      category = category,
      totalReports = totalReports,
      reportsLast24h = reportsLast24h,
      reportsLastHour = reportsLastHour,
      numberIdentity = numberIdentity,
      confidence = confidence
    )

    return ScoreResult(
      riskScore = finalRiskScore,
      confidence = confidence,
      explanation = explanation
    )
  }

  private fun generateExplanation(
    category: CallCategory,
    totalReports: Int,
    reportsLast24h: Int,
    reportsLastHour: Int,
    numberIdentity: NumberIdentity?,
    confidence: ConfidenceLevel
  ): String {
    if (numberIdentity?.isFinancialInstitution == true) {
      return if (numberIdentity.isInboundOnlyHotline && reportsLast24h > 0) {
        "Numer infolinii bankowej. Infolinie banków służą do dzwonienia przez klienta; połączenie przychodzące może być sfałszowane (spoofing CLI)."
      } else {
        "Numer widnieje w rejestrze jako ${numberIdentity.registeredEntityName}. Uwaga: identyfikator dzwoniącego może zostać sfałszowany."
      }
    }

    if (totalReports == 0) {
      return "Brak zgłoszeń od użytkowników w lokalnej bazie. Brak znanych sygnałów ryzyka nie oznacza pewności."
    }

    if (reportsLastHour >= 5) {
      return "$reportsLastHour nowych zgłoszeń w ciągu ostatniej godziny. Wykryto gwałtowną aktywność masową."
    }

    if (reportsLast24h > 0) {
      return "$reportsLast24h podobnych zgłoszeń w ciągu ostatnich 24 godzin (łącznie: $totalReports)."
    }

    return when (confidence) {
      ConfidenceLevel.NISKA -> "Nieliczne zgłoszenia wskazują na kategorię: ${category.displayName}. Zachowaj ostrożność."
      ConfidenceLevel.SREDNIA -> "Umiarkowana liczba zgłoszeń ($totalReports) o charakterze: ${category.displayName}."
      ConfidenceLevel.WYSOKA -> "Wysoka powtarzalność raportów ($totalReports) wskazujących na: ${category.displayName}."
    }
  }
}
