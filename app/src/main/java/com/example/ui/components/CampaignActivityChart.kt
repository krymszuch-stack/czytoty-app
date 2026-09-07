package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RiskSeverity

/**
 * 7-day sparkline and activity distribution chart showing reports trend.
 */
@Composable
fun CampaignActivityChart(
  data: List<Int>,
  riskScore: Int,
  modifier: Modifier = Modifier
) {
  val safeData = if (data.isEmpty()) listOf(0, 0, 0, 0, 0, 0, 0) else data
  val maxVal = (safeData.maxOrNull() ?: 1).coerceAtLeast(1)
  val severity = RiskSeverity.fromScore(riskScore)
  val dayLabels = listOf("-6 dni", "-5 dni", "-4 dni", "-3 dni", "-2 dni", "Wczoraj", "Dzisiaj")

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = modifier
      .fillMaxWidth()
      .testTag("campaign_activity_chart")
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Aktywność zgłoszeń (ostatnie 7 dni)",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Maks. $maxVal/dzień",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.outline
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Sparkline Bars
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(80.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        safeData.forEachIndexed { index, value ->
          val fraction = (value.toFloat() / maxVal.toFloat()).coerceIn(0.06f, 1f)
          val animatedHeight by animateFloatAsState(
            targetValue = fraction,
            animationSpec = tween(durationMillis = 500 + index * 60),
            label = "BarHeightAnim_$index"
          )

          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          ) {
            if (value > 0) {
              Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = if (index == safeData.lastIndex) severity.color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
              )
            } else {
              Spacer(modifier = Modifier.height(14.dp))
            }

            Box(
              modifier = Modifier
                .width(20.dp)
                .fillMaxHeight(animatedHeight)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(
                  if (index == safeData.lastIndex) {
                    severity.color
                  } else if (value > 0) {
                    MaterialTheme.colorScheme.secondary
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant
                  }
                )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // X Axis Labels
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        dayLabels.forEachIndexed { index, label ->
          Text(
            text = if (index == 5) "Wczoraj" else if (index == 6) "Dziś" else "${6 - index}d",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (index == 6) severity.color else MaterialTheme.colorScheme.outline,
            fontWeight = if (index == 6) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
