package com.example.ui.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sms.DeviceSms
import com.example.ui.theme.LocalAppearance
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SmsThreadRow(sms: DeviceSms, name: String, unread: Boolean, pinned: Boolean, draft: String,
    onOpen: () -> Unit, onPin: () -> Unit) {
    val compact = LocalAppearance.current.compact
    val colors = MaterialTheme.colorScheme
    val date = Instant.ofEpochMilli(sms.time).atZone(ZoneId.systemDefault())
    val time = date.format(DateTimeFormatter.ofPattern(if (date.toLocalDate() == LocalDate.now()) "HH:mm" else "d MMM", Locale.forLanguageTag("pl")))
    Surface(onClick = onOpen, shape = RoundedCornerShape(24.dp),
        color = if (unread) colors.primaryContainer.copy(alpha = .28f) else colors.surface,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = if (compact) 8.dp else 16.dp, bottom = if (compact) 8.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box {
                ContactAvatar(name)
                if (unread) Surface(Modifier.size(12.dp).align(Alignment.BottomEnd), shape = CircleShape, color = colors.primary) {}
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name.ifBlank { "Nieznany nadawca" }, fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(time, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Text(if (draft.isNotBlank()) "Szkic · $draft" else sms.body, maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium,
                    color = if (draft.isNotBlank()) colors.primary else colors.onSurfaceVariant)
            }
            IconButton(onClick = onPin) { Icon(Icons.Outlined.PushPin,
                if (pinned) "Odepnij rozmowę z $name" else "Przypnij rozmowę z $name",
                tint = if (pinned) colors.primary else colors.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
        }
    }
}
