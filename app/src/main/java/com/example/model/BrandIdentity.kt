package com.example.model

/**
 * Model tożsamości marki (Brand Identity) w katalogu publicznym.
 *
 * WAŻNA ZASADA ARCHITEKTONICZNA:
 * Rozpoznanie marki (np. po polu Sender ID "InPost" lub "mBank")
 * NIE oznacza kryptograficznego potwierdzenia autentyczności nadawcy w sieci GSM.
 * Protokół SMS nie posiada wbudowanego podpisu cyfrowego nadawcy,
 * dlatego aplikacja rozróżnia ROZPOZNANĄ MARKĘ od ZWERYFIKOWANEGO NADAWCY.
 */
data class BrandIdentity(
  val brandId: String,
  val displayName: String,
  val category: String, // np. "Paczki i Logistyka", "Bankowość", "Usługi IT", "Energetyka"
  val knownSenderIds: List<String>,
  val knownPhoneNumbers: List<String>,
  val officialDomains: List<String>,
  val isFinancialInstitution: Boolean = false,
  val verificationSource: String = "Krajowy rejestr podmiotów gospodarczych / Baza publiczna",
  val confidence: ConfidenceLevel = ConfidenceLevel.WYSOKA,
  val educationalNote: String? = null
) {
  /**
   * Sprawdza, czy podana domena jest oficjalną domeną marki.
   */
  fun isOfficialDomain(domain: String): Boolean {
    val cleanDomain = domain.lowercase().trim().removePrefix("www.")
    return officialDomains.any { official ->
      val cleanOfficial = official.lowercase().trim().removePrefix("www.")
      cleanDomain == cleanOfficial || cleanDomain.endsWith(".$cleanOfficial")
    }
  }
}
