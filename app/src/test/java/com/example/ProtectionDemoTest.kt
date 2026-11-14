package com.example

import android.graphics.Bitmap
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calling.*
import com.example.data.*
import com.example.ui.tabs.NumberEvidenceCard
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Synthetic demonstrations run the production analysis/local-block logic. No bank verification is mocked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "pl-rPL-w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProtectionDemoTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val phone = "+12025550123"
    private fun capture(name: String) {
        rule.waitForIdle()
        rule.runOnIdle {
            val view = rule.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val dir = File("../artifacts/linkedin/demo").apply { mkdirs() }
            File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Composable private fun Frame(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
        MyApplicationTheme(appearance = Appearance(mode = ThemeMode.DARK)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        Text("CzyToTy? · Ochrona", fontWeight = FontWeight.Bold)
                    }
                    Text("DEMO · DANE SYNTETYCZNE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    content()
                }
            }
        }
    }
    @Test fun suspiciousBankSmsUsesActualDomainAndTextRules() {
        val body = "BANK DEMO: konto zablokowane. Potwierdź hasło: https://bank-demo.example"
        val snapshot = CertSnapshot(System.currentTimeMillis(), setOf("bank-demo.example"))
        val result = DomainChecker.analyze(body, snapshot)
        assertEquals("bank-demo.example", result.findings.single().matchedDomain)
        assertEquals(2, result.localSignals.size)
        rule.setContent { Frame("SMS „z banku”", "Fikcyjna wiadomość i testowa lista domen. Tak pokazujemy wynik mechanizmu analizy.") {
            OutlinedCard(Modifier.fillMaxWidth()) { Text(body, Modifier.padding(16.dp)) }
            AnalysisCard(result, fresh = true, sourceLabel = "DEMO")
            Spacer(Modifier.weight(1f))
            Text("W aplikacji źródłem domen jest pobrana lista CERT. Analiza działa lokalnie i nie otwiera linku.", style = MaterialTheme.typography.bodySmall)
        } }
        rule.onNodeWithTag("analysis_result").assertTextContains("Ostrzeżenie DEMO", substring = true)
        capture("07-ochrona-sms-bank.png")
    }
    @Test fun missingDomainMatchNeverMeansSafe() {
        val result = DomainChecker.analyze("https://nowa-strona.example", CertSnapshot(System.currentTimeMillis(), setOf("inna-domena.example")))
        assertNull(result.findings.single().matchedDomain)
        rule.setContent { Frame("Czy numer jest bezpieczny?", "Brak danych nie jest pozytywną weryfikacją.") {
            Text(phone, style = MaterialTheme.typography.headlineSmall)
            NumberEvidenceCard(phone, null, compact = true)
            Text("A jeśli linku nie ma na liście?", style = MaterialTheme.typography.titleMedium)
            AnalysisCard(result, fresh = true, sourceLabel = "DEMO")
        } }
        rule.onNodeWithText("Tożsamość rozmówcy niepotwierdzona").assertExists()
        rule.onNodeWithText("To nie potwierdza bezpieczeństwa.", substring = true).assertExists()
        capture("08-ochrona-brak-danych.png")
    }
    @Test fun personalBankLabelOnIncomingScreenDoesNotVerifyBank() {
        val store = LocalStore.get(rule.activity)
        store.save(PhoneNote(phone, "Bank DEMO · moja etykieta", "Zapis testowy; nie jest to zweryfikowany bank."))
        try {
            rule.setContent { MyApplicationTheme(appearance = Appearance(mode = ThemeMode.DARK)) {
                Surface(Modifier.fillMaxSize()) { Column {
                    Text("DEMO · syntetyczne połączenie, bez operatora", Modifier.padding(16.dp), style = MaterialTheme.typography.labelSmall)
                    Box(Modifier.weight(1f)) {
                        CallScreen(CallSessionState(calls = listOf(CallItem("demo", phone, Call.STATE_RINGING, 0, false, false, false))), onClose = {})
                    }
                } }
            } }
            rule.waitUntil(10_000) { rule.onAllNodesWithText("Bank DEMO · moja etykieta").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText("Tożsamość rozmówcy niepotwierdzona").assertExists()
            rule.onNodeWithTag("call_answer").assertExists()
            capture("09-ochrona-kto-dzwoni.png")
        } finally { store.delete(phone) }
    }
    @Test fun localBlockUsesTheScreeningDecision() {
        val store = LocalStore.get(rule.activity)
        store.save(PhoneNote(phone, "Podejrzenie wyłudzenia · mój zapis", "W rozmowie proszono mnie o kod. Przykład demo, nie zgłoszenie o prawdziwym numerze.", true))
        try {
            assertTrue(PhoneProtection.shouldReject(PhoneNumbers.parse(phone)?.e164, true, store::isBlocked))
            assertFalse(PhoneProtection.shouldReject(PhoneNumbers.parse(phone)?.e164, false, store::isBlocked))
            assertFalse(PhoneProtection.shouldReject(null, true, store::isBlocked))
            assertFalse(PhoneProtection.shouldReject("+12025550124", true, store::isBlocked))
            val saved = store.note(phone)
            rule.setContent { Frame("Twój zapis → blokada", "Decyzja oparta na Twojej liście, bez zapytania do sieci.") {
                Text(phone, style = MaterialTheme.typography.headlineSmall)
                NumberEvidenceCard(phone, saved)
                Text("Jak działa odrzucanie?", style = MaterialTheme.typography.titleLarge)
                Text("1. Android przekazuje połączenie do usługi filtrowania.")
                Text("2. Aplikacja normalizuje numer i sprawdza Twoją blokadę w SQLite.")
                Text("3. Przy włączonym filtrowaniu zleca Androidowi odrzucenie połączenia.")
                Spacer(Modifier.weight(1f))
                Text("Wymaga nadanej roli filtrowania. Ten test sprawdza decyzję w kodzie; nie wykonał rzeczywistego odrzucenia połączenia.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            rule.onNodeWithText("Na Twojej liście blokad").assertExists()
            capture("10-ochrona-blokada.png")
        } finally { store.delete(phone) }
    }
}
