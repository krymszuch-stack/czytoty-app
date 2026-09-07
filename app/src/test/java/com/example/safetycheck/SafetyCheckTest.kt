package com.example.safetycheck

import com.example.safetycheck.alert.SafetyAlertDispatcher
import com.example.safetycheck.detector.DistressSignalDetector
import com.example.safetycheck.location.SafetyLocationProvider
import com.example.safetycheck.manager.SafetyCheckManager
import com.example.safetycheck.model.DistressConfidence
import com.example.safetycheck.model.DistressSignalType
import com.example.safetycheck.model.LocationSnapshot
import com.example.safetycheck.model.PossibleDistressSignal
import com.example.safetycheck.model.SafetyCheckConfig
import com.example.safetycheck.model.SafetyCheckIncident
import com.example.safetycheck.model.SafetyCheckState
import com.example.safetycheck.model.TrustedContact
import com.example.safetycheck.model.UserSafetyResponse
import com.example.safetycheck.repository.SafetyCheckRepository
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
 * Zestaw testów jednostkowych modułu Safety Check / Tryb Pomocy.
 *
 * WERYFIKUJE:
 * 1. False-Positive rejection: TV/film, głośna muzyka, szczekanie psa, zabawa dzieci, siłownia, impreza, kłótnia bez słów pomocy.
 * 2. Detekcja realnego wołania o pomoc (HIGH/MEDIUM confidence, SUSTAINED_CRY_FOR_HELP).
 * 3. Odrzucenie pojedynczych impulsów dźwiękowych (np. trzask, upadek telefonu).
 * 4. 100% Opt-in gate: gdy isEnabled = false, brak aktywacji.
 * 5. Reakcja [Wszystko OK] anuluje odliczanie i nie wysyła powiadomień.
 * 6. Reakcja [Wyślij pomoc teraz] wysyła alert natychmiast.
 * 7. Timeout (brak odpowiedzi) powoduje automatyczną wysyłkę.
 * 8. Asynchroniczna lokalizacja: brak GPS nie blokuje wysyłki SMS.
 * 9. Niezmiennik językowy: zakaz zwrotów panicznych ("jest w niebezpieczeństwie", "został zaatakowany", "dzwoń na policję").
 * 10. Audyt prywatności: zero trwałego zapisu audio.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SafetyCheckTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // =========================================================================
    // 1. ODRZUCANIE FAŁSZYWYCH ALARMÓW (FALSE-POSITIVE SUPPRESSION)
    // =========================================================================

    @Test
    fun `tv action movie impulse explosion is rejected as false alarm`() {
        val result = DistressSignalDetector.evaluate(
            DistressSignalDetector.AudioEnvironmentContext(
                durationMs = 900L,
                rmsEnergy = 0.86f,
                peakEnergy = 0.94f,
                isSingleImpulse = true,
                isRhythmicMusic = false,
                isDogBarkingCadence = false,
                isChildPlayfulSpeech = false,
                transcriptText = ""
            )
        )
        assertNull("Pojedynczy wybuch / wystrzał z filmu TV nie może wyzwolić alarmu", result)
    }

    @Test
    fun `loud rhythmic dance music beat is rejected as false alarm`() {
        val result = DistressSignalDetector.evaluate(
            DistressSignalDetector.AudioEnvironmentContext(
                durationMs = 3500L,
                rmsEnergy = 0.88f,
                peakEnergy = 0.92f,
                isSingleImpulse = false,
                isRhythmicMusic = true,
                isDogBarkingCadence = false,
                isChildPlayfulSpeech = false,
                transcriptText = "la la la beat"
            )
        )
        assertNull("Głośna muzyka z rytmicznym bitem nie może wyzwolić alarmu", result)
    }

    @Test
    fun `dog barking is rejected as false alarm`() {
        val result = DistressSignalDetector.evaluate(
            DistressSignalDetector.AudioEnvironmentContext(
                durationMs = 2800L,
                rmsEnergy = 0.85f,
                peakEnergy = 0.91f,
                isSingleImpulse = false,
                isRhythmicMusic = false,
                isDogBarkingCadence = true,
                isChildPlayfulSpeech = false,
                transcriptText = ""
            )
        )
        assertNull("Szczekanie psa nie może wyzwolić alertu", result)
    }

    @Test
    fun `children playing and laughing loudly is rejected as false alarm`() {
        val result = DistressSignalDetector.evaluate(
            DistressSignalDetector.AudioEnvironmentContext(
                durationMs = 2600L,
                rmsEnergy = 0.84f,
                peakEnergy = 0.90f,
                isSingleImpulse = false,
                isRhythmicMusic = false,
                isDogBarkingCadence = false,
                isChildPlayfulSpeech = true,
                transcriptText = "berek złap mnie hahah"
            )
        )
        assertNull("Pisk i głośny śmiech dzieci podczas zabawy nie może wyzwolić alertu", result)
    }

    @Test
    fun `heated verbal argument without help keywords is suppressed`() {
        val result = DistressSignalDetector.evaluate(
            DistressSignalDetector.AudioEnvironmentContext(
                durationMs = 2600L,
                rmsEnergy = 0.79f,
                peakEnergy = 0.83f,
                isSingleImpulse = false,
                isRhythmicMusic = false,
                isDogBarkingCadence = false,
                isChildPlayfulSpeech = false,
                transcriptText = "nie zgadzam się, przestań gadać, bzdury"
            )
        )
        assertNull("Kłótnia bez słów wzywania pomocy nie może wywołać fałszywego alertu", result)
    }

    // =========================================================================
    // 2. DETEKCJA REALNYCH SYGNAŁÓW ZAGROŻENIA
    // =========================================================================

    @Test
    fun `repeated cries for help trigger HIGH confidence distress signal`() {
        val result = DistressSignalDetector.evaluate(
            DistressSignalDetector.AudioEnvironmentContext(
                durationMs = 2800L,
                rmsEnergy = 0.85f,
                peakEnergy = 0.92f,
                isSingleImpulse = false,
                isRhythmicMusic = false,
                isDogBarkingCadence = false,
                isChildPlayfulSpeech = false,
                transcriptText = "pomocy ratunku zostaw mnie"
            )
        )

        assertNotNull(result)
        assertEquals(DistressConfidence.HIGH, result?.confidence)
        assertEquals(DistressSignalType.SUSTAINED_CRY_FOR_HELP, result?.type)
        assertTrue(result!!.detectedKeywords.isNotEmpty())
    }

    @Test
    fun `sustained high energy screams trigger vocalization distress signal`() {
        val result = DistressSignalDetector.evaluate(
            DistressSignalDetector.AudioEnvironmentContext(
                durationMs = 3200L,
                rmsEnergy = 0.91f,
                peakEnergy = 0.96f,
                isSingleImpulse = false,
                isRhythmicMusic = false,
                isDogBarkingCadence = false,
                isChildPlayfulSpeech = false,
                transcriptText = ""
            )
        )

        assertNotNull(result)
        assertEquals(DistressConfidence.MEDIUM, result?.confidence)
        assertEquals(DistressSignalType.SUSTAINED_HIGH_ENERGY_VOCALIZATION, result?.type)
    }

    // =========================================================================
    // 3. 100% OPT-IN GATE & REPOSITORY
    // =========================================================================

    @Test
    fun `opt-in default is strictly disabled`() {
        val repo = SafetyCheckRepository()
        assertFalse("SafetyCheck musi być domyślnie wyłączony (100% opt-in)", repo.config.value.isEnabled)
    }

    @Test
    fun `disabled safety check completely ignores distress signals`() = testScope.runTest {
        val repo = SafetyCheckRepository()
        repo.updateConfig { it.copy(isEnabled = false) }

        val manager = SafetyCheckManager(
            repository = repo,
            dispatcher = testDispatcher
        )

        val signal = PossibleDistressSignal(
            type = DistressSignalType.SUSTAINED_CRY_FOR_HELP,
            confidence = DistressConfidence.HIGH,
            durationMs = 2500L,
            acousticScore = 0.85f,
            detectedKeywords = listOf("pomocy", "ratunku")
        )

        manager.triggerCheck(signal, this)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Moduł powinien pozostać w stanie Idle gdy isEnabled=false",
            SafetyCheckState.Idle, manager.state.value)
    }

    // =========================================================================
    // 4. PRZEBIEG SPRAWDZANIA: [WSZYSTKO OK] vs [WYŚLIJ POMOC] vs TIMEOUT
    // =========================================================================

    @Test
    fun `user acknowledging everything is OK cancels countdown and sends zero alerts`() = testScope.runTest {
        val repo = SafetyCheckRepository()
        repo.updateConfig {
            it.copy(
                isEnabled = true,
                countdownSeconds = 10,
                trustedContacts = listOf(TrustedContact("c1", "Anna", "+48600111222"))
            )
        }

        val manager = SafetyCheckManager(
            repository = repo,
            dispatcher = testDispatcher
        )

        val signal = PossibleDistressSignal(
            type = DistressSignalType.SUSTAINED_CRY_FOR_HELP,
            confidence = DistressConfidence.HIGH,
            durationMs = 2500L,
            acousticScore = 0.85f
        )

        manager.triggerCheck(signal, this)
        advanceTimeBy(3000L) // Użytkownik reaguje po 3 sekundach

        assertTrue(manager.state.value is SafetyCheckState.Triggered)

        manager.userAcknowledgedOk()

        assertEquals(SafetyCheckState.SafeConfirmed, manager.state.value)

        // Sprawdź log incydentu
        val incidents = repo.getIncidents()
        assertEquals(1, incidents.size)
        assertEquals(UserSafetyResponse.ACKNOWLEDGED_OK, incidents.first().userResponse)
        assertFalse("Nie wolno wysyłać alertu gdy użytkownik potwierdził bezpieczeństwo",
            incidents.first().whetherAlertWasSent)
    }

    @Test
    fun `user clicking send help now triggers immediate alert dispatch`() = testScope.runTest {
        val repo = SafetyCheckRepository()
        repo.updateConfig {
            it.copy(
                isEnabled = true,
                countdownSeconds = 10,
                trustedContacts = listOf(TrustedContact("c1", "Piotr", "+48600333444"))
            )
        }

        val manager = SafetyCheckManager(
            repository = repo,
            dispatcher = testDispatcher
        )

        val signal = PossibleDistressSignal(
            type = DistressSignalType.SUSTAINED_HIGH_ENERGY_VOCALIZATION,
            confidence = DistressConfidence.HIGH,
            durationMs = 3000L,
            acousticScore = 0.9f
        )

        manager.triggerCheck(signal, this)
        advanceTimeBy(1000L)

        manager.userRequestedHelpNow()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(manager.state.value is SafetyCheckState.AlertDispatched)
        val dispatched = manager.state.value as SafetyCheckState.AlertDispatched
        assertEquals(1, dispatched.recipientsCount)

        val incidents = repo.getIncidents()
        assertEquals(1, incidents.size)
        assertTrue(incidents.first().whetherAlertWasSent)
        assertEquals(UserSafetyResponse.MANUAL_HELP_REQUESTED, incidents.first().userResponse)
    }

    @Test
    fun `countdown timeout with no user response auto-dispatches alert`() = testScope.runTest {
        val repo = SafetyCheckRepository()
        repo.updateConfig {
            it.copy(
                isEnabled = true,
                countdownSeconds = 5,
                trustedContacts = listOf(TrustedContact("c1", "Kasia", "+48600555666"))
            )
        }

        val manager = SafetyCheckManager(
            repository = repo,
            dispatcher = testDispatcher
        )

        val signal = PossibleDistressSignal(
            type = DistressSignalType.SUSTAINED_CRY_FOR_HELP,
            confidence = DistressConfidence.HIGH,
            durationMs = 2500L,
            acousticScore = 0.85f
        )

        manager.triggerCheck(signal, this)

        // Odliczamy pełny czas (5 sekund + bufor)
        advanceTimeBy(6000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(manager.state.value is SafetyCheckState.AlertDispatched)
        val dispatched = manager.state.value as SafetyCheckState.AlertDispatched
        assertEquals(1, dispatched.recipientsCount)

        val incidents = repo.getIncidents()
        assertEquals(1, incidents.size)
        assertTrue(incidents.first().whetherAlertWasSent)
        assertEquals(UserSafetyResponse.TIMEOUT_NO_RESPONSE, incidents.first().userResponse)
    }

    // =========================================================================
    // 5. ASYNCHRONICZNA LOKALIZACJA & BRAK BLOKADY
    // =========================================================================

    @Test
    fun `missing location does not prevent alert dispatch`() = testScope.runTest {
        val repo = SafetyCheckRepository()
        repo.updateConfig {
            it.copy(
                isEnabled = true,
                includeLocation = true,
                countdownSeconds = 5,
                trustedContacts = listOf(TrustedContact("c1", "Mama", "+48600777888"))
            )
        }

        // Provider symulujący brak fixa GPS (zwraca null)
        val failingLocationProvider = object : SafetyLocationProvider {
            override suspend fun getCurrentLocation(timeoutMs: Long): LocationSnapshot? = null
        }

        val manager = SafetyCheckManager(
            repository = repo,
            locationProvider = failingLocationProvider,
            dispatcher = testDispatcher
        )

        val signal = PossibleDistressSignal(
            type = DistressSignalType.SUSTAINED_CRY_FOR_HELP,
            confidence = DistressConfidence.HIGH,
            durationMs = 2500L,
            acousticScore = 0.85f
        )

        manager.triggerCheck(signal, this)
        advanceTimeBy(6000L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Alert musi zostać wysłany nawet gdy GPS nie ma fixa",
            manager.state.value is SafetyCheckState.AlertDispatched)
    }

    // =========================================================================
    // 6. SPOKOJNY KOMUNIKAT & NIEZMIENNIKI BEZPIECZEŃSTWA (NO ALARMIST COPY)
    // =========================================================================

    @Test
    fun `alert message uses calm non-alarmist phrasing`() {
        val dispatcher = SafetyAlertDispatcher()
        val location = LocationSnapshot(52.2297, 21.0122, 15f)

        val message = dispatcher.buildAlertMessage(
            userName = "Jan",
            location = location,
            allowLocationForContact = true
        )

        assertTrue(message.contains("CzyToTy? Safety Check:"))
        assertTrue(message.contains("Możliwe, że Jan potrzebuje pomocy."))
        assertTrue(message.contains("https://maps.google.com/?q=52.2297,21.0122"))
        assertFalse("Komunikat NIGDY nie może zawierać 'jest w niebezpieczeństwie'",
            message.contains("jest w niebezpieczeństwie", ignoreCase = true))
        assertFalse("Komunikat NIGDY nie może zawierać 'został zaatakowany'",
            message.contains("został zaatakowany", ignoreCase = true))
        assertFalse("Komunikat NIGDY nie może zawierać 'dzwoń na policję'",
            message.contains("dzwoń na policję", ignoreCase = true))
    }

    // =========================================================================
    // 7. AUDYT PRYWATNOŚCI: ZERO ZAPISU AUDIO
    // =========================================================================

    @Test
    fun `privacy audit confirms zero audio data persistence`() {
        val repo = SafetyCheckRepository()
        repo.logIncident(
            SafetyCheckIncident(
                id = "inc_test",
                triggerTimestamp = System.currentTimeMillis(),
                signalType = DistressSignalType.SUSTAINED_CRY_FOR_HELP,
                confidence = DistressConfidence.HIGH,
                userResponse = UserSafetyResponse.ACKNOWLEDGED_OK,
                whetherAlertWasSent = false,
                locationAttached = false,
                recipientsCount = 0
            )
        )

        val incident = repo.getIncidents().first()
        // Weryfikacja: klasa modelu SafetyCheckIncident nie posiada pola dla próbek audio
        val fields = SafetyCheckIncident::class.java.declaredFields.map { it.name }
        assertFalse("Model incydentu nie może zawierać audio", fields.any { it.contains("audio", ignoreCase = true) })
        assertFalse("Model incydentu nie może zawierać próbek dźwięku", fields.any { it.contains("raw", ignoreCase = true) })
    }
}
