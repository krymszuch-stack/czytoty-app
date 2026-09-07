package com.example.incall.model

import com.example.model.CallerInfo
import com.example.model.ConfidenceLevel

/**
 * Cykl życia i stany ekranu połączenia (Sekcja 91).
 */
enum class CallScreenState(val label: String) {
  INCOMING("Połączenie przychodzące"),
  RINGING("Dzwonienie..."),
  CONNECTING("Łączenie..."),
  ACTIVE("Połączono"),
  HELD("Połączenie zawieszone"),
  CONFERENCE("Konferencja"),
  ENDED("Połączenie zakończone")
}

/**
 * Dostępne trasy audio połączenia (Sekcje 93 i 94).
 */
enum class AudioRoute(val displayName: String) {
  EARPIECE("Słuchawka telefonu"),
  SPEAKER("Głośnomówiący"),
  BLUETOOTH("Słuchawki Bluetooth")
}

/**
 * Odczyt sensora zbliżeniowego (Sekcje 77 i 78).
 */
enum class ProximityState {
  FAR,
  NEAR
}

/**
 * Subtelny status i jakość sieci komórkowej / Wi-Fi (Sekcje 80, 81, 82, 95).
 */
data class NetworkStatusInfo(
  val networkType: String, // np. "5G", "LTE", "Wi-Fi Calling", "3G", "GSM"
  val signalBars: Int = 4, // 1..4 kreski
  val isWifiCalling: Boolean = false,
  val carrierName: String? = null
) {
  /**
   * Zwraca minimalistyczny tekst wskaźnika kresek (np. "▂▄▆█ 5G").
   */
  val visualIndicator: String
    get() {
      val bars = when (signalBars.coerceIn(1, 4)) {
        1 -> "▂"
        2 -> "▂▄"
        3 -> "▂▄▆"
        else -> "▂▄▆█"
      }
      return if (isWifiCalling) "Wi-Fi Calling $bars" else "$bars $networkType"
    }
}

/**
 * Informacje kontekstowe dla rozmówcy (Sekcje 84, 85, 86, 88, 89).
 */
data class InCallContextInfo(
  val lastSmsSnippet: String? = null,
  val lastSmsTimestamp: String? = null,
  val isSmsSensitive: Boolean = false,
  val lastCallTimestamp: String? = null,
  val organizationName: String? = null,
  val officialDomains: List<String> = emptyList(),
  val reputationNote: String? = null,
  val riskScore: Int = 0,
  val confidence: ConfidenceLevel = ConfidenceLevel.WYSOKA
)

/**
 * Stan UI aktywnego połączenia (InCallUiState).
 */
data class InCallUiState(
  val callState: CallScreenState = CallScreenState.ACTIVE,
  val caller: CallerInfo,
  val activeDurationSeconds: Long = 0L,
  val formattedDuration: String = "00:00",
  val audioRoute: AudioRoute = AudioRoute.EARPIECE,
  val isMuted: Boolean = false,
  val isKeypadOpen: Boolean = false,
  val dtmfSequence: String = "",
  val isExpandedContext: Boolean = false,
  val isProximityNear: Boolean = false,
  val isTouchLocked: Boolean = false,
  val networkStatus: NetworkStatusInfo? = NetworkStatusInfo(networkType = "5G", signalBars = 4),
  val contextInfo: InCallContextInfo? = null,
  val dynamicRiskWarning: String? = null
)
