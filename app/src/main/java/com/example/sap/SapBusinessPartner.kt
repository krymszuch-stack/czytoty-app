package com.example.sap

/**
 * Model danych partnera biznesowego z SAP (odpowiada strukturze SAP S/4HANA API_BUSINESS_PARTNER).
 */
data class SapBusinessPartner(
    val businessPartnerId: String,
    val companyName: String,
    val contactPerson: String?,
    val phoneNumber: String,
    val department: String?,
    val isVerifiedPartner: Boolean = true,
    val trustScore: Int = 100,
    val sapSystemInstance: String = "SAP S/4HANA Cloud"
)
