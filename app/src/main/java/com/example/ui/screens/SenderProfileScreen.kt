package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RiskSeverity
import com.example.model.UnifiedSenderProfile
import com.example.ui.components.RiskBadge
import com.example.ui.components.RiskBar
import com.example.ui.components.VerifiedBusinessBadge

/**
 * Wspólny profil nadawcy (Karta Nadawcy).
 * Łączy sygnały z obu kanałów (Połączenia + SMS) w jedną spójną reputację.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderProfileScreen(
  profile: UnifiedSenderProfile,
  onBack: () -> Unit,
  onToggleBlock: (String) -> Unit,
  onReport: (String) -> Unit,
  onOpenSmsThread: (String) -> Unit
) {
  val context = LocalContext.current
  val isHighRisk = profile.combinedRiskScore >= 70

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Wspólny profil nadawcy", fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("sender_profile_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = Modifier.fillMaxSize()
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {

      // Nagłówek tożsamości
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(
                if (isHighRisk) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer
              )
          ) {
            Icon(
              imageVector = when {
                profile.recognizedBrand != null -> Icons.Default.Business
                isHighRisk -> Icons.Default.Shield
                else -> Icons.Default.Phone
              },
              contentDescription = null,
              tint = if (isHighRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(36.dp)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = profile.displayName ?: profile.formattedAddress,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          if (profile.displayName != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = profile.formattedAddress,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Etykiety stanu
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (profile.recognizedBrand != null) {
              VerifiedBusinessBadge(
                label = "Rozpoznana marka",
                orgName = profile.recognizedBrand.displayName
              )
            }
            RiskBadge(
              score = profile.combinedRiskScore,
              severity = RiskSeverity.fromScore(profile.combinedRiskScore),
              confidence = profile.confidence,
              showScore = true,
              showConfidence = true
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Wspólna ocena ryzyka (Risk Bar)
      Text(
        text = "ŁĄCZNA OCENA RYZYKA (POŁĄCZENIA + SMS)",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(8.dp))

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          RiskBar(score = profile.combinedRiskScore)
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Pewność danych: ${profile.confidence.displayName} (${profile.confidence.description})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Podział aktywności (Kanały jako źródła sygnałów)
      Text(
        text = "AKTYWNOŚĆ W KANAŁACH",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Kolumna Połączenia
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Połączenia", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "${profile.callsCount} zarejestrowanych",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (profile.lastCallTime != null) "Ostatnie: ${profile.lastCallTime}" else "Brak historii",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Kolumna SMS
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Message, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("SMS-y", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "${profile.smsCount} wiadomości",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (profile.lastSmsTime != null) "Ostatnia: ${profile.lastSmsTime}" else "Brak wiadomości",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      if (profile.suspiciousLinksCount > 0) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Wykryto ${profile.suspiciousLinksCount} podejrzany(e) link(i) w wiadomościach od tego nadawcy.",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Źródła danych i czynniki
      Text(
        text = "ŹRÓDŁA DANYCH I ARGUMENTACJA",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(8.dp))

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          profile.riskFactors.forEach { factor ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
              Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
              Text(factor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Uwzględnione źródła:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.outline
          )
          profile.dataSources.forEach { source ->
            Text(
              text = "✓ $source",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Przyciski akcji (dostępne, min 48dp)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.address}"))
            context.startActivity(intent)
          },
          modifier = Modifier
            .weight(1f)
            .height(52.dp),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(Icons.Default.Phone, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Zadzwoń", fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = { onOpenSmsThread(profile.address) },
          modifier = Modifier
            .weight(1f)
            .height(52.dp),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(Icons.Default.Message, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Wiadomość", fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = { onToggleBlock(profile.address) },
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            if (profile.isBlocked) Icons.Default.Check else Icons.Default.Block,
            contentDescription = null,
            tint = if (profile.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            if (profile.isBlocked) "Odblokuj" else "Zablokuj",
            color = if (profile.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
          )
        }

        OutlinedButton(
          onClick = { onReport(profile.address) },
          modifier = Modifier
            .weight(1f)
            .height(48.dp),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Report, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Zgłoś nadawcę")
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
