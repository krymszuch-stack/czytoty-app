package com.example.safetycheck.manager

import com.example.safetycheck.alert.SafetyAlertDispatcher
import com.example.safetycheck.location.SafetyLocationProvider
import com.example.safetycheck.location.SyntheticSafetyLocationProvider
import com.example.safetycheck.model.DistressConfidence
import com.example.safetycheck.model.LocationSnapshot
import com.example.safetycheck.model.PossibleDistressSignal
import com.example.safetycheck.model.SafetyCheckIncident
import com.example.safetycheck.model.SafetyCheckState
import com.example.safetycheck.model.UserSafetyResponse
import com.example.safetycheck.repository.SafetyCheckRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Główny koordynator cyklu życia modułu Safety Check (SafetyCheckManager).
 *
 * ZARZĄDZA:
 * 1. Sprawdzaniem warunku 100% Opt-in.
 * 2. Odliczaniem czasu na reakcję użytkownika (domyślnie 10 s).
 * 3. Asynchronicznym pobieraniem lokalizacji.
 * 4. Reakcją na akcję użytkownika: [Wszystko OK] lub [Wyślij pomoc].
 * 5. Automatyczną wysyłką po upływie czasu (Timeout / No response).
 */
class SafetyCheckManager(
    val repository: SafetyCheckRepository = SafetyCheckRepository(),
    private val locationProvider: SafetyLocationProvider = SyntheticSafetyLocationProvider(),
    private val alertDispatcher: SafetyAlertDispatcher = SafetyAlertDispatcher(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _state = MutableStateFlow<SafetyCheckState>(SafetyCheckState.Idle)
    val state: StateFlow<SafetyCheckState> = _state.asStateFlow()

    private var countdownJob: Job? = null
    private var currentLocationSnapshot: LocationSnapshot? = null
    private var activeDistressSignal: PossibleDistressSignal? = null

    /**
     * Wyzwala procedurę sprawdzania bezpieczeństwa.
     * Zgodnie z zasadą: działa TYLKO jeśli użytkownik włączył moduł i sygnał ma pewność >= MEDIUM.
     */
    fun triggerCheck(signal: PossibleDistressSignal, scope: CoroutineScope) {
        val config = repository.config.value
        if (!config.isEnabled) {
            return // 100% Opt-in: Nie uruchamiaj jeśli funkcja jest wyłączona
        }

        if (signal.confidence == DistressConfidence.LOW) {
            return // Ignoruj sygnały o niskiej pewności (eliminacja fałszywych alarmów)
        }

        countdownJob?.cancel()
        activeDistressSignal = signal
        currentLocationSnapshot = null

        // Asynchronicznie pobierz lokalizację w tle bez blokowania UI i odliczania
        if (config.includeLocation) {
            scope.launch(dispatcher) {
                currentLocationSnapshot = locationProvider.getCurrentLocation(timeoutMs = 2500L)
            }
        }

        countdownJob = scope.launch(dispatcher) {
            try {
                val totalSeconds = config.countdownSeconds.coerceIn(5, 30)
                for (sec in totalSeconds downTo 1) {
                    _state.value = SafetyCheckState.Triggered(
                        remainingSeconds = sec,
                        signal = signal
                    )
                    delay(1000L)
                }

                // Czas minął bez odpowiedzi użytkownika (NO RESPONSE)
                dispatchAutoAlertOnTimeout()

            } catch (_: CancellationException) {
                // Zadanie anulowane przez użytkownika
            } catch (e: Throwable) {
                _state.value = SafetyCheckState.Cancelled
            }
        }
    }

    /**
     * Użytkownik kliknął [ Wszystko OK ] -> Natychmiast zatrzymuje alarm.
     */
    fun userAcknowledgedOk() {
        countdownJob?.cancel()
        countdownJob = null

        activeDistressSignal?.let { sig ->
            repository.logIncident(
                SafetyCheckIncident(
                    id = "inc_${System.currentTimeMillis()}",
                    triggerTimestamp = sig.timestamp,
                    signalType = sig.type,
                    confidence = sig.confidence,
                    userResponse = UserSafetyResponse.ACKNOWLEDGED_OK,
                    whetherAlertWasSent = false,
                    locationAttached = false,
                    recipientsCount = 0
                )
            )
        }

        activeDistressSignal = null
        _state.value = SafetyCheckState.SafeConfirmed
    }

    /**
     * Użytkownik kliknął [ Wyślij pomoc ] -> Natychmiastowa wysyłka alertu.
     */
    fun userRequestedHelpNow() {
        countdownJob?.cancel()
        countdownJob = null
        sendAlert(UserSafetyResponse.MANUAL_HELP_REQUESTED)
    }

    /**
     * Anulowanie z dowolnego poziomu.
     */
    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null

        activeDistressSignal?.let { sig ->
            repository.logIncident(
                SafetyCheckIncident(
                    id = "inc_${System.currentTimeMillis()}",
                    triggerTimestamp = sig.timestamp,
                    signalType = sig.type,
                    confidence = sig.confidence,
                    userResponse = UserSafetyResponse.CANCELLED,
                    whetherAlertWasSent = false,
                    locationAttached = false,
                    recipientsCount = 0
                )
            )
        }

        activeDistressSignal = null
        _state.value = SafetyCheckState.Cancelled
    }

    fun resetToIdle() {
        cancel()
        _state.value = SafetyCheckState.Idle
    }

    private fun dispatchAutoAlertOnTimeout() {
        sendAlert(UserSafetyResponse.TIMEOUT_NO_RESPONSE)
    }

    private fun sendAlert(responseType: UserSafetyResponse) {
        val config = repository.config.value
        val result = alertDispatcher.dispatchAlert(
            config = config,
            location = currentLocationSnapshot
        )

        activeDistressSignal?.let { sig ->
            repository.logIncident(
                SafetyCheckIncident(
                    id = "inc_${System.currentTimeMillis()}",
                    triggerTimestamp = sig.timestamp,
                    signalType = sig.type,
                    confidence = sig.confidence,
                    userResponse = responseType,
                    whetherAlertWasSent = result.success,
                    locationAttached = currentLocationSnapshot != null,
                    recipientsCount = result.dispatchedRecipients.size
                )
            )
        }

        _state.value = SafetyCheckState.AlertDispatched(
            recipientsCount = result.dispatchedRecipients.size,
            message = result.formattedMessage
        )
    }
}
