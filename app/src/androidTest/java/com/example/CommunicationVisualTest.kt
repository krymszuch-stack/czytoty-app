package com.example

import android.telecom.Call
import android.provider.Telephony
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.calling.*
import com.example.sms.DeviceSms
import com.example.ui.tabs.MessageBubble
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Synthetic UI fixtures exist only in the test APK. They never enter Telecom or the SMS provider. */
@RunWith(AndroidJUnit4::class)
class CommunicationVisualTest {
    @get:Rule val rule = createAndroidComposeRule<InCallActivity>()
    private fun capture(name: String) {
        rule.waitForIdle()
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "visual-qa").apply { mkdirs() }
        File(dir, name).outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun noTelecomCallHasNoAnswerOrHangupControls() {
        rule.onNodeWithTag("no_live_call").assertExists()
        rule.onNodeWithTag("call_answer").assertDoesNotExist()
        rule.onNodeWithTag("call_end").assertDoesNotExist()
    }
    @Test fun incomingAndHeldCallHaveDifferentControls() {
        rule.activity.runOnUiThread { rule.activity.setContent { MyApplicationTheme(darkTheme = true) {
            CallScreen(CallSessionState(calls = listOf(CallItem("fixture", "+1 202 555 0123", Call.STATE_RINGING, 0, false, false, false))), onClose = {})
        } } }
        rule.onNodeWithTag("call_answer").assertExists()
        rule.onNodeWithTag("call_reject").assertExists()
        rule.onNodeWithTag("call_timer").assertDoesNotExist()
        capture("synthetic-incoming.png")
        rule.activity.runOnUiThread { rule.activity.setContent { MyApplicationTheme(darkTheme = true) {
            CallScreen(CallSessionState(calls = listOf(CallItem("fixture", "+1 202 555 0123", Call.STATE_HOLDING, 0, true, false, false))), onClose = {})
        } } }
        rule.onNodeWithTag("call_answer").assertDoesNotExist()
        rule.onNodeWithText("Wznów").assertExists()
        rule.onNodeWithTag("call_timer").assertDoesNotExist()
    }
    @Test fun messageBubblesHaveRealDirectionAndExplicitSendState() {
        rule.activity.runOnUiThread { rule.activity.setContent { MyApplicationTheme(darkTheme = true) {
            Column(Modifier.fillMaxSize().background(Color(0xFF0A0A12)).safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Podgląd testowy • Wiadomości", style = MaterialTheme.typography.headlineSmall)
                Text("Dane syntetyczne, bez wysyłania SMS", style = MaterialTheme.typography.labelSmall)
                MessageBubble(DeviceSms(1, 1, "+12025550123", "Cześć! Spotkamy się jutro o 17?", 1_788_800_400_000, Telephony.Sms.MESSAGE_TYPE_INBOX), null) {}
                MessageBubble(DeviceSms(2, 1, "+12025550123", "Tak, pasuje. Do zobaczenia!", 1_788_800_460_000, Telephony.Sms.MESSAGE_TYPE_SENT), null) {}
                MessageBubble(DeviceSms(3, 1, "+12025550123", "Przygotuję szczegóły i dam znać.", 1_788_800_520_000, Telephony.Sms.MESSAGE_TYPE_FAILED), null) {}
            }
        } } }
        rule.onNodeWithText(" · wysłano", substring = true).assertExists()
        rule.onNodeWithText(" · błąd", substring = true).assertExists()
        capture("synthetic-message-bubbles.png")
    }
}
