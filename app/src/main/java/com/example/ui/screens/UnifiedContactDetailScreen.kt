package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContactEntityType
import com.example.model.OrganizationCategory
import com.example.model.UnifiedContact
import com.example.ui.components.RiskBadge
import com.example.ui.components.RiskBar
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppGreenContainer
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurpleLight
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Zunifikowany profil kontaktu (Unified Contact Profile - Sekcja 34)
 * Integruje tożsamość, numery, SMS Sender IDs, reputację, połączenia, SMS, OTP i domenę.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedContactDetailScreen(
  contact: UnifiedContact,
  onBack: () -> Unit,
  onTogglePin: (String) -> Unit,
  onToggleQuickContact: (String) -> Unit,
  onOpenSmsThread: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = if (contact.entityType == ContactEntityType.ORGANIZATION) "Profil organizacji" else "Profil kontaktu",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppTextPrimary
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("contact_detail_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Wstecz",
              tint = AppTextPrimary
            )
          }
        },
        actions = {
          // Przypinanie
          IconButton(onClick = { onTogglePin(contact.id) }) {
            Icon(
              imageVector = if (contact.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
              contentDescription = "Przypnij kontakt",
              tint = if (contact.isPinned) AppPurpleLight else AppTextSecondary
            )
          }

          // Szybki kontakt
          IconButton(onClick = { onToggleQuickContact(contact.id) }) {
            Icon(
              imageVector = if (contact.quickContactSlot) Icons.Default.Star else Icons.Outlined.StarBorder,
              contentDescription = "Szybki kontakt",
              tint = if (contact.quickContactSlot) Color(0xFFFFC107) else AppTextSecondary
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = AppDarkBackground
        )
      )
    },
    containerColor = AppDarkBackground,
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
      // 1. Karta tożsamości
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(
                if (contact.entityType == ContactEntityType.ORGANIZATION) Color(0xFF261D42)
                else Color(0xFF193225)
              ),
            contentAlignment = Alignment.Center
          ) {
            if (contact.entityType == ContactEntityType.ORGANIZATION) {
              Icon(
                imageVector = when (contact.organizationCategory) {
                  OrganizationCategory.COURIER_LOGISTICS -> Icons.Default.LocalShipping
                  OrganizationCategory.HEALTHCARE -> Icons.Default.MedicalServices
                  else -> Icons.Default.Business
                },
                contentDescription = null,
                tint = AppPurpleLight,
                modifier = Modifier.size(38.dp)
              )
            } else {
              Text(
                text = contact.avatarInitials,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppGreen
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = contact.primaryDisplayName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Podtytuł (kategoria lub relacja)
          Text(
            text = when {
              contact.relationship != null -> "Relacja: ${contact.relationship.label}"
              contact.organizationCategory != null -> contact.organizationCategory.displayName
              else -> if (contact.entityType == ContactEntityType.PERSON) "Osoba prywatna" else "Podmiot prawny"
            },
            fontSize = 13.sp,
            color = AppTextSecondary
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Etykiety stanu (Typ encji + Reputacja)
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (contact.entityType == ContactEntityType.ORGANIZATION)
                AppPurpleContainer
              else AppGreenContainer
            ) {
              Text(
                text = if (contact.entityType == ContactEntityType.ORGANIZATION) "ORGANIZACJA" else "OSOBA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (contact.entityType == ContactEntityType.ORGANIZATION) AppPurpleLight else AppGreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }

            RiskBadge(
              score = contact.riskScore,
              severity = contact.riskSeverity,
              confidence = contact.confidence,
              showScore = true,
              showConfidence = true
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 2. Główne przyciski komunikacji (Zadzwoń, SMS)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Button(
          onClick = {
            val num = contact.allPhoneNumbers.firstOrNull() ?: ""
            if (num.isNotBlank()) {
              val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
              context.startActivity(intent)
            }
          },
          enabled = contact.allPhoneNumbers.isNotEmpty(),
          modifier = Modifier
            .weight(1f)
            .height(50.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
        ) {
          Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Zadzwoń", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Button(
          onClick = {
            val addr = contact.allPhoneNumbers.firstOrNull() ?: contact.primaryIdentifier
            onOpenSmsThread(addr)
          },
          modifier = Modifier
            .weight(1f)
            .height(50.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AppPurple)
        ) {
          Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Wiadomość", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 3. SEKCJA DLA ORGANIZACJI (zgodnie z przykładem DPD Polska w sekcji 33)
      if (contact.entityType == ContactEntityType.ORGANIZATION) {
        Text(
          text = "DANE ORGANIZACJI",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = AppPurpleLight,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
          border = BorderStroke(1.dp, AppBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // SMS Sender IDs
            if (contact.smsSenderIds.isNotEmpty()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChatBubble, contentDescription = null, tint = AppPurpleLight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Oficjalne SMS Sender ID:", fontSize = 13.sp, color = AppTextSecondary)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                contact.smsSenderIds.forEach { senderId ->
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppPurpleContainer.copy(alpha = 0.5f)
                  ) {
                    Text(
                      text = senderId,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = AppPurpleLight,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
            }

            // Znane numery
            if (contact.knownPhoneNumbers.isNotEmpty()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = AppGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Znane numery / Infolinia:", fontSize = 13.sp, color = AppTextSecondary)
              }
              Spacer(modifier = Modifier.height(4.dp))
              contact.knownPhoneNumbers.forEach { num ->
                Text(
                  text = "• $num",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium,
                  color = AppTextPrimary,
                  modifier = Modifier.padding(vertical = 2.dp)
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
            }

            // Oficjalne domeny & strona WWW
            if (contact.officialWebsite != null || contact.officialDomains.isNotEmpty()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Oficjalna strona i domeny:", fontSize = 13.sp, color = AppTextSecondary)
              }
              Spacer(modifier = Modifier.height(4.dp))
              contact.officialWebsite?.let { url ->
                Text(
                  text = url,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AppPurpleLight
                )
              }
              contact.officialDomains.forEach { domain ->
                Text(
                  text = "Zweryfikowana domena: $domain",
                  fontSize = 12.sp,
                  color = AppTextMuted
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
            }

            // Godziny kontaktu
            if (contact.contactHours != null) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Godziny kontaktu:", fontSize = 13.sp, color = AppTextSecondary)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = contact.contactHours,
                fontSize = 13.sp,
                color = AppTextPrimary
              )
              Spacer(modifier = Modifier.height(12.dp))
            }

            // Źródło identyfikacji
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Security, contentDescription = null, tint = AppGreen, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Źródło identyfikacji:", fontSize = 13.sp, color = AppTextSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = contact.identificationSource,
              fontSize = 12.sp,
              color = AppTextMuted
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
      }

      // 4. SEKCJA DLA OSOBY (PERSON)
      if (contact.entityType == ContactEntityType.PERSON) {
        Text(
          text = "DANE KONTAKTOWE OSOBY",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = AppGreen,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
          border = BorderStroke(1.dp, AppBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            if (contact.personalPhoneNumbers.isNotEmpty()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = AppGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Numery telefonu:", fontSize = 13.sp, color = AppTextSecondary)
              }
              Spacer(modifier = Modifier.height(4.dp))
              contact.personalPhoneNumbers.forEach { num ->
                Text(
                  text = "• $num",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = AppTextPrimary
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
            }

            if (contact.email != null) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adres e-mail:", fontSize = 13.sp, color = AppTextSecondary)
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = contact.email, fontSize = 14.sp, color = AppTextPrimary)
              Spacer(modifier = Modifier.height(12.dp))
            }

            if (contact.customLabels.isNotEmpty()) {
              Text("Etykiety / Grupy:", fontSize = 13.sp, color = AppTextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                contact.customLabels.forEach { label ->
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppGreenContainer.copy(alpha = 0.5f)
                  ) {
                    Text(
                      text = label,
                      fontSize = 11.sp,
                      color = AppGreen,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
      }

      // 5. ZUNIFIKOWANA HISTORIA KONTAKTU (SEKCJA 34: Połączenia + SMS + OTP + Zgłoszenia)
      Text(
        text = "ZUNIFIKOWANA AKTYWNOŚĆ (CALLS + SMS + OTP)",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = AppPurpleLight,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Połączenia
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
          border = BorderStroke(1.dp, AppBorder),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
              val num = contact.allPhoneNumbers.firstOrNull() ?: ""
              if (num.isNotBlank()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                context.startActivity(intent)
              }
            }
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.Phone, contentDescription = null, tint = AppGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "${contact.callsCount} połączeń", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Text(text = contact.lastCallTimestamp ?: "Brak", fontSize = 11.sp, color = AppTextSecondary)
          }
        }

        // SMS-y
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
          border = BorderStroke(1.dp, AppBorder),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
              val addr = contact.allPhoneNumbers.firstOrNull() ?: contact.primaryIdentifier
              onOpenSmsThread(addr)
            }
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.ChatBubble, contentDescription = null, tint = AppPurpleLight, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "${contact.smsCount} SMS-ów", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Text(text = contact.lastSmsTimestamp ?: "Brak", fontSize = 11.sp, color = AppTextSecondary)
          }
        }

        // Kody OTP
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
          border = BorderStroke(1.dp, AppBorder),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "${contact.otpCount} kodów OTP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTextPrimary)
            Text(text = "Autoryzacje", fontSize = 11.sp, color = AppTextSecondary)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 6. Ocena reputacji i transparentność
      Text(
        text = "REPUTACJA I TRANSPARENTNOŚĆ",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = AppPurpleLight,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(8.dp))

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
        border = BorderStroke(1.dp, AppBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          RiskBar(score = contact.riskScore)
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Pewność danych: ${contact.confidence.displayName} (${contact.confidence.description})",
            fontSize = 12.sp,
            color = AppTextSecondary
          )
          if (contact.reputationNote != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "• ${contact.reputationNote}",
              fontSize = 12.sp,
              color = AppTextPrimary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
