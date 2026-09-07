package com.example.engine

import com.example.model.ContactEntityType
import com.example.model.NumberIdentity
import com.example.model.UnifiedContact
import com.example.sap.SapBusinessPartner

/**
 * Wynik analizy tożsamości numeru / nadawcy (Filar 1: IDENTITY).
 * Odpowiada na pytanie: "Co wiemy o numerze / nadawcy?"
 */
data class IdentityAssessment(
    val phoneNumber: String,
    val formattedNumber: String,
    val entityType: ContactEntityType,
    val displayName: String?,
    val organizationName: String?,
    val category: String?,
    val isFinancialInstitution: Boolean,
    val isInboundOnlyHotline: Boolean,
    val isPublicDirectoryListed: Boolean,
    val officialSourceUrl: String?,
    /**
     * Krytyczna reguła architektury:
     * Czy tożsamość dzwoniącego rozmówcy jest potwierdzona?
     * ZAWSZE false w standardowej sieci GSM (ryzyko CLI Spoofingu),
     * chyba że nastąpił kryptograficzny handshake lub weryfikacja w SAP.
     */
    val isCallerIdentityAuthenticated: Boolean,
    val callerAuthenticationNote: String,
    val sapPartner: SapBusinessPartner?,
    val localContact: UnifiedContact?
)

object IdentityEngine {

    fun resolveIdentity(
        phoneNumber: String,
        numberIdentity: NumberIdentity? = null,
        sapPartner: SapBusinessPartner? = null,
        localContact: UnifiedContact? = null
    ): IdentityAssessment {
        val clean = phoneNumber.trim()

        // 1. Priorytet prezentacyjny: Lokalny kontakt użytkownika
        if (localContact != null) {
            return IdentityAssessment(
                phoneNumber = clean,
                formattedNumber = clean,
                entityType = localContact.entityType,
                displayName = localContact.primaryDisplayName,
                organizationName = if (localContact.entityType == ContactEntityType.ORGANIZATION) localContact.primaryDisplayName else null,
                category = localContact.organizationCategory?.displayName ?: localContact.relationship?.label,
                isFinancialInstitution = numberIdentity?.isFinancialInstitution == true,
                isInboundOnlyHotline = numberIdentity?.isInboundOnlyHotline == true,
                isPublicDirectoryListed = numberIdentity?.isPublicDirectoryListed == true,
                officialSourceUrl = numberIdentity?.officialSourceUrl,
                isCallerIdentityAuthenticated = false, // Nawet zapisany numer może być spoofowany
                callerAuthenticationNote = "Kontakt z Twojej lokalnej książki telefonicznej.",
                sapPartner = sapPartner,
                localContact = localContact
            )
        }

        // 2. Partner korporacyjny SAP
        if (sapPartner != null) {
            return IdentityAssessment(
                phoneNumber = clean,
                formattedNumber = sapPartner.phoneNumber,
                entityType = ContactEntityType.ORGANIZATION,
                displayName = sapPartner.companyName,
                organizationName = sapPartner.companyName,
                category = sapPartner.department ?: "Partner Biznesowy SAP",
                isFinancialInstitution = false,
                isInboundOnlyHotline = false,
                isPublicDirectoryListed = true,
                officialSourceUrl = "https://www.sap.com",
                isCallerIdentityAuthenticated = true, // Zweryfikowana kartoteka Enterprise
                callerAuthenticationNote = "Numer zweryfikowany bezpośrednio w rejestrze korporacyjnym SAP S/4HANA.",
                sapPartner = sapPartner,
                localContact = null
            )
        }

        // 3. Oficjalny podmiot z rejestru publicznego
        if (numberIdentity?.isPublicDirectoryListed == true) {
            val isBank = numberIdentity.isFinancialInstitution
            val isInboundHotline = numberIdentity.isInboundOnlyHotline

            val authNote = when {
                isInboundHotline -> "Numer to oficjalna infolinia przychodząca. Banki NIGDY nie dzwonią z numeru infolinii przychodzącej!"
                isBank -> "Numer należy do instytucji finansowej, lecz Caller ID może zostać sfałszowany w technologii GSM."
                else -> "Numer figuruje w oficjalnym wykazie publicznym."
            }

            return IdentityAssessment(
                phoneNumber = clean,
                formattedNumber = clean,
                entityType = ContactEntityType.ORGANIZATION,
                displayName = numberIdentity.registeredEntityName,
                organizationName = numberIdentity.registeredEntityName,
                category = numberIdentity.entityCategory ?: "Instytucja",
                isFinancialInstitution = isBank,
                isInboundOnlyHotline = isInboundHotline,
                isPublicDirectoryListed = true,
                officialSourceUrl = numberIdentity.officialSourceUrl,
                isCallerIdentityAuthenticated = false, // Ważne: znany numer != potwierdzony rozmówca!
                callerAuthenticationNote = authNote,
                sapPartner = null,
                localContact = null
            )
        }

        // 4. Podmiot nieznany
        return IdentityAssessment(
            phoneNumber = clean,
            formattedNumber = clean,
            entityType = ContactEntityType.PERSON,
            displayName = null,
            organizationName = null,
            category = null,
            isFinancialInstitution = false,
            isInboundOnlyHotline = false,
            isPublicDirectoryListed = false,
            officialSourceUrl = null,
            isCallerIdentityAuthenticated = false,
            callerAuthenticationNote = "Brak wpisu w oficjalnych rejestrach tożsamości.",
            sapPartner = null,
            localContact = null
        )
    }
}
