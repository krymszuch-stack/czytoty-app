package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrandIdentity
import com.example.model.UrlSecurityAnalysis

/**
 * Bezpieczny komponent do prezentacji linku w SMS.
 * Zasada: Nigdy nie otwieraj linków automatycznie.
 * Pokazuje domenę i weryfikację z katalogiem oficjalnych domen marki.
 */
@Composable
fun SmsLinkWarningCard(
  analysis: UrlSecurityAnalysis,
  brand: BrandIdentity? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val isWarning = analysis.isHighRisk || !analysis.isRecognizedBrandDomain

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isWarning) {
      MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    },
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = if (isWarning) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
      )
      .testTag("sms_link_warning_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.Link,
          contentDescription = null,
          tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isWarning) "Weryfikacja adresu WWW" else "Link w wiadomości",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Wykryta domena docelowa:",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Text(
        text = analysis.domain,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          fontSize = 16.sp
        ),
        color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(6.dp))

      // Wyjaśnienie statusu domeny
      val explanationText = when {
        analysis.mismatchExplanation != null -> analysis.mismatchExplanation
        analysis.isShortener -> "Link używa skracacza adresów (${analysis.domain}), co uniemożliwia podgląd rzeczywistej strony docelowej."
        brand != null && analysis.isRecognizedBrandDomain -> "Domena należy do oficjalnego rejestru serwisów marki ${brand.displayName}."
        brand != null && !analysis.isRecognizedBrandDomain -> "Domena nie jest rozpoznawana jako oficjalna domena ${brand.displayName}."
        else -> "Nieznana domena. Zwróć uwagę, czy strona nie żąda podawania danych logowania lub haseł."
      }

      Text(
        text = explanationText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Skopiuj link
        OutlinedButton(
          onClick = {
            clipboardManager.setText(AnnotatedString(analysis.fullUrl))
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("copy_link_button")
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Kopiuj", style = MaterialTheme.typography.labelMedium)
        }

        // Otwórz z zachowaniem ostrożności
        Button(
          onClick = {
            try {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(analysis.fullUrl))
              context.startActivity(intent)
            } catch (_: Exception) {}
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier
            .weight(1.3f)
            .height(44.dp)
            .testTag("open_link_button")
        ) {
          Icon(
            imageVector = Icons.Default.OpenInBrowser,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isWarning) "Otwórz mimo to" else "Otwórz link",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }
    }
  }
}
