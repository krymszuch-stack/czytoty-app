package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import com.example.model.ConfidenceLevel
import com.example.model.RiskSeverity

@Composable
fun RiskBadge(
  score: Int,
  modifier: Modifier = Modifier,
  severity: RiskSeverity = RiskSeverity.fromScore(score),
  confidence: ConfidenceLevel? = null,
  showScore: Boolean = true,
  showConfidence: Boolean = false,
  compact: Boolean = false
) {
  val icon = when (severity) {
    RiskSeverity.NO_KNOWN_RISK -> Icons.Default.Shield
    RiskSeverity.MODERATE -> Icons.Default.Info
    RiskSeverity.ELEVATED -> Icons.Default.Warning
    RiskSeverity.HIGH -> Icons.Default.Warning
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = severity.containerColor,
    modifier = modifier.testTag("risk_badge_${severity.name.lowercase()}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(
        horizontal = if (compact) 6.dp else 10.dp,
        vertical = if (compact) 3.dp else 5.dp
      )
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = severity.onContainerColor,
        modifier = Modifier.size(if (compact) 14.dp else 16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))

      val labelText = buildString {
        append(severity.label)
        if (showScore) {
          append(" • $score% ryzyka")
        }
        if (showConfidence && confidence != null) {
          append(" • Pewność: ${confidence.displayName}")
        }
      }

      Text(
        text = if (compact) "$score%" else labelText,
        style = if (compact) {
          MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          )
        } else {
          MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold
          )
        },
        color = severity.onContainerColor
      )
    }
  }
}
