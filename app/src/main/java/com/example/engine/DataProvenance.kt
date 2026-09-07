package com.example.engine

/**
 * Pochodzenie i wiarygodność sygnału (Data Provenance).
 * Każdy sygnał w silniku wie skąd pochodzi, kiedy powstał i jaki posiada stopień zaufania.
 */
enum class SourceType(val displayName: String, val baseTrustFactor: Float) {
    COMMUNITY("Społeczność użytkowników", 0.6f),
    LOCAL_CONTACT("Lokalna książka kontaktów", 0.95f),
    PUBLIC_REGISTER("Rejestr publiczny (KRS/CEIDG/GUS)", 0.90f),
    OFFICIAL_DIRECTORY("Oficjalna infolinia podmiotu", 0.85f),
    BRAND_DIRECTORY("Zweryfikowany katalog marek", 0.95f),
    SECURITY_FEED("Zewnętrzny strumień bezpieczeństwa (CERT/CSIRT)", 0.95f),
    CALL_EVENT("Zdarzenie połączenia na urządzeniu", 0.80f),
    SMS_EVENT("Zdarzenie wiadomości SMS na urządzeniu", 0.80f),
    MANUAL_LOOKUP("Ręczne zapytanie użytkownika", 0.50f),
    CONVERSATION_ANALYSIS("Analiza bezpieczeństwa rozmowy (Lokalna)", 0.85f)
}

enum class VerificationLevel(val levelScore: Float) {
    UNVERIFIED(0.2f),
    COMMUNITY_CONSENSUS(0.6f),
    OPERATOR_CONFIRMED(0.85f),
    OFFICIAL_PUBLIC_REGISTRY(0.95f),
    CRYPTOGRAPHICALLY_VERIFIED(1.0f)
}

data class SignalProvenance(
    val sourceType: SourceType,
    val sourceId: String,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val reporterTrustScore: Float = 0.5f, // 0.0 .. 1.0
    val verificationLevel: VerificationLevel = VerificationLevel.UNVERIFIED,
    val isVerifiedInteractionOnDevice: Boolean = false
) {
    fun ageDays(currentEpoch: Long = System.currentTimeMillis()): Int {
        val diff = (currentEpoch - timestampEpoch).coerceAtLeast(0)
        return (diff / (1000L * 60L * 60L * 24L)).toInt()
    }

    fun ageHours(currentEpoch: Long = System.currentTimeMillis()): Int {
        val diff = (currentEpoch - timestampEpoch).coerceAtLeast(0)
        return (diff / (1000L * 60L * 60L)).toInt()
    }
}
