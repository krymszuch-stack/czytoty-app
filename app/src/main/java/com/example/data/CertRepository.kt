package com.example.data

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.net.IDN
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

const val CERT_SOURCE = "https://hole.cert.pl/domains/v2/domains.txt"
const val CERT_MAX_AGE_MS = 5 * 60 * 1000L

@Serializable
data class CertSnapshot(val fetchedAt: Long, val domains: Set<String>) {
    fun isFresh(now: Long = System.currentTimeMillis()) = now >= fetchedAt && now - fetchedAt <= CERT_MAX_AGE_MS
}
data class DomainFinding(val host: String, val matchedDomain: String?)
data class MessageAnalysis(val findings: List<DomainFinding>, val localSignals: List<String>, val snapshotTime: Long?)

object DomainChecker {
    fun normalizeDomain(raw: String): String? = runCatching {
        val domain = IDN.toASCII(raw.trim().trimEnd('.'), IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
        require(domain.length <= 253 && domain.contains('.') && domain.split('.').all { it.isNotEmpty() && it.length <= 63 })
        domain
    }.getOrNull()

    fun parseFeed(text: String): Set<String> {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty() && lines.size <= 500_000) { "Nieprawidłowa wielkość listy CERT." }
        val domains = lines.map { normalizeDomain(it) ?: error("Nieprawidłowa odpowiedź źródła CERT.") }.toSet()
        return domains
    }

    fun match(host: String, domains: Set<String>): String? {
        var candidate = normalizeDomain(host) ?: return null
        while (candidate.contains('.')) {
            if (candidate in domains) return candidate
            candidate = candidate.substringAfter('.')
        }
        return null
    }

    fun extractHosts(text: String): List<String> {
        // Parse authority with HttpUrl: in https://bank.pl@evil.pl the host is evil.pl.
        val urls = Regex("(?i)https?://[^\\s<>\\\"{}]+")
        val result = mutableSetOf<String>()
        urls.findAll(text).forEach {
            val url = it.value.trimEnd('.', ',', ';', ':', ')', ']', '!', '?').toHttpUrlOrNull()
            url?.host?.let { host -> normalizeDomain(host)?.let(result::add) }
        }
        val rest = urls.replace(text, " ")
        Regex("(?i)(?<![\\p{L}\\p{N}_@.-])(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?\\.)+[\\p{L}]{2,63}(?![\\p{L}\\p{N}_@-])")
            .findAll(rest).forEach { normalizeDomain(it.value)?.let(result::add) }
        return result.take(100)
    }

    fun analyze(text: String, snapshot: CertSnapshot?): MessageAnalysis {
        val lower = text.lowercase(Locale.ROOT)
        val signals = buildList {
            if (listOf("blik", "kod autoryzacyjny", "kod sms", "hasło", "haslo", "pin do").any(lower::contains))
                add("Treść wspomina kod płatności lub dane dostępu. Nikomu ich nie przekazuj.")
            if (listOf("dopła", "doplac", "doplat", "zablokowane", "zablokujemy", "natychmiast", "pilne", "ostatnia szansa").any(lower::contains))
                add("Występuje presja czasu lub wezwanie do dopłaty. To sygnał językowy, nie dowód oszustwa.")
        }
        return MessageAnalysis(extractHosts(text).map { DomainFinding(it, snapshot?.let { s -> match(it, s.domains) }) }, signals, snapshot?.fetchedAt)
    }
}

/** Downloads the public list only. SMS contents and checked hosts never leave this device. */
class CertRepository(context: Context, private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()) {
    private val cache = File(context.filesDir, "cert-warning-list.json")
    private val mutex = Mutex()
    suspend fun load(): CertSnapshot? = withContext(Dispatchers.IO) {
        runCatching { Json.decodeFromString<CertSnapshot>(cache.readText(Charsets.UTF_8)) }.getOrNull()
    }
    suspend fun refresh(): CertSnapshot = mutex.withLock {
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(CERT_SOURCE).header("User-Agent", "CzyToTy-Prototype/0.2").build()
            val text = client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "CERT zwrócił HTTP ${response.code}." }
                val body = response.body ?: error("Pusta odpowiedź CERT.")
                val limit = 12 * 1024 * 1024
                require(body.contentLength() <= limit) { "Lista przekracza limit pobierania." }
                body.byteStream().use { stream ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = stream.read(buffer)
                        if (read == -1) break
                        require(output.size() + read <= limit) { "Lista przekracza limit pobierania." }
                        output.write(buffer, 0, read)
                    }
                    output.toString("UTF-8")
                }
            }
            val snapshot = CertSnapshot(System.currentTimeMillis(), DomainChecker.parseFeed(text))
            val temporary = File.createTempFile("cert-list-", ".tmp", cache.parentFile)
            try {
                FileOutputStream(temporary).use { output ->
                    output.write(Json.encodeToString(snapshot).toByteArray(Charsets.UTF_8))
                    output.fd.sync()
                }
                // Same-filesystem atomic replacement. Unlike File.renameTo, this also replaces on Windows tests.
                Files.move(temporary.toPath(), cache.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } finally {
                temporary.delete()
            }
            snapshot
        }
    }
}
