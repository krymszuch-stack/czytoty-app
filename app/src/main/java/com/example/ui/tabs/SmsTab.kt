package com.example.ui.tabs

import android.Manifest
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.CertSnapshot
import com.example.data.DomainChecker
import com.example.data.PhoneNumbers
import com.example.sms.*
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun SmsTabScrollable(snapshot: CertSnapshot?, initialAddress: String, onAddressConsumed: () -> Unit,
    onCheckText: (String) -> Unit, amber: Color, model: SmsViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var address by rememberSaveable { mutableStateOf("") }
    var draft by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    var drafts by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var accepted by rememberSaveable { mutableIntStateOf(state.sendAccepted) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { model.reload() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.reload() }
        owner.lifecycle.addObserver(observer)
        model.reload()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(5000) } }
    LaunchedEffect(initialAddress) {
        if (initialAddress.isNotBlank()) { address = initialAddress; composing = true; model.select(null); onAddressConsumed() }
    }
    LaunchedEffect(state.sendAccepted) {
        if (state.sendAccepted > accepted) { draft = ""; accepted = state.sendAccepted }
    }
    fun backToInbox() {
        drafts = drafts + ((state.selectedThread?.toString() ?: "new") to draft)
        draft = drafts["new"].orEmpty(); address = ""; model.select(null); composing = false
    }
    BackHandler(state.selectedThread != null || composing) { backToInbox() }
    val unread = remember(state.messages) { state.messages.filter { it.incoming && !it.read }.map { it.thread }.toSet() }
    fun contactName(number: String) = state.contacts[PhoneNumbers.parse(number)?.e164 ?: number] ?: number
    val threads = remember(state.messages, query, unreadOnly, state.contacts) {
        state.messages.groupBy { it.thread }.values.map { it.first() }.filter {
            (!unreadOnly || it.thread in unread) && (query.isBlank() || contactName(it.address).contains(query, true) || it.address.contains(query, true) || it.body.contains(query, true))
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Wyślij SMS?") },
        text = { Text("Do: $address\n\n$draft\n\nWysyłka przez domyślną kartę SIM. Dłuższy tekst może zostać podzielony na kilka płatnych SMS-ów według taryfy operatora.") },
        confirmButton = { TextButton(onClick = { confirm = false; model.send(address, draft) }) { Text("Wyślij przez operatora") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Wróć do edycji") } })
    var settings by remember { mutableStateOf(false) }
    val conversationOpen = composing || state.selectedThread != null
    fun requestInbox() = permissions.launch(buildList {
        add(Manifest.permission.READ_SMS); add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray())
    if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text("Wiadomości i ochrona") },
        text = { Column { Text("SMS-y pozostają na telefonie. Ostrzeżenia korzystają z zapisanej listy CERT; brak ostrzeżenia nie potwierdza bezpieczeństwa.\n\nObecna aplikacja domyślna zapisuje SMS-y i stan przeczytania. MMS i RCS otwieraj w niej. Widok obejmuje do 5000 SMS-ów, wątek — do 500. Nazwy dopasowujemy do maksymalnie 500 wczytanych kontaktów.")
            TextButton(onClick = { settings = false; permissions.launch(arrayOf(Manifest.permission.READ_CONTACTS)) }) { Text("Pokaż nazwy z kontaktów") }
        } },
        confirmButton = { TextButton(onClick = { settings = false; requestInbox() }, modifier = Modifier.testTag("sms_permissions")) { Text("Uprawnienia") } },
        dismissButton = { TextButton(onClick = { settings = false }) { Text("Zamknij") } })
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (conversationOpen) IconButton(onClick = { backToInbox() }) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowBack, "Wróć do wiadomości")
            }
            Text(if (conversationOpen) "${if (state.selectedThread == null) "Nowy SMS" else contactName(address)}" else "Wiadomości",
                style = if (conversationOpen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = { settings = true }) { Icon(androidx.compose.material.icons.Icons.Outlined.MoreHoriz, "Ustawienia wiadomości") }
            if (!conversationOpen) IconButton(onClick = model::reload, modifier = Modifier.testTag("sms_refresh")) { Icon(androidx.compose.material.icons.Icons.Outlined.Refresh, "Odśwież") }
        }
        state.error?.let { Text(it, color = amber, modifier = Modifier.testTag("sms_error"), style = MaterialTheme.typography.bodySmall) }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (conversationOpen) {
            if (state.selectedThread == null) OutlinedTextField(address, { address = it.take(40) }, placeholder = { Text("Do: numer telefonu") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().testTag("sms_address"))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("sms_list"), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(state.sends.filter { it.address == PhoneNumbers.parse(address)?.e164 }, key = { "send_${it.id}" }) { send ->
                    Text(send.status(now), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("sms_receipt"))
                }
                state.conversation.reversed().groupBy { java.time.Instant.ofEpochMilli(it.time).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }.forEach { (date, messages) ->
                    items(messages, key = { "msg_${it.id}" }) { sms -> MessageBubble(sms, snapshot, onCheckText) }
                    item(key = "date_$date") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text(date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("pl"))),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (state.conversation.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ContactAvatar(address.ifBlank { "?" }, Modifier.size(64.dp))
                        Text("Zacznij rozmowę", style = MaterialTheme.typography.titleLarge)
                        Text("Napisz pierwszą wiadomość poniżej.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (address.isNotBlank() && PhoneNumbers.parse(address) == null) Text("Do odpowiedzi potrzebny jest pełny numer telefonu.", color = amber, style = MaterialTheme.typography.bodySmall)
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f)) {
                Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    TextField(draft, { draft = it.take(2000) }, placeholder = { Text("Wiadomość SMS") }, minLines = 1, maxLines = 5,
                        modifier = Modifier.weight(1f).testTag("sms_body"),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                    FilledIconButton(onClick = {
                        if (SmsRepository(context).canSend()) confirm = true else permissions.launch(arrayOf(Manifest.permission.SEND_SMS))
                    }, enabled = PhoneNumbers.parse(address) != null && draft.isNotBlank() && !state.sending,
                        modifier = Modifier.size(48.dp).testTag("sms_send")) { Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Send, "Wyślij SMS") }
                }
            }
            if (draft.isNotBlank()) TextButton(onClick = { onCheckText(draft) }, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(0.dp)) { Text("Sprawdź przed wysłaniem", style = MaterialTheme.typography.labelMedium) }
        } else {
            TextField(query, { query = it.take(100) }, placeholder = { Text("Szukaj rozmowy") }, leadingIcon = { Icon(androidx.compose.material.icons.Icons.Outlined.Search, null) },
                singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxSize().testTag("sms_list"), contentPadding = PaddingValues(bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.canRead) item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !unreadOnly, onClick = { unreadOnly = false }, label = { Text("Wszystkie") })
                        FilterChip(selected = unreadOnly, onClick = { unreadOnly = true }, label = { Text("Nieprzeczytane") })
                    } }
                    if (!state.canRead) item { PermissionPanel("Twoje rozmowy, w jednym miejscu", "Włącz skrzynkę SMS. Wiadomości zostają na telefonie.", "Włącz wiadomości", { requestInbox() }, Modifier.testTag("sms_permissions")) }
                    if (state.canRead && threads.isEmpty() && !state.loading && state.error == null) item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Forum, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(if (query.isBlank()) "Miejsce na Twoje rozmowy" else "Nie znaleziono rozmowy", style = MaterialTheme.typography.titleLarge)
                            Text("Nową wiadomość napiszesz przyciskiem poniżej.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(threads, key = { "thread_${it.thread}" }) { sms ->
                        Surface(onClick = { address = sms.address; draft = drafts[sms.thread.toString()].orEmpty(); model.select(sms.thread) }, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(vertical = 14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                ContactAvatar(contactName(sms.address))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(contactName(sms.address).ifBlank { "Nieznany nadawca" }, fontWeight = if (sms.thread in unread) FontWeight.Bold else FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
                                        Text(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(sms.time)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(sms.body, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (state.canRead && state.messages.size >= state.limit && state.limit < 5000) item { TextButton(onClick = model::loadMore) { Text("Starsze wiadomości") } }
                }
                ExtendedFloatingActionButton(onClick = { model.select(null); composing = true; address = ""; draft = drafts["new"].orEmpty() },
                    icon = { Icon(androidx.compose.material.icons.Icons.Outlined.Edit, null) }, text = { Text("Nowy SMS") },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(bottom = 12.dp).testTag("sms_new"), containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

