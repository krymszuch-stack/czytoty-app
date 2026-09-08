package com.example.calling

import org.junit.Assert.*
import org.junit.Test

class DialTargetTest {
    @Test fun preservesEmergencyAndShortCodesForTelecom() {
        assertEquals("112", DialTarget.normalize("112"))
        assertEquals("911", DialTarget.normalize("911"))
        assertEquals("*123#", DialTarget.normalize("*123#"))
    }
    @Test fun supportsFormattedInternationalNumbers() {
        assertEquals("+12025550123", DialTarget.normalize("+1 (202) 555-0123"))
    }
    @Test fun rejectsMalformedOrNonTelephoneInput() {
        listOf("", "tel:112", "+", "***", "123+456", "https://example.com", "1".repeat(41)).forEach { assertNull(DialTarget.normalize(it)) }
    }
}
