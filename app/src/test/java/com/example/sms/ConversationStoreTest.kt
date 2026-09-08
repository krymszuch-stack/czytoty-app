package com.example.sms

import android.content.Context
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConversationStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() { context.deleteDatabase("sms-conversations.db") }
    @Test fun draftsAndPinsSurviveReopenAndStaySeparate() {
        ConversationStore(context).use {
            it.saveDraft("new", SmsDraft("+12025550123", "Zażółć gęślą jaźń"))
            it.saveDraft("10", SmsDraft("+12025550124", "Pierwszy szkic"))
            it.saveDraft("10", SmsDraft("+12025550124", "Zmieniony szkic"))
            it.pin(10, true); it.pin(10, true)
        }
        ConversationStore(context).use {
            assertEquals("Zażółć gęślą jaźń", it.drafts()["new"]!!.body)
            assertEquals("Zmieniony szkic", it.drafts()["10"]!!.body)
            assertEquals(setOf(10L), it.pins())
            it.saveDraft("10", SmsDraft()); it.pin(10, false)
            assertEquals(setOf("new"), it.drafts().keys)
            assertTrue(it.pins().isEmpty())
            it.readableDatabase.rawQuery("PRAGMA integrity_check", null).use { c -> c.moveToFirst(); assertEquals("ok", c.getString(0)) }
        }
    }
    @Test fun searchFindsOlderContentButShowsLatestPreviewAndPinsFirst() {
        val data = listOf(
            DeviceSms(1, 10, "+12025550123", "Kod rezerwacji ABC", 1, Telephony.Sms.MESSAGE_TYPE_INBOX, false),
            DeviceSms(2, 10, "+12025550123", "Do jutra!", 2, Telephony.Sms.MESSAGE_TYPE_SENT),
            DeviceSms(3, 20, "+12025550124", "Cześć", 3, Telephony.Sms.MESSAGE_TYPE_INBOX)
        )
        assertEquals(2L, visibleThreads(data, "rezerwacji", false, false, emptySet(), emptyMap()).single().id)
        assertEquals(10L, visibleThreads(data, "", true, false, emptySet(), emptyMap()).single().thread)
        assertEquals(listOf(10L, 20L), visibleThreads(data, "", false, false, setOf(10), emptyMap()).map { it.thread })
        assertEquals(20L, visibleThreads(data, "", false, true, setOf(20), emptyMap()).single().thread)
        assertTrue(visibleThreads(data, "nieistniejąca fraza", false, false, emptySet(), emptyMap()).isEmpty())
    }
    @Test fun rejectsInvalidKeyAndOversizeDraftWithoutLosingExistingText() {
        ConversationStore(context).use {
            it.saveDraft("new", SmsDraft("+12025550123", "Zachowaj"))
            assertThrows(IllegalArgumentException::class.java) { it.saveDraft("new", SmsDraft("", "x".repeat(2001))) }
            assertThrows(IllegalArgumentException::class.java) { it.saveDraft("'; DROP TABLE drafts", SmsDraft("", "x")) }
            assertEquals("Zachowaj", it.drafts()["new"]!!.body)
        }
    }
}
