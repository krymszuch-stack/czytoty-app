package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.UserProfile
import com.example.ui.components.ReputationIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  profile: UserProfile,
  onOpenOnboarding: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var activeDialogTitle by remember { mutableStateOf<String?>(null) }
  var activeDialogContent by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Profil i ustawienia",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      contentPadding = PaddingValues(
        top = paddingValues.calculateTopPadding() + 8.dp,
        bottom = paddingValues.calculateBottomPadding() + 24.dp,
        start = 16.dp,
        end = 16.dp
      ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      // User Profile & Reputation Header
      item {
        ReputationIndicator(profile = profile)
      }

      // Settings section header
      item {
        Text(
          text = "Konfiguracja i zasady",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
      }

      // Settings Card: Architektura, Prywatność, Onboarding, Wygląd, Pomoc
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            ProfileSettingRow(
              icon = Icons.Default.Lock,
              title = "Prywatność (Privacy-by-Design)",
              subtitle = "Książka telefoniczna nie opuszcza telefonu",
              onClick = {
                activeDialogTitle = "Prywatność i ochrona danych"
                activeDialogContent = "Twoje kontakty prywatne nigdy nie są przesyłane do serwera ani udostępniane publicznie.\n\nZapytania o nieznane numery są realizowane lokalnie w oparciu o bazę pamięci podręcznej (Room) lub anonimizowane skróty numerów. Zgłoszenia użytkowników nie zawierają danych osobowych zgłaszającego."
              },
              testTag = "setting_privacy"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ProfileSettingRow(
              icon = Icons.Default.Security,
              title = "Zasada Zero Trust dla banków",
              subtitle = "Brak bezwzględnego zaufania do Caller ID",
              onClick = {
                activeDialogTitle = "Zasada Zero Trust dla banków"
                activeDialogContent = "Architektura sieci telefonicznej GSM pozwala dzwoniącemu na fałszowanie identyfikatora rozmówcy (CLI spoofing).\n\nDlatego w naszej aplikacji połączenie z oficjalnego numeru banku NIE jest oznaczane jako 'bezpieczne'. Aplikacja zawsze ostrzega, że dzwoniący może podszywać się pod konsultanta."
              },
              testTag = "setting_zero_trust"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ProfileSettingRow(
              icon = Icons.Default.Info,
              title = "Zasady i architektura (Onboarding)",
              subtitle = "Filozofia Risk vs Confidence, transparentne uzasadnienia",
              onClick = {
                onOpenOnboarding?.invoke()
              },
              testTag = "setting_onboarding_guide"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ProfileSettingRow(
              icon = Icons.Default.Notifications,
              title = "Powiadomienia i screening",
              subtitle = "Działanie w tle poprzez Android CallScreeningService",
              onClick = {
                activeDialogTitle = "Filtrowanie połączeń (Call Screening)"
                activeDialogContent = "Aplikacja integruje się z systemową usługą Android CallScreeningService, co umożliwia natychmiastową weryfikację przychodzącego połączenia jeszcze przed odebraniem."
              },
              testTag = "setting_notifications"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ProfileSettingRow(
              icon = Icons.Default.ColorLens,
              title = "Wygląd (Material You)",
              subtitle = "Dynamiczne kolory M3 i pełna zgodność z trybem ciemnym",
              onClick = {
                activeDialogTitle = "Interfejs Material Design 3"
                activeDialogContent = "Interfejs został zaprojektowany w stylistyce Google Phone / Material 3. Zapewnia to spokój wizualny, dużą czytelność i brak krzykliwych elementów wywołujących panikę."
              },
              testTag = "setting_appearance"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            ProfileSettingRow(
              icon = Icons.AutoMirrored.Filled.Help,
              title = "Edukacja antyfraudowa",
              subtitle = "Zasady ostrożności w rozmowach telefonicznych",
              onClick = {
                activeDialogTitle = "Zasady bezpieczeństwa"
                activeDialogContent = "1. Żaden prawdziwy pracownik banku ani policjant nigdy nie prosi o kod BLIK ani hasło do bankowości.\n2. Nigdy nie instaluj oprogramowania 'pomocniczego' (np. AnyDesk, QuickSupport) na prośbę dzwoniącego.\n3. W razie wątpliwości rozłącz się i samodzielnie wybierz numer infolinii banku."
              },
              testTag = "setting_help"
            )
          }
        }
      }

      // App version label - Honest, no hype
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Ochrona połączeń • Wersja MVP",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Lokalna architektura offline-first • Android CallScreeningService",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
          )
        }
      }
    }
  }

  // Generic Dialog
  if (activeDialogTitle != null && activeDialogContent != null) {
    AlertDialog(
      onDismissRequest = {
        activeDialogTitle = null
        activeDialogContent = null
      },
      title = {
        Text(
          text = activeDialogTitle ?: "",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Text(
          text = activeDialogContent ?: "",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            activeDialogTitle = null
            activeDialogContent = null
          }
        ) {
          Text("Rozumiem")
        }
      }
    )
  }
}

@Composable
private fun ProfileSettingRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.outline,
      modifier = Modifier.size(14.dp)
    )
  }
}
