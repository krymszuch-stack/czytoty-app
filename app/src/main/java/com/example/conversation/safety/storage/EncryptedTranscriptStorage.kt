package com.example.conversation.safety.storage

import com.example.conversation.safety.model.ConversationRiskResult
import com.example.conversation.safety.redaction.TranscriptRedactor
import java.security.KeyStore
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Bezpieczny magazyn transkrypcji z szyfrowaniem AES-256-GCM i automatycznym wygaszaniem (TTL).
 *
 * REGULY PRYWATNOSCI:
 * 1. Domyślnie transkrypcja NIE jest zapisywana w ogóle (isPersistenceEnabled = false).
 * 2. Jeśli użytkownik jawnie włączy zapis:
 *    - Transkrypcja ZAWSZE podlega uprzedniej redakcji (TranscriptRedactor.redact).
 *    - Nigdy nie zawiera surowych kodów BLIK, kart, PIN, PESEL ani haseł.
 *    - Szyfrowana symetrycznym kluczem AES/GCM/NoPadding (256-bit).
 *    - Posiada zdefiniowany czas wygaśnięcia TTL (domyślnie 24 godziny).
 */
class EncryptedTranscriptStorage(
    private val keyAlias: String = "CzyToTy_ConversationSafety_Key"
) {

    var isPersistenceEnabled: Boolean = false
    var defaultTtlMs: Long = 24 * 60 * 60 * 1000L // 24 godziny

    data class EncryptedSnapshotRecord(
        val id: String,
        val callerNumber: String,
        val timestamp: Long,
        val ttlDurationMs: Long,
        val encryptedData: ByteArray,
        val iv: ByteArray,
        val detectedSignalCodes: List<String>,
        val riskScore: Int,
        val engineVersion: String
    ) {
        fun isExpired(currentEpoch: Long = System.currentTimeMillis()): Boolean {
            return currentEpoch > (timestamp + ttlDurationMs)
        }
    }

    private val inMemoryRecords = ConcurrentHashMap<String, EncryptedSnapshotRecord>()
    private val secureRandom = SecureRandom()
    private var fallbackKey: SecretKey? = null

    init {
        initOrGetSecretKey()
    }

    private fun initOrGetSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
                keyGen.init(256)
                keyGen.generateKey()
            } else {
                (keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry).secretKey
            }
        } catch (_: Throwable) {
            // Środowisko testowe JVM / brak providera AndroidKeyStore
            if (fallbackKey == null) {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256)
                fallbackKey = keyGen.generateKey()
            }
            fallbackKey!!
        }
    }

    /**
     * Zapisuje migawkę zredagowanej transkrypcji w postaci zaszyfrowanej.
     * Zwraca rekord lub null jeśli zapis jest wyłączony.
     */
    fun saveSnapshot(
        id: String,
        callerNumber: String,
        rawTranscript: String,
        riskResult: ConversationRiskResult,
        customTtlMs: Long = defaultTtlMs,
        currentEpoch: Long = System.currentTimeMillis()
    ): EncryptedSnapshotRecord? {
        if (!isPersistenceEnabled) return null

        // 1. Bezwzględna redakcja danych wrażliwych przed jakimkolwiek szyfrowaniem/zapisem
        val redacted = TranscriptRedactor.redact(rawTranscript)

        // 2. Szyfrowanie AES-256-GCM
        val key = initOrGetSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(redacted.toByteArray(Charsets.UTF_8))

        val record = EncryptedSnapshotRecord(
            id = id,
            callerNumber = callerNumber,
            timestamp = currentEpoch,
            ttlDurationMs = customTtlMs,
            encryptedData = ciphertext,
            iv = iv,
            detectedSignalCodes = riskResult.signals.map { it.code.name },
            riskScore = riskResult.riskScore,
            engineVersion = riskResult.engineVersion
        )

        inMemoryRecords[id] = record
        cleanupExpired(currentEpoch)
        return record
    }

    /**
     * Odszyfrowuje zredagowaną transkrypcję dla zadanego rekordu.
     */
    fun decryptTranscript(record: EncryptedSnapshotRecord): String {
        val key = initOrGetSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, record.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plaintextBytes = cipher.doFinal(record.encryptedData)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    /**
     * Pobiera aktywne, niewygasłe migawki.
     */
    fun getActiveSnapshots(currentEpoch: Long = System.currentTimeMillis()): List<EncryptedSnapshotRecord> {
        cleanupExpired(currentEpoch)
        return inMemoryRecords.values.toList()
    }

    /**
     * Usuwa wszystkie rekordy, których czas życia TTL minął.
     */
    fun cleanupExpired(currentEpoch: Long = System.currentTimeMillis()): Int {
        var removedCount = 0
        val iterator = inMemoryRecords.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired(currentEpoch)) {
                iterator.remove()
                removedCount++
            }
        }
        return removedCount
    }

    /**
     * Ręczne usunięcie wszystkich zapisanych transkrypcji na żądanie użytkownika.
     */
    fun clearAll() {
        inMemoryRecords.clear()
    }
}
