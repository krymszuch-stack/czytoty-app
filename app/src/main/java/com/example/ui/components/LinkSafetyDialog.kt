package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.LinkSafetyModalState
import com.example.ui.theme.AppBlue
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurplePrimary
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppRedContainer
import com.example.ui.theme.AppRedText
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Dialog weryfikacji linku zawartego w wiadomości SMS (Link Safety - Sekcja 48).
 *
 * REGULA BEZPIECZEŃSTWA:
 * Linki nigdy nie otwierają się automatycznie. Użytkownik widzi czystą domenę,
 * ocenę zgodności z oficjalnym katalogiem i świadomie podejmuje decyzję.
 */
@Composable
fun LinkSafetyDialog(
  state: LinkSafetyModalState,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val analysis = state.analysis
  val isHighRisk = analysis.isHighRisk || analysis.mismatchExplanation != null
  val isOfficial = state.isOfficialBrandDomain

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF161420)),
      border = BorderStroke(1.dp, if (isHighRisk) AppRed.copy(alpha = 0.5f) else AppBorder),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("link_safety_dialog")
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // 1. Ikona statusu
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
              when {
                isHighRisk -> AppRedContainer
                isOfficial -> Color(0xFF15291E)
                else -> Color(0xFF221F35)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when {
              isHighRisk -> Icons.Default.Warning
              isOfficial -> Icons.Default.CheckCircle
              else -> Icons.Default.Security
            },
            contentDescription = null,
            tint = when {
              isHighRisk -> AppRed
              isOfficial -> AppGreen
              else -> AppPurple
            },
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Tytuł
        Text(
          text = if (isHighRisk) "Weryfikacja adresu WWW (Ryzyko)" else "Analiza linku WWW",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Etykieta marki / ostrzeżenie
        Text(
          text = when {
            isOfficial -> "Oficjalna domena marki ${state.verifiedBrandName ?: ""}"
            isHighRisk -> "Uwaga: Wykryto cechy fałszywego adresu lub podszywania się"
            else -> "Domena niezweryfikowana w katalogu oficjalnym"
          },
          fontSize = 13.sp,
          color = if (isHighRisk) AppRedText else AppTextSecondary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Boks domeny
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0F0E16),
          border = BorderStroke(1.dp, if (isHighRisk) AppRed.copy(alpha = 0.3f) else AppBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "Domena docelowa:",
              fontSize = 11.sp,
              color = AppTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = analysis.domain,
              fontSize = 15.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = if (isHighRisk) AppRedText else AppTextPrimary
            )
            if (analysis.mismatchExplanation != null) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = analysis.mismatchExplanation,
                fontSize = 12.sp,
                color = AppRedText
              )
            } else if (analysis.isShortener) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Skrócony link (maskuje docelowy serwer).",
                fontSize = 12.sp,
                color = Color(0xFFFFB020)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Akcje: [Kopiuj link], [Otwórz mimo to], [Anuluj]
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("URL", analysis.fullUrl))
            Toast.makeText(context, "Skopiowano adres do schowka", Toast.LENGTH_SHORT).show()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = AppPurplePrimary),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_copy_link")
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Kopiuj adres linku", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = {
            try {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(analysis.fullUrl))
              context.startActivity(intent)
            } catch (_: Exception) {
              Toast.makeText(context, "Nie można otworzyć przeglądarki", Toast.LENGTH_SHORT).show()
            }
            onDismiss()
          },
          border = BorderStroke(1.dp, if (isHighRisk) AppRed.copy(alpha = 0.6f) else AppBorder),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_open_anyway")
        ) {
          Icon(
            Icons.Default.OpenInBrowser,
            contentDescription = null,
            tint = if (isHighRisk) AppRed else AppTextPrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isHighRisk) "Otwórz mimo ryzyka" else "Otwórz w przeglądarce",
            color = if (isHighRisk) AppRed else AppTextPrimary,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedButton(
          onClick = onDismiss,
          border = BorderStroke(1.dp, Color.Transparent),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Anuluj i zamknij", color = AppTextSecondary, fontSize = 13.sp)
        }
      }
    }
  }
}
