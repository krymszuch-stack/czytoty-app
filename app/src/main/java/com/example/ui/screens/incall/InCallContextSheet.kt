package com.example.ui.screens.incall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.incall.model.InCallContextInfo
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Rozwijana karta kontekstu rozmówcy (Sekcje 86, 88, 89 - COMPACT vs EXPANDED).
 */
@Composable
fun InCallContextSheet(
  contextInfo: InCallContextInfo?,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (contextInfo == null) return

  Card(
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF161422)),
    border = BorderStroke(1.dp, AppBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      // Uchwyt i nagłówek rozwijania
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("in_call_context_toggle"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF262238)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = AppPurple,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Kontekst tożsamości rozmówcy",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTextPrimary
          )
        }

        IconButton(
          onClick = onToggleExpand,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
            contentDescription = if (isExpanded) "Zwiń kontekst" else "Rozwiń kontekst",
            tint = AppTextSecondary
          )
        }
      }

      // KROK 1: ZAJAWKA W WIDOKU COMPACT (Ostatni SMS lub krótki status)
      if (!isExpanded) {
        if (contextInfo.lastSmsSnippet != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1E1B2C))
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Icon(
              imageVector = if (contextInfo.isSmsSensitive) Icons.Default.Lock else Icons.Default.Message,
              contentDescription = null,
              tint = if (contextInfo.isSmsSensitive) Color(0xFFFFB74D) else AppTextSecondary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = contextInfo.lastSmsSnippet,
              fontSize = 12.sp,
              color = if (contextInfo.isSmsSensitive) Color(0xFFFFE0B2) else AppTextSecondary,
              maxLines = 1,
              modifier = Modifier.weight(1f)
            )
            if (contextInfo.lastSmsTimestamp != null) {
              Text(
                text = contextInfo.lastSmsTimestamp,
                fontSize = 11.sp,
                color = AppTextMuted
              )
            }
          }
        }
      }

      // KROK 2: PEŁNY WIDOK EXPANDED (Sekcja 88 i 89)
      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // 1. Ostatni SMS (z filtrem wrażliwości)
          if (contextInfo.lastSmsSnippet != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF1E1B2C),
              border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = if (contextInfo.isSmsSensitive) Icons.Default.Lock else Icons.Default.Message,
                      contentDescription = null,
                      tint = if (contextInfo.isSmsSensitive) Color(0xFFFFB74D) else AppPurple,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Ostatnia wiadomość SMS",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = AppTextSecondary
                    )
                  }
                  if (contextInfo.lastSmsTimestamp != null) {
                    Text(
                      text = contextInfo.lastSmsTimestamp,
                      fontSize = 11.sp,
                      color = AppTextMuted
                    )
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = contextInfo.lastSmsSnippet,
                  fontSize = 13.sp,
                  fontWeight = if (contextInfo.isSmsSensitive) FontWeight.SemiBold else FontWeight.Normal,
                  color = if (contextInfo.isSmsSensitive) Color(0xFFFFE0B2) else AppTextPrimary
                )
              }
            }
          }

          // 2. Historia połączeń
          if (contextInfo.lastCallTimestamp != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Ostatnie połączenie: ${contextInfo.lastCallTimestamp}",
                fontSize = 12.sp,
                color = AppTextSecondary
              )
            }
          }

          // 3. Oficjalne domeny organizacji
          if (contextInfo.officialDomains.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Oficjalne domeny instytucji:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTextSecondary
              )
              for (domain in contextInfo.officialDomains) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AppGreen,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = domain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTextPrimary
                  )
                }
              }
            }
          }

          // 4. Notatka o reputacji
          if (!contextInfo.reputationNote.isNullOrBlank()) {
            Text(
              text = "Notatka reputacji: ${contextInfo.reputationNote}",
              fontSize = 12.sp,
              color = AppTextMuted
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
        }
      }
    }
  }
}
