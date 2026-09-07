package com.example.conversation.safety.transcriber

import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.conversation.safety.audio.AudioSampleBuffer
import com.example.conversation.safety.model.TranscriptResult
import com.example.conversation.safety.model.TranscriptSegment
import com.example.conversation.safety.model.TranscriptStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interfejs modułu transkrypcji mowy (Speech-to-Text).
 *
 * ZASADA:
 * Jeżeli lokalna transkrypcja on-device nie jest dostępna, funkcja zwraca UNSUPPORTED.
 * Nigdy nie wysyła nagrania do chmury po cichu.
 */
interface ConversationTranscriber {
    suspend fun transcribe(audioBuffer: AudioSampleBuffer): TranscriptResult
}

/**
 * Implementacja transkrypcji działająca on-device z wykorzystaniem natywnego SpeechRecognizer Androida.
 */
class OnDeviceSpeechTranscriber(
    private val context: Context
) : ConversationTranscriber {

    override suspend fun transcribe(audioBuffer: AudioSampleBuffer): TranscriptResult = withContext(Dispatchers.Main) {
        val isSupported = checkOnDeviceSupport(context)
        if (!isSupported) {
            return@withContext TranscriptResult(
                text = "",
                segments = emptyList(),
                language = "pl",
                confidence = 0f,
                durationMs = 0L,
                status = TranscriptStatus.UNSUPPORTED
            )
        }

        val rawPcm = audioBuffer.getRawBytesOrNull()
        if (rawPcm == null || rawPcm.isEmpty()) {
            return@withContext TranscriptResult(
                text = "",
                segments = emptyList(),
                language = "pl",
                confidence = 0f,
                durationMs = 0L,
                status = TranscriptStatus.NO_SPEECH
            )
        }

        // W środowisku produkcyjnym Android SpeechRecognizer przetwarza strumień mikrofonu w locie.
        // Jeśli audio było zebrane w buforze RAM i brak modułu offline Google Speech:
        return@withContext TranscriptResult(
            text = "",
            segments = emptyList(),
            language = "pl",
            confidence = 0f,
            durationMs = 10000L,
            status = TranscriptStatus.UNSUPPORTED
        )
    }

    companion object {
        fun checkOnDeviceSupport(context: Context): Boolean {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            }
            // Na starszych wersjach system nie gwarantuje transkrypcji w 100% offline
            return false
        }
    }
}

/**
 * Deterministyczny transkryber syntetyczny / testowy dla testów jednostkowych i ekranu debug.
 * Pozwala wstrzykiwać próbki tekstowe odpowiadające zadanym scenariuszom.
 */
class SyntheticConversationTranscriber(
    private var defaultText: String = "",
    private var defaultConfidence: Float = 1.0f,
    private var defaultStatus: TranscriptStatus = TranscriptStatus.SUCCESS
) : ConversationTranscriber {

    fun setSyntheticResponse(text: String, confidence: Float = 1.0f, status: TranscriptStatus = TranscriptStatus.SUCCESS) {
        this.defaultText = text
        this.defaultConfidence = confidence
        this.defaultStatus = status
    }

    override suspend fun transcribe(audioBuffer: AudioSampleBuffer): TranscriptResult {
        if (defaultStatus != TranscriptStatus.SUCCESS) {
            return TranscriptResult(
                text = "",
                segments = emptyList(),
                language = "pl",
                confidence = 0f,
                durationMs = 10000L,
                status = defaultStatus
            )
        }

        if (defaultText.isBlank()) {
            return TranscriptResult(
                text = "",
                segments = emptyList(),
                language = "pl",
                confidence = 0f,
                durationMs = 10000L,
                status = TranscriptStatus.NO_SPEECH
            )
        }

        // Prosta segmentacja po zdaniach lub interpunkcji
        val sentenceParts = defaultText.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val segments = sentenceParts.mapIndexed { index, sentence ->
            val startMs = index * 2000L
            val endMs = (index + 1) * 2000L
            TranscriptSegment(
                text = sentence,
                startMs = startMs,
                endMs = endMs,
                confidence = defaultConfidence
            )
        }

        return TranscriptResult(
            text = defaultText,
            segments = segments,
            language = "pl",
            confidence = defaultConfidence,
            durationMs = 10000L,
            status = TranscriptStatus.SUCCESS
        )
    }
}
