package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SmsThread
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Menu kontekstowe wątku SMS (Long Press Menu - Sekcja 41).
 * Opcje: Przypnij/Odepnij, Wycisz, Archiwizuj, Oznacz jako spam, Usuń lokalnie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadActionBottomSheet(
  thread: SmsThread,
  onDismiss: () -> Unit,
  onTogglePin: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleArchive: () -> Unit,
  onMarkSpam: () -> Unit,
  onDeleteLocally: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF14121E),
    dragHandle = null,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 20.dp)
        .testTag("thread_action_bottom_sheet")
    ) {
      // Nagłówek wątku
      Text(
        text = thread.displayName ?: thread.senderAddress,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = AppTextPrimary
      )
      Text(
        text = thread.senderAddress,
        fontSize = 13.sp,
        color = AppTextSecondary
      )

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(thickness = 1.dp, color = AppBorder)
      Spacer(modifier = Modifier.height(8.dp))

      // 1. Przypnij / Odepnij
      ActionMenuItem(
        icon = Icons.Default.PushPin,
        iconTint = AppPurple,
        title = if (thread.isPinned) "Odepnij wątek" else "Przypnij na górze",
        testTag = "action_pin",
        onClick = {
          onTogglePin()
          onDismiss()
        }
      )

      // 2. Wycisz / Włącz powiadomienia
      ActionMenuItem(
        icon = if (thread.isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
        iconTint = Color(0xFFC8BAFF),
        title = if (thread.isMuted) "Włącz powiadomienia" else "Wycisz konwersację",
        testTag = "action_mute",
        onClick = {
          onToggleMute()
          onDismiss()
        }
      )

      // 3. Archiwizuj / Przywróć
      ActionMenuItem(
        icon = if (thread.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
        iconTint = Color(0xFFC8BAFF),
        title = if (thread.isArchived) "Przywróć ze schowka" else "Zarchiwizuj wątek",
        testTag = "action_archive",
        onClick = {
          onToggleArchive()
          onDismiss()
        }
      )

      // 4. Oznacz jako spam i zablokuj
      ActionMenuItem(
        icon = Icons.Default.Block,
        iconTint = AppRed,
        title = "Oznacz jako spam i zablokuj",
        textColor = AppRed,
        testTag = "action_spam",
        onClick = {
          onMarkSpam()
          onDismiss()
        }
      )

      // 5. Usuń lokalnie
      ActionMenuItem(
        icon = Icons.Default.Delete,
        iconTint = AppRed.copy(alpha = 0.8f),
        title = "Usuń rozmowę z telefonu",
        textColor = AppRed.copy(alpha = 0.9f),
        testTag = "action_delete",
        onClick = {
          onDeleteLocally()
          onDismiss()
        }
      )

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun ActionMenuItem(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  textColor: Color = AppTextPrimary,
  testTag: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(vertical = 12.dp, horizontal = 6.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = title,
      fontSize = 15.sp,
      fontWeight = FontWeight.Medium,
      color = textColor
    )
  }
}
