package com.example.data.acquisition

import com.example.data.acquisition.connectors.CertPlConnector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy jednostkowe i testy fixture dla CertPlConnector (Lista Ostrzeżeń CERT Polska).
 */
class CertPlConnectorTest {

    private val sampleCertJson = """
        [
            {
                "DomainAddress": "phishing-bank-login.com",
                "InsertDate": "2026-09-01T10:00:00Z",
                "DeleteDate": null
            },
            {
                "DomainAddress": "poczta-polska-fake-doplata.pl",
                "InsertDate": "2026-09-02T12:00:00Z",
                "DeleteDate": null
            },
            {
                "DomainAddress": "old-expired-scam.xyz",
                "InsertDate": "2026-01-01T00:00:00Z",
                "DeleteDate": "2026-02-01T00:00:00Z"
            },
            {
                "DomainAddress": "invalid domain with spaces",
                "InsertDate": "2026-09-03T00:00:00Z",
                "DeleteDate": null
            }
        ]
    """.trimIndent()

    @Test
    fun `normalize correctly parses CERT Polska JSON array`() {
        val connector = CertPlConnector()
        val payload = RawSourcePayload("cert_polska", sampleCertJson)

        val records = connector.normalize(payload)
        assertEquals(4, records.size)

        val active = records.filter { it.isActive }
        assertEquals(3, active.size)

        val inactive = records.first { !it.isActive }
        assertEquals("old-expired-scam.xyz", inactive.domain)
        assertEquals("2026-02-01T00:00:00Z", inactive.deleteDate)
    }

    @Test
    fun `validate rejects syntactically invalid domain names`() {
        val connector = CertPlConnector()
        val payload = RawSourcePayload("cert_polska", sampleCertJson)
        val records = connector.normalize(payload)

        val validation = connector.validate(records)
        assertTrue("Walidacja powinna zaakceptować poprawne domeny", validation.isValid)
        assertEquals(3, validation.validRecords.size)
        assertEquals(1, validation.rejectedRecords.size)
        assertEquals("invalid domain with spaces", validation.rejectedRecords.first().first.domain)
    }

    @Test
    fun `mapToCanonical creates CanonicalDomain and active SecuritySignal`() {
        val connector = CertPlConnector()
        val payload = RawSourcePayload("cert_polska", sampleCertJson)
        val records = connector.normalize(payload)
        val validRecords = connector.validate(records).validRecords

        val canonical = connector.mapToCanonical(validRecords, payload)

        // 3 CanonicalDomain + 2 CanonicalSecuritySignal (tylko dla active domain)
        assertEquals(5, canonical.size)

        val domains = canonical.filterIsInstance<CanonicalDomain>()
        assertEquals(3, domains.size)
        assertTrue(domains.any { it.domainName == "phishing-bank-login.com" && it.isActiveThreat })
        assertTrue(domains.any { it.domainName == "old-expired-scam.xyz" && !it.isActiveThreat })

        val signals = canonical.filterIsInstance<CanonicalSecuritySignal>()
        assertEquals(2, signals.size)
        assertTrue(signals.all { it.signalType == "DOMAIN_ALERT" })
    }

    @Test
    fun `normalize fails closed on corrupted or malformed payload`() {
        val connector = CertPlConnector()
        val brokenPayload = RawSourcePayload("cert_polska", "<html><head>502 Bad Gateway</head></html>")

        val records = connector.normalize(brokenPayload)
        assertTrue("Parser musi zadziałać w trybie fail-closed (pusta lista zamiast wyjątku)", records.isEmpty())

        val validation = connector.validate(records)
        assertFalse("Kwarantanna nie może zatwierdzić pustego lub zepsutego feedu", validation.isValid)
    }

    @Test
    fun `connector fetch invokes client fetcher with baseUrl`() = runBlocking {
        var calledUrl = ""
        val connector = CertPlConnector(clientFetcher = { url ->
            calledUrl = url
            sampleCertJson
        })

        val payload = connector.fetch()
        assertEquals("https://hole.cert.pl/domains/v2/domains.json", calledUrl)
        assertEquals(sampleCertJson, payload.rawContent)
        assertEquals(64, payload.contentHash.length)
    }
}
