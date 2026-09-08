package com.example.sms

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmsTransmissionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() { context.deleteDatabase("sms-transmission.db") }
    @Test fun duplicateAndUnknownReceiptsCannotClaimCompleteTransmission() {
        SmsSendStore(context).use {
            it.create("test", "+12025550123", 2)
            it.record("test", 0, Activity.RESULT_OK)
            it.record("test", 0, Activity.RESULT_OK)
            it.record("test", 2, Activity.RESULT_OK)
            it.record("unknown", 1, Activity.RESULT_OK)
            assertEquals(1, it.recent().single().completed)
            assertTrue(it.recent().single().status().contains("1 z 2"))
        }
        SmsSendStore(context).use {
            it.record("test", 1, Activity.RESULT_OK)
            assertEquals(2, it.recent().single().completed)
            assertTrue(it.recent().single().status().contains("Doręczenie nie jest potwierdzone"))
        }
    }
    @Test fun partialFailureAndTimeoutAreNeverReportedAsSent() {
        SmsSendStore(context).use {
            it.create("test", "+12025550123", 2)
            it.record("test", 0, Activity.RESULT_OK)
            val pending = it.recent().single()
            assertTrue(pending.status(pending.started + 121_000).contains("Brak pełnego potwierdzenia"))
            it.record("test", 1, 1)
            assertTrue(it.recent().single().status().contains("Błąd wysyłki"))
        }
    }
    @Test fun rejectedSubmissionIsPersistedAcrossReopen() {
        SmsSendStore(context).use { it.create("test", "+12025550123", 1); it.fail("test", "Brak SIM") }
        SmsSendStore(context).use { assertEquals("Brak SIM", it.recent().single().status()) }
    }
}
