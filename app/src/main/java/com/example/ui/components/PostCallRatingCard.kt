package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PostCallEvaluation
import com.example.model.QuickRating
import com.example.model.SubCategory
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary
import kotlinx.coroutines.delay

/**
 * Pływająca karta oceny numeru po zakończeniu połączenia (SEKCJE 19, 30).
 *
 * Cechy:
 * - Nie blokuje ekranu (nie jest agresywnym modalnym popupem).
 * - Duże, czytelne przyciski dotykowe (min. 48dp) łatwe dla seniorów.
 * - Automatyczne ciche zniknięcie po 15 sekundach z możliwością natychmiastowego "Pomiń".
 * - Jedno kliknięcie wystarczy do trwałego zapisania oceny (krok 2 uszczegółowienia jest opcjonalny).
 */
@Composable
fun PostCallRatingCard(
  evaluation: PostCallEvaluation,
  onDismiss: () -> Unit,
  onSubmitRating: (QuickRating, SubCategory?) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedRating by remember { mutableStateOf<QuickRating?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }
  var progressTarget by remember { mutableStateOf(1f) }

  // Auto-dismiss po 15 sekundach (jeśli użytkownik nic nie wybierze)
  LaunchedEffect(Unit) {
    progressTarget = 0f
    delay(15_000L)
    if (selectedRating == null) {
      onDismiss()
    }
  }

  val progress by animateFloatAsState(
    targetValue = progressTarget,
    animationSpec = tween(durationMillis = 15_000, easing = LinearEasing),
    label = "AutoDismissProgress"
  )

  AnimatedVisibility(
    visible = true,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 10.dp)
      .testTag("post_call_rating_card")
  ) {
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1A29)),
      border = BorderStroke(1.dp, Color(0xFF38334E)),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {

        // 1. NAGŁÓWEK KARTY
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(AppPurpleContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PhoneCallback,
              contentDescription = null,
              tint = AppPurple,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (selectedRating == null) "Jak oceniasz to połączenie?" else "Dziękujemy za ocenę!",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = evaluation.callerInfo.displayName ?: evaluation.callerInfo.formattedNumber,
              fontSize = 12.sp,
              color = AppTextSecondary
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp).testTag("dismiss_post_call_card")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Pomiń",
              tint = AppTextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. KROK 1: 3 GŁÓWNE DUŻE PRZYCISKI (SEKCJA 19, 30)
        if (selectedRating == null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Przycisk 1: 👍 W porządku
            RatingActionButton(
              title = "W porządku",
              emoji = "👍",
              containerColor = Color(0xFF172B20),
              borderColor = Color(0xFF224430),
              textColor = AppGreen,
              testTag = "rating_positive_btn",
              onClick = {
                selectedRating = QuickRating.POSITIVE
                onSubmitRating(QuickRating.POSITIVE, null)
              },
              modifier = Modifier.weight(1f)
            )

            // Przycisk 2: 😒 Natrętny
            RatingActionButton(
              title = "Natrętny",
              emoji = "😒",
              containerColor = Color(0xFF2B2217),
              borderColor = Color(0xFF45341E),
              textColor = Color(0xFFFFB74D),
              testTag = "rating_nuisance_btn",
              onClick = {
                selectedRating = QuickRating.NUISANCE
                onSubmitRating(QuickRating.NUISANCE, null)
              },
              modifier = Modifier.weight(1f)
            )

            // Przycisk 3: ⚠️ Oszustwo
            RatingActionButton(
              title = "Podejrzany",
              emoji = "⚠️",
              containerColor = Color(0xFF381820),
              borderColor = Color(0xFF561F2A),
              textColor = AppRed,
              testTag = "rating_scam_btn",
              onClick = {
                selectedRating = QuickRating.SCAM
                onSubmitRating(QuickRating.SCAM, null)
              },
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Pasek odliczania (subtelna informacja o braku blokowania telefonu)
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(2.dp)
              .clip(CircleShape),
            color = Color(0xFF50496E),
            trackColor = Color(0xFF252136)
          )
        } else {
          // 3. KROK 2: OPCJONALNE USZCZEGÓŁOWIENIE KATEGORII (SEKCJA 21, 25)
          val subcategories = SubCategory.getForParent(selectedRating!!)

          Column {
            Text(
              text = "Opcjonalnie: co to był za kontakt?",
              fontSize = 12.sp,
              color = AppTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              subcategories.forEach { subCat ->
                Box(
                  modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF2A2444))
                    .clickable {
                      onSubmitRating(selectedRating!!, subCat)
                      onDismiss()
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                  Text(
                    text = subCat.displayName,
                    fontSize = 12.sp,
                    color = Color(0xFFD4C8FF),
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Zapisano ocenę",
                fontSize = 12.sp,
                color = AppGreen,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF2A2444))
                  .clickable { onDismiss() }
                  .padding(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "Zamknij",
                  fontSize = 12.sp,
                  color = AppTextPrimary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Duży, bezpieczny przycisk akcji oceny (Touch target > 48dp dla osób 60+)
 */
@Composable
private fun RatingActionButton(
  title: String,
  emoji: String,
  containerColor: Color,
  borderColor: Color,
  textColor: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .height(52.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(containerColor)
      .clickable { onClick() }
      .testTag(testTag)
      .padding(horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = emoji,
        fontSize = 16.sp
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
      )
    }
  }
}
