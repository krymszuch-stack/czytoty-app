package com.example.model

/**
 * Model pojedynczej wiadomości SMS
 */
data class SmsMessage(
  val id: String,
  val threadId: String,
  val senderAddress: String, // numer telefonu np. "+48 732 123 456" lub Sender ID np. "InPost"
  val body: String,
  val timestamp: String,
  val timestampEpoch: Long = System.currentTimeMillis(),
  val isOutgoing: Boolean = false,
  val isRead: Boolean = true,
  val otpInfo: OtpCodeInfo? = null,
  val urlAnalysis: UrlSecurityAnalysis? = null,
  val riskScore: Int = 0,
  val riskSeverity: RiskSeverity = RiskSeverity.NO_KNOWN_RISK,
  val riskReasons: List<String> = emptyList()
)

/**
 * Wątek konwersacji SMS
 */
data class SmsThread(
  val threadId: String,
  val senderAddress: String,
  val displayName: String? = null,
  val recognizedBrand: BrandIdentity? = null,
  val lastMessageSnippet: String,
  val lastMessageTimestamp: String,
  val lastMessageTimestampEpoch: Long = System.currentTimeMillis(),
  val unreadCount: Int = 0,
  val totalMessagesCount: Int = 1,
  val highestRiskScore: Int = 0,
  val highestSeverity: RiskSeverity = RiskSeverity.NO_KNOWN_RISK,
  val hasOtp: Boolean = false,
  val isSpamOrBlocked: Boolean = false
)

/**
 * Informacja o wykrytym kodzie jednorazowym (OTP / kod weryfikacyjny)
 * Wykrywane całkowicie LOKALNIE heurystyką bez wysyłania tekstu na zewnątrz.
 */
data class OtpCodeInfo(
  val code: String,
  val issuerName: String?,
  val codeType: String = "Kod weryfikacyjny" // np. "Autoryzacja transakcji", "Logowanie"
)

/**
 * Wynik analizy linku zawartego w SMS
 */
data class UrlSecurityAnalysis(
  val fullUrl: String,
  val domain: String,
  val isRecognizedBrandDomain: Boolean,
  val isSuspiciousTld: Boolean,
  val isShortener: Boolean,
  val mismatchExplanation: String? = null,
  val isHighRisk: Boolean = false
)

/**
 * Wspólny profil nadawcy (łączący kanał PHONE i SMS)
 */
data class UnifiedSenderProfile(
  val address: String, // numer lub Sender ID
  val formattedAddress: String,
  val displayName: String?,
  val recognizedBrand: BrandIdentity?,
  val callsCount: Int,
  val lastCallTime: String?,
  val smsCount: Int,
  val lastSmsTime: String?,
  val reportsCount: Int,
  val suspiciousLinksCount: Int,
  val combinedRiskScore: Int,
  val confidence: ConfidenceLevel,
  val dataSources: List<String>,
  val riskFactors: List<String>,
  val isBlocked: Boolean
)
