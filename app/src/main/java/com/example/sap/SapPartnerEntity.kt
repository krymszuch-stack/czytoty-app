package com.example.sap

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Encja lokalnego cache kontrahentów SAP w bazie Room.
 * Zapewnia natychmiastowe rozpoznanie numeru w <10ms podczas połączenia przychodzącego.
 */
@Entity(tableName = "sap_partners")
data class SapPartnerEntity(
    @PrimaryKey
    val phoneNumber: String,
    val businessPartnerId: String,
    val companyName: String,
    val contactPerson: String?,
    val department: String?,
    val isVerifiedPartner: Boolean = true,
    val trustScore: Int = 100,
    val sapSystemInstance: String = "SAP S/4HANA Cloud",
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
