package com.example.model

/**
 * 3 główne, natychmiastowe odpowiedzi w karcie po połączeniu (SEKCJA 19).
 * Proste, duże, jednoznaczne przyciski dostępne dla seniorów.
 */
enum class QuickRating(val displayName: String, val emoji: String) {
  POSITIVE("W porządku", "👍"),
  NUISANCE("Natrętny", "😒"),
  SCAM("Podejrzenie oszustwa", "⚠️")
}

/**
 * Opcjonalne uszczegółowienie kategorii po wyborze głównej oceny (SEKCJA 21, 25).
 * Drugi krok jest ZAWSZE opcjonalny i nie blokuje zapisu oceny.
 */
enum class SubCategory(
  val displayName: String,
  val parentRating: QuickRating
) {
  // Podkategorie dla 👍 W porządku
  COMPANY("Firma", QuickRating.POSITIVE),
  COURIER("Kurier / Dostawa", QuickRating.POSITIVE),
  CLINIC("Przychodnia / Zdrowie", QuickRating.POSITIVE),
  PUBLIC_OFFICE("Urząd", QuickRating.POSITIVE),
  BANK_FINANCE("Bank / Finanse", QuickRating.POSITIVE),
  SERVICE_PROVIDER("Usługodawca", QuickRating.POSITIVE),
  OTHER_SAFE("Inne / Znajomy", QuickRating.POSITIVE),

  // Podkategorie dla 😒 Natrętny
  SALES_OFFER("Oferta handlowa / Telemarketing", QuickRating.NUISANCE),
  ROBOCALL_BOT("Automat / Bot telefoniczny", QuickRating.NUISANCE),
  SURVEY("Ankieta telefoniczna", QuickRating.NUISANCE),
  OTHER_NUISANCE("Inny natrętny kontakt", QuickRating.NUISANCE),

  // Podkategorie dla ⚠️ Podejrzenie oszustwa
  BANK_IMPERSONATION("Podszywanie się pod bank", QuickRating.SCAM),
  BLIK_PHISHING("Wyłudzenie kodu BLIK / hasła", QuickRating.SCAM),
  CRYPTO_INVESTMENT("Fałszywe inwestycje / Krypto", QuickRating.SCAM),
  REMOTE_DESKTOP("Zdalny pulpit (AnyDesk/TeamViewer)", QuickRating.SCAM),
  OTHER_SCAM("Inna próba wyłudzenia", QuickRating.SCAM);

  companion object {
    fun getForParent(rating: QuickRating): List<SubCategory> {
      return values().filter { it.parentRating == rating }
    }
  }
}

/**
 * Rejestrowana realna interakcja z numerem (SEKCJA 20).
 * Pozwala powiązać ocenę z rzeczywistym zdarzeniem bez inwazyjnego READ_CALL_LOG.
 */
data class CallInteraction(
  val id: String,
  val number: String,
  val timestampEpoch: Long = System.currentTimeMillis(),
  val direction: CallDirection = CallDirection.INCOMING,
  val answered: Boolean = true,
  val durationSeconds: Int = 0,
  val source: InteractionSource = InteractionSource.CALL_SCREENING
)

enum class CallDirection {
  INCOMING,
  OUTGOING,
  MISSED,
  BLOCKED
}

enum class InteractionSource {
  CALL_SCREENING,
  INCOMING_CALL_RECEIVER,
  USER_DIALED,
  DEMO_SIMULATION
}

/**
 * Konfiguracja Time Decay (SEKCJA 22).
 * Świeże raporty mają większą wagę niż stare, progi są w pełni konfigurowalne.
 */
data class TimeDecayConfig(
  val freshDaysLimit: Int = 30,
  val freshWeight: Float = 1.0f,
  val mediumDaysLimit: Int = 90,
  val mediumWeight: Float = 0.6f,
  val oldDaysLimit: Int = 180,
  val oldWeight: Float = 0.3f,
  val historicalWeight: Float = 0.1f
)

/**
 * Pojedyncza ocena użytkownika z metadanymi wiarygodności i anty-review-bombingu (SEKCJA 26, 27).
 */
data class UserRatingSubmission(
  val id: String,
  val phoneNumber: String,
  val rating: QuickRating,
  val subCategory: SubCategory? = null,
  val timestampEpoch: Long = System.currentTimeMillis(),
  val hasRealInteraction: Boolean = true,
  val userTrustWeight: Float = 1.0f,
  val isUpdatedRating: Boolean = false
)

/**
 * Dynamiczny model reputacji społecznościowej (SEKCJA 24, 28, 29).
 * Oddziela reputację społeczną od Risk Score oraz tożsamość (Identity) od zachowania.
 */
data class CommunityReputation(
  val phoneNumber: String,
  val positiveScorePercent: Int, // np. 85%
  val negativeScorePercent: Int, // np. 15%
  val totalRatingsCount: Int,
  val ratingsLast7Days: Int,
  val ratingsLast30Days: Int,
  val freshnessSummary: String, // np. "37 ocen w ostatnich 7 dniach"
  val dominantRating: QuickRating,
  val dominantSubCategory: SubCategory?,
  val positiveCount: Int,
  val nuisanceCount: Int,
  val scamCount: Int,
  val isAnomalyDetected: Boolean = false, // Wykrycie nietypowej fali / anty-bombing
  val reputationLabel: String, // Język UI: "Dobrze oceniany przez społeczność", "Użytkownicy zgłaszają próby oszustwa"
  val riskContextNote: String // Transparentne wyjaśnienie
) {
  val positivePercent: Int
    get() = if (totalRatingsCount > 0) (positiveCount * 100 / totalRatingsCount) else 0

  val nuisancePercent: Int
    get() = if (totalRatingsCount > 0) (nuisanceCount * 100 / totalRatingsCount) else 0

  val scamPercent: Int
    get() = if (totalRatingsCount > 0) (scamCount * 100 / totalRatingsCount) else 0
}

/**
 * Stan karty po zakończeniu połączenia
 */
data class PostCallEvaluation(
  val callerInfo: CallerInfo,
  val interaction: CallInteraction,
  val expiresAtEpoch: Long = System.currentTimeMillis() + 15_000 // 15 sekund auto-dismiss
)
