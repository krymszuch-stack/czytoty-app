package com.example.engine

import com.example.model.CallCategory
import com.example.model.CommunityReputation
import com.example.model.ConfidenceLevel
import com.example.model.NumberIdentity
import com.example.model.QuickRating
import com.example.model.ScoreResult
import com.example.model.SubCategory
import com.example.model.TimeDecayConfig
import com.example.model.UserProfile
import com.example.model.UserRatingSubmission
import kotlin.math.roundToInt

/**
 * Zaawansowany, deterministyczny silnik reputacji społecznościowej (SEKCJE 22–29).
 * Realizuje:
 * - Time Decay (wygasanie starszych ocen)
 * - User Trust Score (ważenie wiarygodności zgłaszającego)
 * - Anti-Review-Bombing (detekcja anomalii i ochrona przed falami botów)
 * - Rozdzielenie reputacji społecznej od Risk Score
 * - Rozdzielenie spamu/telemarketingu od oszustwa/scamu
 */
object ReputationEngine {

  val defaultConfig = TimeDecayConfig()

  /**
   * Wyliczanie wagi czasowej (Time Decay - SEKCJA 22).
   */
  fun calculateTimeDecay(
    submissionTimestampEpoch: Long,
    currentEpoch: Long = System.currentTimeMillis(),
    config: TimeDecayConfig = defaultConfig
  ): Float {
    val diffMs = (currentEpoch - submissionTimestampEpoch).coerceAtLeast(0)
    val ageDays = (diffMs / (1000L * 60L * 60L * 24L)).toInt()

    return when {
      ageDays <= config.freshDaysLimit -> config.freshWeight // 0-30 dni: 1.0f
      ageDays <= config.mediumDaysLimit -> config.mediumWeight // 31-90 dni: 0.6f
      ageDays <= config.oldDaysLimit -> config.oldWeight // 91-180 dni: 0.3f
      else -> config.historicalWeight // 180+ dni: 0.1f (dane historyczne)
    }
  }

  /**
   * Obliczanie Trust Score użytkownika / zgłoszenia (SEKCJA 23).
   */
  fun calculateUserTrustWeight(
    userProfile: UserProfile,
    hasRealInteraction: Boolean
  ): Float {
    var weight = (userProfile.credibilityScore / 100f).coerceIn(0.5f, 1.2f)

    // Bonus za potwierdzoną realną interakcję z numerem na urządzeniu
    if (hasRealInteraction) {
      weight += 0.2f
    }

    // Bonus za doświadczone konto ze zweryfikowanymi zgłoszeniami
    if (userProfile.confirmedReports >= 10) {
      weight += 0.15f
    }

    return weight.coerceIn(0.2f, 1.5f)
  }

  /**
   * Detekcja anomalii (Anti-Review-Bombing - SEKCJA 26).
   * Sprawdza, czy w krótkim czasie nastąpił nagły, nienaturalny skok zgłoszeń.
   */
  fun detectAnomaly(
    submissions: List<UserRatingSubmission>,
    currentEpoch: Long = System.currentTimeMillis()
  ): Boolean {
    if (submissions.size < 10) return false

    val last24hEpoch = currentEpoch - (24L * 60L * 60L * 1000L)
    val reportsLast24h = submissions.count { it.timestampEpoch >= last24hEpoch }
    val olderReports = submissions.count { it.timestampEpoch < last24hEpoch }

    // Jeśli ponad 70% wszystkich raportów pochodzi z ostatnich 24h przy łącznej liczbie >= 12
    if (reportsLast24h >= 10 && (olderReports == 0 || reportsLast24h > olderReports * 2.5f)) {
      return true
    }

    return false
  }

  /**
   * Agregacja reputacji społecznościowej na podstawie zbioru zgłoszeń (SEKCJA 24, 28, 29).
   */
  fun aggregateReputation(
    phoneNumber: String,
    submissions: List<UserRatingSubmission>,
    currentEpoch: Long = System.currentTimeMillis(),
    config: TimeDecayConfig = defaultConfig
  ): CommunityReputation {
    if (submissions.isEmpty()) {
      return CommunityReputation(
        phoneNumber = phoneNumber,
        positiveScorePercent = 50,
        negativeScorePercent = 50,
        totalRatingsCount = 0,
        ratingsLast7Days = 0,
        ratingsLast30Days = 0,
        freshnessSummary = "Brak ocen społeczności",
        dominantRating = QuickRating.POSITIVE,
        dominantSubCategory = null,
        positiveCount = 0,
        nuisanceCount = 0,
        scamCount = 0,
        isAnomalyDetected = false,
        reputationLabel = "Brak znanych sygnałów ryzyka",
        riskContextNote = "Numer nie był dotąd zgłaszany przez użytkowników."
      )
    }

    val last7DaysEpoch = currentEpoch - (7L * 24L * 60L * 60L * 1000L)
    val last30DaysEpoch = currentEpoch - (30L * 24L * 60L * 60L * 1000L)

    val ratingsLast7Days = submissions.count { it.timestampEpoch >= last7DaysEpoch }
    val ratingsLast30Days = submissions.count { it.timestampEpoch >= last30DaysEpoch }

    val isAnomaly = detectAnomaly(submissions, currentEpoch)

    var weightedPositive = 0f
    var weightedNegative = 0f

    var positiveCount = 0
    var nuisanceCount = 0
    var scamCount = 0

    val subCategoryCounts = mutableMapOf<SubCategory, Int>()

    for (sub in submissions) {
      val decay = calculateTimeDecay(sub.timestampEpoch, currentEpoch, config)
      val effectiveWeight = decay * sub.userTrustWeight

      when (sub.rating) {
        QuickRating.POSITIVE -> {
          weightedPositive += effectiveWeight
          positiveCount++
        }
        QuickRating.NUISANCE -> {
          weightedNegative += effectiveWeight
          nuisanceCount++
        }
        QuickRating.SCAM -> {
          // Oszustwo ma wyższą wagę w składowej negatywnej
          weightedNegative += (effectiveWeight * 1.3f)
          scamCount++
        }
      }

      sub.subCategory?.let { sc ->
        subCategoryCounts[sc] = (subCategoryCounts[sc] ?: 0) + 1
      }
    }

    val totalWeight = weightedPositive + weightedNegative
    val positivePercent = if (totalWeight > 0f) {
      ((weightedPositive / totalWeight) * 100f).roundToInt().coerceIn(0, 100)
    } else 50
    val negativePercent = 100 - positivePercent

    // Wyznaczenie dominującej oceny
    val dominantRating = when {
      scamCount >= nuisanceCount && scamCount >= positiveCount -> QuickRating.SCAM
      nuisanceCount >= scamCount && nuisanceCount >= positiveCount -> QuickRating.NUISANCE
      else -> QuickRating.POSITIVE
    }

    val dominantSubCategory = subCategoryCounts.maxByOrNull { it.value }?.key

    // Świeżość danych (SEKCJA 22)
    val freshnessSummary = when {
      ratingsLast7Days > 0 -> "$ratingsLast7Days ocen w ostatnich 7 dniach"
      ratingsLast30Days > 0 -> "$ratingsLast30Days ocen w tym miesiącu"
      else -> "Głównie oceny historyczne"
    }

    // Język UI (SEKCJA 28)
    val reputationLabel = when (dominantRating) {
      QuickRating.POSITIVE -> {
        if (positivePercent >= 80) "Dobrze oceniany przez społeczność"
        else "Pozytywna reputacja"
      }
      QuickRating.NUISANCE -> "Użytkownicy zgłaszają uciążliwy telemarketing"
      QuickRating.SCAM -> "Użytkownicy zgłaszają próby oszustwa z tego numeru"
    }

    val riskContextNote = when {
      isAnomaly -> "Wykryto nietypowy wzrost zgłoszeń w krótkim czasie. Wiarygodność danych jest weryfikowana."
      dominantRating == QuickRating.SCAM -> "Zgłoszenia wskazują na próby wyłudzenia danych lub podszywania się pod instytucję."
      dominantRating == QuickRating.NUISANCE -> "Numer klasyfikowany jako oferta handlowa lub ankieta, bez bezpośrednich oznak wyłudzenia."
      else -> "Przewaga pozytywnych opinii społeczności dotyczących kontaktu z tego numeru."
    }

    return CommunityReputation(
      phoneNumber = phoneNumber,
      positiveScorePercent = positivePercent,
      negativeScorePercent = negativePercent,
      totalRatingsCount = submissions.size,
      ratingsLast7Days = ratingsLast7Days,
      ratingsLast30Days = ratingsLast30Days,
      freshnessSummary = freshnessSummary,
      dominantRating = dominantRating,
      dominantSubCategory = dominantSubCategory,
      positiveCount = positiveCount,
      nuisanceCount = nuisanceCount,
      scamCount = scamCount,
      isAnomalyDetected = isAnomaly,
      reputationLabel = reputationLabel,
      riskContextNote = riskContextNote
    )
  }

  /**
   * Wyznaczenie łącznego wyniku ryzyka i pewności danych (SEKCJA 24, 25, 29)
   * Rozdziela:
   * - Tożsamość (Identity) od zachowania
   * - Telemarketing od oszustwa (scam)
   */
  fun calculateCombinedScore(
    reputation: CommunityReputation,
    numberIdentity: NumberIdentity?,
    isSpoofSuspectedOnInbound: Boolean = false
  ): ScoreResult {
    // 1. Podstawowe ryzyko z natury zgłoszeń (Spam != Scam)
    var baseRisk = when (reputation.dominantRating) {
      QuickRating.POSITIVE -> 5
      QuickRating.NUISANCE -> {
        // Telemarketing: umiarkowane ryzyko (35 - 48 punktów), NIE zamienia w scam!
        val nuisanceRatio = if (reputation.totalRatingsCount > 0) {
          reputation.nuisanceCount.toFloat() / reputation.totalRatingsCount
        } else 0.5f
        (30f + (nuisanceRatio * 18f)).roundToInt()
      }
      QuickRating.SCAM -> {
        // Podejrzenie oszustwa: wysokie ryzyko (75 - 95 punktów)
        val scamRatio = if (reputation.totalRatingsCount > 0) {
          reputation.scamCount.toFloat() / reputation.totalRatingsCount
        } else 0.8f
        (70f + (scamRatio * 25f)).roundToInt()
      }
    }

    // Jeśli połączenie to podszycie pod infolinię bankową (CLI spoofing)
    if (isSpoofSuspectedOnInbound && numberIdentity?.isFinancialInstitution == true && reputation.scamCount > 0) {
      baseRisk += 25
    }

    val finalRiskScore = baseRisk.coerceIn(0, 100)

    // Pewność danych (Confidence)
    val confidence = when {
      // Jeśli wykryto anomalię/review-bombing -> NIE zwiększaj ślepo pewności!
      reputation.isAnomalyDetected -> ConfidenceLevel.SREDNIA
      numberIdentity?.isPublicDirectoryListed == true && reputation.totalRatingsCount >= 5 -> ConfidenceLevel.WYSOKA
      reputation.totalRatingsCount >= 15 -> ConfidenceLevel.WYSOKA
      reputation.totalRatingsCount >= 4 -> ConfidenceLevel.SREDNIA
      else -> ConfidenceLevel.NISKA
    }

    val explanation = buildString {
      append(reputation.reputationLabel)
      append(". ")
      if (reputation.totalRatingsCount > 0) {
        append(reputation.freshnessSummary)
        append(" (łącznie ${reputation.totalRatingsCount} zgłoszeń). ")
      }
      append(reputation.riskContextNote)
    }

    return ScoreResult(
      riskScore = finalRiskScore,
      confidence = confidence,
      explanation = explanation
    )
  }
}
