package com.example.conversation.safety.audio

import java.util.Arrays

/**
 * Ulotny bufor audio w pamięci RAM (Zero-Storage Audio Buffer).
 *
 * ZASADY BEZPIECZEŃSTWA:
 * 1. Nigdy nie zapisuje zawartości do pliku (brak metod zapisu WAV/MP3).
 * 2. Przechowuje maksymalnie 10 sekund próbek PCM 16-bit 16kHz mono w RAM.
 * 3. Po zakończeniu transkrypcji i ekstrakcji, natychmiast wywoływana jest metoda [wipe],
 *    która bezpowrotnie nadpisuje pamięć zerami (Arrays.fill).
 */
class AudioSampleBuffer(
    maxDurationSeconds: Int = 10,
    val sampleRate: Int = 16000,
    val bytesPerSample: Int = 2 // 16-bit PCM mono = 2 bajty na próbkę
) {
    // 10 sekund * 16000 próbek/s * 2 bajty = 320 000 bajtów (~312.5 KB w RAM)
    private val maxCapacityBytes: Int = maxDurationSeconds * sampleRate * bytesPerSample
    private var internalBuffer: ByteArray? = ByteArray(maxCapacityBytes)
    private var bytesWritten: Int = 0
    private var isWiped: Boolean = false

    val isCleared: Boolean
        get() = isWiped || internalBuffer == null

    val currentSize: Int
        get() = if (isWiped) 0 else bytesWritten

    val capacity: Int
        get() = maxCapacityBytes

    /**
     * Zapisuje pakiet bajtów audio z mikrofonu do bufora RAM.
     */
    @Synchronized
    fun append(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        if (isWiped || internalBuffer == null) {
            throw IllegalStateException("Audio buffer was wiped and cannot be accessed.")
        }

        val available = maxCapacityBytes - bytesWritten
        if (available <= 0) return 0

        val bytesToCopy = minOf(available, length)
        System.arraycopy(data, offset, internalBuffer!!, bytesWritten, bytesToCopy)
        bytesWritten += bytesToCopy
        return bytesToCopy
    }

    /**
     * Zwraca kopię zarejestrowanych bajtów PCM do przetworzenia w RAM.
     */
    @Synchronized
    fun getRawBytesOrNull(): ByteArray? {
        if (isWiped || internalBuffer == null) return null
        val result = ByteArray(bytesWritten)
        System.arraycopy(internalBuffer!!, 0, result, 0, bytesWritten)
        return result
    }

    /**
     * Krytyczna operacja czyszczenia pamięci:
     * Bezwzględnie nadpisuje bufor zerami i odcina referencje.
     */
    @Synchronized
    fun wipe() {
        if (!isWiped && internalBuffer != null) {
            Arrays.fill(internalBuffer!!, 0.toByte())
            internalBuffer = null
            bytesWritten = 0
            isWiped = true
        }
    }
}
