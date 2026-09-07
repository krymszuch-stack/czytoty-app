package com.example.conversation.safety

import com.example.conversation.safety.analyzer.ConversationRiskAnalyzer
import com.example.conversation.safety.audio.AudioSampleBuffer
import com.example.conversation.safety.diarization.SpeakerEstimator
import com.example.conversation.safety.extractor.SafetySignalExtractor
import com.example.conversation.safety.model.ConversationSafetySignal
import com.example.conversation.safety.model.EstimatedSpeakers
import com.example.conversation.safety.model.SafetySignalCode
import com.example.conversation.safety.model.SpeakerConfidence
import com.example.conversation.safety.model.SpeakerEstimate
import com.example.conversation.safety.model.TranscriptResult
import com.example.conversation.safety.model.TranscriptStatus
import com.example.conversation.safety.redaction.TranscriptRedactor
import com.example.conversation.safety.storage.EncryptedTranscriptStorage
import com.example.engine.CommunicationSignal
import com.example.engine.EngineReasonCode
import com.example.engine.IdentityAssessment
import com.example.engine.IdentityEngine
import com.example.engine.ReputationAssessment
import com.example.engine.ReputationCategory
import com.example.engine.ReputationEngineV1
import com.example.engine.RiskEngineV1
import com.example.engine.RiskLevel
import com.example.engine.SignalChannel
import com.example.engine.SignalProvenance
import com.example.engine.SourceType
import com.example.model.ConfidenceLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kompletny zestaw testów modułu Conversation Safety Snapshot (Test Cases A–F + Privacy Invariants).
 */
class ConversationSafetyTest {

    // =========================================================================
    // 1. OFICJALNE SCENARIUSZE TESTOWE (TEST CASES A – F)
    // =========================================================================

    @Test
    fun `test case A - courier delivery notification is LOW RISK and has no threat signals`() {
        val transcript = "Dzień dobry, kurier, będę za pięć minut."
        val signals = SafetySignalExtractor.extractSignals(transcript)

        assertTrue("Kurier bez żądania dopłaty nie powinien generować sygnałów zagrożenia", signals.isEmpty())

        val transcriptResult = TranscriptResult(text = transcript, confidence = 0.95f)
        val speakerEstimate = SpeakerEstimator.estimateSpeakers(null, transcriptResult)
        val riskResult = ConversationRiskAnalyzer.analyze(transcriptResult, speakerEstimate, signals)

        assertEquals("Oczekiwano LOW RISK dla komunikatu kuriera", RiskLevel.LOW, riskResult.riskLevel)
        assertTrue("Wynik ryzyka powinien być niski (<= 20)", riskResult.riskScore <= 20)
        assertEquals("Powinien zawierać obiektywne wyjaśnienie Safe Case",
            ConversationRiskAnalyzer.SAFE_CASE_REASON, riskResult.reasons.first())
        assertFalse("NIGDY nie wolno twierdzić 'Rozmowa jest bezpieczna'",
            riskResult.reasons.any { it.contains("Rozmowa jest bezpieczna") })
    }

    @Test
    fun `test case B - request for BLIK code is HIGH RISK and extracts REQUEST_BLIK`() {
        val transcript = "Podaj proszę kod BLIK."
        val signals = SafetySignalExtractor.extractSignals(transcript)

        assertTrue("Musi wykryć sygnał REQUEST_BLIK", signals.any { it.code == SafetySignalCode.REQUEST_BLIK })

        val transcriptResult = TranscriptResult(text = transcript, confidence = 0.95f)
        val speakerEstimate = SpeakerEstimator.estimateSpeakers(null, transcriptResult)
        val riskResult = ConversationRiskAnalyzer.analyze(transcriptResult, speakerEstimate, signals)

        assertEquals("Oczekiwano HIGH RISK dla prośby o kod BLIK", RiskLevel.HIGH, riskResult.riskLevel)
        assertTrue("Wynik ryzyka dla wyłudzenia BLIK powinien wynosić co najmniej 90", riskResult.riskScore >= 90)
        assertEquals(ConfidenceLevel.WYSOKA, riskResult.confidence.level)
        assertFalse("NIGDY nie wolno stosować oskarżycielskiego hasła 'ROZMAWIASZ Z OSZUSTEM'",
            riskResult.reasons.any { it.contains("ROZMAWIASZ Z OSZUSTEM") })
    }

    @Test
    fun `test case C - bank warning with negation NEVER triggers REQUEST_BLIK`() {
        // Nadrzędny wymóg: "Bank nigdy nie poprosi Cię o kod BLIK." -> LOW/MEDIUM, NOT REQUEST_BLIK
        val transcript = "Bank nigdy nie poprosi Cię o kod BLIK."
        val signals = SafetySignalExtractor.extractSignals(transcript)

        assertFalse(
            "Ekstraktor NIE MOŻE wygenerować sygnału REQUEST_BLIK, gdy w zdaniu występuje negacja/ostrzeżenie",
            signals.any { it.code == SafetySignalCode.REQUEST_BLIK }
        )

        val transcriptResult = TranscriptResult(text = transcript, confidence = 0.95f)
        val speakerEstimate = SpeakerEstimator.estimateSpeakers(null, transcriptResult)
        val riskResult = ConversationRiskAnalyzer.analyze(transcriptResult, speakerEstimate, signals)

        assertTrue("Oczekiwano niskiego/umiarkowanego ryzyka dla ostrzeżenia bankowego", riskResult.riskScore <= 35)
        assertEquals(RiskLevel.LOW, riskResult.riskLevel)
    }

    @Test
    fun `test case D - anydesk install remote access is VERY HIGH risk`() {
        val transcript = "Zainstaluj AnyDesk, pomogę zabezpieczyć konto."
        val signals = SafetySignalExtractor.extractSignals(transcript)

        assertTrue("Musi wykryć sygnał REMOTE_ACCESS_REQUEST",
            signals.any { it.code == SafetySignalCode.REMOTE_ACCESS_REQUEST })
        assertTrue("Musi wykryć sygnał INSTALL_APP_REQUEST",
            signals.any { it.code == SafetySignalCode.INSTALL_APP_REQUEST })

        val transcriptResult = TranscriptResult(text = transcript, confidence = 0.95f)
        val speakerEstimate = SpeakerEstimator.estimateSpeakers(null, transcriptResult)
        val riskResult = ConversationRiskAnalyzer.analyze(transcriptResult, speakerEstimate, signals)

        assertEquals("Oczekiwano poziomu HIGH dla próby instalacji zdalnego pulpitu", RiskLevel.HIGH, riskResult.riskLevel)
        assertTrue("Wynik ryzyka AnyDesk musi wynosić >= 95", riskResult.riskScore >= 95)
    }

    @Test
    fun `test case E - technical account transfer request is VERY HIGH risk`() {
        val transcript = "Proszę przelać środki na konto techniczne."
        val signals = SafetySignalExtractor.extractSignals(transcript)

        assertTrue("Musi wykryć sygnał TECHNICAL_ACCOUNT",
            signals.any { it.code == SafetySignalCode.TECHNICAL_ACCOUNT })
        assertTrue("Musi wykryć sygnał URGENT_TRANSFER",
            signals.any { it.code == SafetySignalCode.URGENT_TRANSFER })

        val transcriptResult = TranscriptResult(text = transcript, confidence = 0.95f)
        val speakerEstimate = SpeakerEstimator.estimateSpeakers(null, transcriptResult)
        val riskResult = ConversationRiskAnalyzer.analyze(transcriptResult, speakerEstimate, signals)

        assertEquals("Oczekiwano poziomu HIGH dla zlecenia przelewu na konto techniczne", RiskLevel.HIGH, riskResult.riskLevel)
        assertTrue("Wynik ryzyka dla konta technicznego musi wynosić >= 95", riskResult.riskScore >= 95)
    }

    @Test
    fun `test case F - unintelligible noise returns UNKNOWN and LOW CONFIDENCE`() {
        val transcriptResult = TranscriptResult(
            text = "",
            confidence = 0.10f,
            status = TranscriptStatus.NO_SPEECH
        )
        val speakerEstimate = SpeakerEstimator.estimateSpeakers(null, transcriptResult)
        val riskResult = ConversationRiskAnalyzer.analyze(transcriptResult, speakerEstimate, emptyList())

        assertEquals("Dla hałasu oczekiwano niskiej pewności (LOW CONFIDENCE)",
            ConfidenceLevel.NISKA, riskResult.confidence.level)
        assertEquals("Brak mowy nie może fałszywie generować wysokiego ryzyka", 0, riskResult.riskScore)
        assertEquals(EstimatedSpeakers.UNKNOWN, speakerEstimate.estimatedSpeakers)
    }

    // =========================================================================
    // 2. TESTY REDAKCJI DANYCH WRAŻLIWYCH (SENSITIVE DATA REDACTION)
    // =========================================================================

    @Test
    fun `redactor masks 6-digit BLIK code to REDACTED`() {
        val raw = "mój kod BLIK to 123456 podaj go szybko"
        val redacted = TranscriptRedactor.redact(raw)

        assertFalse("Surowy kod BLIK nie może pozostać w transkrypcji", redacted.contains("123456"))
        assertTrue("Musi zawierać znacznik [REDACTED]", redacted.contains("[REDACTED]"))
    }

    @Test
    fun `redactor masks 16-digit credit card number`() {
        val raw = "numer karty to 4123 4567 8901 2345 proszę zapisać"
        val redacted = TranscriptRedactor.redact(raw)

        assertFalse("Karta płatnicza nie może być widoczna", redacted.contains("4123"))
        assertTrue(redacted.contains("[REDACTED]"))
    }

    @Test
    fun `redactor masks PESEL and IBAN numbers`() {
        val raw = "PESEL: 90010112345 oraz numer konta PL12102030400000123456789012"
        val redacted = TranscriptRedactor.redact(raw)

        assertFalse(redacted.contains("90010112345"))
        assertFalse(redacted.contains("12102030400000123456789012"))
        assertTrue(redacted.contains("[REDACTED]"))
    }

    @Test
    fun `redactor masks passwords and otp codes`() {
        val raw = "twoje hasło: TajneHaslo123 a kod sms: 987654"
        val redacted = TranscriptRedactor.redact(raw)

        assertFalse(redacted.contains("TajneHaslo123"))
        assertFalse(redacted.contains("987654"))
        assertTrue(redacted.contains("[REDACTED]"))
    }

    // =========================================================================
    // 3. ZERO-STORAGE AUDIO BUFFER & RAM WIPE INVARIANT
    // =========================================================================

    @Test
    fun `audio buffer wipes RAM completely and blocks subsequent access`() {
        val buffer = AudioSampleBuffer(maxDurationSeconds = 1)
        val dummyPcm = ByteArray(3200) { 0x7F.toByte() } // 100ms próbka

        val written = buffer.append(dummyPcm)
        assertEquals(3200, written)
        assertFalse(buffer.isCleared)
        assertNotNull(buffer.getRawBytesOrNull())

        // Wywołanie krytycznego czyszczenia pamięci
        buffer.wipe()

        assertTrue("Bufor musi mieć flagę isCleared", buffer.isCleared)
        assertEquals(0, buffer.currentSize)
        assertNull("getRawBytesOrNull musi zwracać null po wipe", buffer.getRawBytesOrNull())

        // Kolejna próba zapisu do wyczyszczonego bufora musi rzucić wyjątek
        try {
            buffer.append(dummyPcm)
            org.junit.Assert.fail("Powinien rzucić IllegalStateException po wipe")
        } catch (_: IllegalStateException) {
            // Sukces - pamięć zabezpieczona
        }
    }

    // =========================================================================
    // 4. SZYFROWANE PRZECHOWYWANIE Z WYGASANIEM (ENCRYPTION & TTL)
    // =========================================================================

    @Test
    fun `storage respects user opt-in and encrypts redacted transcript with TTL`() {
        val storage = EncryptedTranscriptStorage()
        val now = 1_000_000_000L

        // Domyślnie wyłączone: brak zapisu
        assertFalse(storage.isPersistenceEnabled)
        val transcriptResult = TranscriptResult(text = "podaj kod blik 123456")
        val riskResult = ConversationRiskAnalyzer.analyze(transcriptResult, SpeakerEstimate(EstimatedSpeakers.TWO, com.example.conversation.safety.model.SpeakerConfidence.HIGH, ""), emptyList())

        val unpersisted = storage.saveSnapshot("id1", "+48732123456", "podaj kod blik 123456", riskResult, currentEpoch = now)
        assertNull("Gdy opcja jest wyłączona, nic nie jest zapisywane", unpersisted)

        // Włącz zapis jawnie
        storage.isPersistenceEnabled = true
        val ttlMs = 24 * 3600 * 1000L // 24h
        val record = storage.saveSnapshot("id2", "+48732123456", "podaj kod blik 123456", riskResult, customTtlMs = ttlMs, currentEpoch = now)
        assertNotNull(record)

        // Odszyfrowanie: transkrypcja musi być zredagowana
        val decrypted = storage.decryptTranscript(record!!)
        assertTrue("Odszyfrowana transkrypcja musi zawierać [REDACTED]", decrypted.contains("[REDACTED]"))
        assertFalse("Odszyfrowana transkrypcja nie może ujawniać 123456", decrypted.contains("123456"))

        // Test wygaszania (TTL)
        assertFalse("Przed upływem TTL nie jest wygasły", record.isExpired(now + 1000L))
        assertTrue("Po upływie 25h rekord musi być wygasły", record.isExpired(now + ttlMs + 1000L))

        val cleaned = storage.cleanupExpired(now + ttlMs + 1000L)
        assertEquals(1, cleaned)
        assertTrue(storage.getActiveSnapshots(now + ttlMs + 1000L).isEmpty())
    }

    // =========================================================================
    // 5. INTEGRACJA Z GŁÓWNYM SILNIKIEM RYZYKA (RISK ENGINE V1)
    // =========================================================================

    @Test
    fun `risk engine incorporates conversation safety signal into final call risk`() {
        val phoneNumber = "+48732123456"
        val identity = IdentityEngine.resolveIdentity(phoneNumber)
        val reputation = ReputationEngineV1.evaluateReputation(emptyList())

        // Bez sygnału rozmowy: niskie ryzyko
        val baselineRisk = RiskEngineV1.assessRisk(identity, reputation)
        assertEquals(0, baselineRisk.riskScore)

        // Dodanie sygnału analizy rozmowy z wykryciem BLIK
        val convSignals = SafetySignalExtractor.extractSignals("Podaj proszę kod BLIK.")
        val transcriptResult = TranscriptResult(text = "Podaj proszę kod BLIK.")
        val convRisk = ConversationRiskAnalyzer.analyze(transcriptResult, SpeakerEstimate(EstimatedSpeakers.TWO, com.example.conversation.safety.model.SpeakerConfidence.HIGH, ""), convSignals)
        val safetySignal = ConversationSafetySignal("+48732123456", convRisk)

        val modulatedRisk = RiskEngineV1.assessRisk(
            identity = identity,
            reputation = reputation,
            conversationSignal = safetySignal
        )

        assertTrue("Sygnał rozmowy musi podnieść ryzyko aktywnej sesji do >= 90", modulatedRisk.riskScore >= 90)
        assertEquals(RiskLevel.HIGH, modulatedRisk.riskLevel)
        assertTrue("Powód ryzyka musi zawierać kod CONVERSATION_HIGH_RISK_SIGNALS",
            modulatedRisk.reasons.any { it.code == EngineReasonCode.CONVERSATION_HIGH_RISK_SIGNALS })
    }
}
