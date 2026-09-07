package com.example.sap

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SapCallerDao {

    @Query("SELECT * FROM sap_partners WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getPartnerByPhone(phoneNumber: String): SapPartnerEntity?

    @Query("SELECT * FROM sap_partners")
    fun getAllPartners(): Flow<List<SapPartnerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartners(partners: List<SapPartnerEntity>)

    @Query("DELETE FROM sap_partners")
    suspend fun clearPartners()
}
