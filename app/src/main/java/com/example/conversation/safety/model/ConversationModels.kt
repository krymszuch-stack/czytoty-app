package com.example.conversation.safety.model

import com.example.engine.ConfidenceAssessment
import com.example.engine.EngineConfig
import com.example.engine.RiskLevel

/**
 * Znormalizowane kody sygnałów wykrywanych w treści rozmowy.
 */
enum class SafetySignalCode(val label: String, val baseWeight: Int) {
    REQUEST_BLIK("Prośba o kod BLIK", 90),
    REQUEST_OTP("Prośba o jednorazowy kod SMS / OTP", 85),
    REQUEST_PASSWORD("Prośba o hasło lub login", 90),
    REQUEST_CARD_DATA("Prośba o dane karty płatniczej", 85),
    REQUEST_PESEL("Prośba o podanie numeru PESEL", 60),
    REMOTE_ACCESS_REQUEST("Żądanie instalacji oprogramowania zdalnego dostępu (np. AnyDesk)", 95),
    INSTALL_APP_REQUEST("Prośba o instalację zewnętrznej aplikacji", 75),
    TECHNICAL_ACCOUNT("Żądanie przelewu na tzw. 'konto techniczne / rezerwowe'", 95),
    URGENT_TRANSFER("Presja na natychmiastowe wykonanie przelewu", 80),
    INVESTMENT_PRESSURE("Presja na szybkie inwestycje (krypto / giełda)", 75),
    BANK_IMPERSONATION("Podejrzenie podszywania się pod pracownika banku", 70),
    POLICE_IMPERSONATION("Podejrzenie podszywania się pod policję lub służby", 75),
    COURIER_PAYMENT("Żądanie dopłaty do przesyłki kurierskiej", 50),
    ACCOUNT_BLOCK_THREAT("Groźba natychmiastowej blokady konta lub środków", 80),
    TIME_PRESSURE("Wywieranie presji czasu na rozmówcy", 60),
    SECRECY_REQUEST("Żądanie zachowania rozmowy w tajemnicy przed bliskimi/pracownikami banku", 85),
    UNKNOWN("Nierozpoznany sygnał", 0)
}

enum class TranscriptStatus {
    SUCCESS,
    UNSUPPORTED,
    NO_SPEECH,
    ERROR,
    CANCELLED
}

data class TranscriptSegment(
    val text: String,
    val startMs: Long = 0,
    val endMs: Long = 0,
    val confidence: Float = 1.0f
)

data class TranscriptResult(
    val text: String,
    val segments: List<TranscriptSegment> = emptyList(),
    val language: String = "pl",
    val confidence: Float = 1.0f,
    val durationMs: Long = 10000L,
    val status: TranscriptStatus = TranscriptStatus.SUCCESS
)

enum class EstimatedSpeakers(val label: String) {
    ONE("1"),
    TWO("2"),
    THREE_PLUS("3+"),
    UNKNOWN("UNKNOWN")
}

enum class SpeakerConfidence {
    LOW,
    MEDIUM,
    HIGH
}

data class SpeakerEstimate(
    val estimatedSpeakers: EstimatedSpeakers,
    val confidence: SpeakerConfidence,
    val explanation: String
)

data class DetectedSignal(
    val code: SafetySignalCode,
    val rawMatchedPhrase: String,
    val weight: Int,
    val confidence: Float,
    val explanation: String
)

data class ConversationRiskResult(
    val riskScore: Int, // 0..100
    val riskLevel: RiskLevel,
    val confidence: ConfidenceAssessment,
    val signals: List<DetectedSignal>,
    val reasons: List<String>,
    val engineVersion: String = EngineConfig.ENGINE_VERSION,
    val processingTimeMs: Long = 0
)

data class ConversationSafetySignal(
    val callerNumber: String,
    val riskResult: ConversationRiskResult,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ConversationAnalysisState {
    data object Idle : ConversationAnalysisState()
    data class Listening(val remainingSeconds: Int) : ConversationAnalysisState()
    data object Processing : ConversationAnalysisState()
    data class Completed(
        val result: ConversationRiskResult,
        val redactedTranscript: String
    ) : ConversationAnalysisState()
    data class Unsupported(val message: String) : ConversationAnalysisState()
    data class Error(val message: String) : ConversationAnalysisState()
    data object Cancelled : ConversationAnalysisState()
}
