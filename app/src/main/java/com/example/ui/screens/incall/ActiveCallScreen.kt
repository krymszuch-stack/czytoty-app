package com.example.ui.screens.incall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.incall.model.AudioRoute
import com.example.incall.model.CallScreenState
import com.example.incall.model.InCallUiState
import com.example.model.RiskSeverity
import com.example.ui.components.ConversationSafetySection
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

/**
 * Główny interaktywny ekran aktywnej rozmowy (Sekcje 76–96).
 *
 * Gwarantuje:
 * 1. Zero accidental touch dzięki warstwie Touch Lock i sensorowi zbliżeniowemu.
 * 2. Precyzyjny centralny stoper (MM:SS / HH:MM:SS).
 * 3. Dyskretny wskaźnik sieci komórkowej (5G/LTE/Wi-Fi Calling).
 * 4. Klawiaturę DTMF (P0) do systemów IVR.
 * 5. Kartę kontekstu (COMPACT vs EXPANDED z gestami swipe up/down).
 * 6. Podgląd SMS z zachowaniem prywatności danych wrażliwych.
 */
@Composable
fun ActiveCallScreen(
  uiState: InCallUiState,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onToggleHold: () -> Unit,
  onToggleKeypad: (Boolean) -> Unit,
  onDtmfDigit: (Char) -> Unit,
  onClearDtmf: () -> Unit,
  onToggleContextExpansion: (Boolean) -> Unit,
  onHangup: () -> Unit,
  onOpenMessages: () -> Unit = {},
  onStartSafetyCheck: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isHighRisk = uiState.caller.severity == RiskSeverity.HIGH
  val isFinancial = uiState.caller.isFinancialInstitution

  // Gest przeciągania w górę/w dół dla przełączania COMPACT <-> EXPANDED
  var dragAccumulator by remember { mutableStateOf(0f) }
  val draggableState = rememberDraggableState { delta ->
    dragAccumulator += delta
    if (dragAccumulator < -60f && !uiState.isExpandedContext) {
      onToggleContextExpansion(true)
      dragAccumulator = 0f
    } else if (dragAccumulator > 60f && uiState.isExpandedContext) {
      onToggleContextExpansion(false)
      dragAccumulator = 0f
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(AppDarkBackground)
      .draggable(state = draggableState, orientation = Orientation.Vertical)
      .testTag("active_call_screen")
  ) {

    // =========================================================================
    // 1. GŁÓWNA ZAWARTOŚĆ EKRANU AKTYWNEJ ROZMOWY
    // =========================================================================
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {

      // GÓRA EKRANU: AVATAR, NAZWA, FIRMA / KATEGORIA (Sekcja 79)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp)
      ) {
        // Wskaźnik sieci u góry
        InCallNetworkIndicator(
          networkStatus = uiState.networkStatus,
          audioRoute = uiState.audioRoute,
          modifier = Modifier.padding(bottom = 14.dp)
        )

        // Duży avatar / logo rozmówcy
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(if (isHighRisk) AppRedContainer else AppPurpleContainer)
        ) {
          Icon(
            imageVector = when {
              uiState.caller.isFinancialInstitution -> Icons.Default.Business
              isHighRisk -> Icons.Default.Shield
              uiState.caller.isOfficialDirectoryNumber || uiState.caller.displayName != null -> Icons.Default.Business
              else -> Icons.Default.Person
            },
            contentDescription = null,
            tint = if (isHighRisk) AppRedText else AppPurple,
            modifier = Modifier.size(42.dp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Nazwa lub numer
        Text(
          text = uiState.caller.displayName ?: uiState.caller.formattedNumber,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        if (uiState.caller.displayName != null) {
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = uiState.caller.formattedNumber,
            fontSize = 15.sp,
            color = AppTextSecondary
          )
        }

        // Kategoria / Firma
        val categoryLabel = uiState.caller.category ?: uiState.contextInfo?.organizationName
        if (!categoryLabel.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = categoryLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppPurple
          )
        }
      }

      // ŚRODEK EKRANU: TIMER, STATUS, DYNAMIC RISK BANNER (Sekcje 79, 83, 90)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Centralny stoper rozmowy (MM:SS lub HH:MM:SS)
        Text(
          text = uiState.formattedDuration,
          fontFamily = FontFamily.Monospace,
          fontSize = 38.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          modifier = Modifier.testTag("in_call_timer")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Status połączenia (Połączono, Łączenie, Zawieszone)
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = when (uiState.callState) {
            CallScreenState.HELD -> Color(0xFF332514)
            CallScreenState.ACTIVE -> Color(0xFF152A18)
            else -> Color(0xFF1E1B2C)
          },
          border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.5f))
        ) {
          Text(
            text = when (uiState.callState) {
              CallScreenState.ACTIVE -> "Połączono"
              CallScreenState.HELD -> "Połączenie zawieszone"
              CallScreenState.CONNECTING -> "Łączenie..."
              CallScreenState.ENDED -> "Zakończono"
              else -> uiState.callState.label
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = when (uiState.callState) {
              CallScreenState.HELD -> Color(0xFFFFB74D)
              CallScreenState.ACTIVE -> AppGreen
              else -> AppTextSecondary
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
          )
        }

        // DYNAMICZNY BANER RYZYKA (Sekcja 90)
        if (uiState.dynamicRiskWarning != null || isHighRisk) {
          Spacer(modifier = Modifier.height(14.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppRedContainer.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color(0xFFEF5350)),
            modifier = Modifier.fillMaxWidth(0.92f)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AppRedText,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = uiState.dynamicRiskWarning ?: "Wykryto sygnały podwyższonego ryzyka połączenia",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppRedText,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      // DÓŁ EKRANU: KARTA KONTEKSTU ORAZ PRZYCISKI AKCJI (Sekcje 86, 87)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Rozwijana karta kontekstu (Swipe up / Tap)
        InCallContextSheet(
          contextInfo = uiState.contextInfo,
          isExpanded = uiState.isExpandedContext,
          onToggleExpand = { onToggleContextExpansion(!uiState.isExpandedContext) },
          modifier = Modifier.padding(bottom = 16.dp)
        )

        // Rząd 1 akcji: [ Mute ] [ Keypad ] [ Speaker ]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          InCallActionButton(
            icon = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (uiState.isMuted) "Wyciszony" else "Wycisz",
            isActive = uiState.isMuted,
            onClick = onToggleMute,
            tag = "btn_in_call_mute"
          )

          InCallActionButton(
            icon = Icons.Default.Dialpad,
            label = "Klawiatura",
            isActive = uiState.isKeypadOpen,
            onClick = { onToggleKeypad(!uiState.isKeypadOpen) },
            tag = "btn_in_call_keypad"
          )

          InCallActionButton(
            icon = if (uiState.audioRoute == AudioRoute.SPEAKER) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
            label = if (uiState.audioRoute == AudioRoute.SPEAKER) "Głośnik" else "Słuchawka",
            isActive = uiState.audioRoute == AudioRoute.SPEAKER,
            onClick = onToggleSpeaker,
            tag = "btn_in_call_speaker"
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Rząd 2 akcji: [ Wiadomość ] [ Zawieś / Wznów ] [ 🛡 Bezpieczeństwo ]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          InCallActionButton(
            icon = Icons.Default.Message,
            label = "Wiadomość",
            isActive = false,
            onClick = onOpenMessages,
            tag = "btn_in_call_sms"
          )

          InCallActionButton(
            icon = if (uiState.callState == CallScreenState.HELD) Icons.Default.PlayArrow else Icons.Default.Pause,
            label = if (uiState.callState == CallScreenState.HELD) "Wznów" else "Zawieś",
            isActive = uiState.callState == CallScreenState.HELD,
            onClick = onToggleHold,
            tag = "btn_in_call_hold"
          )

          InCallActionButton(
            icon = Icons.Default.Shield,
            label = "Bezpieczeństwo",
            isActive = false,
            onClick = onStartSafetyCheck,
            tag = "btn_in_call_safety"
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Główny duży przycisk ZAKOŃCZ (Hangup - min 56dp)
        Button(
          onClick = onHangup,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFD32F2F),
            contentColor = Color.White
          ),
          shape = CircleShape,
          modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(58.dp)
            .testTag("btn_in_call_hangup")
        ) {
          Icon(
            imageVector = Icons.Default.CallEnd,
            contentDescription = "Zakończ połączenie",
            modifier = Modifier.size(26.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Zakończ",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // =========================================================================
    // 2. KLAWIATURA NUMERYCZNA DTMF (Sheet / Dialog - Sekcja 87)
    // =========================================================================
    AnimatedVisibility(
      visible = uiState.isKeypadOpen,
      enter = slideInVertically { it } + fadeIn(),
      exit = slideOutVertically { it } + fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      DtmfKeypadSheet(
        dialedSequence = uiState.dtmfSequence,
        onDigitPress = onDtmfDigit,
        onClear = onClearDtmf,
        onDismiss = { onToggleKeypad(false) }
      )
    }

    // =========================================================================
    // 3. WARSTWA TOUCH LOCK & BLACKOUT OVERLAY (Sekcje 77, 78, 92)
    // =========================================================================
    // Gdy sensor zbliżeniowy wykryje NEAR i telefon jest przy uchu:
    // Całkowite wygaszenie ekranu (Color.Black) i przechwycenie 100% dotknięć (Zero Accidental Touch).
    if (uiState.isTouchLocked) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black)
          .pointerInput(Unit) {
            // Przechwytuje i blokuje wszystkie zdarzenia dotykowe policzkiem/twarzą
            awaitPointerEventScope {
              while (true) {
                awaitPointerEvent()
              }
            }
          }
          .testTag("in_call_touch_lock_overlay")
      )
    }
  }
}

@Composable
private fun InCallActionButton(
  icon: ImageVector,
  label: String,
  isActive: Boolean,
  onClick: () -> Unit,
  tag: String
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp)
      .testTag(tag)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(56.dp)
        .clip(CircleShape)
        .background(if (isActive) Color(0xFF8E7CFF) else Color(0xFF221F32))
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isActive) Color.White else AppTextPrimary,
        modifier = Modifier.size(24.dp)
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
      color = if (isActive) Color(0xFF8E7CFF) else AppTextSecondary
    )
  }
}
