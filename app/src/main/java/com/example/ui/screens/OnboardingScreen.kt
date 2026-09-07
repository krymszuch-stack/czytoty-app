package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConfidenceLevel
import com.example.ui.components.RiskBar
import com.example.ui.theme.RiskSafe
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekScamSurface

enum class OnboardingStep(
  val stepIndex: Int,
  val stepTag: String,
  val title: String,
  val subtitle: String
) {
  WELCOME(
    0,
    "step_welcome",
    "CzyToTy? – Wiesz, zanim odbierzesz",
    "Spokój zaczyna się od informacji. Telefon, Wiadomości i Ochrona zintegrowane w jednym przejrzystym systemie."
  ),
  ZERO_TRUST(
    1,
    "step_zero_trust",
    "Zasada Zero Trust dla banków",
    "Dlaczego znany numer banku nie jest oznaczany jako bezpieczny? Ponieważ identyfikator dzwoniącego może zostać sfałszowany."
  ),
  RISK_SCORING(
    2,
    "step_risk_scoring",
    "Ocena ryzyka i poziom pewności",
    "Zawsze prezentujemy Risk Score, Confidence oraz transparentne uzasadnienie: dlaczego widzisz ten wynik."
  ),
  LOCAL_ENGINE(
    3,
    "step_local_engine",
    "Działanie lokalne i screening",
    "Lokalna pamięć podręczna Room oraz integracja z Android CallScreeningService. Twoje kontakty nie opuszczają urządzenia."
  )
}

@Composable
fun OnboardingScreen(
  onComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var currentStepIndex by remember { mutableIntStateOf(0) }
  val totalSteps = OnboardingStep.values().size
  val currentStep = OnboardingStep.values()[currentStepIndex]

  Scaffold(
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (currentStepIndex > 0) {
          IconButton(
            onClick = { currentStepIndex-- },
            modifier = Modifier.testTag("onboarding_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Wstecz",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        } else {
          Spacer(modifier = Modifier.size(48.dp))
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.testTag("onboarding_dots_indicator")
        ) {
          repeat(totalSteps) { index ->
            val isSelected = index == currentStepIndex
            Box(
              modifier = Modifier
                .height(8.dp)
                .width(if (isSelected) 24.dp else 8.dp)
                .clip(CircleShape)
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant
                )
            )
          }
        }

        TextButton(
          onClick = onComplete,
          modifier = Modifier.testTag("onboarding_skip_button")
        ) {
          Text(
            text = if (currentStepIndex == totalSteps - 1) "Gotowe" else "Pomiń",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          )
        }
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (currentStepIndex > 0) {
            OutlinedButton(
              onClick = { currentStepIndex-- },
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("onboarding_prev_button")
            ) {
              Text(
                text = "Wstecz",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
              )
            }
          }

          Button(
            onClick = {
              if (currentStepIndex < totalSteps - 1) {
                currentStepIndex++
              } else {
                onComplete()
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
              .weight(if (currentStepIndex > 0) 1.5f else 1f)
              .height(52.dp)
              .testTag("onboarding_next_button")
          ) {
            Text(
              text = if (currentStepIndex == totalSteps - 1) "Zacznij korzystać" else "Dalej",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
              )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = if (currentStepIndex == totalSteps - 1) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
          if (targetState.stepIndex > initialState.stepIndex) {
            (slideInHorizontally { it } + fadeIn()) togetherWith
                (slideOutHorizontally { -it } + fadeOut())
          } else {
            (slideInHorizontally { -it } + fadeIn()) togetherWith
                (slideOutHorizontally { it } + fadeOut())
          }
        },
        label = "OnboardingStepAnimation"
      ) { step ->
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag(step.stepTag),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 24.sp,
              lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = step.subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = 15.sp,
              lineHeight = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(28.dp))

          when (step) {
            OnboardingStep.WELCOME -> WelcomeStepContent()
            OnboardingStep.ZERO_TRUST -> ZeroTrustStepContent()
            OnboardingStep.RISK_SCORING -> RiskScoringStepContent()
            OnboardingStep.LOCAL_ENGINE -> LocalEngineStepContent()
          }

          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

@Composable
private fun WelcomeStepContent() {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    ),
    border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.4f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("onboarding_welcome_card")
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      com.example.ui.components.CzyToTyLogo(
        size = 80.dp,
        variant = com.example.ui.components.LogoVariant.DARK
      )

      Spacer(modifier = Modifier.height(18.dp))

      Text(
        text = "Filozofia Privacy-by-Design",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Produkt NIE jest systemem gwarantującym 100% ochrony. Jest systemem reputacji połączeń, pomagającym ocenić ryzyko.",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(20.dp))

      FeaturePillRow(
        icon = Icons.Default.Security,
        title = "Prywatność kontaktów",
        subtitle = "Twoja książka telefoniczna nigdy nie jest wysyłana na serwer"
      )

      Spacer(modifier = Modifier.height(10.dp))

      FeaturePillRow(
        icon = Icons.Default.CheckCircle,
        title = "Przejrzysta reguła bezpieczeństwa",
        subtitle = "Brak zarejestrowanych sygnałów ryzyka != numer bezpieczny"
      )

      Spacer(modifier = Modifier.height(10.dp))

      FeaturePillRow(
        icon = Icons.Default.Business,
        title = "Rejestr podmiotów",
        subtitle = "Potwierdza istnienie numeru w rejestrze, nie tożsamość dzwoniącego"
      )
    }
  }
}

@Composable
private fun ZeroTrustStepContent() {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "+48 22 521 80 00",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Oficjalna infolinia banku (PKO BP)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Uwaga: Ryzyko podszycia (Spoofing CLI)",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Oszust może nadać dowolny numer dzwoniącego w sieci GSM. Prawdziwy bank nigdy nie żąda kodu BLIK ani instalacji aplikacji.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Zasada Zero Trust: Aplikacja chroni przed fałszywym poczuciem bezpieczeństwa, wyraźnie ostrzegając przy każdym połączeniu udającym bank.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun RiskScoringStepContent() {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(
        containerColor = SleekScamSurface
      ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.errorContainer)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "+48 732 123 456",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Wyłudzenie danych (Scam / BLIK)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        RiskBar(
          score = 92,
          confidence = ConfidenceLevel.WYSOKA,
          showLabel = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Dlaczego? 34 zgłoszenia w ostatnich 24h, w tym 12 w ostatniej godzinie (kampania aktywna).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Nigdy nie komunikujemy ryzyka samym kolorem. Zawsze podajemy Risk Score (%), poziom pewności dowodów (Confidence) oraz słowny opis zagrożenia.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun LocalEngineStepContent() {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    NavTourItem(
      icon = Icons.Default.Shield,
      name = "1. Android CallScreeningService",
      desc = "Oficjalny systemowy interfejs Androida do filtrowania połączeń. Pozwala podjąć decyzję w ułamku sekundy bez opóźnień."
    )

    NavTourItem(
      icon = Icons.Default.Security,
      name = "2. Baza lokalna Room (Offline-First)",
      desc = "Kluczowe dane i reputacje najczęstszych numerów przechowywane są w pamięci SQLite na urządzeniu."
    )

    NavTourItem(
      icon = Icons.Default.Search,
      name = "3. Sprawdź numer",
      desc = "Ręczna weryfikacja dowolnego numeru, historia zgłoszeń, dynamika kampanii i ostrzeżenie o spoofingu."
    )

    NavTourItem(
      icon = Icons.Default.Person,
      name = "4. Anonimowe zgłoszenia",
      desc = "Zgłaszaj podejrzane połączenia do lokalnej bazy bez rejestracji i bez przekazywania tożsamości."
    )
  }
}

@Composable
private fun FeaturePillRow(
  icon: ImageVector,
  title: String,
  subtitle: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun NavTourItem(
  icon: ImageVector,
  name: String,
  desc: String
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = name,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = desc,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
