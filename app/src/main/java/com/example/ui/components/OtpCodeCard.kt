package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OtpCodeInfo

/**
 * Dedykowany komponent UX dla kodów jednorazowych (OTP).
 * Zgodnie z wytycznymi: nie zasypuje użytkownika 60+ chaotycznym ciągiem tekstu.
 * Eksponuje duży, czytelny kod z wyraźnym przyciskiem [Kopiuj kod].
 */
@Composable
fun OtpCodeCard(
  otpInfo: OtpCodeInfo,
  onCodeCopied: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val clipboardManager = LocalClipboardManager.current

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
      )
      .testTag("otp_code_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (otpInfo.issuerName != null) "${otpInfo.issuerName} • ${otpInfo.codeType}" else otpInfo.codeType,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Wielki, wyśrodkowany kod ze spacją dla czytelności (np. 847 213)
      val formattedCode = if (otpInfo.code.length == 6) {
        "${otpInfo.code.substring(0, 3)} ${otpInfo.code.substring(3)}"
      } else {
        otpInfo.code
      }

      Box(
        modifier = Modifier
          .background(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp)
          )
          .padding(horizontal = 24.dp, vertical = 10.dp)
      ) {
        Text(
          text = formattedCode,
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            fontFamily = FontFamily.Monospace
          ),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Przycisk kopiowania z dużym celem dotykowym (min 48dp)
      Button(
        onClick = {
          clipboardManager.setText(AnnotatedString(otpInfo.code))
          onCodeCopied?.invoke()
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("copy_otp_button")
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Kopiuj kod do schowka",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
      }
    }
  }
}
