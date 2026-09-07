package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.example.model.CallRecord
import com.example.model.SmsThread
import com.example.model.UnifiedContact
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppGreenContainer
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurpleLight
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppRedBorder
import com.example.ui.theme.AppRedContainer
import com.example.ui.theme.AppRedText
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppSurfaceContainerHigh
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Zunifikowany pulpit główny - "Centrum komunikacji"
 * Wzorowany 1:1 na załączonych makietach Material 3 Dark.
 */
@Composable
fun HomeScreen(
  recentCalls: List<CallRecord>,
  smsThreads: List<SmsThread>,
  unifiedContacts: List<UnifiedContact> = emptyList(),
  onNavigateToCalls: () -> Unit,
  onNavigateToMessages: () -> Unit,
  onNavigateToProtection: () -> Unit,
  onNavigateToCheck: () -> Unit = {},
  onNavigateToContacts: () -> Unit = {},
  onSelectContact: (UnifiedContact) -> Unit = {},
  onLookupNumber: (String) -> Unit,
  onOpenSmsThread: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AppDarkBackground)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 18.dp, vertical = 12.dp)
  ) {

    // 1. BRAND HEADER CzyToTy? ("Serduszko produktu")
    com.example.ui.components.CzyToTyBrandHeader(
      subtitle = "WIESZ, ZANIM ODBIERZESZ",
      showLogo = true,
      logoSize = 48.dp
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 2. CZTERY FILARY MARKI (Ochrona, Telefon, Wiadomości, Dla każdego)
    com.example.ui.components.CzyToTyPillarsSection(
      onPillarClick = { pillarIndex ->
        when (pillarIndex) {
          0 -> onNavigateToProtection()
          1 -> onNavigateToCheck()
          2 -> onNavigateToMessages()
          3 -> onNavigateToContacts()
        }
      }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 2. KARTA GŁÓWNA: Ochrona jest aktywna
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .clickable { onNavigateToProtection() }
        .testTag("home_protection_banner")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Glowing shield circle
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                listOf(Color(0xFF4A3882), Color(0xFF241C42))
              )
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
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "Chroni Cię przed oszustwami,\nspamem i nieznanymi numerami.",
            fontSize = 13.sp,
            color = AppTextSecondary,
            lineHeight = 18.sp
          )
        }

        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = AppTextSecondary,
          modifier = Modifier.size(24.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. TRZY GŁÓWNE AKCJE (Row of 3 cards: Zadzwoń, Wiadomości, Sprawdź numer)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Karta: Zadzwoń
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(20.dp))
          .clickable { onNavigateToCalls() }
          .testTag("home_action_call")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(AppGreen),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Phone,
              contentDescription = "Zadzwoń",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Zadzwoń",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          Text(
            text = "Wybierz numer",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
        }
      }

      // Karta: Wiadomości
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(20.dp))
          .clickable { onNavigateToMessages() }
          .testTag("home_action_sms")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(AppPurple),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.ChatBubble,
              contentDescription = "Wiadomości",
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Wiadomości",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          Text(
            text = "Wyślij SMS",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
        }
      }

      // Karta: Sprawdź numer
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(20.dp))
          .clickable { onNavigateToCheck() }
          .testTag("home_action_check")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(AppPurpleContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Sprawdź numer",
              tint = AppPurple,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Sprawdź numer",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          Text(
            text = "Zobacz ryzyko",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(22.dp))

    // 4. SEKCJA: DZISIAJ (3 metryki obok siebie)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Dzisiaj",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = AppTextPrimary
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { onNavigateToCalls() }
          .padding(4.dp)
      ) {
        Text(
          text = "Zobacz wszystkie",
          fontSize = 13.sp,
          color = AppTextSecondary
        )
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = AppTextSecondary,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // 1. Ostatnie połączenia (3)
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(18.dp))
          .clickable { onNavigateToCalls() }
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(AppGreenContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = AppGreen,
                modifier = Modifier.size(18.dp)
              )
            }
            Text(
              text = "3",
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = AppTextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Ostatnie\npołączenia",
            fontSize = 12.sp,
            color = AppTextSecondary,
            lineHeight = 16.sp
          )
        }
      }

      // 2. Nowe wiadomości (5)
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(18.dp))
          .clickable { onNavigateToMessages() }
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(AppPurpleContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.ChatBubble,
                contentDescription = null,
                tint = AppPurple,
                modifier = Modifier.size(16.dp)
              )
            }
            Text(
              text = "5",
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = AppTextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Nowe\nwiadomości",
            fontSize = 12.sp,
            color = AppTextSecondary,
            lineHeight = 16.sp
          )
        }
      }

      // 3. Podejrzany kontakt (1)
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(18.dp))
          .clickable { onLookupNumber("+48 732 123 456") }
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(AppRedContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AppRed,
                modifier = Modifier.size(18.dp)
              )
            }
            Text(
              text = "1",
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = AppTextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Podejrzany\nkontakt",
            fontSize = 12.sp,
            color = AppTextSecondary,
            lineHeight = 16.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. OSTRZEŻENIE BEZPIECZEŃSTWA (Czerwona karta)
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppRedContainer),
      border = BorderStroke(1.dp, AppRedBorder),
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .clickable { onLookupNumber("+48 732 123 456") }
        .testTag("home_security_alert_card")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0xFF3E181F)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = AppRed,
            modifier = Modifier.size(26.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Ostrzeżenie bezpieczeństwa",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = AppRedText
            )
            Text(
              text = "Dzisiaj, 11:57",
              fontSize = 12.sp,
              color = AppTextSecondary
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "Podejrzane połączenie",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "+48 732 123 456 – możliwe oszustwo (vishing). 81% ryzyka.",
            fontSize = 13.sp,
            color = Color(0xFFD4B2B8)
          )
        }

        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = AppTextSecondary,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    // 5.5 SEKCJA 37: SZYBKIE KONTAKTY (Quick Contacts)
    val quickContacts = unifiedContacts.filter { it.quickContactSlot }
    if (quickContacts.isNotEmpty()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFC107),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Szybkie kontakty",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onNavigateToContacts() }
            .padding(4.dp)
        ) {
          Text(
            text = "Wszystkie kontakty",
            fontSize = 13.sp,
            color = AppPurpleLight,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.width(2.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppPurpleLight,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        quickContacts.take(3).forEach { qc ->
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
            border = BorderStroke(1.dp, AppBorder),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(18.dp))
              .clickable { onSelectContact(qc) }
              .testTag("home_quick_contact_${qc.id}")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(AppPurpleContainer),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = qc.avatarInitials,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = AppPurpleLight
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = qc.primaryDisplayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppTextPrimary,
                maxLines = 1
              )

              Text(
                text = qc.personalPhoneNumbers.firstOrNull() ?: qc.knownPhoneNumbers.firstOrNull() ?: "",
                fontSize = 10.sp,
                color = AppTextSecondary,
                maxLines = 1
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                Surface(
                  shape = CircleShape,
                  color = AppGreen.copy(alpha = 0.15f),
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                      val num = qc.allPhoneNumbers.firstOrNull() ?: ""
                      if (num.isNotBlank()) {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                        context.startActivity(intent)
                      }
                    }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Phone, contentDescription = "Zadzwoń", tint = AppGreen, modifier = Modifier.size(16.dp))
                  }
                }

                Surface(
                  shape = CircleShape,
                  color = AppPurple.copy(alpha = 0.15f),
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                      val addr = qc.allPhoneNumbers.firstOrNull() ?: qc.primaryIdentifier
                      onOpenSmsThread(addr)
                    }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ChatBubble, contentDescription = "SMS", tint = AppPurpleLight, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }

    // 6. SEKCJA: OSTATNIA AKTYWNOŚĆ
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Ostatnia aktywność",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = AppTextPrimary
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { onNavigateToCalls() }
          .padding(4.dp)
      ) {
        Text(
          text = "Zobacz wszystkie",
          fontSize = 13.sp,
          color = AppTextSecondary
        )
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = AppTextSecondary,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Lista 3 aktywności
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        // Pozycja 1: +48 732 123 456 (Podejrzenie oszustwa)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onLookupNumber("+48 732 123 456") }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(AppRedContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = AppRed,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "+48 732 123 456",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = "Podejrzenie oszustwa",
              fontSize = 13.sp,
              color = AppRedText
            )
          }

          Text(
            text = "Dzisiaj, 11:57",
            fontSize = 12.sp,
            color = AppTextSecondary,
            modifier = Modifier.padding(end = 12.dp)
          )

          IconButton(
            onClick = { onNavigateToCalls() },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Phone,
              contentDescription = "Zadzwoń",
              tint = AppTextPrimary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        HorizontalDivider(
          thickness = 1.dp,
          color = Color(0xFF242136),
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Pozycja 2: DPD Polska (Wiadomość SMS)
        val dpdContact = unifiedContacts.find { it.id == "org-dpd" || it.primaryDisplayName.contains("DPD", ignoreCase = true) }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (dpdContact != null) {
                onSelectContact(dpdContact)
              } else {
                onNavigateToMessages()
              }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
              imageVector = Icons.Default.ChatBubble,
              contentDescription = null,
              tint = AppPurple,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "DPD Polska",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = "Wiadomość SMS",
              fontSize = 13.sp,
              color = AppTextSecondary
            )
          }

          Text(
            text = "Dzisiaj, 09:31",
            fontSize = 12.sp,
            color = AppTextSecondary
          )

          Spacer(modifier = Modifier.width(6.dp))

          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppTextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }

        HorizontalDivider(
          thickness = 1.dp,
          color = Color(0xFF242136),
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Pozycja 3: Mama (Połączenie przychodzące)
        val mamaContact = unifiedContacts.find { it.id == "person-mama" || it.primaryDisplayName.contains("Mama", ignoreCase = true) }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (mamaContact != null) {
                onSelectContact(mamaContact)
              } else {
                onNavigateToCalls()
              }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(AppGreenContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Phone,
              contentDescription = null,
              tint = AppGreen,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Mama",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = "Połączenie przychodzące",
              fontSize = 13.sp,
              color = AppTextSecondary
            )
          }

          Text(
            text = "Wczoraj, 18:20",
            fontSize = 12.sp,
            color = AppTextSecondary
          )

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

    Spacer(modifier = Modifier.height(20.dp))

    // 7. HERO BANNER: Spokój w Twoich rękach (z minimalistycznym pejzażem górskim)
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1730)),
      border = BorderStroke(1.dp, Color(0xFF352B58)),
      modifier = Modifier
        .fillMaxWidth()
        .height(84.dp)
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        // Górski pejzaż wektorowy po prawej stronie
        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height

          // Warstwa tylna gór (ciemny fiolet)
          val backPath = Path().apply {
            moveTo(w * 0.55f, h)
            lineTo(w * 0.68f, h * 0.35f)
            lineTo(w * 0.78f, h * 0.55f)
            lineTo(w * 0.88f, h * 0.25f)
            lineTo(w, h * 0.65f)
            lineTo(w, h)
            close()
          }
          drawPath(backPath, color = Color(0xFF131024).copy(alpha = 0.8f))

          // Warstwa przednia gór (sylwetka)
          val frontPath = Path().apply {
            moveTo(w * 0.62f, h)
            lineTo(w * 0.75f, h * 0.45f)
            lineTo(w * 0.83f, h * 0.65f)
            lineTo(w * 0.93f, h * 0.38f)
            lineTo(w, h * 0.52f)
            lineTo(w, h)
            close()
          }
          drawPath(frontPath, color = Color(0xFF0C0A18))
        }

        // Treść baneru
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(listOf(Color(0xFF5B45A8), Color(0xFF2A1C54)))
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = Color(0xFFD4C8FF),
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = "Spokój w Twoich rękach",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Rozmawiaj. Pisz. Bądź bezpieczny.",
              fontSize = 12.sp,
              color = AppTextSecondary
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
