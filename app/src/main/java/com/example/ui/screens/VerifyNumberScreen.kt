package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallerInfo
import com.example.model.CommunityReputation
import com.example.model.ConfidenceLevel
import com.example.model.QuickRating
import com.example.model.SubCategory
import com.example.model.UserRatingSubmission
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppGreenContainer
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppRed
import com.example.ui.theme.AppRedBorder
import com.example.ui.theme.AppRedContainer
import com.example.ui.theme.AppRedText
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Ekran "Sprawdź numer" dopasowany 1:1 do załączonej makiety (Image 3)
 * zintegrowany z silnikiem crowdsourcingu i dynamicznej reputacji (SEKCJE 19–31).
 */
@Composable
fun VerifyNumberScreen(
  initialNumber: String = "+48 732 123 456",
  callerInfo: CallerInfo? = null,
  communityReputation: CommunityReputation? = null,
  userRating: UserRatingSubmission? = null,
  onRateNumber: (QuickRating, SubCategory?) -> Unit = { _, _ -> },
  onBack: () -> Unit = {},
  onSelectNumber: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var searchInput by remember(initialNumber) { mutableStateOf(initialNumber) }
  var selectedChip by remember(initialNumber) { mutableStateOf(initialNumber) }
  var showRatingSubCategories by remember { mutableStateOf<QuickRating?>(null) }

  val displayCaller = callerInfo ?: CallerInfo(
    phoneNumber = selectedChip,
    formattedNumber = selectedChip,
    displayName = if (selectedChip.contains("521")) "Infolinia PKO BP" else if (selectedChip.contains("577")) "Kurier DPD" else null,
    riskScore = if (selectedChip.contains("732")) 81 else if (selectedChip.contains("521")) 32 else 5,
    confidence = if (selectedChip.contains("732")) ConfidenceLevel.WYSOKA else ConfidenceLevel.SREDNIA,
    category = if (selectedChip.contains("732")) "Podejrzenie oszustwa (vishing)" else if (selectedChip.contains("521")) "Infolinia bankowa (ryzyko podszycia)" else "Kurier",
    totalReports = if (selectedChip.contains("732")) 32 else if (selectedChip.contains("521")) 18 else 25,
    reportsLast24h = if (selectedChip.contains("732")) 18 else if (selectedChip.contains("521")) 4 else 1,
    firstSeen = "12 maj 2024",
    lastSeen = "Teraz",
    riskReasonExplanation = if (selectedChip.contains("732")) "Zgłoszenia użytkowników o wyłudzeniach kodów BLIK i próbach kradzieży tożsamości" else "Zweryfikowany podmiot w oficjalnym katalogu",
    isVerifiedCallerIdentityPossible = selectedChip.contains("521")
  )

  val rep = communityReputation ?: displayCaller.communityReputation ?: CommunityReputation(
    phoneNumber = displayCaller.phoneNumber,
    positiveScorePercent = if (displayCaller.riskScore > 50) 6 else if (displayCaller.riskScore > 20) 78 else 96,
    negativeScorePercent = if (displayCaller.riskScore > 50) 94 else if (displayCaller.riskScore > 20) 22 else 4,
    totalRatingsCount = displayCaller.totalReports,
    ratingsLast7Days = displayCaller.reportsLast24h,
    ratingsLast30Days = displayCaller.totalReports,
    freshnessSummary = "Świeża aktywność (ostatnie 7 dni)",
    dominantRating = if (displayCaller.riskScore > 50) QuickRating.SCAM else QuickRating.POSITIVE,
    dominantSubCategory = if (displayCaller.riskScore > 50) SubCategory.BLIK_PHISHING else null,
    positiveCount = if (displayCaller.riskScore > 50) 2 else if (displayCaller.riskScore > 20) 14 else 24,
    nuisanceCount = if (displayCaller.riskScore > 50) 4 else 0,
    scamCount = if (displayCaller.riskScore > 50) 26 else if (displayCaller.riskScore > 20) 4 else 0,
    reputationLabel = if (displayCaller.riskScore > 50) "Podejrzenie oszustwa" else if (displayCaller.riskScore > 20) "Mieszana (spoofing)" else "Bezpieczny",
    riskContextNote = "Zgłoszenia społeczności zweryfikowane czasowo"
  )

  val recentQueryChips = listOf(
    "+48 732 123 456",
    "+48 22 521 80 00",
    "+48 22 577 55 55"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(AppDarkBackground)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 18.dp, vertical = 12.dp)
  ) {

    // 1. TOP BAR (<- Sprawdź numer)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Wstecz",
          tint = Color(0xFFD4C8FF),
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Sprawdź numer",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary
        )
        Text(
          text = "Dowiedz się, kto do Ciebie dzwoni",
          fontSize = 13.sp,
          color = AppTextSecondary
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. SEARCH INPUT (Pill with +48 732 123 456 and X)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
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
          tint = AppPurple,
          modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(modifier = Modifier.weight(1f)) {
          BasicTextField(
            value = searchInput,
            onValueChange = {
              searchInput = it
              selectedChip = it
            },
            singleLine = true,
            textStyle = TextStyle(
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold,
              color = AppTextPrimary
            ),
            cursorBrush = SolidColor(AppPurple),
            modifier = Modifier.fillMaxWidth()
          )
        }

        if (searchInput.isNotEmpty()) {
          IconButton(
            onClick = { searchInput = "" },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Wyczyść",
              tint = AppTextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 3. RECENT QUERY PILLS (+48 732 123 456, +48 22 521 80 00, +48 22 577 55 55)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      recentQueryChips.forEach { chipNumber ->
        val isSelected = selectedChip == chipNumber
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) Color(0xFF281E48) else AppSurfaceVariant)
            .clickable {
              selectedChip = chipNumber
              searchInput = chipNumber
              onSelectNumber(chipNumber)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = chipNumber,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFD4C8FF) else AppTextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. KARTA GŁÓWNA WYNIKU: 81% ryzyka (Czerwona karta)
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppRedContainer),
      border = BorderStroke(1.dp, AppRedBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .background(Color(0xFF3F1820)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = AppRed,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "+48 732 123 456",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "81% ryzyka",
              fontSize = 24.sp,
              fontWeight = FontWeight.ExtraBold,
              color = AppRedText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Podejrzenie oszustwa",
              fontSize = 13.sp,
              color = AppTextPrimary
            )
          }

          // Badge: Pewność danych: Wysoka
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF261A28))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = buildAnnotatedString {
                append("Pewność danych: ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFD4C8FF))) {
                  append("Wysoka")
                }
              },
              fontSize = 11.sp,
              color = AppTextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pasek ryzyka (gradientowy, 81%)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(Color(0xFF3B1A24))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(0.81f)
              .height(10.dp)
              .clip(CircleShape)
              .background(
                Brush.horizontalGradient(
                  listOf(Color(0xFFE53935), Color(0xFFFF6B6B))
                )
              )
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Etykiety pod paskiem (0% Niskie, 50%, 100% Wysokie)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "0% (Niskie)",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
          Text(
            text = "50%",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
          Text(
            text = "100% (Wysokie)",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 5. KARTA KATEGORII: Podejrzenie oszustwa (vishing)
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFF381A22)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Phone,
            contentDescription = null,
            tint = AppRedText,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Kategoria połączenia",
            fontSize = 11.sp,
            color = AppTextSecondary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Podejrzenie oszustwa (vishing)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Fałszywy doradca • Wyłudzenie kodu BLIK • Zdalny pulpit",
            fontSize = 12.sp,
            color = AppTextSecondary
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

    Spacer(modifier = Modifier.height(14.dp))

    // 6. DLACZEGO WIDZISZ TEN WYNIK?
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A34)),
      border = BorderStroke(1.dp, Color(0xFF332958)),
      modifier = Modifier.fillMaxWidth()
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
            .background(Color(0xFF322758)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFFD4C8FF),
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Dlaczego widzisz ten wynik?",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Numer został wielokrotnie zgłoszony przez użytkowników jako próba oszustwa. Zgłoszenia wskazują na podszywanie się pod doradcę banku i próbę wyłudzenia kodu BLIK.",
            fontSize = 12.sp,
            color = Color(0xFFBBB6D8),
            lineHeight = 17.sp
          )
        }

        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = AppTextSecondary,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 7. STATYSTYKI (32 wszystkie zgłoszenia, 18 w ostatnich 24h)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier.weight(1f)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(AppPurpleContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Group,
              contentDescription = null,
              tint = AppPurple,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "32",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = "wszystkie zgłoszenia",
              fontSize = 11.sp,
              color = AppTextSecondary
            )
          }
        }
      }

      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier.weight(1f)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(AppPurpleContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = AppPurple,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "18",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = "w ostatnich 24h",
              fontSize = 11.sp,
              color = AppTextSecondary
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 7b. REPUTACJA SPOŁECZNOŚCI (SEKCJE 19–31: Crowdsourcing, Time Decay, Anti-Abuse)
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AppPurpleContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = AppPurple,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Reputacja społeczności",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppTextPrimary
              )
              Text(
                text = "Dynamiczny crowdsourcing ocen",
                fontSize = 12.sp,
                color = AppTextSecondary
              )
            }
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF261A28))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = rep.freshnessSummary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFFD4C8FF)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pasek proporcji ocen (W porządku / Natrętny / Oszustwo)
        val posWeight = (rep.positivePercent / 100f).coerceAtLeast(0.02f)
        val nuisWeight = (rep.nuisancePercent / 100f).coerceAtLeast(0.02f)
        val scamWeight = (rep.scamPercent / 100f).coerceAtLeast(0.02f)

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF232034))
        ) {
          Row(modifier = Modifier.fillMaxSize()) {
            if (rep.positivePercent > 0) {
              Box(
                modifier = Modifier
                  .weight(posWeight)
                  .fillMaxSize()
                  .background(AppGreen)
              )
            }
            if (rep.nuisancePercent > 0) {
              Box(
                modifier = Modifier
                  .weight(nuisWeight)
                  .fillMaxSize()
                  .background(Color(0xFFFBC02D))
              )
            }
            if (rep.scamPercent > 0) {
              Box(
                modifier = Modifier
                  .weight(scamWeight)
                  .fillMaxSize()
                  .background(AppRed)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legendy pod paskiem
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "👍 ${rep.positivePercent}% w porządku",
            fontSize = 12.sp,
            color = AppGreen,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "😒 ${rep.nuisancePercent}% natrętny",
            fontSize = 12.sp,
            color = Color(0xFFFBC02D),
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "⚠️ ${rep.scamPercent}% oszustwo",
            fontSize = 12.sp,
            color = AppRedText,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Transparentne zasady silnika (Time decay i Anti-bombing)
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1B182B)),
          border = BorderStroke(1.dp, Color(0xFF2E274A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.HourglassBottom,
                contentDescription = null,
                tint = Color(0xFFD4C8FF),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Wygaszanie w czasie (Time Decay)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD4C8FF)
              )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Świeże oceny (1–30 dni) mają pełną wagę. Starsze wygasają, dzięki czemu baza nie blokuje numeru po zmianie abonenta.",
              fontSize = 11.sp,
              color = AppTextSecondary,
              lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AppGreen,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Ochrona przed review-bombingiem",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppGreen
              )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Wyższa waga ocen z potwierdzonym połączeniem (1.0x) vs brak interakcji (0.4x). Każdy użytkownik ma 1 aktywny głos.",
              fontSize = 11.sp,
              color = AppTextSecondary,
              lineHeight = 15.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sekcja szybkiej oceny dla użytkownika
        Text(
          text = if (userRating != null) "Twoja zarejestrowana ocena" else "Jak oceniasz ten numer?",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary
        )

        if (userRating != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                  when (userRating.rating) {
                    QuickRating.POSITIVE -> AppGreenContainer
                    QuickRating.NUISANCE -> Color(0xFF3E3218)
                    QuickRating.SCAM -> AppRedContainer
                  }
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = "${userRating.rating.emoji} ${userRating.rating.displayName}${userRating.subCategory?.let { " • ${it.displayName}" } ?: ""}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when (userRating.rating) {
                  QuickRating.POSITIVE -> AppGreen
                  QuickRating.NUISANCE -> Color(0xFFFBC02D)
                  QuickRating.SCAM -> AppRedText
                }
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Kliknij poniżej, aby zmienić",
              fontSize = 11.sp,
              color = AppTextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3 duże przyciski szybkiej oceny
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // 1. W porządku
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14241B)),
            border = BorderStroke(1.dp, Color(0xFF1E482D)),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                showRatingSubCategories = null
                onRateNumber(QuickRating.POSITIVE, null)
              }
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(text = "👍", fontSize = 20.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "W porządku",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppGreen
              )
            }
          }

          // 2. Natrętny
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2312)),
            border = BorderStroke(1.dp, Color(0xFF4D3E1A)),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                showRatingSubCategories = QuickRating.NUISANCE
              }
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(text = "😒", fontSize = 20.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Natrętny",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFBC02D)
              )
            }
          }

          // 3. Oszustwo
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E151A)),
            border = BorderStroke(1.dp, Color(0xFF56222C)),
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                showRatingSubCategories = QuickRating.SCAM
              }
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(text = "⚠️", fontSize = 20.sp)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Oszustwo",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppRedText
              )
            }
          }
        }

        // Opcjonalne podkategorie (jeśli wybrano Nuisance lub Scam)
        if (showRatingSubCategories != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Doprecyzuj powód (opcjonalnie):",
            fontSize = 12.sp,
            color = AppTextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val subCats = if (showRatingSubCategories == QuickRating.SCAM) {
              listOf(SubCategory.BLIK_PHISHING, SubCategory.BANK_IMPERSONATION, SubCategory.CRYPTO_INVESTMENT, SubCategory.OTHER_SCAM)
            } else {
              listOf(SubCategory.SALES_OFFER, SubCategory.ROBOCALL_BOT, SubCategory.SURVEY, SubCategory.OTHER_NUISANCE)
            }

            subCats.forEach { sub ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF221D38))
                  .clickable {
                    onRateNumber(showRatingSubCategories!!, sub)
                    showRatingSubCategories = null
                  }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = sub.displayName,
                  fontSize = 11.sp,
                  color = Color(0xFFD4C8FF)
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 8. OSTATNIA AKTYWNOŚĆ (Timeline łączony)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.History,
          contentDescription = null,
          tint = AppPurple,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Ostatnia aktywność",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary
        )
      }

      Text(
        text = "Zobacz wszystkie >",
        fontSize = 13.sp,
        color = AppTextSecondary
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Timeline Items z pionową linią
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {

        // Punkt 1: Zgłoszenie jako oszustwo
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
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

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Zgłoszenie jako oszustwo",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = "Fałszywy doradca banku",
              fontSize = 12.sp,
              color = AppTextSecondary
            )
          }

          Text(
            text = "Dzisiaj, 11:57",
            fontSize = 12.sp,
            color = AppTextSecondary
          )
        }

        // Pionowa kreska łącząca
        Box(
          modifier = Modifier
            .padding(start = 18.dp)
            .width(2.dp)
            .height(20.dp)
            .background(Color(0xFF38344E))
        )

        // Punkt 2: Wiadomość SMS
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
              imageVector = Icons.Default.ChatBubble,
              contentDescription = null,
              tint = AppPurple,
              modifier = Modifier.size(16.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Wiadomość SMS",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = "Podejrzana treść",
              fontSize = 12.sp,
              color = AppTextSecondary
            )
          }

          Text(
            text = "Wczoraj, 18:20",
            fontSize = 12.sp,
            color = AppTextSecondary
          )
        }

        // Pionowa kreska łącząca
        Box(
          modifier = Modifier
            .padding(start = 18.dp)
            .width(2.dp)
            .height(20.dp)
            .background(Color(0xFF38344E))
        )

        // Punkt 3: Połączenie przychodzące
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(AppGreenContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Phone,
              contentDescription = null,
              tint = AppGreen,
              modifier = Modifier.size(16.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Połączenie przychodzące",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = AppTextPrimary
            )
            Text(
              text = "Brak odpowiedzi",
              fontSize = 12.sp,
              color = AppTextSecondary
            )
          }

          Text(
            text = "Wczoraj, 16:03",
            fontSize = 12.sp,
            color = AppTextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
  }
}
