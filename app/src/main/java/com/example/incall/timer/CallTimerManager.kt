package com.example.incall.timer

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Menedżer pomiaru czasu aktywnego połączenia (Sekcja 83).
 *
 * Gwarantuje:
 * 1. Start dopiero w stanie ACTIVE (nie w momencie kliknięcia "Odbierz").
 * 2. Format MM:SS, a po godzinie HH:MM:SS.
 * 3. Niezmiennik czasu oparty o zegar monotoniczny (odporny na tło, blokadę ekranu, proximity i rotację).
 */
class CallTimerManager(
  private val scope: CoroutineScope,
  private val elapsedRealtimeProvider: () -> Long = { SystemClock.elapsedRealtime() }
) {

  private var activeStartEpoch: Long = 0L
  private var accumulatedSecondsBeforeHold: Long = 0L
  private var isRunning: Boolean = false
  private var timerJob: Job? = null

  private val _durationSeconds = MutableStateFlow(0L)
  val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

  private val _formattedDuration = MutableStateFlow("00:00")
  val formattedDuration: StateFlow<String> = _formattedDuration.asStateFlow()

  /**
   * Rozpoczyna pomiar czasu w stanie ACTIVE.
   */
  fun start() {
    activeStartEpoch = elapsedRealtimeProvider()
    accumulatedSecondsBeforeHold = 0L
    isRunning = true
    updateDuration()
    launchTicker()
  }

  /**
   * Wstrzymuje licznik (np. stan HELD / połączenie zawieszone).
   */
  fun pause() {
    if (!isRunning) return
    timerJob?.cancel()
    val elapsedSinceStart = (elapsedRealtimeProvider() - activeStartEpoch) / 1000L
    accumulatedSecondsBeforeHold += elapsedSinceStart.coerceAtLeast(0L)
    isRunning = false
    updateDuration()
  }

  /**
   * Wznawia pomiar czasu po odwieszeniu połączenia.
   */
  fun resume() {
    if (isRunning) return
    activeStartEpoch = elapsedRealtimeProvider()
    isRunning = true
    updateDuration()
    launchTicker()
  }

  /**
   * Zatrzymuje stoper i zwraca ostateczny czas trwania w sekundach.
   */
  fun stop(): Long {
    timerJob?.cancel()
    if (isRunning) {
      val elapsedSinceStart = (elapsedRealtimeProvider() - activeStartEpoch) / 1000L
      accumulatedSecondsBeforeHold += elapsedSinceStart.coerceAtLeast(0L)
      isRunning = false
    }
    updateDuration()
    return _durationSeconds.value
  }

  private fun launchTicker() {
    timerJob?.cancel()
    timerJob = scope.launch {
      while (isActive && isRunning) {
        delay(500)
        updateDuration()
      }
    }
  }

  private fun updateDuration() {
    val totalSeconds = if (isRunning) {
      val currentElapsed = (elapsedRealtimeProvider() - activeStartEpoch) / 1000L
      accumulatedSecondsBeforeHold + currentElapsed.coerceAtLeast(0L)
    } else {
      accumulatedSecondsBeforeHold
    }

    _durationSeconds.value = totalSeconds
    _formattedDuration.value = formatDuration(totalSeconds)
  }

  companion object {
    /**
     * Formatuje sekundy do postaci MM:SS lub HH:MM:SS (gdy czas >= 3600 s).
     */
    fun formatDuration(totalSeconds: Long): String {
      val hours = totalSeconds / 3600
      val minutes = (totalSeconds % 3600) / 60
      val seconds = totalSeconds % 60

      return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
      } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
      }
    }
  }
}
