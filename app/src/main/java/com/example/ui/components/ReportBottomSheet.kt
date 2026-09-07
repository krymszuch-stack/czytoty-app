package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CallRecord

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportBottomSheet(
  callRecord: CallRecord?,
  onDismiss: () -> Unit,
  onSubmit: (rating: Int, category: String?) -> Unit
) {
  if (callRecord == null) return

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var rating by remember { mutableIntStateOf(0) }
  var selectedCategory by remember { mutableStateOf<String?>(null) }

  val categories = listOf(
    "Oczekiwane / Neutralne",
    "Telemarketing i oferty",
    "Automaty / boty (robocall)",
    "Podejrzenie oszustwa",
    "Podszywanie się pod bank",
    "Inne"
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = Modifier.testTag("report_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Jak oceniasz to połączenie?",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = callRecord.caller.displayName ?: callRecord.caller.formattedNumber,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(20.dp))

      // 1-5 Star Rating Row (48dp touch targets)
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        for (i in 1..5) {
          IconButton(
            onClick = { rating = i },
            modifier = Modifier
              .size(52.dp)
              .testTag("star_rating_$i")
          ) {
            Icon(
              imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
              contentDescription = "$i gwiazdek",
              tint = if (i <= rating) Color(0xFFE8710A) else MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(36.dp)
            )
          }
        }
      }

      // If rating is 1..3 stars, expand and ask "Co to było?"
      AnimatedVisibility(
        visible = rating in 1..3,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
          horizontalAlignment = Alignment.Start
        ) {
          Text(
            text = "Co to było?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Wybierz kategorię, aby ostrzec innych użytkowników.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
          )

          Spacer(modifier = Modifier.height(12.dp))

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            categories.forEach { cat ->
              val isSelected = selectedCategory == cat
              FilterChip(
                selected = isSelected,
                onClick = {
                  selectedCategory = if (isSelected) null else cat
                },
                label = {
                  Text(
                    text = cat,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                  )
                },
                modifier = Modifier
                  .height(48.dp) // Minimum 48dp touch target
                  .testTag("report_category_chip_${cat.lowercase()}"),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Zgłoszenie dotyczy wyłącznie charakteru połączenia i schematu rozmowy (np. telemarketing, próba wyłudzenia). Aplikacja nie służy do oskarżeń personalnych.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
      )

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.height(48.dp)
        ) {
          Text("Anuluj")
        }

        Spacer(modifier = Modifier.width(12.dp))

        Button(
          onClick = {
            if (rating > 0) {
              onSubmit(rating, selectedCategory)
            }
          },
          enabled = rating > 0,
          modifier = Modifier
            .height(48.dp)
            .testTag("submit_report_button")
        ) {
          Text("Zapisz ocenę")
        }
      }
    }
  }
}
