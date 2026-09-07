package com.example.model

/**
 * Typy wiadomości SMS rozróżniane wizualnie w komunikatorze (Sekcja 44)
 */
enum class SmsMessageType(val displayName: String, val tagLabel: String?) {
  NORMAL("Zwykły SMS", null),
  OTP("Kod jednorazowy", "Kod SMS"),
  DELIVERY("Powiadomienie o przesyłce", "Przesyłka"),
  BANK_FINANCE("Bank i finanse", "Finanse"),
  SYSTEM("Komunikat operatora", "System"),
  PROMOTION("Promocja i oferta", "Promocja"),
  SUSPICIOUS("Podejrzenie oszustwa", "Podejrzane")
}

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
  val riskReasons: List<String> = emptyList(),
  val messageType: SmsMessageType = SmsMessageType.NORMAL,
  val deliveryStatus: String = "Dostarczono"
)

/**
 * Wątek konwersacji SMS w nowoczesnym messengerze (Sekcje 41, 50, 51)
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
  val isSpamOrBlocked: Boolean = false,
  // Nowe właściwości ergonomii messengera
  val isPinned: Boolean = false,
  val pinOrder: Int = 0,
  val isArchived: Boolean = false,
  val isMuted: Boolean = false,
  val categoryTag: String? = null,
  val isBrandVerified: Boolean = false
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
 * Stan dialogu weryfikacji linku (Link Safety Modal - Sekcja 48)
 */
data class LinkSafetyModalState(
  val analysis: UrlSecurityAnalysis,
  val isOfficialBrandDomain: Boolean,
  val verifiedBrandName: String? = null
)

/**
 * Wspólny profil nadawcy (łączący kanał PHONE i SMS - Sekcja 49)
 */
data class UnifiedSenderProfile(
  val address: String, // numer lub Sender ID
  val formattedAddress: String,
  val displayName: String?,
  val email: String? = null,
  val phoneNumbers: List<String> = emptyList(),
  val domains: List<String> = emptyList(),
  val reputationStatus: String = "Brak znanych zagrożeń",
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
