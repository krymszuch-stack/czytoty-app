package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallerDao {

  @Query("SELECT * FROM callers WHERE phoneNumber = :phoneNumber LIMIT 1")
  suspend fun getCallerByNumber(phoneNumber: String): CallerEntity?

  @Query("SELECT * FROM callers")
  fun getAllCallers(): Flow<List<CallerEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateCaller(caller: CallerEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCallers(callers: List<CallerEntity>)

  @Query("UPDATE callers SET isBlocked = :isBlocked WHERE phoneNumber = :phoneNumber")
  suspend fun updateBlockedStatus(phoneNumber: String, isBlocked: Boolean)

  // Call history
  @Query("SELECT * FROM call_history ORDER BY timestampEpoch DESC")
  fun getRecentCalls(): Flow<List<CallRecordEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCallRecord(record: CallRecordEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCallRecords(records: List<CallRecordEntity>)

  @Query("UPDATE call_history SET userRating = :rating, reportedCategory = :category WHERE id = :callId")
  suspend fun updateCallRating(callId: String, rating: Int, category: String?)

  // Crowd Reports
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReport(report: CrowdReportEntity)

  @Query("SELECT * FROM pending_reports WHERE isSyncedToRemote = 0")
  suspend fun getUnsyncedReports(): List<CrowdReportEntity>

  @Query("UPDATE pending_reports SET isSyncedToRemote = 1 WHERE id = :reportId")
  suspend fun markReportSynced(reportId: String)

  // Screening events (Anti-abuse validation)
  @Insert
  suspend fun insertScreeningEvent(event: ScreeningEventEntity)

  @Query("SELECT COUNT(*) FROM screening_event_log WHERE phoneNumber = :phoneNumber AND timestampEpoch > :sinceEpoch")
  suspend fun countCallsFromNumberSince(phoneNumber: String, sinceEpoch: Long): Int

  @Query("SELECT COUNT(*) FROM pending_reports WHERE timestampEpoch > :sinceEpoch")
  suspend fun countReportsSince(sinceEpoch: Long): Int

  // SMS Threads
  @Query("SELECT * FROM sms_threads ORDER BY lastMessageTimestampEpoch DESC")
  fun getSmsThreads(): Flow<List<SmsThreadEntity>>

  @Query("SELECT * FROM sms_threads WHERE threadId = :threadId LIMIT 1")
  suspend fun getThreadById(threadId: String): SmsThreadEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSmsThread(thread: SmsThreadEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSmsThreads(threads: List<SmsThreadEntity>)

  @Query("UPDATE sms_threads SET unreadCount = 0 WHERE threadId = :threadId")
  suspend fun markThreadRead(threadId: String)

  // SMS Messages
  @Query("SELECT * FROM sms_messages WHERE threadId = :threadId ORDER BY timestampEpoch ASC")
  fun getMessagesForThread(threadId: String): Flow<List<SmsMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSmsMessage(message: SmsMessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSmsMessages(messages: List<SmsMessageEntity>)
}
