package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallerInfo
import com.example.ui.components.CampaignActivityChart
import com.example.ui.components.RiskBar
import com.example.ui.components.VerifiedBusinessBadge
import com.example.ui.components.ZeroTrustBankBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberDetailsScreen(
  caller: CallerInfo,
  onBack: () -> Unit,
  onToggleBlock: (String) -> Unit,
  onReportClick: (CallerInfo) -> Unit,
  onShareClick: (CallerInfo) -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Szczegóły numeru",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("details_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Wróć"
            )
          }
        },
        actions = {
          IconButton(
            onClick = { onShareClick(caller) },
            modifier = Modifier.testTag("details_share_button")
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Udostępnij numer"
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      contentPadding = PaddingValues(
        top = paddingValues.calculateTopPadding() + 8.dp,
        bottom = paddingValues.calculateBottomPadding() + 24.dp,
        start = 16.dp,
        end = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. Karta główna nagłówka
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            if (caller.displayName != null) {
              Text(
                text = caller.displayName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = caller.formattedNumber,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              Text(
                text = caller.formattedNumber,
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            if (caller.isOfficialDirectoryNumber && !caller.isFinancialInstitution) {
              Spacer(modifier = Modifier.height(10.dp))
              VerifiedBusinessBadge(
                label = "Numer w rejestrze: ${caller.numberIdentity?.registeredEntityName ?: ""}",
                orgName = caller.numberIdentity?.registeredEntityName ?: caller.displayName
              )
            }

            Spacer(modifier = Modifier.height(18.dp))

            RiskBar(
              score = caller.riskScore,
              confidence = caller.confidence,
              showLabel = true
            )
          }
        }
      }

      // 2. Baner Zero Trust dla instytucji finansowej
      if (caller.isFinancialInstitution) {
        item {
          ZeroTrustBankBanner(caller = caller)
        }
      }

      // 3. Transparentne wyjaśnienie: Dlaczego?
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Ocena sygnałów ryzyka",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = caller.riskReasonExplanation,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // 4. Historia numeru
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Historia numeru",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(label = "Pierwsza aktywność w bazie:", value = caller.firstSeen)
            DetailRow(label = "Ostatnie zgłoszenie:", value = caller.lastSeen)
            DetailRow(label = "Łączna liczba zgłoszeń:", value = "${caller.totalReports}")
            DetailRow(label = "Zgłoszenia w ostatnich 24h:", value = "${caller.reportsLast24h}")
            DetailRow(label = "Zgłoszenia w ostatniej godzinie:", value = "${caller.reportsLastHour}")
            DetailRow(label = "Kategoria dominująca:", value = caller.category)
          }
        }
      }

      // 5. Wykres kampanii
      item {
        CampaignActivityChart(
          data = caller.activityTrend,
          riskScore = caller.riskScore
        )
      }

      // 6. Akcje
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Button(
            onClick = { onToggleBlock(caller.phoneNumber) },
            colors = if (caller.isBlocked) {
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
              )
            } else {
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
              )
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("details_block_button")
          ) {
            Icon(
              imageVector = Icons.Default.Block,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (caller.isBlocked) "Odblokuj" else "Zablokuj",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }

          FilledTonalButton(
            onClick = { onReportClick(caller) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("details_report_button")
          ) {
            Icon(
              imageVector = Icons.Default.Flag,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Zgłoś numer",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DetailRow(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
