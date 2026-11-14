package com.example.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PhoneNote
import com.example.data.PhoneNumbers

/** Evidence from this device, never a reputation score or proof of the caller's identity. */
@Composable
fun NumberEvidenceCard(number: String, note: PhoneNote?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val saved = note?.takeIf { it.phone == PhoneNumbers.parse(number)?.e164 }
    val blocked = saved?.blocked == true
    val colors = MaterialTheme.colorScheme
    Card(modifier.fillMaxWidth().testTag("number_evidence"), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (blocked) colors.errorContainer else colors.surfaceVariant,
            contentColor = if (blocked) colors.onErrorContainer else colors.onSurface)) {
        Column(Modifier.padding(if (compact) 16.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if (blocked) Icons.Outlined.Block else if (saved != null) Icons.Outlined.PersonOutline else Icons.Outlined.HelpOutline, null)
                Text(if (blocked) "Na Twojej liście blokad" else if (saved != null) "Twoje oznaczenie numeru" else "Nieznany numer",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            saved?.label?.takeIf { it.isNotBlank() }?.let { Text(it, fontWeight = FontWeight.SemiBold) }
            if (!compact) saved?.note?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(if (saved != null) "Źródło: Twój zapis na urządzeniu" else "Brak Twojej notatki lub blokady dla tego numeru.",
                style = MaterialTheme.typography.bodySmall)
            Text("Tożsamość rozmówcy niepotwierdzona", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            if (!compact) Text("Etykieta i poprawny numer nie potwierdzają, kto dzwoni. Aplikacja nie ma podłączonej weryfikacji bankowej ani bazy reputacji numerów.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
