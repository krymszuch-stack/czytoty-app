package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SmsSimulationScenario
import com.example.model.RiskSeverity
import com.example.model.SmsThread
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
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

enum class SmsCategoryFilter {
  ALL,
  UNREAD,
  OTP,
  IMPORTANT
}

/**
 * Ekran Wiadomości SMS dopasowany 1:1 do makiet Material 3 Dark.
 */
@Composable
fun SmsConversationsScreen(
  threads: List<SmsThread>,
  onThreadClick: (String) -> Unit,
  onSenderProfileClick: (String) -> Unit,
  onNewMessage: () -> Unit,
  onSimulateSms: (SmsSimulationScenario) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf(SmsCategoryFilter.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  val context = LocalContext.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AppDarkBackground)
      .padding(horizontal = 18.dp, vertical = 12.dp)
  ) {

    // 1. TOP BAR (Wiadomości + U avatar)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(AppPurpleContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Message,
          contentDescription = null,
          tint = AppPurple,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Wiadomości",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary
        )
        Text(
          text = "SMS, kody i ważne powiadomienia",
          fontSize = 13.sp,
          color = AppTextSecondary
        )
      }

      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(AppPurpleContainer)
          .clickable { /* Otwórz profil */ },
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "U",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = AppPurple
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. SEARCH BAR (Pill input)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .clip(CircleShape)
        .background(AppSurfaceVariant)
        .padding(horizontal = 16.dp),
      contentAlignment = Alignment.CenterStart
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          tint = AppTextSecondary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
          if (searchQuery.isEmpty()) {
            Text(
              text = "Szukaj wiadomości, nadawcy lub treści...",
              fontSize = 14.sp,
              color = AppTextSecondary
            )
          }
          BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = TextStyle(
              fontSize = 14.sp,
              color = AppTextPrimary
            ),
            cursorBrush = SolidColor(AppPurple),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("sms_search_input")
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. FILTER PILLS (Wszystkie, Nieprzeczytane [5], Kody SMS, Ważne)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Wszystkie
      FilterPill(
        title = "Wszystkie",
        isSelected = selectedFilter == SmsCategoryFilter.ALL,
        onClick = { selectedFilter = SmsCategoryFilter.ALL }
      )

      // Nieprzeczytane [5]
      FilterPillWithBadge(
        title = "Nieprzeczytane",
        badgeCount = "5",
        isSelected = selectedFilter == SmsCategoryFilter.UNREAD,
        onClick = { selectedFilter = SmsCategoryFilter.UNREAD }
      )

      // Kody SMS
      FilterPill(
        title = "Kody SMS",
        isSelected = selectedFilter == SmsCategoryFilter.OTP,
        onClick = { selectedFilter = SmsCategoryFilter.OTP }
      )

      // Ważne
      FilterPill(
        title = "Ważne",
        isSelected = selectedFilter == SmsCategoryFilter.IMPORTANT,
        onClick = { selectedFilter = SmsCategoryFilter.IMPORTANT }
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 4. LISTA ROZMÓW (Zgodnie z makietą)
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

      // Karta 1: Google z osadzoną kartą kodu OTP
      item {
        GoogleOtpConversationCard(
          onCardClick = {
            val googleThread = threads.firstOrNull { it.senderAddress.contains("Google", ignoreCase = true) }
            if (googleThread != null) onThreadClick(googleThread.threadId)
          },
          onCopyOtp = { code ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Kod OTP", code))
            Toast.makeText(context, "Skopiowano kod: $code", Toast.LENGTH_SHORT).show()
          }
        )
      }

      // Karta 2: DPD Polska
      item {
        StandardConversationCard(
          avatar = {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(24.dp)
              )
            }
          },
          senderName = "DPD Polska",
          isVerified = true,
          messageSnippet = "Twoja przesyłka jest w drodze. Planowana dostawa jutro (12.04)...",
          timestamp = "Dzisiaj, 08:44",
          badgeLabel = "Przesyłka",
          badgeColor = Color(0xFF281E48),
          badgeTextColor = Color(0xFFC8BAFF),
          onClick = {
            val dpdThread = threads.firstOrNull { it.senderAddress.contains("DPD", ignoreCase = true) }
            if (dpdThread != null) onThreadClick(dpdThread.threadId)
          }
        )
      }

      // Karta 3: Bank X
      item {
        StandardConversationCard(
          avatar = {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AppBlue),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
          },
          senderName = "Bank X",
          isVerified = true,
          messageSnippet = "Zalogowano się na Twoje konto z nowego urządzenia. Jeśli to nie Ty...",
          timestamp = "Wczoraj, 19:20",
          badgeLabel = "Ważne",
          badgeColor = Color(0xFF281E48),
          badgeTextColor = Color(0xFFC8BAFF),
          onClick = {
            val bankThread = threads.firstOrNull { it.senderAddress.contains("Bank", ignoreCase = true) }
            if (bankThread != null) onThreadClick(bankThread.threadId)
          }
        )
      }

      // Karta 4: TwojaWygrana.pl (Podejrzane)
      item {
        WarningConversationCard(
          senderName = "TwojaWygrana.pl",
          messageSnippet = "Gratulacje! Wygrałeś nowy telefon! Kliknij tutaj, aby odebrać nagrodę...",
          timestamp = "Wczoraj, 16:03",
          badgeLabel = "Podejrzane",
          onClick = {
            val scamThread = threads.firstOrNull { it.highestSeverity == RiskSeverity.HIGH }
            if (scamThread != null) onThreadClick(scamThread.threadId)
          }
        )
      }

      // Karta 5: Mama
      item {
        StandardConversationCard(
          avatar = {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AppGreen),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "M",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          },
          senderName = "Mama",
          isVerified = false,
          messageSnippet = "Czy możesz zadzwonić, gdy będziesz miał chwilę? ❤️",
          timestamp = "Wczoraj, 13:37",
          badgeLabel = null,
          onClick = {
            val mamaThread = threads.firstOrNull { it.displayName?.contains("Mama", ignoreCase = true) == true }
            if (mamaThread != null) onThreadClick(mamaThread.threadId)
          }
        )
      }

      // Karta 6: Przychodnia
      item {
        StandardConversationCard(
          avatar = {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AppPurplePrimary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.MedicalServices,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
          },
          senderName = "Przychodnia",
          isVerified = false,
          messageSnippet = "Przypominamy o wizycie jutro o godz. 10:00. Prosimy o punktualność.",
          timestamp = "11.04.2024",
          badgeLabel = "Ważne",
          badgeColor = Color(0xFF281E48),
          badgeTextColor = Color(0xFFC8BAFF),
          onClick = {
            val clinicThread = threads.firstOrNull { it.senderAddress.contains("Przychodnia", ignoreCase = true) }
            if (clinicThread != null) onThreadClick(clinicThread.threadId)
          }
        )
      }

      // Podsumowanie na dole listy
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "6 rozmów",
            fontSize = 13.sp,
            color = AppTextMuted
          )
        }
      }
    }
  }
}

/**
 * Pigułka filtru
 */
@Composable
private fun FilterPill(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(CircleShape)
      .background(if (isSelected) AppPurplePrimary else AppSurfaceVariant)
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 9.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      fontSize = 13.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = if (isSelected) Color.White else AppTextSecondary
    )
  }
}

/**
 * Pigułka filtru z licznikiem
 */
@Composable
private fun FilterPillWithBadge(
  title: String,
  badgeCount: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(CircleShape)
      .background(if (isSelected) AppPurplePrimary else AppSurfaceVariant)
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else AppTextSecondary
      )
      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier = Modifier
          .size(20.dp)
          .clip(CircleShape)
          .background(Color(0xFF8B77FF)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = badgeCount,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}

/**
 * Specjalna karta Google z kodem OTP dopasowana do makiety
 */
@Composable
private fun GoogleOtpConversationCard(
  onCardClick: () -> Unit,
  onCopyOtp: (String) -> Unit
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
    border = BorderStroke(1.dp, AppBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable { onCardClick() }
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Logo Google (Białe koło z literą G stylizowaną)
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "G",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF4285F4)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Google",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Zweryfikowana marka",
              tint = AppBlue,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Kod jednorazowy",
            fontSize = 13.sp,
            color = AppTextSecondary
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "Dzisiaj, 09:12",
            fontSize = 12.sp,
            color = AppTextSecondary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF281E48))
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = "Kod SMS",
              fontSize = 11.sp,
              color = Color(0xFFC8BAFF)
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = AppTextSecondary,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Wewnętrzny sub-card: kod OTP
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF201B34)),
        border = BorderStroke(1.dp, Color(0xFF332958)),
        modifier = Modifier.fillMaxWidth()
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
                .background(Color(0xFF352B5C)),
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
                text = "Kod jednorazowy",
                fontSize = 11.sp,
                color = AppTextSecondary
              )
              Text(
                text = "847 213",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = AppTextPrimary
              )
            }

            // Przycisk kopiowania
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2E2452))
                .clickable { onCopyOtp("847213") }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Kopiuj",
                  tint = Color(0xFFD4C8FF),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Kopiuj kod",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFFD4C8FF)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Nie udostępniaj tego kodu nikomu.",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
        }
      }
    }
  }
}

/**
 * Standardowa karta rozmowy SMS
 */
@Composable
private fun StandardConversationCard(
  avatar: @Composable () -> Unit,
  senderName: String,
  isVerified: Boolean,
  messageSnippet: String,
  timestamp: String,
  badgeLabel: String?,
  badgeColor: Color = Color(0xFF281E48),
  badgeTextColor: Color = Color(0xFFC8BAFF),
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
    border = BorderStroke(1.dp, AppBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      avatar()

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = senderName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          if (isVerified) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Zweryfikowany",
              tint = AppBlue,
              modifier = Modifier.size(16.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = messageSnippet,
          fontSize = 13.sp,
          color = AppTextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = timestamp,
          fontSize = 12.sp,
          color = AppTextSecondary
        )
        if (badgeLabel != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(badgeColor)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = badgeLabel,
              fontSize = 11.sp,
              color = badgeTextColor
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = AppTextSecondary,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

/**
 * Czerwona karta ostrzeżenia przed oszustwem (TwojaWygrana.pl)
 */
@Composable
private fun WarningConversationCard(
  senderName: String,
  messageSnippet: String,
  timestamp: String,
  badgeLabel: String,
  onClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = AppRedContainer),
    border = BorderStroke(1.dp, AppRedBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color(0xFF3E181F)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = AppRed,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = senderName,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = messageSnippet,
          fontSize = 13.sp,
          color = Color(0xFFD4B2B8),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = timestamp,
          fontSize = 12.sp,
          color = AppTextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3D1820))
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = badgeLabel,
            fontSize = 11.sp,
            color = AppRedText
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = AppTextSecondary,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
