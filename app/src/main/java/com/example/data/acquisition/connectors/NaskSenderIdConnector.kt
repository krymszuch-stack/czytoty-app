package com.example.data.acquisition.connectors

import com.example.data.acquisition.CanonicalEntity
import com.example.data.acquisition.CanonicalSecuritySignal
import com.example.data.acquisition.CanonicalSenderId
import com.example.data.acquisition.DataSourceConnector
import com.example.data.acquisition.LegalStatus
import com.example.data.acquisition.RawSourcePayload
import com.example.data.acquisition.SourceMetadata
import com.example.data.acquisition.SourceType
import com.example.data.acquisition.ValidationResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Ustrukturyzowany rekord zastrzeżonego nadpisu SMS podmiotu publicznego.
 */
data class NaskSenderIdRecord(
    val senderId: String,               // np. "e-US", "Policja", "ZUS", "mObywatel"
    val entityName: String,             // np. "Ministerstwo Finansów", "Komenda Główna Policji"
    val registeredDate: String? = null,
    val status: String = "ACTIVE"
)

/**
 * Konektor do oficjalnego Wykazu Nadpisów Podmiotów Publicznych (CSIRT NASK).
 *
 * Podstawa prawna: Ustawa z dnia 28 lipca 2023 r. o zwalczaniu nadużyć w komunikacji elektronicznej (UOZnK).
 * Status: APPROVED
 */
class NaskSenderIdConnector(
    private val clientFetcher: (suspend (url: String) -> String)? = null
) : DataSourceConnector<String, NaskSenderIdRecord> {

    override val metadata: SourceMetadata = SourceMetadata(
        id = "csirt_nask_sender_id",
        name = "CSIRT NASK - Wykaz Nadpisów SMS Podmiotów Publicznych",
        sourceType = SourceType.SECURITY_FEED,
        baseUrl = "https://cert.pl/wykaz-nadpisow",
        licenseType = "STATUTORY_TASK",
        legalStatus = LegalStatus.APPROVED,
        commercialUseAllowed = true,
        attributionRequired = true,
        attributionText = "Źródło: CSIRT NASK (Wykaz UOZnK)",
        defaultTtlHours = 168 // 7 dni
    )

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun fetch(): RawSourcePayload {
        val content = clientFetcher?.invoke(metadata.baseUrl) ?: "[]"
        return RawSourcePayload(
            sourceId = metadata.id,
            rawContent = content,
            contentType = "application/json"
        )
    }

    override fun normalize(payload: RawSourcePayload): List<NaskSenderIdRecord> {
        if (payload.rawContent.isBlank()) return emptyList()

        return try {
            // Obsługa zarówno formatu JSON, jak i formatu tekstowego/CSV
            if (payload.rawContent.trim().startsWith("[") || payload.rawContent.trim().startsWith("{")) {
                parseJson(payload.rawContent)
            } else {
                parseTextOrCsv(payload.rawContent)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseJson(content: String): List<NaskSenderIdRecord> {
        val jsonElement = jsonParser.parseToJsonElement(content)
        val jsonArray = when {
            jsonElement is kotlinx.serialization.json.JsonArray -> jsonElement
            jsonElement is kotlinx.serialization.json.JsonObject && jsonElement.containsKey("sender_ids") -> {
                jsonElement["sender_ids"]?.jsonArray ?: return emptyList()
            }
            else -> return emptyList()
        }

        return jsonArray.mapNotNull { item ->
            try {
                val obj = item.jsonObject
                val senderId = obj["sender_id"]?.jsonPrimitive?.content
                    ?: obj["name"]?.jsonPrimitive?.content
                    ?: obj["nadpis"]?.jsonPrimitive?.content
                    ?: return@mapNotNull null

                val entity = obj["entity_name"]?.jsonPrimitive?.content
                    ?: obj["podmiot"]?.jsonPrimitive?.content
                    ?: "Podmiot Publiczny"

                val date = obj["registered_at"]?.jsonPrimitive?.content

                NaskSenderIdRecord(
                    senderId = senderId.trim(),
                    entityName = entity.trim(),
                    registeredDate = date
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseTextOrCsv(content: String): List<NaskSenderIdRecord> {
        val records = mutableListOf<NaskSenderIdRecord>()
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("nadpis")) continue

            val parts = trimmed.split(";", ",")
            if (parts.isNotEmpty()) {
                val senderId = parts[0].trim()
                val entity = if (parts.size > 1) parts[1].trim() else "Podmiot Publiczny"
                records.add(NaskSenderIdRecord(senderId = senderId, entityName = entity))
            }
        }
        return records
    }

    override fun validate(records: List<NaskSenderIdRecord>): ValidationResult<NaskSenderIdRecord> {
        val valid = mutableListOf<NaskSenderIdRecord>()
        val rejected = mutableListOf<Pair<NaskSenderIdRecord, String>>()
        val errors = mutableListOf<String>()

        if (records.isEmpty()) {
            errors.add("Brak rekordów nadpisów SMS do walidacji.")
            return ValidationResult(isValid = false, validRecords = emptyList(), rejectedRecords = emptyList(), errors = errors)
        }

        // Zgodnie ze standardem GSM 03.38 alfanumeryczny Sender ID ma maksymalnie 11 znaków
        for (record in records) {
            if (record.senderId.isBlank()) {
                rejected.add(record to "Pusty identyfikator nadpisu")
            } else if (record.senderId.length > 11) {
                rejected.add(record to "Nadpis przekracza limit standardu GSM (max 11 znaków): ${record.senderId}")
            } else {
                valid.add(record)
            }
        }

        return ValidationResult(
            isValid = valid.isNotEmpty(),
            validRecords = valid,
            rejectedRecords = rejected,
            errors = errors
        )
    }

    override fun mapToCanonical(records: List<NaskSenderIdRecord>, payload: RawSourcePayload): List<CanonicalEntity> {
        val entities = mutableListOf<CanonicalEntity>()

        for (record in records) {
            val prov = createProvenance(
                recordId = "NASK-SID-${record.senderId}",
                payloadHash = payload.contentHash,
                confidence = 1.0f
            )

            // 1. Zastrzeżony nadpis kanoniczny
            entities.add(
                CanonicalSenderId(
                    senderId = record.senderId,
                    organizationName = record.entityName,
                    isStatutoryProtected = true,
                    status = "ACTIVE",
                    provenance = prov
                )
            )

            // 2. Sygnał bezpieczeństwa dla ochrony SMS
            entities.add(
                CanonicalSecuritySignal(
                    signalType = "SENDER_ID_PROTECTED",
                    targetValue = record.senderId,
                    severity = "CRITICAL",
                    threatCategory = "STATUTORY_PROTECTED_SMISHING_TARGET",
                    provenance = prov
                )
            )
        }

        return entities
    }
}
