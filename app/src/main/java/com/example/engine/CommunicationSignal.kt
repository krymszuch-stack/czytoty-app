package com.example.engine

/**
 * Zunifikowany model sygnału komunikacyjnego (Call + SMS = ten sam silnik).
 * Pozwala na łączenie historii połączeń głosowych, wiadomości SMS oraz analiz linków
 * w jeden spójny profil reputacji i oceny ryzyka.
 */
enum class SignalChannel {
    CALL,
    SMS
}

enum class ReputationCategory(val displayName: String, val baseWeight: Float) {
    OK("Pozytywne / Oczekiwane", EngineConfig.ReputationWeights.OK_WEIGHT),
    ANNOYING("Uciążliwy kontakt", EngineConfig.ReputationWeights.ANNOYING_WEIGHT),
    TELEMARKETING("Oferta handlowa / Telemarketing", EngineConfig.ReputationWeights.TELEMARKETING_WEIGHT),
    BOT("Automat / Robocall", EngineConfig.ReputationWeights.BOT_WEIGHT),
    SUSPECTED_SCAM("Podejrzenie oszustwa", EngineConfig.ReputationWeights.SUSPECTED_SCAM_WEIGHT),
    IMPERSONATION("Podszywanie się pod markę/instytucję", EngineConfig.ReputationWeights.IMPERSONATION_WEIGHT),
    OTHER("Inne", EngineConfig.ReputationWeights.OTHER_WEIGHT)
}

data class CommunicationSignal(
    val id: String,
    val channel: SignalChannel,
    val phoneNumber: String,
    val category: ReputationCategory,
    val provenance: SignalProvenance,
    val subCategoryLabel: String? = null,
    // Sygnały specyficzne dla SMS / URL
    val hasBlikKeywords: Boolean = false,
    val hasUrgentPaymentKeywords: Boolean = false,
    val hasCredentialHarvesting: Boolean = false,
    val hasAppInstallKeywords: Boolean = false,
    val hasShortenerUrl: Boolean = false,
    val hasBrandMismatch: Boolean = false,
    val urlThreatScore: Int = 0, // 0..100
    // Sygnały specyficzne dla połączeń telefonicznych
    val callDurationSeconds: Int = 0,
    val isCallAnswered: Boolean = false,
    val isCallRejected: Boolean = false
) {
    val isHighSeverityThreat: Boolean
        get() = category == ReputationCategory.SUSPECTED_SCAM ||
                category == ReputationCategory.IMPERSONATION ||
                hasBlikKeywords ||
                hasBrandMismatch ||
                urlThreatScore >= 70

    val isNuisanceOnly: Boolean
        get() = (category == ReputationCategory.TELEMARKETING ||
                category == ReputationCategory.ANNOYING ||
                category == ReputationCategory.BOT) && !isHighSeverityThreat
}
