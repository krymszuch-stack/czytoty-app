package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safetycheck.model.AlertChannel
import com.example.safetycheck.model.TrustedContact
import com.example.safetycheck.repository.SafetyCheckRepository
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurpleLight
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary
import java.util.UUID

/**
 * Ekran ustawień modułu Safety Check (Tryb Pomocy).
 * Funkcja w 100% OPT-IN – wymaga świadomej konfiguracji przez użytkownika.
 */
@Composable
fun SafetyCheckSettingsScreen(
    repository: SafetyCheckRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by repository.config.collectAsState()

    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var newContactAllowLocation by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = AppDarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz", tint = AppTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AppPurpleLight)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Tryb Pomocy (Safety Check)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppTextPrimary)
                    Text(text = "Eksperymentalny asystent bezpieczeństwa", fontSize = 12.sp, color = AppTextSecondary)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. KARTA GŁÓWNA - 100% OPT-IN PRZEŁĄCZNIK
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                modifier = Modifier.fillMaxWidth().testTag("safety_check_opt_in_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Aktywuj Tryb Pomocy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lokalna detekcja nietypowych sygnałów stresu. Funkcja jest domyślnie wyłączona.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTextSecondary
                        )
                    }
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { repository.setEnabled(it) },
                        modifier = Modifier.testTag("safety_check_opt_in_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. ZAUFANE KONTAKTY (TRUSTED CONTACTS)
            Text(
                text = "Zaufane Kontakty (${config.trustedContacts.size}):",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = AppTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (config.trustedContacts.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Brak skonfigurowanych kontaktów. Dodaj osobę, którą aplikacja powiadomi w razie braku odpowiedzi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTextSecondary
                        )
                    }
                }
            } else {
                config.trustedContacts.forEach { contact ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AppPurpleLight)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.displayName, fontWeight = FontWeight.Bold, color = AppTextPrimary, fontSize = 14.sp)
                                Text("${contact.phone} • Kanał: ${contact.preferredChannel.displayName}", fontSize = 12.sp, color = AppTextSecondary)
                                if (contact.allowLocation) {
                                    Text("Lokalizacja: dołączana", fontSize = 11.sp, color = AppPurpleLight)
                                }
                            }
                            IconButton(onClick = { repository.removeTrustedContact(contact.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Usuń kontakt", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showAddContactDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("safety_check_add_contact_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dodaj Zaufany Kontakt")
            }

            // Formularz dodawania kontaktu w modalu lub sekcji
            if (showAddContactDialog) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Nowy Zaufany Kontakt:", fontWeight = FontWeight.Bold, color = AppTextPrimary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newContactName,
                            onValueChange = { newContactName = it },
                            label = { Text("Imię lub relacja (np. Mama, Brat)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newContactPhone,
                            onValueChange = { newContactPhone = it },
                            label = { Text("Numer telefonu (+48...)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Dołączaj link z lokalizacją", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary, modifier = Modifier.weight(1f))
                            Switch(checked = newContactAllowLocation, onCheckedChange = { newContactAllowLocation = it })
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { showAddContactDialog = false }, modifier = Modifier.weight(1f)) {
                                Text("Anuluj")
                            }
                            Button(
                                onClick = {
                                    if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                                        repository.addTrustedContact(
                                            TrustedContact(
                                                id = UUID.randomUUID().toString(),
                                                displayName = newContactName.trim(),
                                                phone = newContactPhone.trim(),
                                                preferredChannel = AlertChannel.SMS,
                                                allowLocation = newContactAllowLocation
                                            )
                                        )
                                        newContactName = ""
                                        newContactPhone = ""
                                        showAddContactDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Zapisz")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. PARAMETRY ODLICZANIA I LOKALIZACJI
            Text(text = "Parametry procedury sprawdzającej:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = AppTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Czas odliczania przed wysłaniem alertu:", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(5, 10, 15, 30).forEach { sec ->
                            val isSelected = config.countdownSeconds == sec
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) AppPurpleContainer else AppDarkBackground,
                                modifier = Modifier.weight(1f)
                            ) {
                                Button(
                                    onClick = { repository.updateConfig { it.copy(countdownSeconds = sec) } },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else AppSurface,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else AppTextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${sec}s", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dołączaj lokalizację GPS", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppTextPrimary)
                            Text("Pobierana wyłącznie w chwili triggera, bez ciągłego śledzenia.", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                        }
                        Switch(
                            checked = config.includeLocation,
                            onCheckedChange = { enabled -> repository.updateConfig { it.copy(includeLocation = enabled) } }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wysyłaj alert przez SMS", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppTextPrimary)
                            Text("Zezwala aplikacji na wysłanie wiadomości SMS do zaufanych kontaktów.", style = MaterialTheme.typography.bodySmall, color = AppTextSecondary)
                        }
                        Switch(
                            checked = config.isSmsAlertAllowed,
                            onCheckedChange = { enabled -> repository.updateConfig { it.copy(isSmsAlertAllowed = enabled) } }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. KLAUZULA ODPOWIEDZIALNOŚCI (PRODUCT DISCLAIMER)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AppPurpleLight, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Ważne zastrzeżenie prawne:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To jest asystent bezpieczeństwa (Safety Assistant), a nie system ratunkowy. " +
                                    "Aplikacja NIGDY nie zastępuje numeru alarmowego 112 ani 999. " +
                                    "W sytuacji realnego zagrożenia zawsze w pierwszej kolejności kontaktuj się ze służbami ratunkowymi.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
