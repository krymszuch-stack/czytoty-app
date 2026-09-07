package com.example.data.acquisition

import com.example.data.acquisition.connectors.CertPlConnector
import com.example.data.acquisition.pipeline.DataAcquisitionPipeline
import com.example.data.acquisition.pipeline.ImportStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy jednostkowe dla DataAcquisitionPipeline (Bramka prawna, kwarantanna i rejestracja zadań).
 */
class DataAcquisitionPipelineTest {

    private val pipeline = DataAcquisitionPipeline()

    @Test
    fun `legal filter immediately rejects sources marked as DO_NOT_INGEST`() = runBlocking {
        // Symulacja konektora serwisu konkurencyjnego / nielegalnego scrapingu
        val competitorConnector = object : DataSourceConnector<String, String> {
            override val metadata = SourceMetadata(
                id = "nieznany_numer",
                name = "Nieznany-Numer.pl",
                sourceType = SourceType.COMMERCIAL_PLATFORM,
                baseUrl = "https://nieznany-numer.pl",
                licenseType = "PROPRIETARY",
                legalStatus = LegalStatus.DO_NOT_INGEST,
                commercialUseAllowed = false,
                attributionRequired = false,
                attributionText = null
            )

            override suspend fun fetch(): RawSourcePayload = RawSourcePayload(metadata.id, "some raw scraped html")
            override fun normalize(payload: RawSourcePayload): List<String> = listOf("record1")
            override fun validate(records: List<String>): ValidationResult<String> = ValidationResult(true, records, emptyList())
            override fun mapToCanonical(records: List<String>, payload: RawSourcePayload): List<CanonicalEntity> = emptyList()
        }

        val job = pipeline.runIngest(competitorConnector)

        assertEquals(ImportStatus.REJECTED_BY_LEGAL_FILTER, job.status)
        assertEquals(0, job.recordsFetched)
        assertEquals(0, job.recordsInserted)
        assertTrue(job.errorSummary!!.contains("bramkę prawną", ignoreCase = true))
    }

    @Test
    fun `successful ingest attaches provenance and logs metrics`() = runBlocking {
        val sampleJson = """
            [
                {
                    "DomainAddress": "phishing-secure-login.com",
                    "InsertDate": "2026-09-01T10:00:00Z",
                    "DeleteDate": null
                },
                {
                    "DomainAddress": "invalid domain",
                    "InsertDate": "2026-09-01T10:00:00Z",
                    "DeleteDate": null
                }
            ]
        """.trimIndent()

        val connector = CertPlConnector(clientFetcher = { sampleJson })
        val job = pipeline.runIngest(connector)

        assertEquals(ImportStatus.COMPLETED, job.status)
        assertEquals(2, job.recordsFetched)
        assertEquals(1, job.recordsRejected) // 1 invalid domain rejected by quarantine
        assertTrue(job.recordsInserted > 0)
        assertNotNull(job.jobId)

        // Weryfikacja Provenance
        val firstEntity = job.canonicalEntities.first()
        assertEquals("cert_polska", firstEntity.provenance.sourceId)
        assertEquals(SourceType.SECURITY_FEED, firstEntity.provenance.sourceType)
        assertNotNull(firstEntity.provenance.payloadHash)
    }
}
