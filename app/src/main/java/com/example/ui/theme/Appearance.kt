package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role

enum class ThemeMode(val label: String) { SYSTEM("Systemowy"), LIGHT("Jasny"), DARK("Ciemny") }
enum class Accent(val label: String) { LAVENDER("Lawenda"), OCEAN("Ocean"), MINT("Mięta") }
data class Appearance(
    val mode: ThemeMode = ThemeMode.SYSTEM, val accent: Accent = Accent.LAVENDER,
    val dynamic: Boolean = false, val compact: Boolean = false,
    val largeText: Boolean = false, val rounded: Boolean = true, val haptics: Boolean = true
)
val LocalAppearance = staticCompositionLocalOf { Appearance() }

class AppearanceStore(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun read() = Appearance(
        mode = ThemeMode.entries.firstOrNull { it.name == prefs.getString("mode", "") } ?: ThemeMode.SYSTEM,
        accent = Accent.entries.firstOrNull { it.name == prefs.getString("accent", "") } ?: Accent.LAVENDER,
        dynamic = prefs.getBoolean("dynamic", false), compact = prefs.getBoolean("compact", false),
        largeText = prefs.getBoolean("largeText", false), rounded = prefs.getBoolean("rounded", true),
        haptics = prefs.getBoolean("haptics", true)
    )
    fun save(value: Appearance) { prefs.edit().putString("mode", value.mode.name).putString("accent", value.accent.name)
        .putBoolean("dynamic", value.dynamic).putBoolean("compact", value.compact)
        .putBoolean("largeText", value.largeText).putBoolean("rounded", value.rounded)
        .putBoolean("haptics", value.haptics).apply() }
}

@Composable
fun rememberAppearance(): Appearance {
    val context = LocalContext.current
    val store = remember(context) { AppearanceStore(context) }
    var appearance by remember(store) { mutableStateOf(store.read()) }
    DisposableEffect(store) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> appearance = store.read() }
        store.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { store.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return appearance
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSheet(onDismiss: () -> Unit) {
    val appearance = LocalAppearance.current
    val context = LocalContext.current
    val store = remember(context) { AppearanceStore(context) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        AppearanceControls(appearance, store::save, Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp))
    }
}

@Composable
fun AppearanceControls(value: Appearance, onChange: (Appearance) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Po Twojemu", style = MaterialTheme.typography.headlineMedium)
        Text("Wygląd telefonu i wiadomości", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Motyw", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode ->
            FilterChip(value.mode == mode, { onChange(value.copy(mode = mode)) }, label = { Text(mode.label) }, modifier = Modifier.testTag("theme_${mode.name}"))
        } }
        Text("Kolor akcentu", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Accent.entries.forEach { accent ->
            FilterChip(!value.dynamic && value.accent == accent, { onChange(value.copy(accent = accent, dynamic = false)) }, label = { Text(accent.label) })
        } }
        if (Build.VERSION.SDK_INT >= 31) AppearanceSwitch("Kolory tapety", value.dynamic) { onChange(value.copy(dynamic = it)) }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
            Text("Tak będą wyglądały Twoje wiadomości.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = if (value.largeText) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
        }
        AppearanceSwitch("Większy tekst wiadomości", value.largeText) { onChange(value.copy(largeText = it)) }
        AppearanceSwitch("Zwarta lista rozmów", value.compact) { onChange(value.copy(compact = it)) }
        AppearanceSwitch("Zaokrąglone dymki", value.rounded) { onChange(value.copy(rounded = it)) }
        AppearanceSwitch("Wibracje klawiatury telefonu", value.haptics) { onChange(value.copy(haptics = it)) }
        TextButton(onClick = { onChange(Appearance()) }) { Text("Przywróć domyślny wygląd") }
    }
}

@Composable
private fun AppearanceSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, null)
    }
}
