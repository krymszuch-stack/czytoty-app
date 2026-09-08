package com.example

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.tabs.DialerTabScrollable
import com.example.ui.tabs.SmsTabScrollable
import com.example.ui.theme.MyApplicationTheme
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val model: PrototypeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        handleShared(intent)
        setContent { MyApplicationTheme {
            val dark = MaterialTheme.colorScheme.background.luminance() < .5f
            SideEffect { enableEdgeToEdge(
                statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                navigationBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)) }
            PrototypeApp(model)
        } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShared(intent)
    }
    private fun handleShared(intent: Intent?) {
        if (intent?.action == Intent.ACTION_DIAL) model.acceptDial(intent.data?.schemeSpecificPart.orEmpty())
        if (intent?.getBooleanExtra("open_sms", false) == true) model.requestSms()
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain")
            model.acceptSharedText(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
    }
}

private fun timestamp(value: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(value))
private val Amber: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < .5f) Color(0xFFFFD089) else Color(0xFF805000)

@Composable
private fun PrototypeApp(model: PrototypeViewModel) {
    val tabStates = rememberSaveableStateHolder()
    val state by model.state.collectAsStateWithLifecycle()
    val phoneSession by com.example.calling.CallSession.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf("") }
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var smsPrefill by rememberSaveable { mutableStateOf("") }
    var smsConversationOpen by remember { mutableStateOf(false) }
    var dialPrefill by rememberSaveable { mutableStateOf<String?>(null) }
    var roleGranted by remember { mutableStateOf(false) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val currentTime = maxOf(clock, System.currentTimeMillis())
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }
    var appearanceOpen by remember { mutableStateOf(false) }
    if (appearanceOpen) com.example.ui.theme.AppearanceSheet { appearanceOpen = false }
    fun updateRole() {
        roleGranted = Build.VERSION.SDK_INT >= 29 &&
            context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateRole() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { updateRole(); model.reloadLocal() }
        }
        owner.lifecycle.addObserver(observer)
        updateRole()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                clock = System.currentTimeMillis()
                model.refreshIfNeeded()
                delay(30_000)
            }
        }
    }
    LaunchedEffect(state.notice) {
        state.notice?.let { snackbar.showSnackbar(it); model.dismissNotice() }
    }
    LaunchedEffect(state.sharedText) {
        if (state.sharedText.isNotEmpty()) {
            message = state.sharedText; tab = 2; model.consumeSharedText()
        }
    }
    val labels = listOf("Telefon", "SMS", "Sprawdź", "Numery", "Ochrona", "Konto")
    LaunchedEffect(state.dialNumber) { state.dialNumber?.let { dialPrefill = it; tab = 0; model.consumeDial() } }
    LaunchedEffect(state.openSms) { if (state.openSms) { tab = 1; model.consumeSmsRequest() } }
    val icons = listOf(R.drawable.ic_phone_lookup, R.drawable.ic_messages_sms, R.drawable.ic_search, R.drawable.ic_account, R.drawable.ic_protection_shield, R.drawable.ic_account)
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tab != 1 || !smsConversationOpen) NavigationBar {
                listOf(0, 1, 2, 4).forEach { index ->
                    val label = labels[index]
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = { Icon(painterResource(icons[index]), null, modifier = Modifier.size(24.dp)) }, label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("tab_" + index))
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab != 1 || !smsConversationOpen) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.ic_brand_mark), "Logo CzyToTy?", modifier = Modifier.size(28.dp))
                Text("CzyToTy?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { appearanceOpen = true }, modifier = Modifier.testTag("appearance")) { Icon(Icons.Outlined.Palette, "Dostosuj wygląd") }
                IconButton(onClick = { tab = 3 }, modifier = Modifier.testTag("tab_3")) { Icon(Icons.Outlined.Contacts, "Numery i notatki") }
                IconButton(onClick = { tab = 5 }, modifier = Modifier.testTag("tab_5")) { Icon(painterResource(R.drawable.ic_account), "Konto", modifier = Modifier.size(24.dp)) }
            }
            if (phoneSession.calls.any { it.live }) FilledTonalButton(onClick = {
                context.startActivity(Intent(context, com.example.calling.InCallActivity::class.java))
            }, modifier = Modifier.fillMaxWidth().testTag("return_to_call")) {
                Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(8.dp)); Text("Wróć do połączenia")
            }
            tabStates.SaveableStateProvider(tab) {
            when (tab) {
                0 -> {
                    DialerTabScrollable(
                        model = model,
                        blockedLookup = { phone -> state.notes.any { it.phone == phone && it.blocked } },
                        notesCount = state.notes.size,
                        onEditNumber = { phoneInput = it; tab = 3 },
                        onSmsNumber = { smsPrefill = it; tab = 1 },
                        initialNumber = dialPrefill,
                        onNumberConsumed = { dialPrefill = null },
                        amber = Amber
                    )
                }
                1 -> {
                    SmsTabScrollable(
                        snapshot = state.snapshot,
                        initialAddress = smsPrefill,
                        onAddressConsumed = { smsPrefill = "" },
                        onCheckText = { message = it.take(20_000); model.clearAnalysis(); tab = 2 },
                        amber = Amber,
                        onConversationChanged = { smsConversationOpen = it }
                    )
                }
                2 -> {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Wiadomość lub link", style = MaterialTheme.typography.headlineSmall)
                    Text("Wklej treść SMS-a albo udostępnij ją do CzyToTy? z komunikatora. Analiza odbywa się na tym urządzeniu.")
                    SourceCard(state, currentTime, model::refreshCert)
                    OutlinedTextField(value = message, onValueChange = { message = it.take(20_000); model.clearAnalysis() },
                        label = { Text("Treść wiadomości lub adres strony") }, minLines = 4, maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("message_input"))
                    Button(onClick = { model.analyze(message) }, enabled = message.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().testTag("analyze")) {
                        Icon(Icons.Outlined.Search, null); Spacer(Modifier.width(8.dp)); Text("Sprawdź treść i domeny")
                    }
                    state.analysis?.let {
                        AnalysisCard(it, it.snapshotTime?.let { time -> currentTime - time in 0..CERT_MAX_AGE_MS } == true)
                    }
                    if (message.isNotBlank()) {
                        TextButton(onClick = {
                            openIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:8080")).putExtra("sms_body", message))
                        }) { Text("Przekaż podejrzany SMS do CERT (8080)") }
                        Text("Otworzy się Twoja aplikacja SMS. Samodzielnie zatwierdzasz wysyłkę.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    InfoCard("Granice sprawdzania", "Lista CERT dotyczy domen phishingowych. Nie sprawdza tożsamości rozmówcy. Nowa niebezpieczna strona może jeszcze nie figurować na liście; przekierowania i skrócone linki nie są rozwijane.")
                    }
                }
                3 -> {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Twoje numery", style = MaterialTheme.typography.headlineSmall)
                    Text("Sprawdź format, dodaj własną notatkę lub blokadę. Numery bez prefiksu kraju traktujemy jako polskie.")
                    OutlinedTextField(phoneInput, { phoneInput = it.take(40) }, label = { Text("Numer telefonu") },
                        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("phone_input"))
                    val parsed = remember(phoneInput) { PhoneNumbers.parse(phoneInput) }
                    if (phoneInput.isNotBlank() && parsed == null) Text("Wpisz pełny, prawidłowy numer, np. z prefiksem +48.", color = Amber)
                    if (parsed != null) {
                        PhoneEditor(parsed, state, model, roleGranted)
                    }
                    Text("Zapisane lokalnie · ${state.notes.size}", style = MaterialTheme.typography.titleMedium)
                    if (state.notes.isEmpty()) Text("Nie masz jeszcze zapisanych numerów. Lista zaczyna się pusta.")
                    state.notes.forEach { note ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(note.label.ifBlank { note.phone }, fontWeight = FontWeight.Bold)
                                if (note.label.isNotBlank()) Text(note.phone)
                                Text(if (note.blocked) "Na Twojej liście blokad" else "Twoja notatka", color = if (note.blocked) Amber else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (note.note.isNotBlank()) Text(note.note)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { phoneInput = note.phone }) { Text("Edytuj") }
                                    TextButton(onClick = { model.removeLocal(note.phone) }) { Text("Usuń lokalnie") }
                                }
                            }
                        }
                    }
                    }
                }
                4 -> {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Ochrona połączeń", style = MaterialTheme.typography.headlineSmall)
                    InfoCard(
                        if (roleGranted && state.protectionEnabled) "Blokowanie Twojej listy jest włączone" else "Blokowanie jest nieaktywne",
                        if (roleGranted) "Aplikacja ma systemową rolę filtrowania połączeń. Blokuje wyłącznie numery, które samodzielnie dodasz do listy."
                        else "Aby blokować zapisane numery, nadaj CzyToTy? rolę identyfikowania i filtrowania połączeń w Androidzie."
                    )
                    if (!roleGranted) Button(onClick = {
                        if (Build.VERSION.SDK_INT >= 29) {
                            val manager = context.getSystemService(RoleManager::class.java)
                            if (manager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true)
                                roleLauncher.launch(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                            else Toast.makeText(context, "Ten telefon nie udostępnia roli filtrowania połączeń.", Toast.LENGTH_LONG).show()
                        } else Toast.makeText(context, "Konfiguracja filtrowania w prototypie wymaga Androida 10 lub nowszego.", Toast.LENGTH_LONG).show()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Włącz filtrowanie w Androidzie") }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Blokuj numery z mojej listy", Modifier.weight(1f))
                        Switch(checked = state.protectionEnabled, onCheckedChange = model::setProtection)
                    }
                    Text("Zapisane blokady: ${state.notes.count { it.blocked }}")
                    Text("Rejestr filtrowania · ${state.events.size}", style = MaterialTheme.typography.titleMedium)
                    Text("Zdarzenia odebrane przez tę aplikację, maksymalnie 200. To nie jest pełna historia rozmów. Android może pomijać numery z kontaktów i połączenia o ograniczonej prezentacji numeru.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { model.reloadLocal() }) { Text("Odśwież rejestr") }
                    if (state.events.isEmpty()) Text("Nie zarejestrowano jeszcze żadnych połączeń.")
                    state.events.take(50).forEach { event ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(event.phone, fontWeight = FontWeight.Bold)
                                Text(if (event.blocked) "Odrzucono według Twojej listy" else "Dopuszczono przez filtr")
                                Text(timestamp(event.time), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (state.events.isNotEmpty()) TextButton(onClick = model::clearEvents) { Text("Wyczyść rejestr na urządzeniu") }
                    InfoCard("Tożsamość rozmówcy", "Numer wyświetlany podczas połączenia może być sfałszowany. Prototyp nie nasłuchuje rozmów, nie rozpoznaje głosu i nie potwierdza, że dzwoni bank lub inna instytucja.")
                    }
                }
                5 -> {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Prywatne notatki w chmurze", style = MaterialTheme.typography.headlineSmall)
                    Text("Konto jest opcjonalne. Sprawdzanie treści i blokady działają bez logowania.")
                    InfoCard("Supabase · czytoty-app", state.cloudStatus)
                    OutlinedButton(onClick = model::checkCloud, enabled = !state.busy) { Text("Sprawdź połączenie z bazą") }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.email == null) AuthForm(state.busy, model::authenticate)
                    else {
                        Text(state.email.orEmpty(), fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = model::loadCloud, enabled = !state.busy) { Text("Pobierz notatki") }
                            TextButton(onClick = model::signOut, enabled = !state.busy) { Text("Wyloguj") }
                        }
                        Text("Notatki na koncie · ${state.cloudNotes.size} (do 100)", style = MaterialTheme.typography.titleMedium)
                        if (state.cloudNotes.isEmpty()) Text("Brak pobranych notatek. Zapisz wybraną notatkę w zakładce Numery.")
                        state.cloudNotes.forEach { note ->
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(note.label.ifBlank { note.phone }, fontWeight = FontWeight.Bold)
                                    Text(note.phone)
                                    Text(note.note)
                                    OutlinedButton(onClick = { model.restoreLocal(note) }, enabled = !state.busy) { Text("Kopiuj na urządzenie") }
                                    TextButton(onClick = { model.removeCloud(note.phone) }, enabled = !state.busy) { Text("Usuń z chmury") }
                                }
                            }
                        }
                    }
                    InfoCard("Co trafia do chmury?", "Tylko numer, etykieta i notatka, które zapiszesz przyciskiem „Zapisz w chmurze”, oraz dane konta. Notatki są prywatne, dostępne po zalogowaniu. SMS-y, sprawdzane linki, blokady i rejestr połączeń pozostają na urządzeniu.")
                    TextButton(onClick = { openIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://cert.pl/lista-ostrzezen/"))) }) { Text("Źródło danych: CERT Polska / NASK") }
                    Text("${BuildConfig.VERSION_NAME} · Android 8+; filtrowanie konfigurowane na Androidzie 10+.",
                        style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SourceCard(state: PrototypeState, now: Long, refresh: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lista Ostrzeżeń CERT Polska", fontWeight = FontWeight.Bold)
            val snapshot = state.snapshot
            Text(when {
                state.refreshing -> "Pobieranie aktualnej listy…"
                snapshot == null -> "Brak pobranej listy. Sprawdzanie domen jest niedostępne."
                snapshot.isFresh(now) -> "${snapshot.domains.size} domen · świeża kopia"
                else -> "${snapshot.domains.size} domen · kopia wymaga aktualizacji"
            }, color = if (snapshot?.isFresh(now) == true) MaterialTheme.colorScheme.primary else Amber)
            snapshot?.let { Text("Pobrano: " + timestamp(it.fetchedAt), style = MaterialTheme.typography.bodySmall) }
            if (state.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
            else OutlinedButton(onClick = refresh) { Text("Aktualizuj listę CERT") }
        }
    }
}

@Composable
private fun AnalysisCard(result: MessageAnalysis, fresh: Boolean) {
    val matches = result.findings.filter { it.matchedDomain != null }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = if (matches.isNotEmpty()) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    )) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when {
                result.snapshotTime == null -> "Brak danych do sprawdzenia domen"
                matches.isNotEmpty() && fresh -> "Ostrzeżenie CERT: wykryto domenę z listy"
                matches.isNotEmpty() -> "Domena figuruje w zapisanej kopii listy"
                result.findings.isEmpty() -> "Nie znaleziono rozpoznawalnego adresu"
                else -> "Nie znaleziono domen na liście CERT"
            }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("analysis_result"))
            if (!fresh) Text("Lista nie jest aktualna lub nie została pobrana. Ten wynik nie opisuje bieżącego stanu CERT.")
            result.findings.forEach {
                Text(it.host, fontWeight = FontWeight.Bold)
                Text(when {
                    result.snapshotTime == null -> "Nie sprawdzono — brak listy."
                    it.matchedDomain != null -> "Wpis CERT: ${it.matchedDomain}"
                    else -> "Brak wpisu w sprawdzonej kopii. To nie potwierdza bezpieczeństwa."
                })
            }
            result.localSignals.forEach { Text(it) }
            if (matches.isNotEmpty()) Text("Nie otwieraj podejrzanego linku. Sprawdź sprawę w oficjalnej aplikacji lub niezależnie znalezionym kanale kontaktu.")
            Text("Analiza tekstu jest oparta na prostych regułach. Nie rozstrzyga, czy wiadomość jest oszustwem.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PhoneEditor(phone: ParsedPhone, state: PrototypeState, model: PrototypeViewModel, roleGranted: Boolean) {
    val saved = state.notes.find { it.phone == phone.e164 }
    var label by remember(phone.e164, saved) { mutableStateOf(saved?.label.orEmpty()) }
    var note by remember(phone.e164, saved) { mutableStateOf(saved?.note.orEmpty()) }
    var blocked by remember(phone.e164, saved) { mutableStateOf(saved?.blocked == true) }
    val context = LocalContext.current
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(phone.formatted, style = MaterialTheme.typography.titleLarge)
            Text("Plan numeracji: ${phone.country} · poprawny format")
            Text("Format sprawdzono na podstawie Google libphonenumber. To nie potwierdza aktywności numeru, jego właściciela ani reputacji.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(label, { label = it.take(80) }, label = { Text("Twoja etykieta") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it.take(500) }, label = { Text("Prywatna notatka") },
                minLines = 2, modifier = Modifier.fillMaxWidth().testTag("note_input"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dodaj do mojej listy blokad", Modifier.weight(1f))
                Switch(blocked, { blocked = it }, modifier = Modifier.testTag("blocked_switch"))
            }
            if (blocked && (!roleGranted || !state.protectionEnabled)) Text("Blokada zacznie działać po zapisaniu i włączeniu filtrowania w zakładce Ochrona.", color = Amber)
            Button(onClick = { model.saveLocal(PhoneNote(phone.e164, label, note, blocked)) },
                modifier = Modifier.fillMaxWidth().testTag("save_local")) { Text("Zapisz na urządzeniu") }
            if (state.email != null) OutlinedButton(onClick = { model.saveCloud(PhoneNote(phone.e164, label, note)) }, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text("Zapisz w chmurze") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { openIntent(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone.e164, null))) }) { Text("Otwórz telefon") }
                TextButton(onClick = { openIntent(context, Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", phone.e164, null))) }) { Text("Otwórz SMS") }
            }
        }
    }
}

@Composable
private fun AuthForm(busy: Boolean, authenticate: (String, String, Boolean) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // Password stays in composition memory only; it is never persisted by saved instance state.
    var password by remember { mutableStateOf("") }
    OutlinedTextField(email, { email = it.take(254) }, label = { Text("Adres e-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(password, { password = it.take(128) }, label = { Text("Hasło (minimum 8 znaków)") },
        visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(onClick = { authenticate(email, password, false); password = "" }, enabled = !busy && email.isNotBlank() && password.length >= 8,
        modifier = Modifier.fillMaxWidth()) { Text("Zaloguj się") }
    OutlinedButton(onClick = { authenticate(email, password, true); password = "" }, enabled = !busy && email.isNotBlank() && password.length >= 8,
        modifier = Modifier.fillMaxWidth()) { Text("Utwórz konto e-mail") }
    Text("Rejestracja może wymagać potwierdzenia adresu przez wiadomość Supabase. Po potwierdzeniu wróć tutaj i zaloguj się.",
        style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun InfoCard(title: String, text: String) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun openIntent(context: android.content.Context, intent: Intent) {
    try { context.startActivity(intent) }
    catch (_: ActivityNotFoundException) { Toast.makeText(context, "Brak aplikacji obsługującej tę czynność.", Toast.LENGTH_LONG).show() }
}
