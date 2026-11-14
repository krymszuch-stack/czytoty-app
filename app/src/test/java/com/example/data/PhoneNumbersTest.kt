package com.example.data

import org.junit.Assert.*
import org.junit.Test

class PhoneNumbersTest {
    @Test fun normalizesEquivalentFormats() {
        assertEquals("+48225218000", PhoneNumbers.parse("22 521 80 00")?.e164)
        assertEquals(PhoneNumbers.parse("0048 22 521 80 00"), PhoneNumbers.parse("+48 (22) 521-80-00"))
    }
    @Test fun rejectsEmergencyShortCodesAndArbitraryText() {
        listOf("112", "999", "8080", "", "bank", "000000000", "abc +48 22 521 80 00").forEach { assertNull(it, PhoneNumbers.parse(it)) }
    }
    @Test fun internationalNumbersRetainCountry() {
        val phone = PhoneNumbers.parse("+1 202-555-0123")
        assertEquals("+12025550123", phone?.e164)
        assertEquals("US", phone?.country)
    }
}
