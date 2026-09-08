package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CertRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Before fun clearCache() {
        listOf("cert-warning-list.json", "cert-warning-list.json.bak", "cert-warning-list.json.new").forEach {
            File(context.filesDir, it).delete()
        }
    }
    private fun repository(body: String, code: Int = 200) = CertRepository(context, OkHttpClient.Builder().addInterceptor {
        Response.Builder().request(it.request()).protocol(Protocol.HTTP_1_1).code(code).message("Test fixture")
            .body(body.toResponseBody()).build()
    }.build())

    @Test fun failedRefreshKeepsPreviousSnapshot() = runBlocking {
        val previous = repository("old.example\ncurrent.example").refresh()
        val broken = repository("server unavailable", 503)
        assertTrue(runCatching { broken.refresh() }.isFailure)
        assertEquals(previous, broken.load())
    }
    @Test fun malformedResponseCannotEraseKnownThreats() = runBlocking {
        val previous = repository("threat.example").refresh()
        val malformed = repository("<html>maintenance</html>")
        assertTrue(runCatching { malformed.refresh() }.isFailure)
        assertEquals(previous, malformed.load())
    }
    @Test fun withdrawnDomainDisappearsFromPersistedSnapshot() = runBlocking {
        repository("withdrawn.example\nactive.example").refresh()
        val replacement = repository("active.example")
        replacement.refresh()
        assertEquals(setOf("active.example"), replacement.load()!!.domains)
    }
}
