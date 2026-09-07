package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    CallerEntity::class,
    CallRecordEntity::class,
    CrowdReportEntity::class,
    ScreeningEventEntity::class,
    SmsThreadEntity::class,
    SmsMessageEntity::class,
    com.example.sap.SapPartnerEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun callerDao(): CallerDao
  abstract fun sapCallerDao(): com.example.sap.SapCallerDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "caller_reputation.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
