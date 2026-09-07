package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallerInfo
import com.example.model.RiskSeverity

@Composable
fun CallerIdentityCard(
  caller: CallerInfo,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val isHighRisk = caller.severity == RiskSeverity.HIGH || caller.severity == RiskSeverity.ELEVATED

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("caller_identity_card_${caller.phoneNumber}")
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
              if (caller.isOfficialDirectoryNumber) {
                MaterialTheme.colorScheme.primaryContainer
              } else if (isHighRisk) {
                MaterialTheme.colorScheme.errorContainer
              } else {
                MaterialTheme.colorScheme.surfaceVariant
              }
            )
        ) {
          Icon(
            imageVector = when {
              caller.isOfficialDirectoryNumber -> Icons.Default.Business
              isHighRisk -> Icons.Default.Warning
              caller.displayName != null -> Icons.Default.Person
              else -> Icons.Default.Phone
            },
            contentDescription = null,
            tint = when {
              caller.isOfficialDirectoryNumber -> MaterialTheme.colorScheme.onPrimaryContainer
              isHighRisk -> MaterialTheme.colorScheme.onErrorContainer
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
          if (caller.displayName != null) {
            Text(
              text = caller.displayName,
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = caller.formattedNumber,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            Text(
              text = caller.formattedNumber,
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          if (caller.isBlocked) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Numer zablokowany",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Badges Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (caller.isOfficialDirectoryNumber) {
          caller.numberIdentity?.registeredEntityName?.let { orgName ->
            VerifiedBusinessBadge(
              label = "W rejestrze",
              orgName = orgName
            )
          }
        }
        RiskBadge(
          score = caller.riskScore,
          severity = caller.severity,
          confidence = caller.confidence,
          showScore = true,
          showConfidence = true
        )
      }

      if (caller.category.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = caller.category,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.outline
        )
      }
    }
  }
}
