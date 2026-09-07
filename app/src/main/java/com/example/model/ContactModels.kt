package com.example.model

/**
 * Typ kontaktu w architekturze Contacts 2.0
 */
enum class ContactEntityType {
  PERSON,
  ORGANIZATION
}

/**
 * Relacja / etykieta relacji dla osoby fizycznej
 */
enum class PersonRelationship(val label: String) {
  FAMILY("Rodzina"),
  FRIEND("Znajomy"),
  WORK("Praca"),
  DOCTOR("Lekarz / Zdrowie"),
  SERVICES("Usługi"),
  OTHER("Inne")
}

/**
 * Kategoria organizacji / instytucji
 */
enum class OrganizationCategory(val displayName: String, val iconName: String) {
  COURIER_LOGISTICS("Kurier / Logistyka", "local_shipping"),
  BANKING_FINANCE("Bankowość i finanse", "account_balance"),
  PUBLIC_OFFICE("Urząd / Instytucja publiczna", "account_balance"),
  HEALTHCARE("Zdrowie / Przychodnia", "medical_services"),
  TELECOM_IT("Telekomunikacja / IT", "cloud"),
  ENERGY_UTILITIES("Energetyka i media", "bolt"),
  RETAIL_SHOPPING("Zakupy / E-commerce", "shopping_bag"),
  EMERGENCY("Służby ratunkowe", "emergency"),
  SERVICES("Usługi ogólne", "business")
}

/**
 * Tryb sortowania kontaktów (Custom Contact Order)
 */
enum class ContactSortMode(val label: String) {
  MANUAL("Kolejność własna (Ręczna)"),
  MOST_FREQUENT("Najczęściej używane"),
  LEAST_FREQUENT("Najrzadziej używane"),
  RECENT("Ostatnia aktywność"),
  ALPHABETICAL("Alfabetycznie (A-Z)"),
  ORGANIZATIONS("Tylko organizacje"),
  PEOPLE("Tylko osoby")
}

/**
 * Główny model Zunifikowanego Kontaktu (Contacts 2.0)
 * Reprezentuje TOŻSAMOŚĆ KOMUNIKACYJNĄ (nie tylko surowy numer).
 */
data class UnifiedContact(
  val id: String,
  val entityType: ContactEntityType,

  // Dane wspólne
  val primaryDisplayName: String, // np. "Mama", "DPD Polska", "Przychodnia Rejonowa"
  val avatarInitials: String = primaryDisplayName.take(2).uppercase(),
  val avatarColorSeed: Long = id.hashCode().toLong(),
  val isPinned: Boolean = false,
  val manualOrderIndex: Int = 0,
  val quickContactSlot: Boolean = false, // Pojawia się w kafelkach Szybki Kontakt
  val singleClickToCall: Boolean = false, // Przydatne np. dla seniorów

  // Metryki agregowane (Calls + SMS + OTP + Reports)
  val callsCount: Int = 0,
  val lastCallTimestamp: String? = null,
  val lastCallEpoch: Long = 0L,
  val smsCount: Int = 0,
  val lastSmsTimestamp: String? = null,
  val lastSmsEpoch: Long = 0L,
  val otpCount: Int = 0,
  val reportsCount: Int = 0,
  val suspiciousLinksCount: Int = 0,

  // Reputacja i bezpieczeństwo
  val riskScore: Int = 0, // 0-100
  val riskSeverity: RiskSeverity = RiskSeverity.NO_KNOWN_RISK,
  val confidence: ConfidenceLevel = ConfidenceLevel.WYSOKA,
  val reputationNote: String? = null,

  // Dane specyficzne dla PERSON
  val firstName: String? = null,
  val lastName: String? = null,
  val email: String? = null,
  val relationship: PersonRelationship? = null,
  val customLabels: List<String> = emptyList(),
  val personalPhoneNumbers: List<String> = emptyList(),

  // Dane specyficzne dla ORGANIZATION
  val organizationCategory: OrganizationCategory? = null,
  val knownPhoneNumbers: List<String> = emptyList(),
  val smsSenderIds: List<String> = emptyList(),
  val officialDomains: List<String> = emptyList(),
  val officialWebsite: String? = null,
  val contactHours: String? = null, // np. "Pn-Pt: 8:00 - 18:00"
  val identificationSource: String = "Rejestr podmiotów gospodarczych (GUS/KRS/CERT)",
  val brandIdentity: BrandIdentity? = null,

  // Sugestia asystenta (Smart but optional)
  val smartPinSuggestion: String? = null
) {
  /**
   * Zwraca główny/reprezentatywny numer telefonu lub Sender ID do wyświetlenia
   */
  val primaryIdentifier: String
    get() = when (entityType) {
      ContactEntityType.PERSON -> personalPhoneNumbers.firstOrNull() ?: email ?: ""
      ContactEntityType.ORGANIZATION -> smsSenderIds.firstOrNull() ?: knownPhoneNumbers.firstOrNull() ?: ""
    }

  /**
   * Zwraca listę wszystkich numerów telefonu
   */
  val allPhoneNumbers: List<String>
    get() = if (entityType == ContactEntityType.PERSON) personalPhoneNumbers else knownPhoneNumbers

  /**
   * Całkowita częstotliwość interakcji (połączenia + wiadomości)
   */
  val interactionFrequency: Int
    get() = callsCount + smsCount

  /**
   * Najnowszy czas aktywności w milisekundach
   */
  val latestActivityEpoch: Long
    get() = maxOf(lastCallEpoch, lastSmsEpoch)

  /**
   * Sformatowany tekst ostatniej aktywności
   */
  val lastActivitySummary: String
    get() = when {
      lastCallTimestamp != null && lastSmsTimestamp != null ->
        if (lastCallEpoch >= lastSmsEpoch) "Połączenie: $lastCallTimestamp" else "SMS: $lastSmsTimestamp"
      lastCallTimestamp != null -> "Połączenie: $lastCallTimestamp"
      lastSmsTimestamp != null -> "SMS: $lastSmsTimestamp"
      else -> "Brak niedawnej aktywności"
    }
}
