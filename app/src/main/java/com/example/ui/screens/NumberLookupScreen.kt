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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallerInfo
import com.example.ui.components.CampaignActivityChart
import com.example.ui.components.RiskBar
import com.example.ui.components.VerifiedBusinessBadge
import com.example.ui.components.ZeroTrustBankBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberLookupScreen(
  searchQuery: String,
  searchResult: CallerInfo?,
  onQueryChange: (String) -> Unit,
  onSearch: (String) -> Unit,
  onBlockToggle: (String) -> Unit,
  onReportClick: (CallerInfo) -> Unit,
  onShareClick: (CallerInfo) -> Unit,
  onSelectSample: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val sampleNumbers = listOf(
    "+48 732 123 456",
    "+48 22 521 80 00",
    "+48 22 577 55 55",
    "+48 22 307 88 12"
  )

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Sprawdź numer",
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
      // 1. Pole wyszukiwania
      item {
        Column {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_number_input"),
            placeholder = { Text("Wpisz numer telefonu do weryfikacji") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                  Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Wyczyść"
                  )
                }
              }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Phone,
              imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
              onSearch = { onSearch(searchQuery) }
            ),
            shape = RoundedCornerShape(20.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Szybkie próbki do testów
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(sampleNumbers) { num ->
              SuggestionChip(
                onClick = { onSelectSample(num) },
                label = { Text(num, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("sample_chip_$num")
              )
            }
          }
        }
      }

      // 2. Wynik wyszukiwania
      if (searchResult != null) {
        val result = searchResult

        // Karta główna tożsamości i wskaźnika ryzyka
        item {
          Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("lookup_result_card")
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              if (result.displayName != null) {
                Text(
                  text = result.displayName,
                  style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = result.formattedNumber,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              } else {
                Text(
                  text = result.formattedNumber,
                  style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              if (result.isOfficialDirectoryNumber && !result.isFinancialInstitution) {
                Spacer(modifier = Modifier.height(8.dp))
                VerifiedBusinessBadge(
                  label = "Numer w rejestrze: ${result.numberIdentity?.registeredEntityName ?: ""}",
                  orgName = result.numberIdentity?.registeredEntityName
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // RiskBar: Risk Score + Confidence + tekstowa severity
              RiskBar(
                score = result.riskScore,
                confidence = result.confidence,
                showLabel = true
              )
            }
          }
        }

        // Zero Trust Banner dla banków
        if (result.isFinancialInstitution) {
          item {
            ZeroTrustBankBanner(caller = result)
          }
        }

        // Szczegóły: Kategoria, świeżość, powód
        item {
          Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text(
                text = "Kategoria połączenia:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = result.category,
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )

              if (result.subCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = result.subCategories.joinToString(" • "),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.outline
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Uzasadnienie deterministyczne: „Dlaczego?”
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.HelpOutline,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Transparentne uzasadnienie:",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = result.riskReasonExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Metryki wolumenu i świeżości
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                      text = "${result.totalReports}",
                      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "wszystkie zgłoszenia",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = MaterialTheme.colorScheme.surface,
                  modifier = Modifier.weight(1f)
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                      text = "${result.reportsLast24h}",
                      style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (result.reportsLast24h > 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                      )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "w ostatnich 24h",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }

        // Wykres aktywności w czasie
        item {
          CampaignActivityChart(
            data = result.activityTrend,
            riskScore = result.riskScore
          )
        }

        // Ważne zastrzeżenie prawne i techniczne
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                  .size(18.dp)
                  .padding(top = 2.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Zastrzeżenie: Identyfikator Caller ID w sieci GSM może zostać sfałszowany (spoofing). Brak zarejestrowanych sygnałów ryzyka nie stanowi gwarancji, że połączenie jest w 100% bezpieczne.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Przyciski akcji: Zablokuj, Zgłoś, Udostępnij
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { onBlockToggle(result.phoneNumber) },
              colors = if (result.isBlocked) {
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
              } else {
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError
                )
              },
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("action_block_button")
            ) {
              Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (result.isBlocked) "Odblokuj" else "Zablokuj",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }

            FilledTonalButton(
              onClick = { onReportClick(result) },
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("action_report_button")
            ) {
              Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Zgłoś",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }

            OutlinedButton(
              onClick = { onShareClick(result) },
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("action_share_button")
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Udostępnij",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }
    }
  }
}
