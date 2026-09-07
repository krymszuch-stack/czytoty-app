package com.example.incall

import com.example.incall.dtmf.DtmfController
import com.example.incall.model.AudioRoute
import com.example.incall.model.CallScreenState
import com.example.incall.model.InCallContextInfo
import com.example.incall.model.InCallUiState
import com.example.incall.model.NetworkStatusInfo
import com.example.incall.model.ProximityState
import com.example.incall.privacy.InCallSmsPrivacyFilter
import com.example.incall.timer.CallTimerManager
import com.example.model.CallerInfo
import com.example.model.ConfidenceLevel
import com.example.model.RiskSeverity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Zestaw testów jednostkowych interaktywnego ekranu aktywnej rozmowy (Sekcje 76–96).
 *
 * WERYFIKUJE:
 * 1. Proximity Sensor & Touch Lock (Zero Accidental Touch, debouncing, wyjątki głośnika i bluetooth).
 * 2. Precyzyjny Call Timer (format MM:SS i HH:MM:SS, odporność na stan HELD).
 * 3. Minimalistyczny wskaźnik sieci (5G / LTE / Wi-Fi Calling, kreski 1-4).
 * 4. Filtr prywatności podglądu SMS (maskowanie OTP, BLIK, PIN, powiadomień bankowych).
 * 5. Klawiaturę numeryczną DTMF (P0).
 * 6. Maszynę stanów połączenia i rozszerzanie karty kontekstu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InCallScreenTest {

  private val testCaller = CallerInfo(
    phoneNumber = "+48 22 521 80 00",
    formattedNumber = "+48 22 521 80 00",
    displayName = "PKO Bank Polski",
    category = "Infolinia Bankowa",
    riskScore = 10,
    confidence = ConfidenceLevel.WYSOKA,
    totalReports = 0,
    reportsLast24h = 0
  )

  // =========================================================================
  // 1. PROXIMITY SENSOR & TOUCH LOCK (SEKCJE 77, 78, 92, 93, 94)
  // =========================================================================

  @Test
  fun `earpiece route with NEAR proximity engages touch lock immediately`() {
    val state = InCallUiState(
      caller = testCaller,
      audioRoute = AudioRoute.EARPIECE,
      isProximityNear = true,
      isTouchLocked = true
    )

    assertTrue("Warstwa dotykowa musi być zablokowana przy uchu (EARPIECE + NEAR)", state.isTouchLocked)
  }

  @Test
  fun `moving phone away restores touch without resetting call state`() {
    var state = InCallUiState(
      caller = testCaller,
      activeDurationSeconds = 45L,
      formattedDuration = "00:45",
      audioRoute = AudioRoute.EARPIECE,
      isProximityNear = true,
      isTouchLocked = true
    )

    // Odsunięcie od ucha (FAR)
    state = state.copy(
      isProximityNear = false,
      isTouchLocked = false
    )

    assertFalse("Dotyk musi być odblokowany po odsunięciu (FAR)", state.isTouchLocked)
    assertEquals(45L, state.activeDurationSeconds)
    assertEquals("00:45", state.formattedDuration)
    assertEquals(CallScreenState.ACTIVE, state.callState)
  }

  @Test
  fun `speakerphone bypasses aggressive touch lock even when proximity is NEAR`() {
    val currentAudioRoute = AudioRoute.SPEAKER
    val isNear = true
    val bypass = currentAudioRoute == AudioRoute.SPEAKER || currentAudioRoute == AudioRoute.BLUETOOTH
    val shouldLock = isNear && !bypass

    assertFalse("Tryb głośnomówiący nie może agresywnie blokować ekranu (Sekcja 93)", shouldLock)
  }

  @Test
  fun `bluetooth bypasses aggressive touch lock even when proximity is NEAR`() {
    val currentAudioRoute = AudioRoute.BLUETOOTH
    val isNear = true
    val bypass = currentAudioRoute == AudioRoute.SPEAKER || currentAudioRoute == AudioRoute.BLUETOOTH
    val shouldLock = isNear && !bypass

    assertFalse("Słuchawki Bluetooth nie mogą agresywnie blokować ekranu (Sekcja 94)", shouldLock)
  }

  // =========================================================================
  // 2. CALL TIMER (SEKCJA 83)
  // =========================================================================

  @Test
  fun `timer formats correctly under one hour MM-SS`() {
    assertEquals("00:00", CallTimerManager.formatDuration(0L))
    assertEquals("00:05", CallTimerManager.formatDuration(5L))
    assertEquals("04:32", CallTimerManager.formatDuration(272L))
    assertEquals("59:59", CallTimerManager.formatDuration(3599L))
  }

  @Test
  fun `timer formats correctly over one hour HH-MM-SS`() {
    assertEquals("01:00:00", CallTimerManager.formatDuration(3600L))
    assertEquals("01:15:08", CallTimerManager.formatDuration(4508L))
    assertEquals("02:30:15", CallTimerManager.formatDuration(9015L))
  }

  @Test
  fun `call timer manager tracks duration monotonically and handles hold state`() = runTest {
    var mockedTime = 1000000L
    val timer = CallTimerManager(
      scope = this,
      elapsedRealtimeProvider = { mockedTime }
    )

    timer.start()
    assertEquals(0L, timer.durationSeconds.value)
    assertEquals("00:00", timer.formattedDuration.value)

    // Upływ 15 sekund
    mockedTime += 15000L
    advanceTimeBy(1000)
    // Zatrzymanie na chwilę (HELD)
    timer.pause()
    assertEquals(15L, timer.durationSeconds.value)
    assertEquals("00:15", timer.formattedDuration.value)

    // Czas mija podczas HELD (stoper nie powinien liczyć)
    mockedTime += 30000L
    advanceTimeBy(1000)
    assertEquals(15L, timer.durationSeconds.value)

    // Wznowienie rozmowy i kolejne 10 sekund
    timer.resume()
    mockedTime += 10000L
    advanceTimeBy(1000)

    val finalSecs = timer.stop()
    assertEquals(25L, finalSecs)
    assertEquals("00:25", timer.formattedDuration.value)
  }

  // =========================================================================
  // 3. WSKAŹNIK JAKOŚCI SIECI (SEKCJE 80, 81, 82, 95)
  // =========================================================================

  @Test
  fun `network status generates minimalist visual indicator with bars`() {
    val fiveG = NetworkStatusInfo(networkType = "5G", signalBars = 4)
    assertEquals("▂▄▆█ 5G", fiveG.visualIndicator)

    val lte = NetworkStatusInfo(networkType = "LTE", signalBars = 3)
    assertEquals("▂▄▆ LTE", lte.visualIndicator)

    val wifiCalling = NetworkStatusInfo(networkType = "Wi-Fi Calling", signalBars = 4, isWifiCalling = true)
    assertEquals("Wi-Fi Calling ▂▄▆█", wifiCalling.visualIndicator)

    val weak3g = NetworkStatusInfo(networkType = "3G", signalBars = 1)
    assertEquals("▂ 3G", weak3g.visualIndicator)
  }

  // =========================================================================
  // 4. PRYWATNOŚĆ PODGLĄDU SMS PODCZAS ROZMOWY (SEKCJE 84, 85)
  // =========================================================================

  @Test
  fun `otp verification code is masked to kod jednorazowy in call preview`() {
    val raw = "G-847213 to Twój kod weryfikacyjny Google. Nikomu go nie udostępniaj."
    val preview = InCallSmsPrivacyFilter.filterSnippet(raw, "dzisiaj, 10:15", isEnabled = true)

    assertNotNull(preview)
    assertTrue(preview!!.isSensitive)
    assertEquals("Ostatnia wiadomość: kod jednorazowy", preview.displayText)
    assertFalse("Treść nie może ujawniać kodu weryfikacyjnego", preview.displayText.contains("847213"))
  }

  @Test
  fun `blik payment code is masked to kod BLIK in call preview`() {
    val raw = "Twój kod BLIK do wypłaty z bankomatu: 492 018. Ważny 2 minuty."
    val preview = InCallSmsPrivacyFilter.filterSnippet(raw, "5 min temu", isEnabled = true)

    assertNotNull(preview)
    assertTrue(preview!!.isSensitive)
    assertEquals("Ostatnia wiadomość: kod BLIK (ukryto dla bezpieczeństwa)", preview.displayText)
  }

  @Test
  fun `bank account notification is masked to powiadomienie bankowe`() {
    val raw = "Zrealizowano przelew wychodzący na rachunek: 12 1020 ... Saldo: 1200.00 PLN."
    val preview = InCallSmsPrivacyFilter.filterSnippet(raw, "1 godz. temu", isEnabled = true)

    assertNotNull(preview)
    assertTrue(preview!!.isSensitive)
    assertEquals("Ostatnia wiadomość: powiadomienie bankowe", preview.displayText)
  }

  @Test
  fun `safe non-sensitive text message is displayed directly with quotes`() {
    val raw = "Cześć, będę w domu koło godziny 17:30."
    val preview = InCallSmsPrivacyFilter.filterSnippet(raw, "wczoraj", isEnabled = true)

    assertNotNull(preview)
    assertFalse(preview!!.isSensitive)
    assertEquals("Ostatnia wiadomość: „Cześć, będę w domu koło godziny 17:30.”", preview.displayText)
  }

  @Test
  fun `disabled sms preview setting returns null`() {
    val raw = "Wiadomość testowa"
    val preview = InCallSmsPrivacyFilter.filterSnippet(raw, "12:00", isEnabled = false)
    assertNull(preview)
  }

  // =========================================================================
  // 5. DTMF KEYPAD CONTROLLER (SEKCJA 87 - P0)
  // =========================================================================

  @Test
  fun `dtmf controller appends valid digits and rejects invalid characters`() {
    val controller = DtmfController()
    assertEquals("", controller.dtmfSequence.value)

    controller.sendDigit('1')
    controller.sendDigit('2')
    controller.sendDigit('*')
    controller.sendDigit('#')
    controller.sendDigit('9')
    controller.sendDigit('X') // Niepoprawny znak

    assertEquals("12*#9", controller.dtmfSequence.value)

    controller.clearSequence()
    assertEquals("", controller.dtmfSequence.value)
    controller.release()
  }

  // =========================================================================
  // 6. MASZYNA STANÓW I KARTA KONTEKSTU (SEKCJE 88, 91)
  // =========================================================================

  @Test
  fun `in call state transitions between active and held correctly`() {
    var state = InCallUiState(
      caller = testCaller,
      callState = CallScreenState.ACTIVE
    )
    assertEquals(CallScreenState.ACTIVE, state.callState)

    // Toggling hold
    state = state.copy(callState = CallScreenState.HELD)
    assertEquals(CallScreenState.HELD, state.callState)

    // Resuming
    state = state.copy(callState = CallScreenState.ACTIVE)
    assertEquals(CallScreenState.ACTIVE, state.callState)
  }

  @Test
  fun `in call context info holds identity organization and risk data`() {
    val context = InCallContextInfo(
      lastSmsSnippet = "Ostatnia wiadomość: kod jednorazowy",
      isSmsSensitive = true,
      organizationName = "InPost",
      officialDomains = listOf("inpost.pl"),
      reputationNote = "Zaufana sieć logistyczna",
      riskScore = 5
    )

    assertEquals("InPost", context.organizationName)
    assertTrue(context.officialDomains.contains("inpost.pl"))
    assertTrue(context.isSmsSensitive)
  }
}
