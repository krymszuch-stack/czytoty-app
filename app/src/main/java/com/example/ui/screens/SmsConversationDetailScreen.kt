package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrandIdentity
import com.example.model.RiskSeverity
import com.example.model.SmsMessage
import com.example.model.SmsThread
import com.example.ui.components.OtpCodeCard
import com.example.ui.components.RiskBadge
import com.example.ui.components.SmsLinkWarningCard
import kotlinx.coroutines.launch

/**
 * Ekran szczegółów konwersacji SMS.
 * Zapewnia warstwę ochrony anty-smishing:
 * - podgląd tożsamości nadawcy w nagłówku,
 * - boks szybkiego kopiowania OTP,
 * - analizę domen linków bez automatycznego ich uruchamiania.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsConversationDetailScreen(
  thread: SmsThread,
  messages: List<SmsMessage>,
  onBack: () -> Unit,
  onSenderProfileClick: (String) -> Unit,
  onSendMessage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var replyText by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  // Scroll to bottom when messages change
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  val isHighRisk = thread.highestSeverity == RiskSeverity.HIGH || thread.isSpamOrBlocked
  val brand = thread.recognizedBrand

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onSenderProfileClick(thread.senderAddress) }
          ) {
            Text(
              text = thread.displayName ?: thread.senderAddress,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1
            )
            // Informacja o tożsamości
            val identityLabel = when {
              brand != null -> "Rozpoznana marka: ${brand.displayName}"
              isHighRisk -> "⚠️ Podejrzenie oszustwa (${thread.highestRiskScore}%)"
              else -> "Tożsamość niezweryfikowana w GSM"
            }
            Text(
              text = identityLabel,
              style = MaterialTheme.typography.labelSmall,
              color = if (isHighRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("conversation_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
          }
        },
        actions = {
          IconButton(
            onClick = { onSenderProfileClick(thread.senderAddress) },
            modifier = Modifier.testTag("conversation_info")
          ) {
            Icon(Icons.Default.Info, contentDescription = "Szczegóły nadawcy")
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.fillMaxSize()
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {

      // Baner bezpieczeństwa jeśli nadawca to instytucja finansowa lub podejrzany numer
      if (brand?.isFinancialInstitution == true) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Bank nigdy nie przesyła linków do logowania ani nie prosi o kody autoryzacyjne.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      } else if (isHighRisk) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Uwaga: Treść wiadomości lub link nosi cechy kampanii wyłudzającej dane (smishing).",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
        }
      }

      // Lista dymków wiadomości
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        items(messages, key = { it.id }) { message ->
          SmsMessageBubble(
            message = message,
            brand = brand,
            onCodeCopied = {
              scope.launch {
                snackbarHostState.showSnackbar("Skopiowano kod do schowka")
              }
            }
          )
        }
      }

      // Dolne pole wysyłania odpowiedzi
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it },
            placeholder = { Text("Wpisz wiadomość...") },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("sms_reply_input"),
            maxLines = 4
          )

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = {
              if (replyText.isNotBlank()) {
                onSendMessage(replyText.trim())
                replyText = ""
              }
            },
            enabled = replyText.isNotBlank(),
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(
                if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
              )
              .testTag("sms_send_button")
          ) {
            Icon(
              Icons.AutoMirrored.Filled.Send,
              contentDescription = "Wyślij wiadomość",
              tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
            )
          }
        }
      }
    }
  }
}

@Composable
fun SmsMessageBubble(
  message: SmsMessage,
  brand: BrandIdentity?,
  onCodeCopied: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isOutgoing = message.isOutgoing

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
  ) {
    Card(
      shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isOutgoing) 18.dp else 4.dp,
        bottomEnd = if (isOutgoing) 4.dp else 18.dp
      ),
      colors = CardDefaults.cardColors(
        containerColor = if (isOutgoing) {
          MaterialTheme.colorScheme.primary
        } else if (message.riskSeverity == RiskSeverity.HIGH) {
          MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        } else {
          MaterialTheme.colorScheme.surfaceContainerHigh
        }
      ),
      modifier = Modifier.fillMaxWidth(0.92f)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {

        // 1. Zwykła treść wiadomości
        Text(
          text = message.body,
          style = MaterialTheme.typography.bodyLarge,
          color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )

        // 2. Jeśli wiadomość zawiera kod OTP: eksponujemy duży blok z przyciskiem [Kopiuj kod]
        if (message.otpInfo != null) {
          Spacer(modifier = Modifier.height(12.dp))
          OtpCodeCard(
            otpInfo = message.otpInfo,
            onCodeCopied = onCodeCopied
          )
        }

        // 3. Jeśli wiadomość zawiera link: eksponujemy blok bezpiecznej analizy domeny
        if (message.urlAnalysis != null) {
          Spacer(modifier = Modifier.height(12.dp))
          SmsLinkWarningCard(
            analysis = message.urlAnalysis,
            brand = brand
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Czas i status
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = message.timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOutgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.outline
          )
        }
      }
    }
  }
}
