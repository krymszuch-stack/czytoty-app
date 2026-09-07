package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProtectionSettings
import com.example.ui.theme.RiskSafe
import com.example.ui.theme.SleekBorder

@Composable
fun ProtectionStatusCard(
  settings: ProtectionSettings,
  onToggleActive: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.5f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("protection_status_card")
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(
                if (settings.isProtectionActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
              )
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = if (settings.isProtectionActive) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(26.dp)
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          Column {
            Text(
              text = "Status ochrony",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = if (settings.isProtectionActive) "Aktywna" else "Wyłączona",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = if (settings.isProtectionActive) RiskSafe else MaterialTheme.colorScheme.error
            )
          }
        }

        Switch(
          checked = settings.isProtectionActive,
          onCheckedChange = onToggleActive,
          modifier = Modifier.testTag("protection_master_switch")
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Statistics Row (3 zablokowane dzisiaj, 7 ostrzeżeń)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Block,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "${settings.blockedTodayCount}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "zablokowane dzisiaj",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "${settings.warningsTodayCount}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "ostrzeżeń dzisiaj",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
