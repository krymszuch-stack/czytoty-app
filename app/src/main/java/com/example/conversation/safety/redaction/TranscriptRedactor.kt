package com.example.conversation.safety.redaction

import java.util.regex.Pattern

/**
 * Silnik redagowania i anonimizacji danych wrażliwych w transkrypcji (Privacy-First Redaction).
 * Maskuje wszelkie kody autoryzacyjne, dane kart, numery identyfikacyjne, rachunki oraz poświadczenia
 * zastępując je bezpiecznym znacznikiem [REDACTED].
 */
object TranscriptRedactor {

    private const val REDACTED_TAG = "[REDACTED]"

    // Karty płatnicze: 16 cyfr (z opcjonalnymi spacjami lub myślnikami co 4 cyfry)
    private val CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b")

    // Rachunki bankowe IBAN (polski format 26 cyfr z opcjonalnym PL)
    private val IBAN_PATTERN = Pattern.compile("\\b(?:PL\\s*)?\\d{2}(?:\\s*\\d{4}){6}\\b|\\b\\d{26}\\b", Pattern.CASE_INSENSITIVE)

    // PESEL: dokładnie 11 cyfr
    private val PESEL_PATTERN = Pattern.compile("\\b\\d{11}\\b")

    // Kody BLIK: dokładnie 6 cyfr (ciągłe lub ze spacją po 3 cyfrach)
    private val BLIK_PATTERN = Pattern.compile("\\b\\d{3}\\s?\\d{3}\\b")

    // Kody jednorazowe OTP / SMS (4 do 8 cyfr występujące po frazach autoryzacyjnych)
    private val OTP_PATTERN = Pattern.compile("(?i)\\b(kod(?:em|u)?(?:\\s+sms)?|otp|hasł[oa]\\s+jednorazow[eey])\\s*:?\\s*(\\d{4,8})\\b")

    // Hasła / PIN (fraza "hasło: xyz", "pin: 1234")
    private val PASSWORD_PATTERN = Pattern.compile("(?i)\\b(hasło|hasła|hasłem|hasle|kod\\s*pin|pin)\\s*:?\\s*([^\\s,.;!?]+)")

    // Adresy e-mail
    private val EMAIL_PATTERN = Pattern.compile("\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b")

    /**
     * Redaguje surowy tekst transkrypcji, usuwając dane wrażliwe.
     * Nigdy nie modyfikuje oryginalnej intencji wypowiedzi, a jedynie wartości poufne.
     */
    fun redact(rawText: String?): String {
        if (rawText.isNullOrBlank()) return ""

        var result = rawText

        // 1. Redaguj rachunki IBAN
        result = IBAN_PATTERN.matcher(result).replaceAll(REDACTED_TAG)

        // 2. Redaguj karty płatnicze
        result = CARD_PATTERN.matcher(result).replaceAll(REDACTED_TAG)

        // 3. Redaguj PESEL
        result = PESEL_PATTERN.matcher(result).replaceAll(REDACTED_TAG)

        // 4. Redaguj kody autoryzacyjne OTP po słowach kluczowych
        val otpMatcher = OTP_PATTERN.matcher(result)
        val sbOtp = StringBuffer()
        while (otpMatcher.find()) {
            val prefix = otpMatcher.group(1)
            otpMatcher.appendReplacement(sbOtp, "$prefix $REDACTED_TAG")
        }
        otpMatcher.appendTail(sbOtp)
        result = sbOtp.toString()

        // 5. Redaguj hasła i kody PIN
        val passMatcher = PASSWORD_PATTERN.matcher(result)
        val sbPass = StringBuffer()
        while (passMatcher.find()) {
            val prefix = passMatcher.group(1)
            passMatcher.appendReplacement(sbPass, "$prefix $REDACTED_TAG")
        }
        passMatcher.appendTail(sbPass)
        result = sbPass.toString()

        // 6. Redaguj 6-cyfrowe kody BLIK (zostają tylko zamaskowane cyfry)
        result = BLIK_PATTERN.matcher(result).replaceAll(REDACTED_TAG)

        // 7. Redaguj adresy e-mail
        result = EMAIL_PATTERN.matcher(result).replaceAll(REDACTED_TAG)

        return result
    }
}
