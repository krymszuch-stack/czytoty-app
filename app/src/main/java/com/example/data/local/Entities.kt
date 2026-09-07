package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lokalny cache reputacji numeru (Offline-First)
 */
@Entity(tableName = "callers")
data class CallerEntity(
  @PrimaryKey val phoneNumber: String,
  val formattedNumber: String,
  val displayName: String?,
  val isDirectoryListed: Boolean,
  val registeredEntityName: String?,
  val entityCategory: String?,
  val isFinancialInstitution: Boolean,
  val isInboundOnlyHotline: Boolean,
  val officialSourceUrl: String?,
  val callPurpose: String?,
  val riskScore: Int,
  val confidence: String, // "WYSOKA", "SREDNIA", "NISKA"
  val category: String,
  val totalReports: Int,
  val reportsLast24h: Int,
  val reportsLastHour: Int,
  val firstSeen: String,
  val lastSeen: String,
  val isBlocked: Boolean,
  val riskReasonExplanation: String
)

/**
 * Historia połączeń zarejestrowana lokalnie przez aplikację lub CallScreeningService
 * (Bez konieczności żądania inwazyjnego uprawnienia READ_CALL_LOG).
 */
@Entity(tableName = "call_history")
data class CallRecordEntity(
  @PrimaryKey val id: String,
  val phoneNumber: String,
  val timestamp: String,
  val timestampEpoch: Long,
  val callType: String, // "INCOMING", "MISSED", "BLOCKED", "OUTGOING"
  val durationSeconds: Int,
  val userRating: Int?,
  val reportedCategory: String?,
  val isLocalCallVerified: Boolean
)

/**
 * Lokalne kolejkowanie zgłoszeń użytkownika (gotowe do synchronizacji z backendem przez WorkManager)
 */
@Entity(tableName = "pending_reports")
data class CrowdReportEntity(
  @PrimaryKey val id: String,
  val phoneNumber: String,
  val category: String,
  val timestampEpoch: Long,
  val rating: Int,
  val source: String,
  val isVerifiedCallReceivedOnDevice: Boolean,
  val reporterTrustWeight: Float,
  val isSyncedToRemote: Boolean
)

/**
 * Dziennik zdarzeń CallScreeningService (dla celów anty-abuse i weryfikacji otrzymania połączenia)
 */
@Entity(tableName = "screening_event_log")
data class ScreeningEventEntity(
  @PrimaryKey(autoGenerate = true) val eventId: Long = 0,
  val phoneNumber: String,
  val timestampEpoch: Long,
  val actionTaken: String // "ALLOWED", "BLOCKED", "SILENCED"
)

/**
 * Lokalna encja wątku konwersacji SMS
 */
@Entity(tableName = "sms_threads")
data class SmsThreadEntity(
  @PrimaryKey val threadId: String,
  val senderAddress: String,
  val displayName: String?,
  val recognizedBrandId: String?,
  val lastMessageSnippet: String,
  val lastMessageTimestamp: String,
  val lastMessageTimestampEpoch: Long,
  val unreadCount: Int,
  val highestRiskScore: Int,
  val highestSeverity: String,
  val hasOtp: Boolean,
  val isBlocked: Boolean
)

/**
 * Lokalna encja wiadomości SMS
 */
@Entity(tableName = "sms_messages")
data class SmsMessageEntity(
  @PrimaryKey val id: String,
  val threadId: String,
  val senderAddress: String,
  val body: String,
  val timestamp: String,
  val timestampEpoch: Long,
  val isOutgoing: Boolean,
  val isRead: Boolean,
  val hasOtp: Boolean,
  val otpCode: String?,
  val otpIssuer: String?,
  val otpType: String?,
  val detectedUrl: String?,
  val urlDomain: String?,
  val isRecognizedBrandDomain: Boolean,
  val isSuspiciousTld: Boolean,
  val isShortener: Boolean,
  val urlMismatchNote: String?,
  val riskScore: Int,
  val riskSeverity: String,
  val riskReasonsJson: String
)
