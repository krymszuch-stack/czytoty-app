package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProtectionSettings
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurpleLight
import com.example.ui.theme.AppPurplePrimary
import com.example.ui.theme.AppPurpleTrack
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppRedContainer
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary
import kotlin.math.roundToInt

/**
 * Ekran Ochrony dopasowany 1:1 do załączonej makiety (Image 4).
 */
@Composable
fun ProtectionScreen(
  settings: ProtectionSettings,
  onUpdateSettings: ((ProtectionSettings) -> ProtectionSettings) -> Unit,
  modifier: Modifier = Modifier
) {
  val currentThresholdPercent = remember(settings.blockThreshold) {
    if (settings.blockThreshold <= 1.0f) (settings.blockThreshold * 100f).roundToInt()
    else settings.blockThreshold.roundToInt()
  }

  var sliderValue by remember(settings.blockThreshold) {
    mutableFloatStateOf(currentThresholdPercent.toFloat())
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AppDarkBackground)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 18.dp, vertical = 12.dp)
  ) {

    // 1. TOP BAR (Ochrona + U avatar)
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
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = AppPurple,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Ochrona",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary
        )
        Text(
          text = "Ustawienia bezpieczeństwa połączeń i wiadomości",
          fontSize = 13.sp,
          color = AppTextSecondary
        )
      }

      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(AppPurpleContainer)
          .clickable { /* Profil */ },
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

    // 2. KARTA STATUSU OCHRONY ZE SWITCHEM I MINI-KARTAMI
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(listOf(Color(0xFF4A3882), Color(0xFF241C42)))
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = Color(0xFFD4C8FF),
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = buildAnnotatedString {
                append("Ochrona jest ")
                withStyle(SpanStyle(color = AppPurpleLight, fontWeight = FontWeight.Bold)) {
                  append("aktywna")
                }
              },
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Chroni Cię przed oszustwami,\nspamem i niechcianymi kontaktami.",
              fontSize = 12.sp,
              color = AppTextSecondary,
              lineHeight = 16.sp
            )
          }

          Switch(
            checked = settings.isProtectionActive,
            onCheckedChange = { checked ->
              onUpdateSettings { it.copy(isProtectionActive = checked) }
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = AppPurplePrimary,
              uncheckedThumbColor = Color(0xFF86829A),
              uncheckedTrackColor = Color(0xFF2B283A)
            ),
            modifier = Modifier.testTag("protection_master_switch")
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mini-karty: 12 zablokowanych połączeń, 4 ostrzeżenia
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Karta zielona
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15221B)),
            border = BorderStroke(1.dp, Color(0xFF1D3828)),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Box(
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B3224)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = AppGreen,
                    modifier = Modifier.size(16.dp)
                  )
                }
                Text(
                  text = "12",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = AppTextPrimary
                )
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = null,
                  tint = AppTextSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "zablokowanych\npołączeń dzisiaj",
                fontSize = 11.sp,
                color = AppTextSecondary,
                lineHeight = 14.sp
              )
            }
          }

          // Karta czerwona
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF28151D)),
            border = BorderStroke(1.dp, Color(0xFF481D26)),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Box(
                  modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3C1820)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AppRed,
                    modifier = Modifier.size(16.dp)
                  )
                }
                Text(
                  text = "4",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = AppTextPrimary
                )
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = null,
                  tint = AppTextSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "ostrzeżenia\ndzisiaj",
                fontSize = 11.sp,
                color = AppTextSecondary,
                lineHeight = 14.sp
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. SEKCJA: AUTOMATYCZNE BLOKOWANIE
    Text(
      text = "Automatyczne blokowanie",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = AppTextPrimary
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "Wybierz, jakie połączenia mają być automatycznie blokowane.",
      fontSize = 13.sp,
      color = AppTextSecondary
    )

    Spacer(modifier = Modifier.height(12.dp))

    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        // Pozycja 1: Bardzo wysokie ryzyko
        ProtectionToggleItem(
          icon = Icons.Default.Warning,
          iconTint = AppRed,
          iconContainerColor = AppRedContainer,
          title = "Bardzo wysokie ryzyko",
          subtitle = "Blokuje potwierdzone oszustwa i numery o skrajnym ryzyku",
          isChecked = settings.blockHighRisk,
          onCheckedChange = { checked ->
            onUpdateSettings { it.copy(blockHighRisk = checked) }
          }
        )

        HorizontalDivider(
          thickness = 1.dp,
          color = Color(0xFF242136),
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Pozycja 2: Telemarketing
        ProtectionToggleItem(
          icon = Icons.Default.Group,
          iconTint = AppPurple,
          iconContainerColor = AppPurpleContainer,
          title = "Telemarketing",
          subtitle = "Blokuje natrętne oferty handlowe i ankiety telefoniczne",
          isChecked = settings.blockTelemarketing,
          onCheckedChange = { checked ->
            onUpdateSettings { it.copy(blockTelemarketing = checked) }
          }
        )

        HorizontalDivider(
          thickness = 1.dp,
          color = Color(0xFF242136),
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Pozycja 3: Nieznane numery
        ProtectionToggleItem(
          icon = Icons.Default.Block,
          iconTint = AppTextSecondary,
          iconContainerColor = Color(0xFF221F30),
          title = "Nieznane numery",
          subtitle = "Odrzuca wszystkie połączenia spoza Twojej listy kontaktów",
          isChecked = settings.blockUnknown,
          onCheckedChange = { checked ->
            onUpdateSettings { it.copy(blockUnknown = checked) }
          }
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4. SEKCJA: OCHRONA WIADOMOŚCI SMS
    Text(
      text = "Ochrona wiadomości SMS",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = AppTextPrimary
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "Analizujemy wiadomości i ostrzegamy przed zagrożeniami.",
      fontSize = 13.sp,
      color = AppTextSecondary
    )

    Spacer(modifier = Modifier.height(12.dp))

    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        // Pozycja 1: Podejrzane linki w SMS
        ProtectionToggleItem(
          icon = Icons.Default.ChatBubble,
          iconTint = AppPurple,
          iconContainerColor = AppPurpleContainer,
          title = "Podejrzane linki w SMS",
          subtitle = "Wykrywa i ostrzega przed niebezpiecznymi linkami w wiadomościach",
          isChecked = true,
          onCheckedChange = { }
        )

        HorizontalDivider(
          thickness = 1.dp,
          color = Color(0xFF242136),
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Pozycja 2: Wyróżniaj kody OTP
        ProtectionToggleItem(
          icon = Icons.Default.Lock,
          iconTint = AppPurple,
          iconContainerColor = AppPurpleContainer,
          title = "Wyróżniaj kody OTP",
          subtitle = "Automatycznie wykrywa i wyróżnia kody jednorazowe (OTP)",
          isChecked = true,
          onCheckedChange = { }
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. SEKCJA: PRÓG AUTOMATYCZNEJ BLOKADY
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Próg automatycznej blokady",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = AppTextPrimary
      )

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(AppPurplePrimary)
          .padding(horizontal = 12.dp, vertical = 4.dp)
      ) {
        Text(
          text = "${sliderValue.roundToInt()}%",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = "Połączenia powyżej tego wskaźnika zostaną automatycznie zablokowane.",
      fontSize = 13.sp,
      color = AppTextSecondary
    )

    Spacer(modifier = Modifier.height(12.dp))

    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Slider(
          value = sliderValue,
          onValueChange = {
            sliderValue = it
            onUpdateSettings { s -> s.copy(blockThreshold = it) }
          },
          valueRange = 50f..95f,
          steps = 8,
          colors = SliderDefaults.colors(
            thumbColor = Color(0xFFD4C8FF),
            activeTrackColor = AppPurpleTrack,
            inactiveTrackColor = Color(0xFF332B4C),
            activeTickColor = Color(0xFF7A68BD),
            inactiveTickColor = Color(0xFF433966)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("protection_threshold_slider")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Niższy próg (mniej blokad)",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
          Text(
            text = "Zrównoważony",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
          Text(
            text = "Wyższy próg (większa ochrona)",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
  }
}

/**
 * Wiersz z przełącznikiem (switch) w sekcjach ochrony
 */
@Composable
private fun ProtectionToggleItem(
  icon: ImageVector,
  iconTint: Color,
  iconContainerColor: Color,
  title: String,
  subtitle: String,
  isChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(iconContainerColor),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(20.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = AppTextPrimary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        fontSize = 12.sp,
        color = AppTextSecondary,
        lineHeight = 16.sp
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    Switch(
      checked = isChecked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = AppPurplePrimary,
        uncheckedThumbColor = Color(0xFF86829A),
        uncheckedTrackColor = Color(0xFF2B283A)
      )
    )
  }
}
