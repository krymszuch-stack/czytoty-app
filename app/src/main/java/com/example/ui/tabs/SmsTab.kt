package com.example.ui.tabs

import android.Manifest
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
    onCheckText: (String) -> Unit, amber: Color, model: SmsViewModel = viewModel(),
    onConversationChanged: (Boolean) -> Unit = {}) {
    val state by model.state.collectAsStateWithLifecycle()
    val metadata by model.metadata.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var address by rememberSaveable { mutableStateOf("") }
    val draftKey = state.selectedThread?.toString() ?: "new"
    val storedDraft = metadata.drafts[draftKey] ?: SmsDraft()
    val draft = storedDraft.body
    val recipient = if (state.selectedThread == null) storedDraft.address else address
    var query by rememberSaveable { mutableStateOf("") }
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    var pinsOnly by rememberSaveable { mutableStateOf(false) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var pendingRecipient by rememberSaveable { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { model.reload() }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.reload() }
        owner.lifecycle.addObserver(observer)
        model.reload()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(5000) } }
    LaunchedEffect(initialAddress, metadata.ready) {
        if (initialAddress.isNotBlank() && metadata.ready) {
            // A new recipient must not inherit another person's unfinished message.
            val previous = metadata.drafts["new"] ?: SmsDraft()
            if (previous.body.isBlank() || PhoneNumbers.parse(previous.address)?.e164 == PhoneNumbers.parse(initialAddress)?.e164) {
                model.draft("new", previous.copy(address = initialAddress)); composing = true; model.select(null)
            } else { pendingRecipient = initialAddress }
            onAddressConsumed()
        }
    }
    fun backToInbox() {
        address = ""; model.select(null); composing = false
    }
    BackHandler(state.selectedThread != null || composing) { backToInbox() }
    pendingRecipient?.let { target -> AlertDialog(onDismissRequest = { pendingRecipient = null },
        title = { Text("Masz niedokończony SMS") },
        text = { Text("Szkic do ${metadata.drafts["new"]?.address.orEmpty()} pozostaje na telefonie. Rozpoczęcie nowej wiadomości do $target zastąpi ten szkic.") },
        confirmButton = { TextButton(onClick = { model.draft("new", SmsDraft(target)); model.select(null); composing = true; pendingRecipient = null }) { Text("Zastąp szkic") } },
        dismissButton = { TextButton(onClick = { pendingRecipient = null }) { Text("Zachowaj szkic") } }) }
    val unread = remember(state.messages) { state.messages.filter { it.incoming && !it.read }.map { it.thread }.toSet() }
    fun contactName(number: String) = state.contacts[PhoneNumbers.parse(number)?.e164 ?: number] ?: number
    val threads = remember(state.messages, query, unreadOnly, pinsOnly, state.contacts, metadata.pins) {
        visibleThreads(state.messages, query, unreadOnly, pinsOnly, metadata.pins, state.contacts)
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Wyślij SMS?") },
        text = { Text("Do: $recipient\n\n$draft\n\nWysyłka przez domyślną kartę SIM. Dłuższy tekst może zostać podzielony na kilka płatnych SMS-ów według taryfy operatora.") },
        confirmButton = { TextButton(onClick = { confirm = false; model.send(recipient, draft, draftKey) }) { Text("Wyślij przez operatora") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Wróć do edycji") } })
    var settings by remember { mutableStateOf(false) }
    val conversationOpen = composing || state.selectedThread != null
    val conversationCallback by rememberUpdatedState(onConversationChanged)
    DisposableEffect(conversationOpen) {
        conversationCallback(conversationOpen)
        onDispose { conversationCallback(false) }
    }
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
            if (conversationOpen && PhoneNumbers.parse(recipient) != null) IconButton(onClick = {
                com.example.calling.DefaultPhone.openDialer(context, recipient)
            }) { Icon(androidx.compose.material.icons.Icons.Outlined.Call, "Otwórz numer w telefonie") }
            IconButton(onClick = { settings = true }) { Icon(androidx.compose.material.icons.Icons.Outlined.MoreHoriz, "Ustawienia wiadomości") }
            if (!conversationOpen) IconButton(onClick = model::reload, modifier = Modifier.testTag("sms_refresh")) { Icon(androidx.compose.material.icons.Icons.Outlined.Refresh, "Odśwież") }
        }
        state.error?.let { Text(it, color = amber, modifier = Modifier.testTag("sms_error"), style = MaterialTheme.typography.bodySmall) }
        metadata.error?.let { Text(it, color = amber, style = MaterialTheme.typography.bodySmall) }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (conversationOpen) {
            if (state.selectedThread == null) OutlinedTextField(recipient, { model.draft(draftKey, SmsDraft(it.take(40), draft)) }, placeholder = { Text("Do: numer telefonu") }, singleLine = true,
                enabled = metadata.ready && !state.sending,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().testTag("sms_address"))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("sms_list"), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(state.sends.filter { it.address == PhoneNumbers.parse(recipient)?.e164 }, key = { "send_${it.id}" }) { send ->
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
                        ContactAvatar(recipient.ifBlank { "?" }, Modifier.size(64.dp))
                        Text("Zacznij rozmowę", style = MaterialTheme.typography.titleLarge)
                        Text("Napisz pierwszą wiadomość poniżej.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (recipient.isNotBlank() && PhoneNumbers.parse(recipient) == null) Text("Do odpowiedzi potrzebny jest pełny numer telefonu.", color = amber, style = MaterialTheme.typography.bodySmall)
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f)) {
                Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    TextField(draft, { model.draft(draftKey, SmsDraft(recipient, it.take(2000))) }, placeholder = { Text("Wiadomość SMS") }, minLines = 1, maxLines = 5,
                        enabled = metadata.ready && !state.sending,
                        modifier = Modifier.weight(1f).testTag("sms_body"),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                    FilledIconButton(onClick = {
                        if (SmsRepository(context).canSend()) confirm = true else permissions.launch(arrayOf(Manifest.permission.SEND_SMS))
                    }, enabled = metadata.ready && PhoneNumbers.parse(recipient) != null && draft.isNotBlank() && !state.sending,
                        modifier = Modifier.size(48.dp).testTag("sms_send")) { Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Send, "Wyślij SMS") }
                }
            }
            if (draft.isNotBlank()) Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = { onCheckText(draft) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Sprawdź przed wysłaniem", style = MaterialTheme.typography.labelMedium) }
                val parts = remember(draft) { android.telephony.SmsMessage.calculateLength(draft, false)[0] }
                Text("$parts SMS · ${draft.length}/2000", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            TextField(query, { query = it.take(100) }, placeholder = { Text("Szukaj rozmowy lub treści") }, leadingIcon = { Icon(androidx.compose.material.icons.Icons.Outlined.Search, null) },
                singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(Modifier.fillMaxSize().testTag("sms_list"), contentPadding = PaddingValues(bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.canRead) item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !unreadOnly && !pinsOnly, onClick = { unreadOnly = false; pinsOnly = false }, label = { Text("Wszystkie") })
                        FilterChip(selected = unreadOnly, onClick = { unreadOnly = true; pinsOnly = false }, label = { Text("Nieprzeczytane") })
                        FilterChip(selected = pinsOnly, onClick = { pinsOnly = true; unreadOnly = false }, label = { Text("Przypięte") })
                    } }
                    if (!state.canRead) item { PermissionPanel("Twoje rozmowy, w jednym miejscu", "Włącz skrzynkę SMS. Wiadomości zostają na telefonie.", "Włącz wiadomości", { requestInbox() }, Modifier.testTag("sms_permissions")) }
                    if (state.canRead && threads.isEmpty() && !state.loading && state.error == null) item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Forum, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(if (query.isNotBlank()) "Brak wyników" else if (pinsOnly) "Brak przypiętych rozmów" else if (unreadOnly) "Wszystko przeczytane" else "Miejsce na Twoje rozmowy", style = MaterialTheme.typography.titleLarge)
                            Text("Nową wiadomość napiszesz przyciskiem poniżej.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(threads, key = { "thread_${it.thread}" }) { sms ->
                        SmsThreadRow(sms, contactName(sms.address), sms.thread in unread, sms.thread in metadata.pins,
                            metadata.drafts[sms.thread.toString()]?.body.orEmpty(),
                            onOpen = { address = sms.address; model.select(sms.thread) }, onPin = { model.togglePin(sms.thread) })
                    }
                    if (state.canRead && state.messages.size >= state.limit && state.limit < 5000) item { TextButton(onClick = model::loadMore) { Text("Starsze wiadomości") } }
                }
                ExtendedFloatingActionButton(onClick = { model.select(null); composing = true; address = "" },
                    icon = { Icon(androidx.compose.material.icons.Icons.Outlined.Edit, null) }, text = { Text("Nowy SMS") },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd).padding(bottom = 12.dp).testTag("sms_new"), containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

