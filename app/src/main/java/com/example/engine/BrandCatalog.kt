package com.example.engine

import com.example.model.BrandIdentity
import com.example.model.ConfidenceLevel

/**
 * Katalog oficjalnych podmiotów i marek na rynku polskim.
 * Służy do porównywania domen z linków w SMS oraz sprawdzania identyfikatorów Sender ID.
 *
 * Ważna zasada: Rozpoznanie marki w polu "OD" (Sender ID) NIE jest tożsame
 * z kryptograficznym potwierdzeniem tożsamości w standardzie SMS GSM.
 */
object BrandCatalog {

  val inPost = BrandIdentity(
    brandId = "inpost_pl",
    displayName = "InPost",
    category = "Paczki i Logistyka",
    knownSenderIds = listOf("InPost", "INPOST", "Paczkomaty"),
    knownPhoneNumbers = listOf("+48 722 444 000", "+48 746 600 000"),
    officialDomains = listOf("inpost.pl", "twoj.inpost.pl", "inpost.freshdesk.com"),
    isFinancialInstitution = false,
    educationalNote = "InPost nigdy nie wysyła linków z prośbą o dopłatę 1-2 zł do przesyłki za pośrednictwem zewnętrznych bramek."
  )

  val mBank = BrandIdentity(
    brandId = "mbank_pl",
    displayName = "mBank",
    category = "Bankowość i finanse",
    knownSenderIds = listOf("mBank", "MBANK"),
    knownPhoneNumbers = listOf("+48 42 630 00 00", "+48 22 627 99 77"),
    officialDomains = listOf("mbank.pl", "online.mbank.pl"),
    isFinancialInstitution = true,
    educationalNote = "Bank nigdy nie prosi przez SMS o hasło, instalację certyfikatów ani podanie kodu BLIK."
  )

  val pkoBp = BrandIdentity(
    brandId = "pkobp_pl",
    displayName = "PKO Bank Polski",
    category = "Bankowość i finanse",
    knownSenderIds = listOf("PKO BP", "PKOBP", "iPKO"),
    knownPhoneNumbers = listOf("+48 22 521 80 00", "+48 800 302 302"),
    officialDomains = listOf("pkobp.pl", "ipko.pl"),
    isFinancialInstitution = true,
    educationalNote = "PKO BP nie przesyła linków do logowania z prośbą o potwierdzenie danych osobowych."
  )

  val google = BrandIdentity(
    brandId = "google_auth",
    displayName = "Google",
    category = "Usługi internetowe",
    knownSenderIds = listOf("Google", "GOOGLE"),
    knownPhoneNumbers = emptyList(),
    officialDomains = listOf("google.com", "google.pl", "accounts.google.com"),
    isFinancialInstitution = false,
    educationalNote = "Kody weryfikacyjne Google są poufne. Nigdy nie dyktuj ich nikomu przez telefon."
  )

  val dpd = BrandIdentity(
    brandId = "dpd_pl",
    displayName = "DPD Polska",
    category = "Paczki i Kurierzy",
    knownSenderIds = listOf("DPD", "DPD Polska"),
    knownPhoneNumbers = listOf("+48 22 577 55 55"),
    officialDomains = listOf("dpd.com.pl", "dpd.com"),
    isFinancialInstitution = false,
    educationalNote = "Status przesyłki DPD można zawsze zweryfikować bezpośrednio na oficjalnej stronie dpd.com.pl."
  )

  val pge = BrandIdentity(
    brandId = "pge_obrot",
    displayName = "PGE Polska Grupa Energetyczna",
    category = "Energetyka i media",
    knownSenderIds = listOf("PGE", "PGE Dystrybucja"),
    knownPhoneNumbers = listOf("+48 422 222 222"),
    officialDomains = listOf("gkpge.pl", "pgeobrot.pl"),
    isFinancialInstitution = false,
    educationalNote = "Ostrzeżenia o 'natychmiastowym wyłączeniu prądu w razie braku dopłaty kilkunastu złotych' to popularny schemat oszustwa."
  )

  val allegro = BrandIdentity(
    brandId = "allegro_pl",
    displayName = "Allegro",
    category = "E-commerce i zakupy",
    knownSenderIds = listOf("Allegro", "ALLEGRO"),
    knownPhoneNumbers = emptyList(),
    officialDomains = listOf("allegro.pl", "allegrolokalnie.pl"),
    isFinancialInstitution = false,
    educationalNote = "Opłacenie zakupu na Allegro odbywa się wewnątrz aplikacji lub bezpośrednio w portalu allegro.pl."
  )

  private val allBrands = listOf(inPost, mBank, pkoBp, google, dpd, pge, allegro)

  fun findBrandBySender(sender: String): BrandIdentity? {
    val clean = sender.trim()
    return allBrands.firstOrNull { brand ->
      brand.knownSenderIds.any { it.equals(clean, ignoreCase = true) } ||
          brand.knownPhoneNumbers.any { normalizePhone(it) == normalizePhone(clean) }
    }
  }

  fun getAllBrands(): List<BrandIdentity> = allBrands

  private fun normalizePhone(num: String): String =
    num.filter { it.isDigit() || it == '+' }
}
