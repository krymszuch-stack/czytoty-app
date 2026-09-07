package com.example.safetycheck.model

/**
 * Typy potencjalnych sygnałów stresu wykrywanych lokalnie przez moduł Safety Check.
 * Zgodnie z zasadą: NIE DIAGNOZOWAĆ "ataku paniki", "przemocy" ani "zagrożenia życia".
 */
enum class DistressSignalType(val displayName: String) {
    SUSTAINED_CRY_FOR_HELP("Powtarzające się wołanie o pomoc"),
    SUSTAINED_HIGH_ENERGY_VOCALIZATION("Utrzymujący się wysoki poziom wokalizacji"),
    PANIC_SEQUENCE("Ciąg nietypowych sygnałów głosowych"),
    UNKNOWN("Nierozpoznany sygnał dźwiękowy")
}

enum class DistressConfidence {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Zdarzenie wykrycia potencjalnego sygnału stresu.
 */
data class PossibleDistressSignal(
    val type: DistressSignalType,
    val confidence: DistressConfidence,
    val durationMs: Long,
    val acousticScore: Float, // 0.0 .. 1.0
    val detectedKeywords: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertChannel(val displayName: String) {
    SMS("Wiadomość SMS"),
    PUSH("Powiadomienie Push")
}

/**
 * Zaufany kontakt wybrany świadomie i wyłącznie przez użytkownika.
 */
data class TrustedContact(
    val id: String,
    val displayName: String,
    val phone: String,
    val preferredChannel: AlertChannel = AlertChannel.SMS,
    val allowLocation: Boolean = true
)

/**
 * Konfiguracja modułu Safety Check (w 100% OPT-IN).
 */
data class SafetyCheckConfig(
    val isEnabled: Boolean = false, // Domyślnie NIEAKTYWNE
    val userName: String = "Użytkownik",
    val trustedContacts: List<TrustedContact> = emptyList(),
    val isSmsAlertAllowed: Boolean = true,
    val isPushAlertAllowed: Boolean = false,
    val includeLocation: Boolean = true,
    val countdownSeconds: Int = 10 // Domyślnie 10 sekund (5..30)
)

enum class UserSafetyResponse {
    ACKNOWLEDGED_OK,
    MANUAL_HELP_REQUESTED,
    TIMEOUT_NO_RESPONSE,
    CANCELLED
}

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"
}

/**
 * Audyt zdarzenia bezpieczeństwa (Privacy-First: ZERO SUROWEGO AUDIO).
 */
data class SafetyCheckIncident(
    val id: String,
    val triggerTimestamp: Long,
    val signalType: DistressSignalType,
    val confidence: DistressConfidence,
    val userResponse: UserSafetyResponse,
    val whetherAlertWasSent: Boolean,
    val locationAttached: Boolean,
    val recipientsCount: Int
)

/**
 * Stan maszyny stanowej modułu Safety Check.
 */
sealed class SafetyCheckState {
    data object Idle : SafetyCheckState()
    data object Monitoring : SafetyCheckState()
    data class Triggered(
        val remainingSeconds: Int,
        val signal: PossibleDistressSignal
    ) : SafetyCheckState()
    data class AlertDispatched(
        val recipientsCount: Int,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SafetyCheckState()
    data object SafeConfirmed : SafetyCheckState()
    data object Cancelled : SafetyCheckState()
}
