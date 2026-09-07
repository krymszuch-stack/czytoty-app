package com.example.incall.dtmf

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Kontroler klawiatury numerycznej DTMF (Dual-Tone Multi-Frequency) dla systemów IVR (Sekcja 87 - P0).
 */
class DtmfController {

  private var toneGenerator: ToneGenerator? = null

  private val _dtmfSequence = MutableStateFlow("")
  val dtmfSequence: StateFlow<String> = _dtmfSequence.asStateFlow()

  init {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
    } catch (_: Exception) {
      // Bezpieczny fallback na wypadek braku uprawnień audio lub urządzenia bez głośnika
      try {
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
      } catch (_: Exception) {
        toneGenerator = null
      }
    }
  }

  /**
   * Rejestruje wpisany znak DTMF i generuje ton telefoniczny.
   */
  fun sendDigit(digit: Char): String {
    if (isValidDtmfChar(digit)) {
      _dtmfSequence.value += digit
      playToneForDigit(digit)
    }
    return _dtmfSequence.value
  }

  fun clearSequence() {
    _dtmfSequence.value = ""
  }

  fun release() {
    try {
      toneGenerator?.release()
    } catch (_: Exception) {}
    toneGenerator = null
  }

  private fun isValidDtmfChar(c: Char): Boolean {
    return c in '0'..'9' || c == '*' || c == '#'
  }

  private fun playToneForDigit(digit: Char) {
    val tone = when (digit) {
      '0' -> ToneGenerator.TONE_DTMF_0
      '1' -> ToneGenerator.TONE_DTMF_1
      '2' -> ToneGenerator.TONE_DTMF_2
      '3' -> ToneGenerator.TONE_DTMF_3
      '4' -> ToneGenerator.TONE_DTMF_4
      '5' -> ToneGenerator.TONE_DTMF_5
      '6' -> ToneGenerator.TONE_DTMF_6
      '7' -> ToneGenerator.TONE_DTMF_7
      '8' -> ToneGenerator.TONE_DTMF_8
      '9' -> ToneGenerator.TONE_DTMF_9
      '*' -> ToneGenerator.TONE_DTMF_S
      '#' -> ToneGenerator.TONE_DTMF_P
      else -> return
    }
    try {
      toneGenerator?.startTone(tone, 120) // 120 ms ton DTMF
    } catch (_: Exception) {
      // Ignorujemy błędy odtwarzania w tle
    }
  }
}
