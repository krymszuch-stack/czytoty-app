package com.example.ui.screens.incall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Klawiatura DTMF (Dual-Tone Multi-Frequency) dla połączeń infolinii i IVR (Sekcja 87 - P0).
 */
@Composable
fun DtmfKeypadSheet(
  dialedSequence: String,
  onDigitPress: (Char) -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF14121E)),
    border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Uchwyt i przycisk ukrycia
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Klawiatura tonowa (IVR)",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = AppTextSecondary
        )
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("btn_close_dtmf")
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Ukryj klawiaturę",
            tint = AppTextPrimary,
            modifier = Modifier.size(28.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Podgląd wpisanych cyfr
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF0C0A12))
          .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (dialedSequence.isEmpty()) "Wybierz cyfrę z menu..." else dialedSequence,
          fontFamily = FontFamily.Monospace,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = if (dialedSequence.isEmpty()) AppTextMuted else Color(0xFF8E7CFF),
          maxLines = 1,
          modifier = Modifier.weight(1f)
        )

        if (dialedSequence.isNotEmpty()) {
          IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.Backspace,
              contentDescription = "Wyczyść",
              tint = AppTextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Klawisze numeryczne (Siatka 3x4)
      val keys = listOf(
        listOf(KeyData('1', ""), KeyData('2', "ABC"), KeyData('3', "DEF")),
        listOf(KeyData('4', "GHI"), KeyData('5', "JKL"), KeyData('6', "MNO")),
        listOf(KeyData('7', "PQRS"), KeyData('8', "TUV"), KeyData('9', "WXYZ")),
        listOf(KeyData('*', ""), KeyData('0', "+"), KeyData('#', ""))
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        for (row in keys) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            for (key in row) {
              DtmfButton(
                key = key,
                onClick = { onDigitPress(key.digit) }
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

private data class KeyData(val digit: Char, val subtext: String)

@Composable
private fun DtmfButton(
  key: KeyData,
  onClick: () -> Unit
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(68.dp)
      .clip(CircleShape)
      .background(Color(0xFF221F32))
      .clickable(onClick = onClick)
      .testTag("dtmf_key_${key.digit}")
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = key.digit.toString(),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      if (key.subtext.isNotEmpty()) {
        Text(
          text = key.subtext,
          fontSize = 9.sp,
          fontWeight = FontWeight.SemiBold,
          color = AppTextSecondary,
          letterSpacing = 1.sp
        )
      }
    }
  }
}
