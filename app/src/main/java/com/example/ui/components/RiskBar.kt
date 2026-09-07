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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConfidenceLevel
import com.example.model.RiskSeverity

/**
 * Reusable RiskBar representing score between 0 and 100.
 * Follows strict accessibility: never relies on color alone.
 * Always presents numerical score, confidence level, and textual severity.
 */
@Composable
fun RiskBar(
  score: Int,
  confidence: ConfidenceLevel? = null,
  showLabel: Boolean = true,
  modifier: Modifier = Modifier
) {
  val clampedScore = score.coerceIn(0, 100)
  val severity = RiskSeverity.fromScore(clampedScore)

  val animatedProgress by animateFloatAsState(
    targetValue = clampedScore / 100f,
    animationSpec = tween(durationMillis = 600),
    label = "RiskProgressAnimation"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("risk_bar_component")
  ) {
    if (showLabel) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "$clampedScore% ryzyka",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = severity.label,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold
            ),
            color = severity.color
          )
        }

        if (confidence != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
          ) {
            Text(
              text = "Pewność: ${confidence.displayName}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }
    }

    // Accessible Progress Track with clear rounded bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(14.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(animatedProgress)
          .fillMaxHeight()
          .clip(CircleShape)
          .background(severity.color)
      )
    }

    // Step milestone ticks (Niskie, Umiarkowane, Podwyższone, Wysokie)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "0% (Niskie)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
      )
      Text(
        text = "50%",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
      )
      Text(
        text = "100% (Wysokie)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
      )
    }
  }
}
