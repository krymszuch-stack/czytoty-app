package com.example.data.acquisition

import com.example.data.acquisition.connectors.NaskSenderIdConnector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy jednostkowe dla NaskSenderIdConnector (Wykaz Nadpisów SMS CSIRT NASK).
 */
class NaskSenderIdConnectorTest {

    private val sampleJson = """
        [
            {
                "sender_id": "e-US",
                "entity_name": "Ministerstwo Finansów",
                "registered_at": "2024-03-01T00:00:00Z"
            },
            {
                "sender_id": "Policja",
                "entity_name": "Komenda Główna Policji",
                "registered_at": "2024-03-15T00:00:00Z"
            },
            {
                "sender_id": "ZUS",
                "entity_name": "Zakład Ubezpieczeń Społecznych",
                "registered_at": "2024-04-01T00:00:00Z"
            },
            {
                "sender_id": "ToooLongSenderIdExceeds11",
                "entity_name": "Invalid Entity",
                "registered_at": "2024-04-01T00:00:00Z"
            }
        ]
    """.trimIndent()

    @Test
    fun `normalize correctly parses NASK JSON payload`() {
        val connector = NaskSenderIdConnector()
        val payload = RawSourcePayload("csirt_nask_sender_id", sampleJson)

        val records = connector.normalize(payload)
        assertEquals(4, records.size)

        val eus = records.first { it.senderId == "e-US" }
        assertEquals("Ministerstwo Finansów", eus.entityName)
    }

    @Test
    fun `validate enforces GSM 0338 11-character limit on Sender IDs`() {
        val connector = NaskSenderIdConnector()
        val payload = RawSourcePayload("csirt_nask_sender_id", sampleJson)
        val records = connector.normalize(payload)

        val validation = connector.validate(records)
        assertTrue(validation.isValid)
        assertEquals(3, validation.validRecords.size)
        assertEquals(1, validation.rejectedRecords.size)
        assertEquals("ToooLongSenderIdExceeds11", validation.rejectedRecords.first().first.senderId)
    }

    @Test
    fun `mapToCanonical generates statutory protected CanonicalSenderId and critical security signals`() {
        val connector = NaskSenderIdConnector()
        val payload = RawSourcePayload("csirt_nask_sender_id", sampleJson)
        val records = connector.normalize(payload)
        val validRecords = connector.validate(records).validRecords

        val canonical = connector.mapToCanonical(validRecords, payload)
        // 3 CanonicalSenderId + 3 CanonicalSecuritySignal
        assertEquals(6, canonical.size)

        val sids = canonical.filterIsInstance<CanonicalSenderId>()
        assertEquals(3, sids.size)
        assertTrue(sids.all { it.isStatutoryProtected })

        val signals = canonical.filterIsInstance<CanonicalSecuritySignal>()
        assertEquals(3, signals.size)
        assertTrue(signals.all { it.signalType == "SENDER_ID_PROTECTED" })
        assertTrue(signals.all { it.severity == "CRITICAL" })
    }

    @Test
    fun `normalize handles CSV format fallback`() {
        val csv = """
            nadpis;podmiot
            mObywatel;Ministerstwo Cyfryzacji
            KAS;Krajowa Administracja Skarbowa
        """.trimIndent()

        val connector = NaskSenderIdConnector()
        val payload = RawSourcePayload("csirt_nask_sender_id", csv, contentType = "text/csv")

        val records = connector.normalize(payload)
        assertEquals(2, records.size)
        assertEquals("mObywatel", records[0].senderId)
        assertEquals("Ministerstwo Cyfryzacji", records[0].entityName)
    }
}
