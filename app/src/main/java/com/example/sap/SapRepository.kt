package com.example.sap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repozytorium obsługujące integrację z SAP (SAP S/4HANA / SAP BTP).
 * Odpowiada za synchronizację bazy kontaktów biznesowych oraz natychmiastowe sprawdzanie
 * tożsamości rozmówcy podczas połączenia przychodzącego w CzyToTy?.
 */
class SapRepository(private val sapDao: SapCallerDao? = null) {

    // Wzorcowe dane kontrahentów SAP do weryfikacji tożsamości
    private val defaultSapPartners = listOf(
        SapBusinessPartner(
            businessPartnerId = "SAP-BP-100290",
            companyName = "SAP Polska Sp. z o.o.",
            contactPerson = "Centrum Wsparcia Klienta SAP",
            phoneNumber = "+48 22 541 67 00",
            department = "Wsparcie Techniczne Enterprise",
            trustScore = 100,
            sapSystemInstance = "SAP S/4HANA Cloud (EMEA)"
        ),
        SapBusinessPartner(
            businessPartnerId = "SAP-BP-204510",
            companyName = "PKO Bank Polski SA (SAP Integrated)",
            contactPerson = "Infolinia Korporacyjna B2B",
            phoneNumber = "+48 800 302 302",
            department = "Bankowość Transakcyjna",
            trustScore = 99,
            sapSystemInstance = "SAP Banking Services"
        ),
        SapBusinessPartner(
            businessPartnerId = "SAP-BP-308112",
            companyName = "DHL Express Polska (SAP Logistics)",
            contactPerson = "Dyspozytor Przesyłek Kurierskich",
            phoneNumber = "+48 42 634 51 00",
            department = "SAP Transportation Management (TM)",
            trustScore = 98,
            sapSystemInstance = "SAP S/4HANA Logistics"
        ),
        SapBusinessPartner(
            businessPartnerId = "SAP-BP-490219",
            companyName = "Orange Polska SA (SAP Billing)",
            contactPerson = "Dział Obsługi Klienta Biznesowego",
            phoneNumber = "+48 510 100 100",
            department = "SAP BRIM (Billing & Revenue)",
            trustScore = 97,
            sapSystemInstance = "SAP BRIM Production"
        )
    )

    /**
     * Błyskawiczna weryfikacja czy dany numer jest zarejestrowanym partnerem w systemie SAP.
     */
    suspend fun findSapPartner(phoneNumber: String): SapBusinessPartner? = withContext(Dispatchers.IO) {
        val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")

        // 1. Sprawdź lokalną bazę Room jeśli jest dostępna
        val fromDb = sapDao?.getPartnerByPhone(phoneNumber)
            ?: sapDao?.getPartnerByPhone(cleanNumber)

        if (fromDb != null) {
            return@withContext SapBusinessPartner(
                businessPartnerId = fromDb.businessPartnerId,
                companyName = fromDb.companyName,
                contactPerson = fromDb.contactPerson,
                phoneNumber = fromDb.phoneNumber,
                department = fromDb.department,
                isVerifiedPartner = fromDb.isVerifiedPartner,
                trustScore = fromDb.trustScore,
                sapSystemInstance = fromDb.sapSystemInstance
            )
        }

        // 2. Sprawdź pamięć podręczną defaultSapPartners
        defaultSapPartners.firstOrNull { partner ->
            val partnerClean = partner.phoneNumber.replace(" ", "").replace("-", "")
            partnerClean == cleanNumber || cleanNumber.endsWith(partnerClean.removePrefix("+48"))
        }
    }

    /**
     * Pobierz wszystkich partnerów SAP.
     */
    fun getAllPartners(): Flow<List<SapBusinessPartner>>? {
        return sapDao?.getAllPartners()?.map { list ->
            list.map {
                SapBusinessPartner(
                    businessPartnerId = it.businessPartnerId,
                    companyName = it.companyName,
                    contactPerson = it.contactPerson,
                    phoneNumber = it.phoneNumber,
                    department = it.department,
                    isVerifiedPartner = it.isVerifiedPartner,
                    trustScore = it.trustScore,
                    sapSystemInstance = it.sapSystemInstance
                )
            }
        }
    }

    /**
     * Wstępna inicjalizacja bazy SAP.
     */
    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        if (sapDao != null) {
            val entities = defaultSapPartners.map {
                SapPartnerEntity(
                    phoneNumber = it.phoneNumber,
                    businessPartnerId = it.businessPartnerId,
                    companyName = it.companyName,
                    contactPerson = it.contactPerson,
                    department = it.department,
                    isVerifiedPartner = it.isVerifiedPartner,
                    trustScore = it.trustScore,
                    sapSystemInstance = it.sapSystemInstance
                )
            }
            sapDao.insertPartners(entities)
        }
    }
}
