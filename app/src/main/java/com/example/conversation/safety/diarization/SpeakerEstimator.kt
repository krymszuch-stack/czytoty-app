package com.example.conversation.safety.diarization

import com.example.conversation.safety.audio.AudioSampleBuffer
import com.example.conversation.safety.model.EstimatedSpeakers
import com.example.conversation.safety.model.SpeakerConfidence
import com.example.conversation.safety.model.SpeakerEstimate
import com.example.conversation.safety.model.TranscriptResult
import kotlin.math.abs

/**
 * Estymator liczby rozmówców (Speaker Diarization).
 *
 * ZASADA BEZWZGLĘDNA:
 * Moduł NIE identyfikuje osób ani nie tworzy profili biometrycznych.
 * Zwraca wyłącznie oszacowanie liczby unikalnych rozmówców (1, 2, 3+, UNKNOWN).
 */
object SpeakerEstimator {

    /**
     * Szacuje liczbę rozmówców na podstawie parametrów akustycznych oraz segmentacji wypowiedzi.
     */
    fun estimateSpeakers(
        audioBuffer: AudioSampleBuffer?,
        transcriptResult: TranscriptResult?
    ): SpeakerEstimate {
        // Jeśli transkrypcja nie ma słów lub jest błąd/hałas
        if (transcriptResult == null || transcriptResult.text.isBlank()) {
            return SpeakerEstimate(
                estimatedSpeakers = EstimatedSpeakers.UNKNOWN,
                confidence = SpeakerConfidence.LOW,
                explanation = "Niewystarczająca jakość lub brak czytelnych segmentów mowy."
            )
        }

        val rawPcm = audioBuffer?.getRawBytesOrNull()
        if (rawPcm != null && rawPcm.isNotEmpty()) {
            // Analiza akustyczna zmian energii i pauz (naprzemienność wypowiedzi)
            val turnsDetected = estimateTurnsFromPcm(rawPcm)
            return when {
                turnsDetected >= 4 -> SpeakerEstimate(
                    estimatedSpeakers = EstimatedSpeakers.THREE_PLUS,
                    confidence = SpeakerConfidence.MEDIUM,
                    explanation = "Wykryto prawdopodobnie 3 lub więcej nakładających się głosów w tle."
                )
                turnsDetected in 1..3 -> SpeakerEstimate(
                    estimatedSpeakers = EstimatedSpeakers.TWO,
                    confidence = SpeakerConfidence.HIGH,
                    explanation = "Wykryto prawdopodobnie 2 różnych rozmówców (naprzemienna wymiana zdań)."
                )
                turnsDetected == 0 -> SpeakerEstimate(
                    estimatedSpeakers = EstimatedSpeakers.ONE,
                    confidence = SpeakerConfidence.MEDIUM,
                    explanation = "Wykryto pojedynczy ciągły monolog (1 rozmówca)."
                )
                else -> SpeakerEstimate(
                    estimatedSpeakers = EstimatedSpeakers.UNKNOWN,
                    confidence = SpeakerConfidence.LOW,
                    explanation = "Jakość sygnału uniemożliwia jednoznaczne rozróżnienie mówców."
                )
            }
        }

        // Fallback w oparciu o strukturę segmentów transkrypcji
        val segmentCount = transcriptResult.segments.size
        return when {
            segmentCount >= 3 -> SpeakerEstimate(
                estimatedSpeakers = EstimatedSpeakers.TWO,
                confidence = SpeakerConfidence.MEDIUM,
                explanation = "Wykryto prawdopodobnie 2 różnych rozmówców."
            )
            segmentCount in 1..2 -> SpeakerEstimate(
                estimatedSpeakers = EstimatedSpeakers.ONE,
                confidence = SpeakerConfidence.MEDIUM,
                explanation = "Wykryto prawdopodobnie 1 rozmówcę."
            )
            else -> SpeakerEstimate(
                estimatedSpeakers = EstimatedSpeakers.UNKNOWN,
                confidence = SpeakerConfidence.LOW,
                explanation = "Brak jednoznacznego sygnału mówców."
            )
        }
    }

    /**
     * Prosty, szybki algorytm on-device liczący naprzemienne przejścia poziomów energii (turns)
     * w próbkach PCM 16-bit.
     */
    private fun estimateTurnsFromPcm(pcm: ByteArray): Int {
        if (pcm.size < 3200) return 0 // Mniej niż 100ms przy 16kHz 16-bit

        val sampleCount = pcm.size / 2
        var previousEnergy = 0.0
        var turns = 0
        val windowSize = 3200 // ok. 100 ms okno

        var windowSum = 0L
        var windowSamples = 0

        for (i in 0 until sampleCount) {
            val low = pcm[i * 2].toInt()
            val high = pcm[i * 2 + 1].toInt()
            val sample = (high shl 8) or (low and 0xFF)
            windowSum += abs(sample)
            windowSamples++

            if (windowSamples >= windowSize) {
                val currentEnergy = windowSum.toDouble() / windowSize
                if (previousEnergy > 0) {
                    val ratio = currentEnergy / (previousEnergy + 1.0)
                    // Znaczący skok lub spadek profilu energii sugerujący zmianę mówcy
                    if (ratio > 2.8 || ratio < 0.35) {
                        turns++
                    }
                }
                previousEnergy = currentEnergy
                windowSum = 0L
                windowSamples = 0
            }
        }

        return (turns / 2).coerceAtMost(5)
    }
}
