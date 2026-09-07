package com.example.data.acquisition

import com.example.data.acquisition.privacy.PrivacyProtectionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy jednostkowe dla PrivacyProtectionService (E.164, HMAC-SHA256 i cenzurowanie logów).
 */
class PrivacyProtectionServiceTest {

    @Test
    fun `normalizeToE164 properly formats standard Polish mobile numbers`() {
        assertEquals("+48732123456", PrivacyProtectionService.normalizeToE164("732 123 456"))
        assertEquals("+48732123456", PrivacyProtectionService.normalizeToE164("+48 732-123-456"))
        assertEquals("+48732123456", PrivacyProtectionService.normalizeToE164("0048 732 123 456"))
        assertEquals("+48732123456", PrivacyProtectionService.normalizeToE164(" (732) 123-456 "))
    }

    @Test
    fun `normalizeToE164 properly formats Polish landline numbers`() {
        assertEquals("+48221234567", PrivacyProtectionService.normalizeToE164("22 123 45 67"))
        assertEquals("+48221234567", PrivacyProtectionService.normalizeToE164("+48 (22) 123-45-67"))
        assertEquals("+48123456789", PrivacyProtectionService.normalizeToE164("12 345 67 89"))
    }

    @Test
    fun `normalizeToE164 rejects invalid numbers`() {
        assertNull(PrivacyProtectionService.normalizeToE164(""))
        assertNull(PrivacyProtectionService.normalizeToE164("   "))
        assertNull(PrivacyProtectionService.normalizeToE164("12345"))          // Za krótki
        assertNull(PrivacyProtectionService.normalizeToE164("abcdefghijk"))    // Litery
        assertNull(PrivacyProtectionService.normalizeToE164("+48 732 ABC 456"))
        assertNull(PrivacyProtectionService.normalizeToE164(null))
    }

    @Test
    fun `computePrivacyPseudonym is deterministic and key-dependent`() {
        val phone = "+48732123456"
        val key1 = "secret-key-1".toByteArray(Charsets.UTF_8)
        val key2 = "secret-key-2".toByteArray(Charsets.UTF_8)

        val token1a = PrivacyProtectionService.computePrivacyPseudonym(phone, key1)
        val token1b = PrivacyProtectionService.computePrivacyPseudonym(phone, key1)
        val token2 = PrivacyProtectionService.computePrivacyPseudonym(phone, key2)

        // Deterministyczność dla tego samego klucza
        assertEquals(token1a, token1b)
        assertEquals(64, token1a.length) // 256-bit hex = 64 znaki

        // Różne tokeny dla różnych kluczy (ochrona przed atakiem słownikowym / rainbow tables)
        assertNotEquals(token1a, token2)
        assertFalse(token1a.contains("732123456"))
    }

    @Test
    fun `sanitizeForLogging redacts phone numbers from log output`() {
        val logLine = "Przetwarzanie zapytania dla numeru +48 732 123 456 od klienta."
        val sanitized = PrivacyProtectionService.sanitizeForLogging(logLine)

        assertFalse("Numer nie może pojawić się w logach!", sanitized.contains("123 456"))
        assertTrue("Musi zawierać znacznik cenzury", sanitized.contains("REDACTED"))
    }

    @Test
    fun `classifyNumber distinguishes verified business from telecom ranges and private numbers`() {
        val classBiz = PrivacyProtectionService.classifyNumber(isVerifiedBusiness = true, isTelecomRange = true)
        assertEquals(DataClassification.PUBLIC_BUSINESS, classBiz)

        val classTelco = PrivacyProtectionService.classifyNumber(isVerifiedBusiness = false, isTelecomRange = true)
        assertEquals(DataClassification.PUBLIC_TELECOM, classTelco)

        val classPrivate = PrivacyProtectionService.classifyNumber(isVerifiedBusiness = false, isTelecomRange = false)
        assertEquals(DataClassification.PRIVATE_RESIDENTIAL, classPrivate)
    }
}
