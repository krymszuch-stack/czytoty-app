package com.example.ui.tabs

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.PrototypeViewModel
import com.example.data.DialerData
import com.example.data.PhoneNote
import com.example.data.PhoneNumbers
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.CallHistoryEntry
import com.example.data.DeviceContact
import android.widget.Toast
import com.example.calling.DefaultPhone
import com.example.calling.DefaultPhoneSetup
import com.example.calling.DialTarget
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun DialerTab(
    model: PrototypeViewModel,
    blockedLookup: (String) -> Boolean,
    notesCount: Int,
    onEditNumber: (String) -> Unit,
    onSmsNumber: (String) -> Unit,
    amber: Color,
    initialNumber: String? = null,
    onNumberConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    var number by rememberSaveable { mutableStateOf("") }
    var contactFilter by rememberSaveable { mutableStateOf("") }
    var historyTick by remember { mutableIntStateOf(0) }
    var callLogGranted by remember { mutableStateOf(DialerData.hasPermission(context, Manifest.permission.READ_CALL_LOG)) }
    var contactsGranted by remember { mutableStateOf(DialerData.hasPermission(context, Manifest.permission.READ_CONTACTS)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        callLogGranted = grants[Manifest.permission.READ_CALL_LOG] == true || DialerData.hasPermission(context, Manifest.permission.READ_CALL_LOG)
        contactsGranted = grants[Manifest.permission.READ_CONTACTS] == true || DialerData.hasPermission(context, Manifest.permission.READ_CONTACTS)
        historyTick++
    }
    val parsed = remember(number) { PhoneNumbers.parse(number) }
    var history by remember { mutableStateOf(emptyList<CallHistoryEntry>()) }
    var contacts by remember { mutableStateOf(emptyList<DeviceContact>()) }
    var dataError by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            callLogGranted = DialerData.hasPermission(context, Manifest.permission.READ_CALL_LOG)
            contactsGranted = DialerData.hasPermission(context, Manifest.permission.READ_CONTACTS)
            historyTick++
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(callLogGranted, contactsGranted, contactFilter, historyTick) {
        dataError = false
        try {
            history = withContext(Dispatchers.IO) { DialerData.recentCalls(context) }
            contacts = withContext(Dispatchers.IO) { DialerData.contacts(context, contactFilter) }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { history = emptyList(); contacts = emptyList(); dataError = true }
    }

    var section by rememberSaveable { mutableIntStateOf(2) }
    LaunchedEffect(initialNumber) { if (initialNumber != null) { number = initialNumber; section = 2; onNumberConsumed() } }
    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val target = remember(number) { DialTarget.normalize(number) }
    var info by remember { mutableStateOf(false) }
    if (info) AlertDialog(onDismissRequest = { info = false }, title = { Text("Telefon w CzyToTy?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DefaultPhoneSetup()
            Text("Numer nie potwierdza tożsamości rozmówcy. Twoje notatki: $notesCount — edytor jest pod ikoną kontaktów u góry.", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton(onClick = { info = false }) { Text("Rozumiem") } })
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Telefon", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { info = true }) { Icon(Icons.Outlined.Info, "O funkcjach telefonu") }
        }
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)) {
            Row(Modifier.fillMaxWidth().padding(4.dp)) {
                listOf("Ostatnie", "Kontakty", "Klawiatura").forEachIndexed { index, label ->
                    TextButton(onClick = { section = index }, modifier = Modifier.weight(1f).testTag("dial_section_$index"),
                        colors = ButtonDefaults.textButtonColors(containerColor = if (section == index) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            contentColor = if (section == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(label) }
                }
            }
        }
        if (dataError) Text("Nie udało się odczytać danych telefonu. Sprawdź uprawnienia.", color = amber)
        if (section == 2) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(Modifier.height(12.dp))
                TextField(number, { number = it.take(40) }, placeholder = { Text("Wybierz numer", style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true, textStyle = MaterialTheme.typography.headlineLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().testTag("dial_input"),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                if (parsed != null) Text(parsed.formatted, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (parsed != null && blockedLookup(parsed.e164)) Text("Na Twojej liście blokad", color = amber)
                DialerKeypad(number) { number = it.take(40) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { parsed?.let { onSmsNumber(it.e164) } }, enabled = parsed != null, modifier = Modifier.testTag("dial_sms")) { Icon(Icons.Outlined.Message, "Napisz SMS") }
                    Button(onClick = {
                        if (DefaultPhone.isDefault(context) && ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
                            callPermission.launch(Manifest.permission.CALL_PHONE)
                        else target?.let { DefaultPhone.call(context, it) }
                    }, enabled = target != null, modifier = Modifier.width(140.dp).height(62.dp).testTag("dial_call"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BE0BB), contentColor = Color(0xFF10392A))) {
                        Icon(Icons.Outlined.Call, "Zadzwoń", modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { number = number.dropLast(1) }, modifier = Modifier.testTag("dial_backspace")) { Icon(Icons.Outlined.Backspace, "Usuń cyfrę") }
                }
                if (number.isNotEmpty()) Row {
                    TextButton(onClick = { parsed?.let { onEditNumber(it.e164) } }, enabled = parsed != null) { Text("Notatka / blokada") }
                    TextButton(onClick = { number = "" }) { Text("Wyczyść") }
                }
                if (number.isNotBlank() && target == null) Text("Sprawdź wpisany numer", style = MaterialTheme.typography.bodySmall, color = amber)
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    if ((section == 0 && !callLogGranted) || (section == 1 && !contactsGranted)) PermissionPanel(
                        if (section == 0) "Twoje ostatnie połączenia" else "Blisko Twoich kontaktów",
                        "Udostępnij dane z telefonu. Pozostaną na urządzeniu.", "Włącz dostęp",
                        { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS)) }, Modifier.testTag("dial_permissions"))
                    else if (section == 1) OutlinedTextField(contactFilter, { contactFilter = it.take(60) }, placeholder = { Text("Szukaj kontaktu") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth())
                    else TextButton(onClick = { historyTick++ }) { Text("Odśwież ostatnie połączenia") }
                }
                if (section == 0 && callLogGranted) {
                    if (history.isEmpty()) item { Text("Tutaj pojawią się Twoje połączenia.", modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(history.size) { index ->
                        val entry = history[index]
                        val name = entry.name?.takeIf { it.isNotBlank() } ?: entry.number.ifBlank { "Numer prywatny" }
                        Surface(onClick = { number = entry.number; section = 2 }, color = Color.Transparent) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                ContactAvatar(name)
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(entry.typeLabel(), style = MaterialTheme.typography.bodySmall, color = if (entry.type == android.provider.CallLog.Calls.MISSED_TYPE) amber else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.date)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onSmsNumber(entry.number) }, enabled = entry.number.isNotBlank()) { Icon(Icons.Outlined.Message, "SMS") }
                            }
                        }
                    }
                }
                if (section == 1 && contactsGranted) {
                    if (contacts.isEmpty()) item { Text("Nie znaleziono kontaktów.", modifier = Modifier.padding(vertical = 24.dp)) }
                    items(contacts.size) { index ->
                        val contact = contacts[index]
                        Surface(onClick = { number = contact.phone; section = 2 }, color = Color.Transparent) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                ContactAvatar(contact.name)
                                Column(Modifier.weight(1f)) { Text(contact.name, fontWeight = FontWeight.SemiBold); Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun DialerKeypad(number: String, onChange: (String) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val letters = listOf("", "ABC", "DEF", "GHI", "JKL", "MNO", "PQRS", "TUV", "WXYZ", "", "+", "")
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.chunked(3).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEachIndexed { columnIndex, key ->
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (pressed) .9f else 1f, label = "keyPress")
                    Surface(shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (pressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f),
                        modifier = Modifier.size(76.dp).graphicsLayer { scaleX = scale; scaleY = scale }.testTag("dial_key_$key")
                            .combinedClickable(interactionSource = interaction, indication = null,
                                onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange(number + key) },
                                onLongClick = if (key == "0") ({ haptics.performHapticFeedback(HapticFeedbackType.LongPress); onChange(number + "+") }) else null)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(key, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Normal)
                            Text(letters[rowIndex * 3 + columnIndex], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialerTabScrollable(model: PrototypeViewModel, blockedLookup: (String) -> Boolean, notesCount: Int,
    onEditNumber: (String) -> Unit, onSmsNumber: (String) -> Unit, amber: Color, initialNumber: String? = null, onNumberConsumed: () -> Unit = {}) {
    DialerTab(model, blockedLookup, notesCount, onEditNumber, onSmsNumber, amber, initialNumber, onNumberConsumed)
}
