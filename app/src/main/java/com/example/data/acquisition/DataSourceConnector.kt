package com.example.data.acquisition

import java.security.MessageDigest
import java.time.Instant

/**
 * Status prawny źródła danych w audycie CzyToTy?.
 *
 * Zasada: LEGAL ACCESS > DATA VOLUME.
 * Tylko źródła o statusie [APPROVED] lub [APPROVED_WITH_CONDITIONS] trafiają do bazy produkcyjnej.
 */
enum class LegalStatus {
    APPROVED,
    APPROVED_WITH_CONDITIONS,
    NEEDS_PERMISSION,
    API_ONLY,
    DO_NOT_INGEST
}

/**
 * Typ źródła danych.
 */
enum class SourceType {
    SECURITY_FEED,
    PUBLIC_REGISTRY,
    FIRST_PARTY_COMMUNITY,
    OFFICIAL_BRAND_CATALOG,
    COMMERCIAL_PLATFORM
}

/**
 * Poziom wrażliwości / klasyfikacja danych z perspektywy RODO i prywatności.
 */
enum class DataClassification {
    PUBLIC_TELECOM,       // Ogólne zakresy numeracyjne, operatorzy, prefiksy UKE
    PUBLIC_BUSINESS,      // Oficjalne infolinie firm, urzędy, szpitale, rejestry KRS
    PRIVATE_RESIDENTIAL   // Numery osób prywatnych / jednoosobowych działalności
}

/**
 * Metadane źródła danych wymagane dla każdego konektora.
 */
data class SourceMetadata(
    val id: String,
    val name: String,
    val sourceType: SourceType,
    val baseUrl: String,
    val licenseType: String,
    val legalStatus: LegalStatus,
    val commercialUseAllowed: Boolean,
    val attributionRequired: Boolean,
    val attributionText: String?,
    val defaultTtlHours: Long = 24
)

/**
 * Pełny audyt pochodzenia danych (Provenance) dołączany do każdego rekordu kanonicznego.
 */
data class RecordProvenance(
    val sourceId: String,
    val sourceName: String,
    val sourceType: SourceType,
    val sourceUrl: String,
    val license: String,
    val retrievedAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis(),
    val sourceRecordId: String,
    val payloadHash: String,
    val confidence: Float = 1.0f,
    val verificationLevel: String = "OFFICIAL"
)

/**
 * Surowy ładunek pobrany ze źródła.
 */
data class RawSourcePayload(
    val sourceId: String,
    val rawContent: String,
    val contentType: String = "application/json",
    val fetchedAt: Long = System.currentTimeMillis(),
    val eTag: String? = null,
    val lastModified: String? = null
) {
    val contentHash: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawContent.toByteArray(Charsets.UTF_8))
        hashBytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Wynik walidacji surowych rekordów w warstwie kwarantanny.
 */
data class ValidationResult<T>(
    val isValid: Boolean,
    val validRecords: List<T>,
    val rejectedRecords: List<Pair<T, String>>,
    val errors: List<String> = emptyList()
)

/**
 * Baza dla encji kanonicznych w CzyToTy?.
 */
sealed interface CanonicalEntity {
    val provenance: RecordProvenance
}

/**
 * Kanoniczna tożsamość numeru telefonicznego (UKE TZN / Rejestry).
 */
data class CanonicalPhoneIdentity(
    val rawValue: String,
    val normalizedNumber: String,        // E.164, np. +48732123456
    val countryCode: String = "48",
    val operatorName: String,
    val networkType: String,             // PLMN, PSTN, VOIP, PREMIUM_RATE, TOLL_FREE
    val isBusinessNumber: Boolean = false,
    val isPremiumRate: Boolean = false,
    val classification: DataClassification = DataClassification.PUBLIC_TELECOM,
    val privacyPseudonymToken: String? = null,
    override val provenance: RecordProvenance
) : CanonicalEntity

/**
 * Kanoniczna domena internetowa (Oficjalna lub Ostrzeżenie CERT).
 */
data class CanonicalDomain(
    val domainName: String,              // lowercase / punycode
    val category: String,                // OFFICIAL_BRAND, PHISHING, FRAUD
    val isActiveThreat: Boolean,
    val threatDescription: String? = null,
    override val provenance: RecordProvenance
) : CanonicalEntity

/**
 * Kanoniczny nadpis SMS (Sender ID) - CSIRT NASK / UOZnK.
 */
data class CanonicalSenderId(
    val senderId: String,
    val organizationName: String?,
    val isStatutoryProtected: Boolean,   // Rejestr Ustawowy UOZnK
    val status: String = "ACTIVE",
    override val provenance: RecordProvenance
) : CanonicalEntity

/**
 * Kanoniczny sygnał bezpieczeństwa z feedu CERT / CSIRT.
 */
data class CanonicalSecuritySignal(
    val signalType: String,              // DOMAIN_ALERT, PHONE_ALERT, URL_ALERT, SENDER_ID_SPOOF
    val targetValue: String,
    val severity: String,                // LOW, MEDIUM, HIGH, CRITICAL
    val threatCategory: String,
    override val provenance: RecordProvenance
) : CanonicalEntity

/**
 * Uniwersalny interfejs konektora źródła danych.
 *
 * Każde źródło w CzyToTy? jest zaimplementowane jako niezależny konektor
 * podlegający ścisłej weryfikacji legalności, kwarantannie i normalizacji.
 */
interface DataSourceConnector<RawType, NormalizedType> {
    val metadata: SourceMetadata

    /** Pobiera surowy ładunek z zewnętrznego źródła. */
    suspend fun fetch(): RawSourcePayload

    /** Dokonuje parsowania i normalizacji surowego tekstu do ustrukturyzowanych obiektów. */
    fun normalize(payload: RawSourcePayload): List<NormalizedType>

    /** Waliduje znormalizowane rekordy pod kątem poprawności formatów, reguł i spójności. */
    fun validate(records: List<NormalizedType>): ValidationResult<NormalizedType>

    /**
     * Filtruje dane przez bramkę prawną.
     * Jeżeli źródło nie posiada aktywnej licencji lub statusu APPROVED, zwraca pustą listę.
     */
    fun legalFilter(records: List<NormalizedType>): List<NormalizedType> {
        if (metadata.legalStatus != LegalStatus.APPROVED &&
            metadata.legalStatus != LegalStatus.APPROVED_WITH_CONDITIONS) {
            return emptyList()
        }
        return records
    }

    /** Mapuje zweryfikowane rekordy do kanonicznych encji bazy CzyToTy?. */
    fun mapToCanonical(records: List<NormalizedType>, payload: RawSourcePayload): List<CanonicalEntity>

    /** Generuje obiekt Provenance dla pojedynczego rekordu. */
    fun createProvenance(recordId: String, payloadHash: String, confidence: Float = 1.0f): RecordProvenance {
        return RecordProvenance(
            sourceId = metadata.id,
            sourceName = metadata.name,
            sourceType = metadata.sourceType,
            sourceUrl = metadata.baseUrl,
            license = metadata.licenseType,
            sourceRecordId = recordId,
            payloadHash = payloadHash,
            confidence = confidence
        )
    }
}
