package com.example.data.acquisition.connectors

import com.example.data.acquisition.CanonicalDomain
import com.example.data.acquisition.CanonicalEntity
import com.example.data.acquisition.CanonicalSecuritySignal
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
 * Ustrukturyzowany rekord ostrzeżenia z listy CERT Polska.
 */
data class CertPlRecord(
    val domain: String,
    val insertDate: String?,
    val deleteDate: String?,
    val rawJson: String
) {
    val isActive: Boolean
        get() = deleteDate.isNullOrBlank()
}

/**
 * Konektor do oficjalnej Listy Ostrzeżeń CERT Polska (NASK-PIB).
 *
 * Źródło: https://hole.cert.pl/domains/v2/domains.json
 * Licencja: Zadanie Ustawowe / Otwarte Dane
 * Status: APPROVED
 */
class CertPlConnector(
    private val clientFetcher: (suspend (url: String) -> String)? = null
) : DataSourceConnector<String, CertPlRecord> {

    override val metadata: SourceMetadata = SourceMetadata(
        id = "cert_polska",
        name = "CERT Polska / NASK - Lista Ostrzeżeń",
        sourceType = SourceType.SECURITY_FEED,
        baseUrl = "https://hole.cert.pl/domains/v2/domains.json",
        licenseType = "STATUTORY_TASK",
        legalStatus = LegalStatus.APPROVED,
        commercialUseAllowed = true,
        attributionRequired = true,
        attributionText = "Źródło: CERT Polska (NASK-PIB)",
        defaultTtlHours = 1
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

    override fun normalize(payload: RawSourcePayload): List<CertPlRecord> {
        if (payload.rawContent.isBlank()) return emptyList()

        return try {
            val jsonElement = jsonParser.parseToJsonElement(payload.rawContent)
            val jsonArray = when {
                jsonElement is kotlinx.serialization.json.JsonArray -> jsonElement
                jsonElement is kotlinx.serialization.json.JsonObject && jsonElement.containsKey("domains") -> {
                    jsonElement["domains"]?.jsonArray ?: return emptyList()
                }
                else -> return emptyList()
            }

            jsonArray.mapNotNull { item ->
                try {
                    val obj = item.jsonObject
                    val domain = obj["DomainAddress"]?.jsonPrimitive?.content
                        ?: obj["domain"]?.jsonPrimitive?.content
                        ?: obj["name"]?.jsonPrimitive?.content
                        ?: return@mapNotNull null

                    val insertElem = obj["InsertDate"] ?: obj["insertDate"]
                    val insertDate = if (insertElem == null || insertElem is kotlinx.serialization.json.JsonNull) {
                        null
                    } else {
                        val str = insertElem.jsonPrimitive.content
                        if (str == "null" || str.isBlank()) null else str
                    }

                    val deleteElem = obj["DeleteDate"] ?: obj["deleteDate"]
                    val deleteDate = if (deleteElem == null || deleteElem is kotlinx.serialization.json.JsonNull) {
                        null
                    } else {
                        val str = deleteElem.jsonPrimitive.content
                        if (str == "null" || str.isBlank()) null else str
                    }

                    CertPlRecord(
                        domain = domain.trim().lowercase().removeSuffix("."),
                        insertDate = insertDate,
                        deleteDate = deleteDate,
                        rawJson = item.toString()
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            // Fail-closed: przy uszkodzonym formacie zwracamy pustą listę
            emptyList()
        }
    }

    override fun validate(records: List<CertPlRecord>): ValidationResult<CertPlRecord> {
        val valid = mutableListOf<CertPlRecord>()
        val rejected = mutableListOf<Pair<CertPlRecord, String>>()
        val errors = mutableListOf<String>()

        if (records.isEmpty()) {
            errors.add("Brak rekordów do walidacji - możliwy błąd parsowania lub pusty feed.")
            return ValidationResult(isValid = false, validRecords = emptyList(), rejectedRecords = emptyList(), errors = errors)
        }

        // Walidacja syntaktyczna nazwy domeny
        val domainPattern = Regex("^[a-z0-9]([a-z0-9\\-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9\\-]{0,61}[a-z0-9])?)+$")

        for (record in records) {
            if (record.domain.isBlank()) {
                rejected.add(record to "Pusta nazwa domeny")
            } else if (!domainPattern.matches(record.domain)) {
                rejected.add(record to "Niepoprawna składnia FQDN: ${record.domain}")
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

    override fun mapToCanonical(records: List<CertPlRecord>, payload: RawSourcePayload): List<CanonicalEntity> {
        val canonicalEntities = mutableListOf<CanonicalEntity>()

        for (record in records) {
            val prov = createProvenance(
                recordId = record.domain,
                payloadHash = payload.contentHash,
                confidence = 1.0f
            )

            // 1. Kanoniczna domena
            canonicalEntities.add(
                CanonicalDomain(
                    domainName = record.domain,
                    category = "PHISHING",
                    isActiveThreat = record.isActive,
                    threatDescription = "Lista Ostrzeżeń CERT Polska przed niebezpiecznymi stronami",
                    provenance = prov
                )
            )

            // 2. Kanoniczny sygnał bezpieczeństwa (tylko dla aktualnie złośliwych domen)
            if (record.isActive) {
                canonicalEntities.add(
                    CanonicalSecuritySignal(
                        signalType = "DOMAIN_ALERT",
                        targetValue = record.domain,
                        severity = "HIGH",
                        threatCategory = "PHISHING_SMISHING",
                        provenance = prov
                    )
                )
            }
        }

        return canonicalEntities
    }
}
