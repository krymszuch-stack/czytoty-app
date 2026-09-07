package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.RiskElevated
import com.example.ui.theme.RiskElevatedContainer
import com.example.ui.theme.RiskElevatedOnContainer
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskHighContainer
import com.example.ui.theme.RiskHighOnContainer
import com.example.ui.theme.RiskModerate
import com.example.ui.theme.RiskModerateContainer
import com.example.ui.theme.RiskModerateOnContainer
import com.example.ui.theme.RiskSafe
import com.example.ui.theme.RiskSafeContainer
import com.example.ui.theme.RiskSafeOnContainer

/**
 * Poziom pewności danych (Confidence)
 * Oznacza ilość i spójność dostępnych sygnałów o numerze.
 * Nie łączyć z Risk Score!
 */
enum class ConfidenceLevel(val displayName: String, val description: String) {
  WYSOKA("Wysoka", "Duża liczba spójnych raportów lub oficjalny rejestr publiczny"),
  SREDNIA("Średnia", "Umiarkowana liczba raportów wymagająca ostrożności"),
  NISKA("Niska", "Nieliczne lub niejednoznaczne zgłoszenia")
}

/**
 * Poziom ryzyka (Risk Severity)
 * Oparty na deterministycznej analizie dostępnych sygnałów.
 * Zgodnie z wytycznymi: brak sygnałów ryzyka != numer bezpieczny.
 */
enum class RiskSeverity(
  val label: String,
  val shortLabel: String,
  val color: Color,
  val containerColor: Color,
  val onContainerColor: Color
) {
  NO_KNOWN_RISK(
    label = "Brak znanych sygnałów ryzyka",
    shortLabel = "Brak sygnałów",
    color = RiskSafe,
    containerColor = RiskSafeContainer,
    onContainerColor = RiskSafeOnContainer
  ),
  MODERATE(
    label = "Umiarkowane ryzyko",
    shortLabel = "Umiarkowane",
    color = RiskModerate,
    containerColor = RiskModerateContainer,
    onContainerColor = RiskModerateOnContainer
  ),
  ELEVATED(
    label = "Podwyższone ryzyko",
    shortLabel = "Podwyższone",
    color = RiskElevated,
    containerColor = RiskElevatedContainer,
    onContainerColor = RiskElevatedOnContainer
  ),
  HIGH(
    label = "Podejrzenie oszustwa",
    shortLabel = "Oszustwo",
    color = RiskHigh,
    containerColor = RiskHighContainer,
    onContainerColor = RiskHighOnContainer
  );

  companion object {
    fun fromScore(score: Int): RiskSeverity = when {
      score < 25 -> NO_KNOWN_RISK
      score < 50 -> MODERATE
      score < 75 -> ELEVATED
      else -> HIGH
    }
  }
}

/**
 * Standardowe kategorie zgłoszeń społecznościowych (MVP)
 * Zdefiniowane pod kątem charakteru połączenia (bez oskarżeń personalnych).
 */
enum class CallCategory(val displayName: String, val baseRiskWeight: Int) {
  SAFE_OR_EXPECTED("Oczekiwane / Neutralne", 0),
  TELEMARKETING("Telemarketing i oferty", 45),
  ROBOCALL("Automat / Bot telefoniczny", 60),
  DEBT_COLLECTION("Windykacja / Sprawy finansowe", 40),
  FRAUD_SUSPICION("Podejrzenie oszustwa (vishing)", 85),
  IMPERSONATION_BANK("Podszywanie się pod bank lub instytucję", 95),
  OTHER("Inny charakter", 20)
}

/**
 * Metadane dotyczące tożsamości numeru vs tożsamości rozmówcy.
 * Kluczowe dla rozwiązania paradoksu "Zweryfikowanego Banku":
 * Numer może być oficjalny, ale rozmówca może fałszować Caller ID (CLI spoofing).
 */
data class NumberIdentity(
  val isPublicDirectoryListed: Boolean = false,
  val registeredEntityName: String? = null,
  val entityCategory: String? = null,
  val isFinancialInstitution: Boolean = false,
  val isInboundOnlyHotline: Boolean = false,
  val officialSourceUrl: String? = null
)

/**
 * Pełna informacja o reputacji numeru
 */
data class CallerInfo(
  val phoneNumber: String,
  val formattedNumber: String,
  val displayName: String? = null,
  val numberIdentity: NumberIdentity? = null,
  val callPurpose: String? = null,
  val riskScore: Int,
  val confidence: ConfidenceLevel,
  val category: String,
  val totalReports: Int,
  val reportsLast24h: Int,
  val reportsLastHour: Int = 0,
  val activityTrend: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
  val firstSeen: String = "Brak danych",
  val lastSeen: String = "Dzisiaj",
  val isBlocked: Boolean = false,
  val subCategories: List<String> = emptyList(),
  val riskReasonExplanation: String = "Brak wystarczającej liczby zgłoszeń do analizy",
  val isVerifiedCallerIdentityPossible: Boolean = false, // Zawsze false dla standardowej telefonii GSM!
  val communityReputation: CommunityReputation? = null,
  val sapPartner: com.example.sap.SapBusinessPartner? = null
) {
  val severity: RiskSeverity
    get() = RiskSeverity.fromScore(riskScore)

  val isFinancialInstitution: Boolean
    get() = numberIdentity?.isFinancialInstitution == true

  val isOfficialDirectoryNumber: Boolean
    get() = numberIdentity?.isPublicDirectoryListed == true

  val isSapVerified: Boolean
    get() = sapPartner != null && sapPartner.isVerifiedPartner
}

enum class CallType {
  INCOMING,
  MISSED,
  BLOCKED,
  OUTGOING
}

data class CallRecord(
  val id: String,
  val caller: CallerInfo,
  val timestamp: String,
  val callType: CallType,
  val durationSeconds: Int = 0,
  val userRating: Int? = null,
  val quickRating: QuickRating? = null,
  val reportedCategory: String? = null,
  val isLocalCallVerified: Boolean = true
)

/**
 * Model zgłoszenia crowdsourcingowego (MVP)
 * Zawiera metadane wiarygodności, bez przechowywania prywatnych oskarżeń personalnych.
 */
data class CrowdReport(
  val id: String,
  val phoneNumber: String,
  val category: CallCategory,
  val timestamp: Long = System.currentTimeMillis(),
  val source: String = "Aplikacja mobilna (lokalne zgłoszenie)",
  val rating: Int, // 1 - 5
  val reporterTrustWeight: Float = 1.0f,
  val isVerifiedCallReceivedOnDevice: Boolean = true,
  val note: String? = null
)

data class ProtectionSettings(
  val isProtectionActive: Boolean = true,
  val blockHighRisk: Boolean = true,
  val blockTelemarketing: Boolean = false,
  val blockUnknown: Boolean = false,
  val blockThreshold: Float = 85f,
  val lastUpdatedTime: String = "Przed chwilą (lokalny cache)",
  val blockedTodayCount: Int = 1,
  val warningsTodayCount: Int = 4,
  val isCallScreeningRoleGranted: Boolean = false,
  val showSmsPreviewInCall: Boolean = true
)

data class UserProfile(
  val name: String = "Użytkownik",
  val initials: String = "U",
  val rank: String = "Uczestnik społeczności",
  val credibilityScore: Int = 85,
  val confirmedReports: Int = 12,
  val helpfulReports: Int = 8
)
