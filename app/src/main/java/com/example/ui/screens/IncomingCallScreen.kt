package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.conversation.safety.manager.ConversationSafetyManager
import com.example.model.CallerInfo
import com.example.model.RiskSeverity
import com.example.ui.components.ConversationSafetySection
import com.example.ui.components.RiskBadge
import com.example.ui.components.VerifiedBusinessBadge
import com.example.ui.components.ZeroTrustBankBanner
import com.example.ui.theme.RiskSafe

@Composable
fun IncomingCallScreen(
  caller: CallerInfo,
  onAnswer: () -> Unit,
  onReject: () -> Unit,
  isSimulatedPreview: Boolean = true,
  safetyManager: ConversationSafetyManager = remember { ConversationSafetyManager() },
  modifier: Modifier = Modifier
) {
  val isSuspicious = caller.severity == RiskSeverity.HIGH || caller.severity == RiskSeverity.ELEVATED
  val coroutineScope = rememberCoroutineScope()
  val safetyState by safetyManager.state.collectAsState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(horizontal = 20.dp, vertical = 16.dp)
      .testTag("incoming_call_screen_${if (isSuspicious) "suspicious" else "safe"}")
  ) {
    // Przycisk zamknięcia podglądu symulacji
    IconButton(
      onClick = onReject,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .size(48.dp)
        .testTag("incoming_call_close_preview_btn")
    ) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Zamknij podgląd",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = 8.dp, bottom = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Nagłówek: Oznaczenie trybu demonstracyjnego (Source of Truth)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        if (isSimulatedPreview) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "TRYB DEMONSTRACYJNY • SYMULACJA MVP",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 12.dp)
        ) {
          com.example.ui.components.CzyToTyLogo(
            size = 28.dp,
            variant = com.example.ui.components.LogoVariant.DARK
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "CzyToTy? • WIESZ, ZANIM ODBIERZESZ",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = com.example.ui.theme.AppPurpleLight
          )
        }
      }

      // 2. Środkowa sekcja: Informacje o numerze i ocena ryzyka (przewijalna w razie małego ekranu)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState())
      ) {
        // Awatar kontaktu
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
              when {
                caller.isFinancialInstitution -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                caller.isOfficialDirectoryNumber -> MaterialTheme.colorScheme.primaryContainer
                isSuspicious -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surfaceVariant
              }
            )
        ) {
          Icon(
            imageVector = when {
              caller.isFinancialInstitution -> Icons.Default.Warning
              caller.isOfficialDirectoryNumber -> Icons.Default.Business
              isSuspicious -> Icons.Default.Warning
              else -> Icons.Default.Call
            },
            contentDescription = null,
            tint = when {
              caller.isFinancialInstitution -> MaterialTheme.colorScheme.error
              caller.isOfficialDirectoryNumber -> MaterialTheme.colorScheme.primary
              isSuspicious -> MaterialTheme.colorScheme.error
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Nazwa lub numer
        if (caller.displayName != null) {
          Text(
            text = caller.displayName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
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
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Kategoria
        Text(
          text = caller.category,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Ocena ryzyka + pewność danych (zgodnie z wytycznymi: nie samym kolorem!)
        RiskBadge(
          severity = caller.severity,
          score = caller.riskScore,
          confidence = caller.confidence,
          showScore = true,
          showConfidence = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Weryfikacja SAP lub Specjalny baner Zero Trust dla instytucji finansowej
        if (caller.sapPartner != null) {
          com.example.ui.components.SapVerifiedBadge(partner = caller.sapPartner)
          Spacer(modifier = Modifier.height(12.dp))
        } else if (caller.isFinancialInstitution) {
          ZeroTrustBankBanner(caller = caller)
          Spacer(modifier = Modifier.height(12.dp))
        } else if (caller.isOfficialDirectoryNumber) {
          caller.numberIdentity?.registeredEntityName?.let { org ->
            VerifiedBusinessBadge(
              label = "Numer w rejestrze: $org",
              orgName = org
            )
            Spacer(modifier = Modifier.height(12.dp))
          }
        }

        // 4. Transparentne uzasadnienie: „Dlaczego widzisz ten wynik?”
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Dlaczego widzisz ten wynik?",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = caller.riskReasonExplanation,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4b. Analiza bezpieczeństwa rozmowy na żądanie użytkownika (Conversation Safety Snapshot)
        ConversationSafetySection(
          state = safetyState,
          onStartAnalysis = {
            safetyManager.startAnalysis(
              callerNumber = caller.phoneNumber,
              scope = coroutineScope
            )
          },
          onCancelAnalysis = {
            safetyManager.cancel()
          },
          onRejectCall = onReject,
          onContinueCall = onAnswer
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 5. Dolne akcje: Odrzuć / Odbierz
      if (isSuspicious) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Button(
            onClick = onReject,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("incoming_reject_suspicious_btn")
          ) {
            Icon(
              imageVector = Icons.Default.CallEnd,
              contentDescription = null,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Odrzuć połączenie (Zalecane)",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedButton(
            onClick = onAnswer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("incoming_answer_anyway_btn")
          ) {
            Text(
              text = "Odbierz mimo to",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          FilledTonalButton(
            onClick = onReject,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
              .weight(1f)
              .height(54.dp)
              .testTag("incoming_reject_safe_btn")
          ) {
            Icon(
              imageVector = Icons.Default.CallEnd,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Odrzuć",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          Button(
            onClick = onAnswer,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = RiskSafe,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
              .weight(1f)
              .height(54.dp)
              .testTag("incoming_answer_safe_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Call,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Odbierz",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}
