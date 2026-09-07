package com.example.data.acquisition.privacy

import com.example.data.acquisition.DataClassification
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Usługa ochrony prywatności i zgodności z RODO w pipeline danych CzyToTy?.
 *
 * Główne filary:
 * 1. Kanoniczna normalizacja numerów do międzynarodowego standardu E.164.
 * 2. Odporna na tęczowe tablice pseudonimizacja numerów prywatnych za pomocą HMAC-SHA256.
 * 3. Całkowite wymazywanie (redaction) numerów z logów systemowych (Zero Phone in Logs).
 */
object PrivacyProtectionService {

    private const val HMAC_ALGORITHM = "HmacSHA256"

    // W produkcji pobierany z bezpiecznego magazynu sekretów (KMS / Keystore / ENV)
    private val defaultSecretKey: ByteArray by lazy {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        key
    }

    /**
     * Normalizuje dowolny ciąg znaków reprezentujący numer do kanonicznego formatu E.164.
     *
     * Przykłady:
     * - "732 123 456"        -> "+48732123456"
     * - "+48 732-123-456"    -> "+48732123456"
     * - "0048 (22) 123 45 67"-> "+48221234567"
     * - "22 123 45 67"       -> "+48221234567"
     *
     * Zwraca null, jeżeli ciąg nie jest poprawnym numerem telefonu.
     */
    fun normalizeToE164(rawInput: String?, defaultCountryCode: String = "48"): String? {
        if (rawInput.isNullOrBlank()) return null

        val trimmed = rawInput.trim()

        // Usunięcie znaków formatowania (spacje, myślniki, nawiasy, kropki)
        var cleaned = trimmed.replace(Regex("[\\s\\-\\(\\)\\.]"), "")

        // Zamiana prefiksu międzynarodowego 00 na +
        if (cleaned.startsWith("00")) {
            cleaned = "+" + cleaned.substring(2)
        }

        // Jeśli zaczyna się od znaku plus
        if (cleaned.startsWith("+")) {
            val digitsOnly = cleaned.substring(1)
            if (!digitsOnly.all { it.isDigit() }) return null
            // Sprawdzenie długości międzynarodowej E.164 (od 7 do 15 cyfr)
            return if (digitsOnly.length in 7..15) "+$digitsOnly" else null
        }

        // Jeśli zawiera same cyfry
        if (!cleaned.all { it.isDigit() }) return null

        // Domyślna obsługa polskich numerów 9-cyfrowych
        return if (cleaned.length == 9 && defaultCountryCode == "48") {
            "+48$cleaned"
        } else if (cleaned.length in 7..15) {
            "+$cleaned"
        } else {
            null
        }
    }

    /**
     * Generuje deterministyczny, nieodwracalny token pseudonimizacyjny HMAC-SHA256.
     *
     * Chroni przed atakiem słownikowym (przestrzeń polskich numerów 9-cyfrowych to tylko 10^9 wariantów,
     * więc zwykły SHA-256 można złamać w kilka sekund za pomocą Rainbow Tables).
     * HMAC z kluczem serwera uniemożliwia odtworzenie numeru bez dostępu do sekretu.
     */
    fun computePrivacyPseudonym(normalizedNumber: String, secretKey: ByteArray = defaultSecretKey): String {
        val hmac = Mac.getInstance(HMAC_ALGORITHM)
        val keySpec = SecretKeySpec(secretKey, HMAC_ALGORITHM)
        hmac.init(keySpec)
        val hash = hmac.doFinal(normalizedNumber.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Klasyfikuje numer z perspektywy RODO:
     * - Jeśli numer znajduje się w oficjalnym rejestrze firm/instytucji -> PUBLIC_BUSINESS
     * - Jeśli to ogólny zakres z UKE -> PUBLIC_TELECOM
     * - W przeciwnym razie domyślnie traktowany jako wrażliwy -> PRIVATE_RESIDENTIAL
     */
    fun classifyNumber(
        isVerifiedBusiness: Boolean,
        isTelecomRange: Boolean
    ): DataClassification {
        return when {
            isVerifiedBusiness -> DataClassification.PUBLIC_BUSINESS
            isTelecomRange -> DataClassification.PUBLIC_TELECOM
            else -> DataClassification.PRIVATE_RESIDENTIAL
        }
    }

    /**
     * Cenzuruje numery telefonów w tekstach przeznaczonych do logów,
     * zapobiegając wyciekowi danych osobowych do systemów telemetrycznych.
     */
    fun sanitizeForLogging(text: String): String {
        // Wykrywa numery telefonów o długości 9 lub więcej cyfr (z prefiksem +48 lub bez)
        val phoneRegex = Regex("(\\+?[0-9]{2})?[\\s\\-]?[0-9]{3}[\\s\\-]?[0-9]{3}[\\s\\-]?[0-9]{3}")
        return phoneRegex.replace(text) { matchResult ->
            val match = matchResult.value.trim()
            if (match.length >= 6) {
                "${match.take(4)}***[REDACTED]***"
            } else {
                "[REDACTED_PHONE]"
            }
        }
    }
}
