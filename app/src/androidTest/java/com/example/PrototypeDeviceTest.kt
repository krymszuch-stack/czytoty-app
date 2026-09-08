package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrototypeDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun keepScreenAwakeDuringTest() {
        rule.activity.runOnUiThread {
            rule.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun awaitUi() = rule.waitUntil(30_000) {
        runCatching { rule.onAllNodes(hasTestTag("tab_2")).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
    }

    @Test fun actualCertFeedAndLocalAnalysisWorkOnDevice() {
        awaitUi()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = CertRepository(context)
        val snapshot = runBlocking { repo.refresh() }
        assertTrue("Actual CERT feed must contain domains", snapshot.domains.size > 1000)
        assertTrue(runBlocking { repo.load() }!!.isFresh())
        val host = snapshot.domains.first()
        assertNotNull(DomainChecker.analyze("https://$host", snapshot).findings.single().matchedDomain)
        rule.onNodeWithTag("tab_2").performClick()
        rule.onNodeWithTag("message_input").performScrollTo().performTextReplacement("https://$host")
        rule.waitUntil(45_000) {
            rule.onAllNodes(hasTestTag("analyze") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("analyze").performScrollTo().performClick()
        rule.onNodeWithTag("analysis_result").performScrollTo()
            .assertTextContains("Ostrzeżenie CERT", substring = true)
    }

    @Test fun saveAndReloadPrivateLocalNote() {
        awaitUi()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = LocalStore.get(context)
        val phone = "+12025550123"
        try {
            rule.onNodeWithTag("tab_3").performClick()
            rule.onNodeWithTag("phone_input").performScrollTo().performTextReplacement(phone)
            rule.onNodeWithTag("note_input").performScrollTo().performTextReplacement("QA device persistence")
            rule.onNodeWithTag("save_local").performScrollTo().performClick()
            rule.waitUntil(10_000) { store.notes().any { it.phone == phone && it.note == "QA device persistence" } }
            store.close()
            assertEquals("QA device persistence", store.notes().first { it.phone == phone }.note)
            assertFalse(store.isBlocked(phone))
        } finally { store.delete(phone) }
    }

    @Test fun actualSupabaseSdkCanReadSourceCatalogue() = runBlocking {
        val sources = CloudRepository().checkConnection()
        assertTrue(sources.any { it.id == "cert_pl" && it.url == CERT_SOURCE })
    }

    @Test fun smsComposerRequiresTextAndKeepsDraftAcrossTabs() {
        awaitUi()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("sms_new").performClick()
        rule.onNodeWithTag("sms_send").assertIsNotEnabled()
        rule.onNodeWithTag("sms_address").performTextReplacement("+12025550123")
        rule.onNodeWithTag("sms_body").performTextReplacement("Syntetyczny szkic QA — nie wysyłać")
        rule.onNodeWithTag("sms_send").assertIsEnabled()
        rule.onNodeWithTag("tab_0").performClick()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("sms_body").assertTextContains("Syntetyczny szkic QA — nie wysyłać")
    }

    @Test fun dialerKeypadAndSectionsRemainUsable() {
        awaitUi()
        rule.onNodeWithTag("tab_0").performClick()
        rule.onNodeWithTag("dial_input").performTextReplacement("+12025550123")
        rule.onNodeWithTag("dial_call").assertIsEnabled()
        rule.onNodeWithTag("dial_section_0").performClick()
        rule.onNodeWithTag("dial_section_2").performClick()
        rule.onNodeWithTag("dial_input").assertTextContains("+12025550123")
    }
}

