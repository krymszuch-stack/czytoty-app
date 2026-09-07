package com.example.data.acquisition.pipeline

import com.example.data.acquisition.CanonicalEntity
import com.example.data.acquisition.DataSourceConnector
import com.example.data.acquisition.LegalStatus
import com.example.data.acquisition.RawSourcePayload
import java.util.UUID

/**
 * Podsumowanie zadania importowego (Obserwowalność potoku).
 */
data class ImportJobSummary(
    val jobId: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long = System.currentTimeMillis(),
    val status: ImportStatus,
    val recordsFetched: Int,
    val recordsInserted: Int,
    val recordsRejected: Int,
    val canonicalEntities: List<CanonicalEntity> = emptyList(),
    val errorSummary: String? = null
)

enum class ImportStatus {
    COMPLETED,
    REJECTED_BY_LEGAL_FILTER,
    VALIDATION_FAILED,
    FAILED
}

/**
 * Główny koordynator procesu pozyskiwania i przetwarzania danych w CzyToTy?.
 *
 * Implementuje ścisły przepływ jakościowy i prawny:
 * 1. POBRANIE (FETCH)
 * 2. KWARANTANNA SUROWYCH DANYCH (RAW STAGING)
 * 3. BRAMKA PRAWNA (LEGAL FILTER) - fail-closed dla źródeł niedozwolonych
 * 4. WALIDACJA I DEDUKCJA BŁĘDÓW (VALIDATION)
 * 5. MAPOWANIE DO POSTACI KANONICZNEJ (CANONICAL MERGE & PROVENANCE)
 */
class DataAcquisitionPipeline {

    suspend fun <RawType, NormalizedType> runIngest(
        connector: DataSourceConnector<RawType, NormalizedType>
    ): ImportJobSummary {
        val jobId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()
        val sourceId = connector.metadata.id

        // 1. Weryfikacja bramki prawnej na poziomie źródła
        if (connector.metadata.legalStatus == LegalStatus.DO_NOT_INGEST ||
            connector.metadata.legalStatus == LegalStatus.NEEDS_PERMISSION) {
            return ImportJobSummary(
                jobId = jobId,
                sourceId = sourceId,
                startedAt = startedAt,
                finishedAt = System.currentTimeMillis(),
                status = ImportStatus.REJECTED_BY_LEGAL_FILTER,
                recordsFetched = 0,
                recordsInserted = 0,
                recordsRejected = 0,
                errorSummary = "Źródło odrzucone przez bramkę prawną (LegalStatus: ${connector.metadata.legalStatus}). Scrapowanie lub import jest zabroniony."
            )
        }

        return try {
            // 2. Pobranie ładunku (Fetch)
            val rawPayload: RawSourcePayload = connector.fetch()

            // 3. Normalizacja
            val normalizedRecords = connector.normalize(rawPayload)
            val recordsFetched = normalizedRecords.size

            if (normalizedRecords.isEmpty()) {
                return ImportJobSummary(
                    jobId = jobId,
                    sourceId = sourceId,
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                    status = ImportStatus.VALIDATION_FAILED,
                    recordsFetched = 0,
                    recordsInserted = 0,
                    recordsRejected = 0,
                    errorSummary = "Brak rekordów do przetworzenia. Sprawdź format lub dostępność feedu."
                )
            }

            // 4. Bramka prawna na poziomie rekordów
            val legallyAllowedRecords = connector.legalFilter(normalizedRecords)
            val legalRejections = normalizedRecords.size - legallyAllowedRecords.size

            // 5. Walidacja jakościowa i kwarantanna
            val validation = connector.validate(legallyAllowedRecords)
            if (!validation.isValid && validation.validRecords.isEmpty()) {
                return ImportJobSummary(
                    jobId = jobId,
                    sourceId = sourceId,
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                    status = ImportStatus.VALIDATION_FAILED,
                    recordsFetched = recordsFetched,
                    recordsInserted = 0,
                    recordsRejected = validation.rejectedRecords.size + legalRejections,
                    errorSummary = "Błędy walidacji kwarantanny: ${validation.errors.joinToString("; ")}"
                )
            }

            // 6. Mapowanie do encji kanonicznych wraz z Provenance
            val canonicalEntities = connector.mapToCanonical(validation.validRecords, rawPayload)

            ImportJobSummary(
                jobId = jobId,
                sourceId = sourceId,
                startedAt = startedAt,
                finishedAt = System.currentTimeMillis(),
                status = ImportStatus.COMPLETED,
                recordsFetched = recordsFetched,
                recordsInserted = canonicalEntities.size,
                recordsRejected = validation.rejectedRecords.size + legalRejections,
                canonicalEntities = canonicalEntities,
                errorSummary = null
            )
        } catch (e: Exception) {
            ImportJobSummary(
                jobId = jobId,
                sourceId = sourceId,
                startedAt = startedAt,
                finishedAt = System.currentTimeMillis(),
                status = ImportStatus.FAILED,
                recordsFetched = 0,
                recordsInserted = 0,
                recordsRejected = 0,
                errorSummary = "Nieoczekiwany błąd potoku importu: ${e.message}"
            )
        }
    }
}
