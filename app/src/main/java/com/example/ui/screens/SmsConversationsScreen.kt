package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.ThreadActionBottomSheet
import com.example.ui.theme.AppBlue
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurplePrimary
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppRedContainer
import com.example.ui.theme.AppRedText
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

enum class SmsCategoryFilter(val displayName: String) {
  ALL("Wszystkie"),
  UNREAD("Nieprzeczytane"),
  BRANDS("Firmy"),
  OTP("Kody OTP"),
  SUSPICIOUS("Podejrzane"),
  ARCHIVED("Archiwum")
}

/**
 * Nowoczesny ekran listy konwersacji SMS (Wiadomości / Messenger UX - Sekcje 40, 41, 50, 51, 54, 56).
 */
@Composable
fun SmsConversationsScreen(
  threads: List<SmsThread>,
  onThreadClick: (String) -> Unit,
  onSenderProfileClick: (String) -> Unit,
  onNewMessage: () -> Unit,
  onSimulateSms: (SmsSimulationScenario) -> Unit,
  onTogglePin: (String) -> Unit = {},
  onReorderPinned: (Int, Int) -> Unit = { _, _ -> },
  onToggleMute: (String) -> Unit = {},
  onToggleArchive: (String) -> Unit = {},
  onDeleteThread: (String) -> Unit = {},
  onMarkSpam: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf(SmsCategoryFilter.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var activeThreadForActions by remember { mutableStateOf<SmsThread?>(null) }
  val context = LocalContext.current

  // Filtrowanie wątków
  val filteredThreads = remember(threads, selectedFilter, searchQuery) {
    threads.filter { thread ->
      // 1. Filtr Archiwum
      if (selectedFilter == SmsCategoryFilter.ARCHIVED) {
        if (!thread.isArchived) return@filter false
      } else {
        if (thread.isArchived) return@filter false
      }

      // 2. Filtry kategorii
      val matchesCategory = when (selectedFilter) {
        SmsCategoryFilter.ALL -> true
        SmsCategoryFilter.UNREAD -> thread.unreadCount > 0
        SmsCategoryFilter.BRANDS -> thread.recognizedBrand != null
        SmsCategoryFilter.OTP -> thread.hasOtp
        SmsCategoryFilter.SUSPICIOUS -> thread.highestSeverity == RiskSeverity.HIGH || thread.isSpamOrBlocked
        SmsCategoryFilter.ARCHIVED -> true
      }

      // 3. Filtr wyszukiwania
      val matchesSearch = if (searchQuery.isBlank()) true else {
        val q = searchQuery.trim().lowercase()
        (thread.displayName?.lowercase()?.contains(q) == true) ||
          thread.senderAddress.lowercase().contains(q) ||
          thread.lastMessageSnippet.lowercase().contains(q)
      }

      matchesCategory && matchesSearch
    }
  }

  val pinnedThreads = remember(filteredThreads) { filteredThreads.filter { it.isPinned } }
  val unpinnedThreads = remember(filteredThreads) { filteredThreads.filterNot { it.isPinned } }

  val unreadTotal = remember(threads) { threads.filterNot { it.isArchived }.sumOf { it.unreadCount } }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AppDarkBackground)
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {

    // 1. TOP BAR (Nagłówek i status)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Wiadomości",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          if (unreadTotal > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(AppPurplePrimary)
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "$unreadTotal nowe",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
        Text(
          text = "Bezpieczny komunikator SMS • Ochrona lokalna",
          fontSize = 13.sp,
          color = AppTextSecondary
        )
      }

      // Przycisk nowej wiadomości
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(AppPurplePrimary)
          .clickable { onNewMessage() }
          .testTag("btn_new_sms"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Message,
          contentDescription = "Nowa wiadomość",
          tint = Color.White,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. SEARCH BAR (Pill input z przyciskiem wyczyszczenia)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .clip(CircleShape)
        .background(AppSurfaceVariant)
        .padding(horizontal = 14.dp),
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
              text = "Szukaj kontaktu, firmy lub treści...",
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
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { searchQuery = "" },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = "Wyczyść",
              tint = AppTextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 3. FILTER PILLS (Chips)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      SmsCategoryFilter.entries.forEach { filter ->
        val isSelected = selectedFilter == filter
        val badge = when (filter) {
          SmsCategoryFilter.UNREAD -> if (unreadTotal > 0) unreadTotal.toString() else null
          SmsCategoryFilter.SUSPICIOUS -> {
            val count = threads.count { it.highestSeverity == RiskSeverity.HIGH }
            if (count > 0) count.toString() else null
          }
          else -> null
        }

        MessengerFilterChip(
          title = filter.displayName,
          badgeCount = badge,
          isSelected = isSelected,
          onClick = { selectedFilter = filter }
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. LISTA ROZMÓW (Z podziałem na Przypięte i Pozostałe)
    if (filteredThreads.isEmpty()) {
      // EMPTY STATE
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("sms_empty_state"),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Box(
            modifier = Modifier
              .size(60.dp)
              .clip(CircleShape)
              .background(AppSurfaceVariant),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (selectedFilter) {
                SmsCategoryFilter.SUSPICIOUS -> Icons.Default.Security
                SmsCategoryFilter.ARCHIVED -> Icons.Default.Archive
                else -> Icons.Default.Message
              },
              contentDescription = null,
              tint = AppTextSecondary,
              modifier = Modifier.size(30.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = when (selectedFilter) {
              SmsCategoryFilter.SUSPICIOUS -> "Brak nowych ostrzeżeń"
              SmsCategoryFilter.ARCHIVED -> "Brak zarchiwizowanych wiadomości"
              SmsCategoryFilter.UNREAD -> "Wszystkie wiadomości są przeczytane"
              else -> if (searchQuery.isNotEmpty()) "Brak wyników wyszukiwania" else "Nie masz jeszcze wiadomości"
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = when (selectedFilter) {
              SmsCategoryFilter.SUSPICIOUS -> "CzyToTy? nie wykryło żadnych podejrzanych linków ani prób smishingu."
              SmsCategoryFilter.ARCHIVED -> "Wątki zarchiwizowane pojawią się w tym folderze."
              else -> "Wiadomości SMS od osób i instytucji pojawią się tutaj automatycznie."
            },
            fontSize = 13.sp,
            color = AppTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )

          if (selectedFilter == SmsCategoryFilter.ALL && searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
              onClick = onNewMessage,
              colors = ButtonDefaults.buttonColors(containerColor = AppPurplePrimary),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.height(48.dp)
            ) {
              Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Napisz nową wiadomość", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .testTag("sms_thread_list"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // SEKCJA: PRZYPIĘTE (jeśli są)
        if (pinnedThreads.isNotEmpty() && selectedFilter != SmsCategoryFilter.ARCHIVED) {
          item(key = "header_pinned") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = AppPurple,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "PRZYPIĘTE (${pinnedThreads.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AppPurple,
                letterSpacing = 0.5.sp
              )
            }
          }

          itemsIndexed(pinnedThreads, key = { _, t -> "pinned_${t.threadId}" }) { index, thread ->
            MessengerThreadCard(
              thread = thread,
              isPinned = true,
              canMoveUp = index > 0,
              canMoveDown = index < pinnedThreads.size - 1,
              onMoveUp = { onReorderPinned(index, index - 1) },
              onMoveDown = { onReorderPinned(index, index + 1) },
              onClick = { onThreadClick(thread.threadId) },
              onLongClick = { activeThreadForActions = thread },
              onAvatarClick = { onSenderProfileClick(thread.senderAddress) }
            )
          }

          if (unpinnedThreads.isNotEmpty()) {
            item(key = "header_all") {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "WSZYSTKIE ROZMOWY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AppTextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
              )
            }
          }
        }

        // SEKCJA: POZOSTAŁE WĄTKI
        itemsIndexed(unpinnedThreads, key = { _, t -> t.threadId }) { _, thread ->
          MessengerThreadCard(
            thread = thread,
            isPinned = false,
            onClick = { onThreadClick(thread.threadId) },
            onLongClick = { activeThreadForActions = thread },
            onAvatarClick = { onSenderProfileClick(thread.senderAddress) }
          )
        }

        // Stopka z liczbą wątków
        item(key = "footer_count") {
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${filteredThreads.size} wątków w skrzynce",
              fontSize = 12.sp,
              color = AppTextMuted
            )
          }
        }
      }
    }
  }

  // BOTTOM SHEET DLA AKCJI (LONG PRESS - Sekcja 41)
  activeThreadForActions?.let { thread ->
    ThreadActionBottomSheet(
      thread = thread,
      onDismiss = { activeThreadForActions = null },
      onTogglePin = { onTogglePin(thread.threadId) },
      onToggleMute = { onToggleMute(thread.threadId) },
      onToggleArchive = { onToggleArchive(thread.threadId) },
      onMarkSpam = { onMarkSpam(thread.threadId) },
      onDeleteLocally = { onDeleteThread(thread.threadId) }
    )
  }
}

/**
 * Nowoczesna karta wątku SMS (WhatsApp + Discord ergonomics - Sekcja 41 & 54 Senior UX).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessengerThreadCard(
  thread: SmsThread,
  isPinned: Boolean,
  canMoveUp: Boolean = false,
  canMoveDown: Boolean = false,
  onMoveUp: () -> Unit = {},
  onMoveDown: () -> Unit = {},
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onAvatarClick: () -> Unit
) {
  val isHighRisk = thread.highestSeverity == RiskSeverity.HIGH || thread.isSpamOrBlocked
  val hasUnread = thread.unreadCount > 0

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = when {
        isHighRisk -> AppRedContainer
        isPinned -> Color(0xFF1E1A30)
        else -> AppSurfaceVariant
      }
    ),
    border = BorderStroke(
      width = 1.dp,
      color = when {
        isHighRisk -> AppRed.copy(alpha = 0.4f)
        isPinned -> AppPurple.copy(alpha = 0.4f)
        else -> AppBorder
      }
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .testTag("thread_card_${thread.threadId}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. AVATAR / LOGO (Klikalny -> profil)
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(
            when {
              isHighRisk -> Color(0xFF38141C)
              thread.recognizedBrand != null -> Color.White
              else -> Color(0xFF2E2648)
            }
          )
          .clickable { onAvatarClick() }
          .testTag("avatar_${thread.threadId}"),
        contentAlignment = Alignment.Center
      ) {
        when {
          isHighRisk -> {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = AppRed,
              modifier = Modifier.size(24.dp)
            )
          }
          thread.recognizedBrand?.brandId == "google_auth" -> {
            Text(text = "G", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4285F4))
          }
          thread.recognizedBrand?.brandId == "dpd_pl" -> {
            Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
          }
          thread.recognizedBrand?.brandId == "inpost_pl" -> {
            Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(24.dp))
          }
          thread.recognizedBrand?.isFinancialInstitution == true -> {
            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = AppBlue, modifier = Modifier.size(24.dp))
          }
          thread.displayName?.contains("Przychodnia", ignoreCase = true) == true -> {
            Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, tint = AppPurple, modifier = Modifier.size(24.dp))
          }
          else -> {
            // Pierwsza litera nazwy (Senior readability)
            val initial = (thread.displayName ?: thread.senderAddress).firstOrNull()?.uppercase() ?: "S"
            Text(
              text = initial,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // 2. NAZWA, STATUS I TREŚĆ WIADOMOŚCI (Accessibility & Senior UX)
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = thread.displayName ?: thread.senderAddress,
            fontSize = 16.sp,
            fontWeight = if (hasUnread) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isHighRisk) AppRedText else AppTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )

          if (thread.isBrandVerified) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Zweryfikowany",
              tint = AppBlue,
              modifier = Modifier.size(16.dp)
            )
          }

          if (thread.isMuted) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.NotificationsOff,
              contentDescription = "Wyciszony",
              tint = AppTextSecondary,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Ostatnia wiadomość
        Text(
          text = thread.lastMessageSnippet,
          fontSize = 13.sp,
          color = if (hasUnread) AppTextPrimary else AppTextSecondary,
          fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        // Etykieta kategorii (jeśli jest)
        if (thread.categoryTag != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(
                when {
                  isHighRisk -> Color(0xFF4A1A22)
                  thread.hasOtp -> Color(0xFF2C224E)
                  else -> Color(0xFF201D30)
                }
              )
              .padding(horizontal = 7.dp, vertical = 2.dp)
          ) {
            Text(
              text = thread.categoryTag,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = when {
                isHighRisk -> AppRedText
                thread.hasOtp -> Color(0xFFC8BAFF)
                else -> AppTextSecondary
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // 3. CZAS, LICZNIK UNREAD I REORDERING PINNED
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = thread.lastMessageTimestamp,
          fontSize = 12.sp,
          fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
          color = if (hasUnread) AppPurplePrimary else AppTextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (hasUnread) {
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(AppPurplePrimary)
                .padding(horizontal = 7.dp, vertical = 2.dp)
                .testTag("unread_badge_${thread.threadId}")
            ) {
              Text(
                text = "${thread.unreadCount}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }

          // Klawisze manualnego reorderingu dla przypiętych (Sekcja 50: Manual order > automatyka)
          if (isPinned) {
            Spacer(modifier = Modifier.width(4.dp))
            if (canMoveUp) {
              IconButton(onClick = onMoveUp, modifier = Modifier.size(24.dp).testTag("pin_up_${thread.threadId}")) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Przesuń wyżej", tint = AppPurple, modifier = Modifier.size(18.dp))
              }
            }
            if (canMoveDown) {
              IconButton(onClick = onMoveDown, modifier = Modifier.size(24.dp).testTag("pin_down_${thread.threadId}")) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Przesuń niżej", tint = AppPurple, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Pigułka filtru (Messenger Chip)
 */
@Composable
private fun MessengerFilterChip(
  title: String,
  badgeCount: String?,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = CircleShape,
    color = if (isSelected) AppPurplePrimary else AppSurfaceVariant,
    border = BorderStroke(1.dp, if (isSelected) AppPurple else AppBorder),
    modifier = Modifier
      .clip(CircleShape)
      .clickable { onClick() }
      .testTag("filter_chip_$title")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else AppTextSecondary
      )
      if (badgeCount != null) {
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(if (isSelected) Color.White else AppPurple),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = badgeCount,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) AppPurplePrimary else Color.White
          )
        }
      }
    }
  }
}
