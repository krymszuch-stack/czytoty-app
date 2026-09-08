package com.example

import android.graphics.Bitmap
import android.provider.Telephony
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sms.DeviceSms
import com.example.ui.tabs.*
import com.example.ui.theme.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Production Compose components, rendered on the JVM. Fixtures never reach a provider or network. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pl-rPL-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CommunicationDemoTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private fun capture(name: String) {
        rule.waitForIdle()
        val dir = File("../artifacts/linkedin/demo").apply { mkdirs() }
        // Windows/JVM PixelCopy does not dispatch a hardware frame. Draw the actual view tree
        // into a native Android Canvas; no mock screenshot or HTML recreation is involved.
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Composable private fun Frame(title: String, content: @Composable ColumnScope.() -> Unit) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("CzyToTy?", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("PROTOTYP 0.6", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text("DEMO · DANE SYNTETYCZNE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                content()
            }
        }
    }
    private val time = 1_788_858_000_000L
    private fun sms(id: Long, body: String, incoming: Boolean = true, read: Boolean = true) =
        DeviceSms(id, id, "+12025550123", body, time + id * 60_000,
            if (incoming) Telephony.Sms.MESSAGE_TYPE_INBOX else Telephony.Sms.MESSAGE_TYPE_SENT, read)

    @Test fun inboxDarkAndCompactLight() {
        var appearance by mutableStateOf(Appearance(mode = ThemeMode.DARK))
        rule.setContent { MyApplicationTheme(appearance = appearance) { Frame("Wiadomości") {
            OutlinedTextField("", {}, placeholder = { Text("Szukaj rozmowy lub treści") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(true, {}, label = { Text("Wszystkie") }); FilterChip(false, {}, label = { Text("Nieprzeczytane") }); FilterChip(false, {}, label = { Text("Przypięte") })
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmsThreadRow(sms(1, "Spotkajmy się jutro o 17. Pasuje?", read = false), "Alicja • demo", true, true, "", {}, {})
                SmsThreadRow(sms(2, "Dzięki za pomoc. Wszystko działa!"), "Marek • demo", false, true, "", {}, {})
                SmsThreadRow(sms(3, "Jestem już w drodze."), "Ola • demo", false, false, "Będę za chwilę", {}, {})
                SmsThreadRow(sms(4, "Zadzwoń, kiedy będziesz mieć chwilę."), "Tomek • demo", false, false, "", {}, {})
            }
            Spacer(Modifier.weight(1f))
            ExtendedFloatingActionButton(onClick = {}, icon = { Icon(Icons.Outlined.Edit, null) }, text = { Text("Nowy SMS") }, modifier = Modifier.align(Alignment.End))
            Text("Szkice i przypięcia zapisane lokalnie", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
        rule.onNodeWithText("Szkic · Będę za chwilę").assertExists()
        capture("01-wiadomosci-ciemne.png")
        rule.runOnIdle { appearance = appearance.copy(mode = ThemeMode.LIGHT, accent = Accent.OCEAN, compact = true) }
        capture("02-wiadomosci-jasne.png")
    }
    @Test fun conversationShowsDirectionAndHonestState() {
        rule.setContent { MyApplicationTheme(appearance = Appearance(mode = ThemeMode.DARK, accent = Accent.MINT)) { Frame("Alicja • demo") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContactAvatar("Alicja"); Text("SMS · +1 202 555 0123", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("8 września · rozmowa przykładowa", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            MessageBubble(sms(1, "Cześć! Masz chwilę, żeby pokazać mi nowy interfejs?"), null) {}
            MessageBubble(sms(2, "Jasne. Możesz wybrać motyw, kolor i rozmiar tekstu.", false), null) {}
            MessageBubble(sms(3, "Super, wybieram miętę 🌿"), null) {}
            Spacer(Modifier.weight(1f))
            OutlinedTextField("", {}, placeholder = { Text("Wiadomość SMS") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Outlined.Edit, null) }, shape = MaterialTheme.shapes.extraLarge)
            Text("Zrzut komponentów UI. Nie wysłano SMS-a.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
        rule.onNodeWithText(" · wysłano", substring = true).assertExists()
        capture("03-rozmowa-mieta.png")
    }
    @Test fun keypadActuallyEditsNumberAndLongPressZeroAddsPlus() {
        rule.setContent { MyApplicationTheme(appearance = Appearance(mode = ThemeMode.DARK)) { Frame("") {
            Box(Modifier.weight(1f)) { DialerTab({ false }, 0, {}, {}, MaterialTheme.colorScheme.error) }
            Text("Ekran telefonu · bez połączenia operatora", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
        rule.onNodeWithTag("dial_key_0").performTouchInput { longClick() }
        rule.onNodeWithTag("dial_input").assertTextContains("+")
        rule.onNodeWithTag("dial_key_4").performClick()
        rule.onNodeWithTag("dial_input").assertTextContains("+4")
        rule.onNodeWithTag("dial_input").performTextReplacement("")
        capture("04-telefon.png")
    }
    @Test fun appearanceControlsChangeTheme() {
        var value by mutableStateOf(Appearance(mode = ThemeMode.DARK))
        rule.setContent { MyApplicationTheme(appearance = value) { Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("CzyToTy? · DEMO", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))
                AppearanceControls(value, { value = it })
            }
        } } }
        rule.onNodeWithTag("theme_LIGHT").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals(ThemeMode.LIGHT, value.mode) }
        capture("05-personalizacja.png")
    }
    @Test fun realComposerKeepsDraftWhenReturningToInbox() {
        val application = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        application.deleteDatabase("sms-conversations.db")
        val models = androidx.lifecycle.ViewModelStore()
        val model = androidx.lifecycle.ViewModelProvider(models, androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(application))[com.example.sms.SmsViewModel::class.java]
        try {
            rule.setContent { MyApplicationTheme(appearance = Appearance(mode = ThemeMode.LIGHT, accent = Accent.OCEAN)) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("CzyToTy? · DEMO · bez wysyłki", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(16.dp))
                        SmsTabScrollable(null, "", {}, {}, MaterialTheme.colorScheme.error, model)
                    }
                }
            } }
            rule.waitUntil(10_000) { model.metadata.value.ready }
            rule.onNodeWithTag("sms_new").performClick()
            rule.onNodeWithTag("sms_send").assertIsNotEnabled()
            rule.onNodeWithTag("sms_address").performTextReplacement("+12025550123")
            rule.onNodeWithTag("sms_body").performTextReplacement("Cześć! To szkic wiadomości demo.")
            rule.onNodeWithTag("sms_send").assertIsEnabled()
            rule.onNodeWithContentDescription("Wróć do wiadomości").performClick()
            rule.onNodeWithTag("sms_new").performClick()
            rule.onNodeWithTag("sms_body").assertTextContains("Cześć! To szkic wiadomości demo.")
            rule.waitUntil(10_000) { com.example.sms.ConversationStore(application).use { it.drafts()["new"]?.body == "Cześć! To szkic wiadomości demo." } }
            capture("06-edytor-sms.png")
        } finally { models.clear() }
    }
}
