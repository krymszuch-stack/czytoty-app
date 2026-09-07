package com.example.data.acquisition

import com.example.data.acquisition.connectors.UkeNumberingConnector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy jednostkowe dla UkeNumberingConnector (Tablice Numeracji UKE).
 */
class UkeNumberingConnectorTest {

    private val sampleUkeCsv = """
        prefix;operator;type;region
        732;P4 Sp. z o.o.;PLMN;Polska
        501;Orange Polska S.A.;MOBILNY;Polska
        22;Orange Polska S.A.;PSTN;Warszawa
        390;Netia S.A.;VOIP;Polska
        700;Telekomunikacja Premium;PREMIUM;Polska
        800;Infolinia Zaufania;TOLL_FREE;Polska
        abc;Unknown Operator;PSTN;Brak
    """.trimIndent()

    @Test
    fun `normalize correctly categorizes telecommunication ranges`() {
        val connector = UkeNumberingConnector()
        val payload = RawSourcePayload("uke_tzn", sampleUkeCsv)

        val records = connector.normalize(payload)
        assertEquals(7, records.size)

        val playMobile = records.first { it.prefix == "732" }
        assertEquals("PLMN", playMobile.networkType)
        assertEquals("P4 Sp. z o.o.", playMobile.operator)
        assertFalse(playMobile.isPremium)

        val warsawPstn = records.first { it.prefix == "22" }
        assertEquals("PSTN", warsawPstn.networkType)
        assertEquals("Warszawa", warsawPstn.areaCodeOrRegion)

        val premium = records.first { it.prefix == "700" }
        assertEquals("PREMIUM_RATE", premium.networkType)
        assertTrue(premium.isPremium)

        val tollFree = records.first { it.prefix == "800" }
        assertEquals("TOLL_FREE", tollFree.networkType)
    }

    @Test
    fun `validate rejects non-numeric or invalid prefixes`() {
        val connector = UkeNumberingConnector()
        val payload = RawSourcePayload("uke_tzn", sampleUkeCsv)
        val records = connector.normalize(payload)

        val validation = connector.validate(records)
        assertTrue(validation.isValid)
        assertEquals(6, validation.validRecords.size)
        assertEquals(1, validation.rejectedRecords.size)
        assertEquals("abc", validation.rejectedRecords.first().first.prefix)
    }

    @Test
    fun `mapToCanonical produces valid CanonicalPhoneIdentity with E164 normalized prefix`() {
        val connector = UkeNumberingConnector()
        val payload = RawSourcePayload("uke_tzn", sampleUkeCsv)
        val records = connector.normalize(payload)
        val validRecords = connector.validate(records).validRecords

        val canonical = connector.mapToCanonical(validRecords, payload)
        assertEquals(6, canonical.size)

        val play = canonical.filterIsInstance<com.example.data.acquisition.CanonicalPhoneIdentity>()
            .first { it.rawValue == "732" }

        assertEquals("+48732", play.normalizedNumber)
        assertEquals("P4 Sp. z o.o.", play.operatorName)
        assertEquals("PLMN", play.networkType)
        assertEquals("48", play.countryCode)
        assertEquals("uke_tzn", play.provenance.sourceId)
    }

    @Test
    fun `fail-closed on empty payload`() {
        val connector = UkeNumberingConnector()
        val emptyPayload = RawSourcePayload("uke_tzn", "")

        val records = connector.normalize(emptyPayload)
        assertTrue(records.isEmpty())

        val validation = connector.validate(records)
        assertFalse(validation.isValid)
    }
}
