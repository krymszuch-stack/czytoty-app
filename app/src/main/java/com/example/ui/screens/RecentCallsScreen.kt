package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.CallerInfo
import com.example.model.RiskSeverity
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekScamSurface
import com.example.ui.theme.VerifiedBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentCallsScreen(
  calls: List<CallRecord>,
  onCallClick: (CallerInfo) -> Unit,
  onReportClick: (CallRecord) -> Unit,
  onSimulateBankCall: () -> Unit,
  onSimulateScamCall: () -> Unit,
  onSimulateCourierCall: () -> Unit,
  onProtectionClick: (() -> Unit)? = null,
  onOpenOnboarding: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var showMenu by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Ostatnie połączenia",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.3).sp
            )
          )
        },
        actions = {
          IconButton(
            onClick = { showMenu = !showMenu },
            modifier = Modifier.testTag("recent_calls_menu_button")
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Więcej opcji",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Zasady i architektura (Onboarding)") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              onClick = {
                showMenu = false
                onOpenOnboarding?.invoke()
              }
            )
            DropdownMenuItem(
              text = { Text("Symulacja: Infolinia banku (Zero Trust)") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error
                )
              },
              onClick = {
                showMenu = false
                onSimulateBankCall()
              }
            )
            DropdownMenuItem(
              text = { Text("Symulacja: Wyłudzenie (Scam)") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error
                )
              },
              onClick = {
                showMenu = false
                onSimulateScamCall()
              }
            )
            DropdownMenuItem(
              text = { Text("Symulacja: Kurier (Rejestr)") },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Business,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              onClick = {
                showMenu = false
                onSimulateCourierCall()
              }
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
        top = paddingValues.calculateTopPadding() + 4.dp,
        bottom = paddingValues.calculateBottomPadding() + 16.dp,
        start = 16.dp,
        end = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // Baner statusu bazy lokalnej (Uczciwy, bez fałszywych 100% gwarancji)
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          border = BorderStroke(1.dp, SleekBorder),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .testTag("sleek_protection_banner")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer)
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(20.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Lokalna reputacja połączeń",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Offline-first • Brak sygnałów ryzyka != bezpieczny",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              TextButton(
                onClick = { onProtectionClick?.invoke() },
                modifier = Modifier.testTag("protection_banner_details_btn")
              ) {
                Text(
                  text = "USTAWIENIA",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                  ),
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Szybkie przyciski symulacji demonstracyjnej (jawne DEMO)
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              FilledTonalButton(
                onClick = onSimulateBankCall,
                modifier = Modifier
                  .weight(1f)
                  .height(36.dp)
                  .testTag("simulate_bank_call_btn")
              ) {
                Text(
                  text = "Test: Bank",
                  style = MaterialTheme.typography.labelSmall
                )
              }
              FilledTonalButton(
                onClick = onSimulateScamCall,
                modifier = Modifier
                  .weight(1f)
                  .height(36.dp)
                  .testTag("simulate_scam_call_btn")
              ) {
                Text(
                  text = "Test: Scam",
                  style = MaterialTheme.typography.labelSmall
                )
              }
              FilledTonalButton(
                onClick = onSimulateCourierCall,
                modifier = Modifier
                  .weight(1f)
                  .height(36.dp)
                  .testTag("simulate_courier_call_btn")
              ) {
                Text(
                  text = "Test: Kurier",
                  style = MaterialTheme.typography.labelSmall
                )
              }
            }
          }
        }
      }

      items(calls, key = { it.id }) { call ->
        RecentCallListItem(
          call = call,
          onClick = { onCallClick(call.caller) },
          onReport = { onReportClick(call) }
        )
      }
    }
  }
}

@Composable
fun RecentCallListItem(
  call: CallRecord,
  onClick: () -> Unit,
  onReport: () -> Unit,
  modifier: Modifier = Modifier
) {
  val caller = call.caller
  val isHighRisk = caller.severity == RiskSeverity.HIGH
  val isBank = caller.isFinancialInstitution

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = if (isHighRisk) SleekScamSurface else MaterialTheme.colorScheme.surfaceContainer,
    border = if (isHighRisk) BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer) else null,
    modifier = modifier
      .fillMaxWidth()
      .testTag("recent_call_item_${caller.phoneNumber}")
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(
            when {
              isHighRisk -> MaterialTheme.colorScheme.errorContainer
              isBank -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
              caller.isOfficialDirectoryNumber -> MaterialTheme.colorScheme.primaryContainer
              else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
          )
      ) {
        when {
          isHighRisk -> {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Podejrzenie oszustwa",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(24.dp)
            )
          }
          isBank -> {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Bank",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(22.dp)
            )
          }
          caller.displayName != null && caller.displayName.isNotEmpty() -> {
            val initial = caller.displayName.first().uppercaseChar().toString()
            Text(
              text = initial,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          else -> {
            Icon(
              imageVector = Icons.Default.Call,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = caller.displayName ?: caller.formattedNumber,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.Medium,
              fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )

          Text(
            text = call.timestamp,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (isBank) {
            Text(
              text = "Ryzyko podszycia (Spoofing)",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              ),
              color = MaterialTheme.colorScheme.error
            )
          } else if (isHighRisk) {
            Text(
              text = "Podejrzenie oszustwa",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
              ),
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .size(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${caller.riskScore}%",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              ),
              color = MaterialTheme.colorScheme.error
            )
          } else if (caller.isOfficialDirectoryNumber) {
            Icon(
              imageVector = Icons.Default.Business,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "W rejestrze publicznym",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Icon(
              imageVector = when (call.callType) {
                CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
                CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed
                CallType.BLOCKED -> Icons.Default.Block
              },
              contentDescription = null,
              tint = if (call.callType == CallType.BLOCKED || call.callType == CallType.MISSED)
                MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.outline,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = caller.category,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      IconButton(
        onClick = onReport,
        modifier = Modifier
          .size(44.dp)
          .testTag("report_call_action_${caller.phoneNumber}")
      ) {
        Icon(
          imageVector = Icons.Default.RateReview,
          contentDescription = "Oceń połączenie",
          tint = if (call.userRating != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}
