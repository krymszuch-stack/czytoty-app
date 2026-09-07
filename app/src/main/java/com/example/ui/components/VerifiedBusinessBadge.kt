package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.VerifiedBlueContainer
import com.example.ui.theme.VerifiedBlueOnContainer

@Composable
fun VerifiedBusinessBadge(
  label: String = "Numer w rejestrze publicznym",
  orgName: String? = null,
  showInfoDialogOnTap: Boolean = true,
  modifier: Modifier = Modifier
) {
  var showDialog by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = VerifiedBlueContainer,
    modifier = modifier
      .testTag("verified_badge")
      .then(
        if (showInfoDialogOnTap) {
          Modifier.clickable { showDialog = true }
        } else Modifier
      )
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Verified,
        contentDescription = "Numer w rejestrze",
        tint = VerifiedBlue,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.SemiBold
        ),
        color = VerifiedBlueOnContainer
      )
    }
  }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Default.Verified,
          contentDescription = null,
          tint = VerifiedBlue,
          modifier = Modifier.size(32.dp)
        )
      },
      title = {
        Text(
          text = orgName?.let { "$it • Tożsamość numeru" } ?: "Numer w rejestrze publicznym",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Text(
          text = "Ten numer figuruje w publicznym wykazie jako przypisany do danej instytucji.\n\nWAŻNE: Status ten dotyczy wyłącznie numeru, a NIE tożsamości dzwoniącej osoby. Aplikacja nie może zagwarantować autentyczności rozmówcy, gdyż identyfikator dzwoniącego (Caller ID) może zostać sfałszowany w technologii GSM (spoofing).",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        TextButton(
          onClick = { showDialog = false },
          modifier = Modifier.testTag("verified_dialog_dismiss_button")
        ) {
          Text("Rozumiem")
        }
      }
    )
  }
}
