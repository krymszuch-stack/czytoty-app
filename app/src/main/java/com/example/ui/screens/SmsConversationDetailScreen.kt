package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrandIdentity
import com.example.model.LinkSafetyModalState
import com.example.model.OtpCodeInfo
import com.example.model.RiskSeverity
import com.example.model.SmsMessage
import com.example.model.SmsMessageType
import com.example.model.SmsThread
import com.example.model.UrlSecurityAnalysis
import com.example.ui.components.LinkSafetyDialog
import com.example.ui.theme.AppBlue
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurplePrimary
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppRedBorder
import com.example.ui.theme.AppRedContainer
import com.example.ui.theme.AppRedText
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary
import kotlinx.coroutines.launch

/**
 * Nowoczesny ekran czatu SMS (Messenger UX - Sekcje 42–48, 54, 57).
 * Ergonomia: WhatsApp / Discord / Google Messages + CzyToTy? Privacy & Security.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsConversationDetailScreen(
  thread: SmsThread,
  messages: List<SmsMessage>,
  onBack: () -> Unit,
  onSenderProfileClick: (String) -> Unit,
  onSendMessage: (String) -> Unit,
  onOpenLinkModal: (UrlSecurityAnalysis, Boolean, String?) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier
) {
  var replyText by remember { mutableStateOf("") }
  var isWhyRiskBannerExpanded by remember { mutableStateOf(false) }
  var activeLocalLinkModal by remember { mutableStateOf<LinkSafetyModalState?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()
  val context = LocalContext.current

  // Automatyczny scroll na dół przy nowych wiadomościach
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  val isHighRisk = thread.highestSeverity == RiskSeverity.HIGH || thread.isSpamOrBlocked
  val isModerateRisk = thread.highestRiskScore >= 30 && !isHighRisk
  val brand = thread.recognizedBrand

  Scaffold(
    topBar = {
      // NAGŁÓWEK CZATU (Sekcja 42: avatar, nazwa, status; tap otwiera pełny profil)
      CenterAlignedTopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clickable { onSenderProfileClick(thread.senderAddress) }
              .padding(vertical = 4.dp, horizontal = 8.dp)
              .testTag("conversation_header_profile")
          ) {
            // Mały avatar w nagłówku
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                  when {
                    isHighRisk -> AppRedContainer
                    brand != null -> Color.White
                    else -> AppPurpleContainer
                  }
                ),
              contentAlignment = Alignment.Center
            ) {
              when {
                isHighRisk -> Icon(Icons.Default.Warning, contentDescription = null, tint = AppRed, modifier = Modifier.size(18.dp))
                brand?.brandId == "google_auth" -> Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                brand?.brandId == "dpd_pl" -> Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                brand?.brandId == "inpost_pl" -> Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(18.dp))
                brand?.isFinancialInstitution == true -> Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AppBlue, modifier = Modifier.size(18.dp))
                else -> {
                  val initial = (thread.displayName ?: thread.senderAddress).firstOrNull()?.uppercase() ?: "S"
                  Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.Start) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = thread.displayName ?: thread.senderAddress,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = AppTextPrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                if (thread.isBrandVerified) {
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Zweryfikowany",
                    tint = AppBlue,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }

              // Status tożsamości / ostrzeżenie
              val statusText = when {
                isHighRisk -> "⚠️ Ryzyko smishingu (${thread.highestRiskScore}%)"
                brand != null -> if (thread.isBrandVerified) "Oficjalny nadawca • ${brand.displayName}" else "Rozpoznana marka: ${brand.displayName} (heurystyka)"
                else -> "Wiadomość SMS • ${thread.senderAddress}"
              }

              Text(
                text = statusText,
                fontSize = 11.sp,
                color = if (isHighRisk) AppRedText else AppTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("conversation_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz", tint = AppTextPrimary)
          }
        },
        actions = {
          IconButton(
            onClick = { onSenderProfileClick(thread.senderAddress) },
            modifier = Modifier.testTag("conversation_info_action")
          ) {
            Icon(Icons.Default.Info, contentDescription = "Szczegóły profilu", tint = AppPurple)
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = Color(0xFF12101B)
        )
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier
      .fillMaxSize()
      .background(AppDarkBackground)
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {

      // 1. STICKY SAFETY BANNER (Sekcja 47: Ostrzeżenie przed podwyższonym ryzykiem)
      if (isHighRisk || isModerateRisk) {
        StickySafetyBanner(
          isHighRisk = isHighRisk,
          riskScore = thread.highestRiskScore,
          isExpanded = isWhyRiskBannerExpanded,
          onToggleExpand = { isWhyRiskBannerExpanded = !isWhyRiskBannerExpanded },
          messages = messages
        )
      }

      // 2. LISTA DYMKÓW ROZMOWY (Sekcja 42, 44, 45, 46)
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Nagłówek dnia
        item(key = "day_header") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF1C1928),
              border = BorderStroke(1.dp, AppBorder)
            ) {
              Text(
                text = "Dzisiaj • SMS GSM",
                fontSize = 11.sp,
                color = AppTextSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
              )
            }
          }
        }

        items(messages, key = { it.id }) { message ->
          ModernSmsBubble(
            message = message,
            brand = brand,
            onCopyOtp = { code ->
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("OTP Code", code))
              Toast.makeText(context, "Skopiowano kod $code do schowka", Toast.LENGTH_SHORT).show()
              scope.launch {
                snackbarHostState.showSnackbar("Skopiowano kod weryfikacyjny: $code")
              }
            },
            onLinkClick = { analysis ->
              val isOfficial = brand?.officialDomains?.any { analysis.domain.equals(it, ignoreCase = true) } == true
              activeLocalLinkModal = LinkSafetyModalState(
                analysis = analysis,
                isOfficialBrandDomain = isOfficial,
                verifiedBrandName = if (isOfficial) brand?.displayName else null
              )
            }
          )
        }
      }

      // 3. STAŁY COMPOSER NA DOLE (Sekcja 43: [ + ] [ wpisz wiadomość SMS... ] [ wyślij SMS ])
      MessengerInputBar(
        text = replyText,
        onTextChange = { replyText = it },
        onSend = {
          if (replyText.isNotBlank()) {
            onSendMessage(replyText.trim())
            replyText = ""
          }
        },
        onPlusClick = {
          Toast.makeText(context, "Załączniki MMS będą dostępne wkrótce", Toast.LENGTH_SHORT).show()
        }
      )
    }
  }

  // MODAL LINK SAFETY (Sekcja 48: Nigdy nie otwieraj linków automatycznie)
  activeLocalLinkModal?.let { modalState ->
    LinkSafetyDialog(
      state = modalState,
      onDismiss = { activeLocalLinkModal = null }
    )
  }
}

/**
 * Sticky Safety Banner nad rozmową (Sekcja 47).
 */
@Composable
private fun StickySafetyBanner(
  isHighRisk: Boolean,
  riskScore: Int,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  messages: List<SmsMessage>
) {
  Surface(
    color = if (isHighRisk) AppRedContainer else Color(0xFF281F26),
    border = BorderStroke(1.dp, if (isHighRisk) AppRed.copy(alpha = 0.4f) else Color(0xFFFFB020).copy(alpha = 0.3f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("safety_banner")
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = if (isHighRisk) AppRed else Color(0xFFFFB020),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isHighRisk) "Ta wiadomość zawiera sygnały podwyższonego ryzyka." else "Zalecana ostrożność: nieznany nadawca",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isHighRisk) AppRedText else Color(0xFFFFDE99)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Przycisk [Dlaczego?]
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighRisk) AppRed.copy(alpha = 0.2f) else Color(0x33FFB020))
            .clickable { onToggleExpand() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("btn_why_risk")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = if (isExpanded) "Ukryj" else "Dlaczego?",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (isHighRisk) AppRedText else Color(0xFFFFB020)
            )
            Icon(
              imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = null,
              tint = if (isHighRisk) AppRedText else Color(0xFFFFB020),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      // Rozwinięcie szczegółów przyczyn
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          HorizontalDivider(thickness = 1.dp, color = if (isHighRisk) AppRed.copy(alpha = 0.25f) else Color(0x22FFB020))
          Spacer(modifier = Modifier.height(8.dp))

          val reasons = messages.flatMap { it.riskReasons }.distinct()
          if (reasons.isNotEmpty()) {
            reasons.forEach { reason ->
              Text(
                text = "• $reason",
                fontSize = 12.sp,
                color = if (isHighRisk) AppRedText.copy(alpha = 0.9f) else Color(0xFFE8DCC0),
                modifier = Modifier.padding(vertical = 2.dp)
              )
            }
          } else {
            Text(
              text = "• Wykryto nietypowy schemat wiadomości lub niezgodność z oficjalnym rejestrem nadawców.",
              fontSize = 12.sp,
              color = AppTextSecondary
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Pamiętaj: Instytucje zaufane nigdy nie żądają dopłat do paczek ani logowania przez nieoficjalne domeny.",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTextPrimary
          )
        }
      }
    }
  }
}

/**
 * Nowoczesny dymek wiadomości SMS (Sekcje 42, 44, 45).
 */
@Composable
private fun ModernSmsBubble(
  message: SmsMessage,
  brand: BrandIdentity?,
  onCopyOtp: (String) -> Unit,
  onLinkClick: (UrlSecurityAnalysis) -> Unit
) {
  val isOutgoing = message.isOutgoing
  val isSuspicious = message.riskSeverity == RiskSeverity.HIGH || message.messageType == SmsMessageType.SUSPICIOUS

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
  ) {
    Card(
      shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isOutgoing) 20.dp else 4.dp,
        bottomEnd = if (isOutgoing) 4.dp else 20.dp
      ),
      colors = CardDefaults.cardColors(
        containerColor = when {
          isOutgoing -> AppPurplePrimary
          isSuspicious -> AppRedContainer
          else -> AppSurfaceVariant
        }
      ),
      border = BorderStroke(
        width = 1.dp,
        color = when {
          isOutgoing -> Color.Transparent
          isSuspicious -> AppRed.copy(alpha = 0.4f)
          else -> AppBorder
        }
      ),
      modifier = Modifier
        .fillMaxWidth(if (message.otpInfo != null || message.urlAnalysis != null) 0.95f else 0.86f)
        .testTag("bubble_${message.id}")
    ) {
      Column(modifier = Modifier.padding(14.dp)) {

        // Etykieta typu wiadomości (Sekcja 44)
        if (!isOutgoing && message.messageType != SmsMessageType.NORMAL) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                  when (message.messageType) {
                    SmsMessageType.SUSPICIOUS -> Color(0xFF4A1A22)
                    SmsMessageType.OTP -> Color(0xFF2C224E)
                    SmsMessageType.DELIVERY -> Color(0xFF2B2518)
                    SmsMessageType.BANK_FINANCE -> Color(0xFF162534)
                    else -> Color(0xFF232034)
                  }
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = message.messageType.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (message.messageType) {
                  SmsMessageType.SUSPICIOUS -> AppRedText
                  SmsMessageType.OTP -> Color(0xFFC8BAFF)
                  SmsMessageType.DELIVERY -> Color(0xFFFFDE99)
                  SmsMessageType.BANK_FINANCE -> Color(0xFF8DC5FF)
                  else -> AppTextSecondary
                }
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        // Treść tekstowa wiadomości (duża czytelność - Senior UX)
        Text(
          text = message.body,
          fontSize = 15.sp,
          lineHeight = 22.sp,
          color = if (isOutgoing) Color.White else if (isSuspicious) AppRedText else AppTextPrimary
        )

        // 1. INTELIGENTNA KARTA OTP (Sekcja 45)
        if (message.otpInfo != null) {
          Spacer(modifier = Modifier.height(12.dp))
          MessengerOtpCard(
            otpInfo = message.otpInfo,
            onCopy = { onCopyOtp(message.otpInfo.code) }
          )
        }

        // 2. KARTA LINK SAFETY (Sekcja 48: klikalna do weryfikacji domeny)
        if (message.urlAnalysis != null) {
          Spacer(modifier = Modifier.height(10.dp))
          MessengerLinkCard(
            analysis = message.urlAnalysis,
            brand = brand,
            onClick = { onLinkClick(message.urlAnalysis) }
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dyskretny timestamp i status dostarczenia
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${message.timestamp} • ${message.deliveryStatus}",
            fontSize = 11.sp,
            color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else AppTextSecondary
          )
        }
      }
    }
  }
}

/**
 * Inteligentna karta jednorazowego kodu OTP (Sekcja 45).
 */
@Composable
private fun MessengerOtpCard(
  otpInfo: OtpCodeInfo,
  onCopy: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color(0xFF1A162B),
    border = BorderStroke(1.dp, Color(0xFF3B3066)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("otp_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF2F2554)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = null,
            tint = Color(0xFFC8BAFF),
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = otpInfo.issuerName ?: "Kod jednorazowy",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AppTextSecondary
          )
          Text(
            text = otpInfo.codeType,
            fontSize = 11.sp,
            color = Color(0xFFC8BAFF)
          )
        }

        // Przycisk kopiowania
        Button(
          onClick = onCopy,
          colors = ButtonDefaults.buttonColors(containerColor = AppPurplePrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .height(38.dp)
            .testTag("btn_copy_otp")
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Kopiuj", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Kopiuj", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Duży kod w centrum
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF100E1C))
          .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = otpInfo.code,
          fontSize = 24.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 2.sp,
          color = Color.White
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Informacja: Wykryto lokalnie (Sekcja 45)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Wykryto lokalnie na urządzeniu",
          fontSize = 11.sp,
          color = AppTextSecondary
        )
        Text(
          text = "Nie udostępniaj nikomu",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFFFB020)
        )
      }
    }
  }
}

/**
 * Karta linku w wiadomości (Sekcja 48: Link Safety).
 */
@Composable
private fun MessengerLinkCard(
  analysis: UrlSecurityAnalysis,
  brand: BrandIdentity?,
  onClick: () -> Unit
) {
  val isHighRisk = analysis.isHighRisk || analysis.mismatchExplanation != null
  val isOfficialDomain = brand?.officialDomains?.any { analysis.domain.equals(it, ignoreCase = true) } == true

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isHighRisk) Color(0xFF38141C) else Color(0xFF141A24),
    border = BorderStroke(1.dp, if (isHighRisk) AppRed.copy(alpha = 0.5f) else AppBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .testTag("sms_link_card")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(if (isHighRisk) AppRedContainer else Color(0xFF1C2836)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isHighRisk) Icons.Default.Warning else Icons.Default.Link,
          contentDescription = null,
          tint = if (isHighRisk) AppRed else AppBlue,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = analysis.domain,
          fontSize = 14.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = if (isHighRisk) AppRedText else AppTextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = when {
            isOfficialDomain -> "Oficjalna domena marki ${brand?.displayName}"
            isHighRisk -> "⚠️ Wykryto cechy fałszywego adresu"
            else -> "Sprawdź bezpieczeństwo adresu WWW"
          },
          fontSize = 11.sp,
          color = if (isHighRisk) AppRedText else AppTextSecondary
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(if (isHighRisk) AppRed.copy(alpha = 0.2f) else Color(0xFF1D2838))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "Sprawdź",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (isHighRisk) AppRedText else AppBlue
        )
      }
    }
  }
}

/**
 * Stały dolny composer SMS (Sekcja 43: duży touch target, jasna komunikacja SMS).
 */
@Composable
private fun MessengerInputBar(
  text: String,
  onTextChange: (String) -> Unit,
  onSend: () -> Unit,
  onPlusClick: () -> Unit
) {
  Surface(
    color = Color(0xFF100E19),
    border = BorderStroke(1.dp, Color(0xFF201B34)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Przycisk [ + ] (MMS / Załączniki)
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(AppSurfaceVariant)
          .clickable { onPlusClick() }
          .testTag("btn_plus_attachment"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Dodaj załącznik",
          tint = AppPurple,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Pole wpisywania wiadomości SMS (z wyraźną informacją o SMS GSM)
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(24.dp))
          .background(AppSurfaceVariant)
          .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        if (text.isEmpty()) {
          Text(
            text = "Wpisz wiadomość SMS...",
            fontSize = 15.sp,
            color = AppTextSecondary
          )
        }
        BasicTextField(
          value = text,
          onValueChange = onTextChange,
          textStyle = TextStyle(
            fontSize = 15.sp,
            color = AppTextPrimary
          ),
          cursorBrush = SolidColor(AppPurple),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_input_field")
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Przycisk wysyłania [ Wyślij SMS ] (Duży touch target > 48dp)
      val canSend = text.isNotBlank()
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(if (canSend) AppPurplePrimary else Color(0xFF221F32))
          .clickable(enabled = canSend) { onSend() }
          .testTag("btn_send_sms"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Wyślij SMS",
          tint = if (canSend) Color.White else Color(0xFF5E5874),
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}
