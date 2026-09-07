package com.example.conversation.safety.manager

import com.example.conversation.safety.analyzer.ConversationRiskAnalyzer
import com.example.conversation.safety.audio.AudioSampleBuffer
import com.example.conversation.safety.diarization.SpeakerEstimator
import com.example.conversation.safety.extractor.SafetySignalExtractor
import com.example.conversation.safety.model.ConversationAnalysisState
import com.example.conversation.safety.model.ConversationRiskResult
import com.example.conversation.safety.model.ConversationSafetySignal
import com.example.conversation.safety.model.TranscriptStatus
import com.example.conversation.safety.redaction.TranscriptRedactor
import com.example.conversation.safety.storage.EncryptedTranscriptStorage
import com.example.conversation.safety.transcriber.ConversationTranscriber
import com.example.conversation.safety.transcriber.SyntheticConversationTranscriber
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
 * Główny koordynator cyklu życia 10-sekundowego okna analizy bezpieczeństwa rozmowy
 * (ConversationSafetyManager).
 *
 * ZARZĄDZA:
 * 1. Świadomą aktywacją przez użytkownika.
 * 2. Odliczaniem czasu (10 s -> 1 s) i stanem anulowania.
 * 3. Alokacją i natychmiastowym czyszczeniem (wipe) bufora RAM.
 * 4. Transkrypcją on-device lub obsługą statusu UNSUPPORTED.
 * 5. Ekstrakcją sygnałów i estymacją rozmówców.
 */
class ConversationSafetyManager(
    private val transcriber: ConversationTranscriber = SyntheticConversationTranscriber(),
    private val storage: EncryptedTranscriptStorage = EncryptedTranscriptStorage(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val totalListeningSeconds: Int = 10
) {
    private val _state = MutableStateFlow<ConversationAnalysisState>(ConversationAnalysisState.Idle)
    val state: StateFlow<ConversationAnalysisState> = _state.asStateFlow()

    private var currentJob: Job? = null
    private var activeBuffer: AudioSampleBuffer? = null

    /**
     * Rozpoczyna 10-sekundową analizę bezpieczeństwa rozmowy.
     * Wywoływana WYŁĄCZNIE po kliknięciu przez użytkownika przycisku w UI.
     */
    fun startAnalysis(
        callerNumber: String,
        scope: CoroutineScope,
        sampleBuffer: AudioSampleBuffer? = null,
        onComplete: ((ConversationSafetySignal) -> Unit)? = null
    ) {
        cancel() // Zatrzymaj ewentualne trwające zadanie

        val buffer = sampleBuffer ?: AudioSampleBuffer(maxDurationSeconds = totalListeningSeconds)
        activeBuffer = buffer

        currentJob = scope.launch(dispatcher) {
            val startTime = System.currentTimeMillis()
            try {
                // 1. Odliczanie od totalListeningSeconds do 1 sekundy
                for (sec in totalListeningSeconds downTo 1) {
                    _state.value = ConversationAnalysisState.Listening(remainingSeconds = sec)
                    delay(1000L)
                }

                // 2. Przetwarzanie zebranego materiału
                _state.value = ConversationAnalysisState.Processing

                // 3. Transkrypcja mowy
                val transcriptResult = transcriber.transcribe(buffer)

                // Jeśli lokalna transkrypcja nie jest wspierana
                if (transcriptResult.status == TranscriptStatus.UNSUPPORTED) {
                    buffer.wipe()
                    _state.value = ConversationAnalysisState.Unsupported(
                        "Lokalna analiza mowy nie jest wspierana na tym urządzeniu. Zgodnie z polityką prywatności audio nie zostało wysłane do chmury."
                    )
                    return@launch
                }

                // 4. Diarizacja (estymacja liczby rozmówców)
                val speakerEstimate = SpeakerEstimator.estimateSpeakers(buffer, transcriptResult)

                // 5. Kontekstowa ekstrakcja sygnałów bezpieczeństwa
                val rawText = transcriptResult.text
                val detectedSignals = SafetySignalExtractor.extractSignals(rawText)

                // 6. Bezwzględna redakcja danych wrażliwych (BLIK, karty, hasła)
                val redactedTranscript = TranscriptRedactor.redact(rawText)

                // 7. Obliczenie ryzyka i pewności
                val processingTimeMs = System.currentTimeMillis() - startTime
                val riskResult = ConversationRiskAnalyzer.analyze(
                    transcriptResult = transcriptResult,
                    speakerEstimate = speakerEstimate,
                    signals = detectedSignals,
                    processingTimeMs = processingTimeMs
                )

                // 8. KRYTYCZNY PUNKT: Natychmiastowe bezpowrotne zerowanie pamięci RAM audio
                buffer.wipe()
                activeBuffer = null

                // 9. Opcjonalne zaszyfrowane utrwalenie zredagowanej transkrypcji
                if (storage.isPersistenceEnabled && redactedTranscript.isNotBlank()) {
                    storage.saveSnapshot(
                        id = "snap_${System.currentTimeMillis()}",
                        callerNumber = callerNumber,
                        rawTranscript = redactedTranscript,
                        riskResult = riskResult
                    )
                }

                val safetySignal = ConversationSafetySignal(
                    callerNumber = callerNumber,
                    riskResult = riskResult,
                    timestamp = System.currentTimeMillis()
                )

                _state.value = ConversationAnalysisState.Completed(
                    result = riskResult,
                    redactedTranscript = redactedTranscript
                )

                onComplete?.invoke(safetySignal)

            } catch (e: CancellationException) {
                buffer.wipe()
                activeBuffer = null
                _state.value = ConversationAnalysisState.Cancelled
            } catch (e: Throwable) {
                buffer.wipe()
                activeBuffer = null
                _state.value = ConversationAnalysisState.Error("Wystąpił błąd podczas analizy: ${e.localizedMessage ?: "Nieznany błąd"}")
            }
        }
    }

    /**
     * Anuluje bieżącą analizę na żądanie użytkownika (przycisk [Anuluj]).
     * Natychmiast czyści bufor audio z pamięci RAM.
     */
    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        activeBuffer?.wipe()
        activeBuffer = null
        if (_state.value is ConversationAnalysisState.Listening || _state.value is ConversationAnalysisState.Processing) {
            _state.value = ConversationAnalysisState.Cancelled
        }
    }

    /**
     * Resetuje stan analizatora do trybu bezczynnego (IDLE).
     */
    fun reset() {
        cancel()
        _state.value = ConversationAnalysisState.Idle
    }
}
