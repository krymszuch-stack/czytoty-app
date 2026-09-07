package com.example.incall.privacy

import java.util.regex.Pattern

/**
 * Rezultat przetworzenia podglądu ostatniej wiadomości SMS (Sekcje 84 i 85).
 */
data class FilteredSmsPreview(
  val displayText: String,
  val isSensitive: Boolean,
  val timestamp: String?
)

/**
 * Filtr prywatności dla podglądu SMS w trakcie połączenia telefonicznego (Sekcja 85).
 *
 * Gwarantuje Privacy-by-Design:
 * Nigdy nie wyświetla w podglądzie kodów OTP, haseł, kodów BLIK czy poufnych danych bankowych.
 */
object InCallSmsPrivacyFilter {

  private val OTP_CODE_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b|\\b(G-\\d{6})\\b", Pattern.CASE_INSENSITIVE)
  private val BLIK_PATTERN = Pattern.compile("\\b(blik|kod\\s+blik)\\b|\\b(\\d{3}\\s*\\d{3})\\b", Pattern.CASE_INSENSITIVE)

  private val OTP_KEYWORDS = listOf(
    "kod", "otp", "weryfikac", "hasło jednorazowe", "token", "sms-kod", "aktywac"
  )

  private val BANKING_KEYWORDS = listOf(
    "przelew", "rachunek", "saldo", "karta płatnicza", "karta debetowa", "transakcj", "wyciąg"
  )

  private val CREDENTIAL_KEYWORDS = listOf(
    "hasło", "pin", "cvv", "cvc", "dane do logowania"
  )

  /**
   * Filtruje treść wiadomości, maskując elementy wrażliwe.
   *
   * @param rawBody Surowa treść ostatniego SMS-a
   * @param timestamp Czas otrzymania wiadomości (np. "dzisiaj, 08:42" lub "2 godz. temu")
   * @param isEnabled Ustawienie użytkownika: czy podgląd wiadomości jest włączony
   */
  fun filterSnippet(
    rawBody: String?,
    timestamp: String?,
    isEnabled: Boolean = true
  ): FilteredSmsPreview? {
    if (!isEnabled || rawBody.isNullOrBlank()) return null

    val lower = rawBody.lowercase()
    val containsOtpDigits = OTP_CODE_PATTERN.matcher(rawBody).find()

    return when {
      lower.contains("blik") -> {
        FilteredSmsPreview(
          displayText = "Ostatnia wiadomość: kod BLIK (ukryto dla bezpieczeństwa)",
          isSensitive = true,
          timestamp = timestamp
        )
      }
      CREDENTIAL_KEYWORDS.any { lower.contains(it) } -> {
        FilteredSmsPreview(
          displayText = "Ostatnia wiadomość: dane uwierzytelniające",
          isSensitive = true,
          timestamp = timestamp
        )
      }
      containsOtpDigits && OTP_KEYWORDS.any { lower.contains(it) } -> {
        FilteredSmsPreview(
          displayText = "Ostatnia wiadomość: kod jednorazowy",
          isSensitive = true,
          timestamp = timestamp
        )
      }
      BANKING_KEYWORDS.any { lower.contains(it) } -> {
        FilteredSmsPreview(
          displayText = "Ostatnia wiadomość: powiadomienie bankowe",
          isSensitive = true,
          timestamp = timestamp
        )
      }
      else -> {
        // Bezpieczna wiadomość (np. kurier lub rodzina) – obcinamy do 75 znaków
        val trimmed = if (rawBody.length > 75) rawBody.take(72) + "..." else rawBody
        FilteredSmsPreview(
          displayText = "Ostatnia wiadomość: „$trimmed”",
          isSensitive = false,
          timestamp = timestamp
        )
      }
    }
  }
}
