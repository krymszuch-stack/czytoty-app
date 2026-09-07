package com.example.data.acquisition.connectors

import com.example.data.acquisition.CanonicalEntity
import com.example.data.acquisition.CanonicalPhoneIdentity
import com.example.data.acquisition.DataClassification
import com.example.data.acquisition.DataSourceConnector
import com.example.data.acquisition.LegalStatus
import com.example.data.acquisition.RawSourcePayload
import com.example.data.acquisition.SourceMetadata
import com.example.data.acquisition.SourceType
import com.example.data.acquisition.ValidationResult
import com.example.data.acquisition.privacy.PrivacyProtectionService

/**
 * Ustrukturyzowany rekord przydziału numeracyjnego z tablic UKE.
 */
data class UkeNumberingRecord(
    val prefix: String,                 // np. "732", "22", "700", "800"
    val operator: String,               // np. "P4 Sp. z o.o.", "Orange Polska S.A."
    val networkType: String,            // "PLMN", "PSTN", "VOIP", "PREMIUM_RATE", "TOLL_FREE"
    val areaCodeOrRegion: String? = null,
    val isPremium: Boolean = false
)

/**
 * Konektor do oficjalnych Tablic Zagospodarowania Numeracji (TZN) UKE.
 *
 * Źródło: https://dane.gov.pl/dataset/tablice-numeracji (UKE)
 * Licencja: Ustawa o otwartych danych / CC0
 * Status: APPROVED
 */
class UkeNumberingConnector(
    private val clientFetcher: (suspend (url: String) -> String)? = null
) : DataSourceConnector<String, UkeNumberingRecord> {

    override val metadata: SourceMetadata = SourceMetadata(
        id = "uke_tzn",
        name = "UKE - Tablice Zagospodarowania Numeracji",
        sourceType = SourceType.PUBLIC_REGISTRY,
        baseUrl = "https://dane.gov.pl/dataset/tablice-numeracji",
        licenseType = "OPEN_DATA_CC0",
        legalStatus = LegalStatus.APPROVED,
        commercialUseAllowed = true,
        attributionRequired = true,
        attributionText = "Źródło: Urząd Komunikacji Elektronicznej (dane.gov.pl)",
        defaultTtlHours = 720 // 30 dni
    )

    override suspend fun fetch(): RawSourcePayload {
        val content = clientFetcher?.invoke(metadata.baseUrl) ?: ""
        return RawSourcePayload(
            sourceId = metadata.id,
            rawContent = content,
            contentType = "text/csv"
        )
    }

    override fun normalize(payload: RawSourcePayload): List<UkeNumberingRecord> {
        if (payload.rawContent.isBlank()) return emptyList()

        val lines = payload.rawContent.lines()
        val records = mutableListOf<UkeNumberingRecord>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("prefix")) {
                continue
            }

            // Obsługa formatu CSV (prefix;operator;type;region)
            val parts = trimmed.split(";", ",")
            if (parts.size >= 3) {
                val prefix = parts[0].trim().replace("+", "").replace(" ", "")
                val operator = parts[1].trim()
                val typeRaw = parts[2].trim().uppercase()
                val region = if (parts.size > 3) parts[3].trim() else null

                val detectedType = when {
                    typeRaw.contains("PREMIUM") || prefix.startsWith("70") -> "PREMIUM_RATE"
                    prefix.startsWith("800") -> "TOLL_FREE"
                    prefix.startsWith("801") || prefix.startsWith("804") -> "SHARED_COST"
                    prefix.startsWith("39") || typeRaw.contains("VOIP") -> "VOIP"
                    typeRaw.contains("PLMN") || typeRaw.contains("MOBIL") || isPolishMobilePrefix(prefix) -> "PLMN"
                    else -> "PSTN"
                }

                val isPremium = detectedType == "PREMIUM_RATE"

                records.add(
                    UkeNumberingRecord(
                        prefix = prefix,
                        operator = operator,
                        networkType = detectedType,
                        areaCodeOrRegion = region,
                        isPremium = isPremium
                    )
                )
            }
        }

        return records
    }

    override fun validate(records: List<UkeNumberingRecord>): ValidationResult<UkeNumberingRecord> {
        val valid = mutableListOf<UkeNumberingRecord>()
        val rejected = mutableListOf<Pair<UkeNumberingRecord, String>>()
        val errors = mutableListOf<String>()

        if (records.isEmpty()) {
            errors.add("Brak rekordów tablic numeracyjnych do walidacji.")
            return ValidationResult(isValid = false, validRecords = emptyList(), rejectedRecords = emptyList(), errors = errors)
        }

        for (record in records) {
            if (record.prefix.length < 2 || record.prefix.length > 7 || !record.prefix.all { it.isDigit() }) {
                rejected.add(record to "Niepoprawna długość lub znaki prefiksu UKE: ${record.prefix}")
            } else if (record.operator.isBlank()) {
                rejected.add(record to "Brak nazwy operatora dla prefiksu ${record.prefix}")
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

    override fun mapToCanonical(records: List<UkeNumberingRecord>, payload: RawSourcePayload): List<CanonicalEntity> {
        return records.map { record ->
            val prov = createProvenance(
                recordId = "UKE-${record.prefix}",
                payloadHash = payload.contentHash,
                confidence = 1.0f
            )

            val normalizedPrefixNumber = "+48${record.prefix}"

            CanonicalPhoneIdentity(
                rawValue = record.prefix,
                normalizedNumber = normalizedPrefixNumber,
                countryCode = "48",
                operatorName = record.operator,
                networkType = record.networkType,
                isBusinessNumber = record.isPremium || record.networkType == "TOLL_FREE" || record.networkType == "SHARED_COST",
                isPremiumRate = record.isPremium,
                classification = if (record.isPremium) DataClassification.PUBLIC_BUSINESS else DataClassification.PUBLIC_TELECOM,
                provenance = prov
            )
        }
    }

    private fun isPolishMobilePrefix(prefix: String): Boolean {
        if (prefix.length < 2) return false
        val p2 = prefix.take(2).toIntOrNull() ?: return false
        // Polskie zakresy komórkowe: 50x-57x, 60x, 66x, 69x, 72x, 73x, 78x, 79x, 88x
        return p2 in 50..57 || p2 in listOf(60, 66, 69, 72, 73, 78, 79, 88)
    }
}
