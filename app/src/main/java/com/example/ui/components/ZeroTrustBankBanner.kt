package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallerInfo

/**
 * Specjalny komponent Zero Trust dla numerów przypisanych do banków i instytucji finansowych.
 * Rozwiązuje paradoks "Zweryfikowanego Banku" poprzez wyraźne rozdzielenie tożsamości
 * opublikowanego numeru od tożsamości dzwoniącej osoby (ryzyko podszywania się / CLI spoofingu).
 */
@Composable
fun ZeroTrustBankBanner(
  caller: CallerInfo,
  modifier: Modifier = Modifier
) {
  val orgName = caller.numberIdentity?.registeredEntityName ?: caller.displayName ?: "Instytucja finansowa"
  val isInboundOnly = caller.numberIdentity?.isInboundOnlyHotline == true

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("zero_trust_bank_banner")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Ostrzeżenie o spoofingu",
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Zero Trust: Ryzyko podszycia (Spoofing)",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.error
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Numer jest przypisany w rejestrze do: $orgName.",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Tożsamość dzwoniącego NIE może zostać potwierdzona przez aplikację. Identyfikator Caller ID może być dowolnie sfałszowany.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (isInboundOnly) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Infolinia tylko do połączeń przychodzących! Połączenie przychodzące z tego numeru jest wysoce nietypowe i potencjalnie podszyte.",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Text(
            text = "Żelazne zasady bezpieczeństwa:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "• Prawdziwy pracownik banku NIGDY nie prosi o podanie haseł, PIN-u ani kodów BLIK.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "• Bank NIGDY nie żąda instalacji aplikacji zdalnego pulpitu (AnyDesk, QuickSupport).",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
