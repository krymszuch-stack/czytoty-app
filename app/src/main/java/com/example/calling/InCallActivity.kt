package com.example.calling

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.PowerManager
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.data.DialerData
import com.example.ui.tabs.ContactAvatar
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        setContent { MyApplicationTheme(darkTheme = true) {
            val state by CallSession.state.collectAsStateWithLifecycle()
            val actualCall = state.calls.any { it.live }
            LaunchedEffect(actualCall) {
                if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(actualCall); setTurnScreenOn(actualCall) }
                else {
                    @Suppress("DEPRECATION")
                    val flags = android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    if (actualCall) window.addFlags(flags) else window.clearFlags(flags)
                }
            }
            CallScreen(state, intent?.getBooleanExtra("dialpad", false) == true, onClose = ::finish)
        } }
    }
}

@Composable
internal fun CallScreen(state: CallSessionState, initialDialpad: Boolean = false, onClose: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val call = state.calls.firstOrNull { it.ringing } ?: state.calls.firstOrNull { it.id == selected && it.live }
        ?: state.calls.firstOrNull { it.state == Call.STATE_ACTIVE } ?: state.calls.firstOrNull { it.live }
    var keypad by rememberSaveable { mutableStateOf(initialDialpad) }
    var audioDialog by remember { mutableStateOf(false) }
    var digits by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(call?.number) {
        name = null
        if (!call?.number.isNullOrBlank()) name = withContext(Dispatchers.IO) { runCatching { DialerData.contactName(context, call!!.number) }.getOrNull() }
    }
    LaunchedEffect(call?.id) { digits = ""; while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val nearEar = call?.state == Call.STATE_ACTIVE && state.audio.any { it.id == state.selectedAudio && it.nearEar }
    DisposableEffect(nearEar) {
        val power = context.getSystemService(PowerManager::class.java)
        val lock = if (nearEar && power.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK))
            power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "czytoty:proximity").apply { acquire(4 * 60 * 60 * 1000L) } else null
        onDispose { if (lock?.isHeld == true) lock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY) }
    }
    if (audioDialog) AlertDialog(onDismissRequest = { audioDialog = false }, title = { Text("Dźwięk rozmowy") },
        text = { Column { state.audio.forEach { endpoint ->
            TextButton(onClick = { CallSession.audio(endpoint.id); audioDialog = false }) {
                Text((if (endpoint.id == state.selectedAudio) "✓  " else "") + endpoint.label)
            }
        }; if (state.audio.isEmpty()) Text("Android nie przekazał jeszcze dostępnych urządzeń audio.") } },
        confirmButton = { TextButton(onClick = { audioDialog = false }) { Text("Zamknij") } })
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF25183F), Color(0xFF0A0A12), Color(0xFF11101C))))
        .safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("CzyToTy?", style = MaterialTheme.typography.titleMedium, color = Color(0xFFC49BFF))
        if (call == null) {
            Spacer(Modifier.height(72.dp))
            Icon(Icons.Outlined.CallEnd, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Brak trwającego połączenia", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("no_live_call"))
            Text("Ekran rozmowy pokazuje wyłącznie połączenia przekazane przez Androida.", textAlign = TextAlign.Center)
            FilledTonalButton(onClick = onClose) { Text("Wróć") }
        } else {
            Text(call.stateLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("call_state"))
            Spacer(Modifier.height(16.dp))
            ContactAvatar(name ?: call.number.ifBlank { "?" }, Modifier.size(112.dp))
            Text(name ?: call.number.ifBlank { if (call.conference) "Konferencja" else "Numer prywatny" }, style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            if (name != null) Text(call.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (call.state == Call.STATE_ACTIVE && call.connectedAt > 0) {
                val seconds = ((now - call.connectedAt) / 1000).coerceAtLeast(0)
                Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("call_timer"))
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = CallSession::dismissError) { Text("Zamknij komunikat") } }
            if (state.calls.count { it.live } > 1) {
                state.calls.filter { it.live && it.id != call.id }.forEach { other ->
                    OutlinedButton(onClick = { selected = other.id }) { Text("${other.number.ifBlank { "Inne połączenie" }} · ${other.stateLabel}") }
                }
            }
            call.accountChoices.forEach { (index, label) -> FilledTonalButton(onClick = { CallSession.account(call.id, index.toInt()) }) { Text(label) } }
            Spacer(Modifier.height(if (call.ringing) (LocalConfiguration.current.screenHeightDp * .10f).dp else 24.dp))
            if (call.ringing) {
                Text("Wyświetlany numer nie potwierdza tożsamości rozmówcy.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallControl("Odrzuć", Icons.Outlined.CallEnd, Color(0xFFE5697B), { CallSession.end(call.id) }, "call_reject")
                    CallControl("Odbierz", Icons.Outlined.Call, Color(0xFF8BE0BB), { CallSession.answer(call.id) }, "call_answer")
                }
            } else {
                if (keypad) {
                    Text(digits, style = MaterialTheme.typography.headlineSmall)
                    listOf("123", "456", "789", "*0#").forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { digit ->
                                Surface(onClick = {
                                    digits = (digits + digit).takeLast(30)
                                    CallSession.tone(call.id, digit)
                                    scope.launch { delay(180); CallSession.tone(call.id, null) }
                                }, enabled = call.state == Call.STATE_ACTIVE, shape = CircleShape, color = Color(0xFF272338), modifier = Modifier.size(62.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text(digit.toString(), style = MaterialTheme.typography.headlineSmall) }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CallControl(if (state.muted) "Wyciszony" else "Mikrofon", if (state.muted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                        if (state.muted) Color(0xFFC49BFF) else Color(0xFF353044), CallSession::mute, "call_mute")
                    CallControl("Dźwięk", Icons.Outlined.VolumeUp, Color(0xFF353044), { audioDialog = true }, "call_audio")
                    CallControl("Klawiatura", Icons.Outlined.Dialpad, if (keypad) Color(0xFFC49BFF) else Color(0xFF353044), { keypad = !keypad }, "call_keypad")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (call.canHold) TextButton(onClick = { CallSession.hold(call.id) }) { Text(if (call.state == Call.STATE_HOLDING) "Wznów" else "Zawieś") }
                    if (call.canMerge) TextButton(onClick = { CallSession.merge(call.id) }) { Text("Połącz rozmowy") }
                    if (state.canAdd) TextButton(onClick = { context.startActivity(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_DIAL)) }) { Text("Dodaj rozmowę") }
                }
                CallControl("Rozłącz", Icons.Outlined.CallEnd, Color(0xFFE5697B), { CallSession.end(call.id) }, "call_end")
            }
        }
    }
}

@Composable
private fun CallControl(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, tag: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledIconButton(onClick = onClick, modifier = Modifier.size(70.dp).testTag(tag), colors = IconButtonDefaults.filledIconButtonColors(containerColor = color,
            contentColor = if (color == Color(0xFF353044)) Color.White else Color(0xFF171321))) { Icon(icon, label, modifier = Modifier.size(28.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
