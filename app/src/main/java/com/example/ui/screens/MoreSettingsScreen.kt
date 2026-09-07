package com.example.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.example.model.UserProfile
import com.example.ui.theme.AppBlue
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurplePrimary
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Ekran Profilu (5. zakładka "Profil")
 */
@Composable
fun MoreSettingsScreen(
  userProfile: UserProfile,
  onOpenOnboarding: () -> Unit,
  onOpenContacts: () -> Unit = {},
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

    // 1. TOP BAR (Profil)
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
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = AppPurple,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Profil",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = AppTextPrimary
        )
        Text(
          text = "Konto, uprawnienia i reputacja zgłoszeń",
          fontSize = 13.sp,
          color = AppTextSecondary
        )
      }

      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(AppPurpleContainer),
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

    Spacer(modifier = Modifier.height(16.dp))

    // 2. KARTA UŻYTKOWNIKA
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(AppPurpleContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "U",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AppPurple
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Użytkownik Bezpieczny Telefon",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "adrianowicz11@gmail.com",
            fontSize = 13.sp,
            color = AppTextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF281E48))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "Zaufany Obywatel",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC8BAFF)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "98% trafności",
              fontSize = 12.sp,
              color = AppGreen
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SEKCJA: KONTAKTY 2.0 (Ludzie i Organizacje)
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .clickable { onOpenContacts() }
        .testTag("settings_contacts_button")
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
            .background(AppPurpleContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Group,
            contentDescription = null,
            tint = AppPurple,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Książka kontaktów (Osoby i Firmy)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Zarządzaj kontaktami 2.0, przypinaniem i kolejnością",
            fontSize = 12.sp,
            color = AppTextSecondary
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

    Spacer(modifier = Modifier.height(20.dp))

    // 3. SEKCJA: INTEGRACJA SYSTEMOWA (Role Androida)
    Text(
      text = "Integracja systemowa (3 w 1)",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = AppTextPrimary
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "Ustaw aplikację jako domyślną, aby zapewnić pełną ochronę.",
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
        // Telefon (Dialer)
        SystemRoleItem(
          icon = Icons.Default.Phone,
          iconTint = AppGreen,
          iconBg = Color(0xFF15221B),
          title = "Domyślna aplikacja telefonu",
          subtitle = "Umożliwia odbieranie połączeń i ekran weryfikacji",
          onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              val roleManager = context.getSystemService(RoleManager::class.java)
              if (roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                context.startActivity(intent)
              }
            }
          }
        )

        HorizontalDivider(thickness = 1.dp, color = Color(0xFF242136), modifier = Modifier.padding(horizontal = 16.dp))

        // SMS
        SystemRoleItem(
          icon = Icons.Default.Message,
          iconTint = AppPurple,
          iconBg = AppPurpleContainer,
          title = "Domyślna aplikacja SMS",
          subtitle = "Lokalna analiza linków i automatyczne kody OTP",
          onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              val roleManager = context.getSystemService(RoleManager::class.java)
              if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                context.startActivity(intent)
              }
            }
          }
        )

        HorizontalDivider(thickness = 1.dp, color = Color(0xFF242136), modifier = Modifier.padding(horizontal = 16.dp))

        // Call Screening
        SystemRoleItem(
          icon = Icons.Default.Security,
          iconTint = AppBlue,
          iconBg = Color(0xFF1A263C),
          title = "Ochrona połączeń (Call Screening)",
          subtitle = "Aktywna ochrona i natychmiastowe blokowanie spamu",
          onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              val roleManager = context.getSystemService(RoleManager::class.java)
              if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                context.startActivity(intent)
              }
            }
          }
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4. SEKCJA: PRYWATNOŚĆ I EDUKACJA
    Text(
      text = "Prywatność i bezpieczeństwo",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = AppTextPrimary
    )
    Spacer(modifier = Modifier.height(12.dp))

    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
      border = BorderStroke(1.dp, AppBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        SystemRoleItem(
          icon = Icons.Default.Lock,
          iconTint = AppGreen,
          iconBg = Color(0xFF15221B),
          title = "Gwarancja Zero-Cloud dla SMS",
          subtitle = "Twoje wiadomości są analizowane wyłącznie lokalnie na urządzeniu",
          onClick = {}
        )

        HorizontalDivider(thickness = 1.dp, color = Color(0xFF242136), modifier = Modifier.padding(horizontal = 16.dp))

        SystemRoleItem(
          icon = Icons.Default.Info,
          iconTint = AppPurple,
          iconBg = AppPurpleContainer,
          title = "Zasady działania i wskaźniki",
          subtitle = "Dowiedz się, jak wyliczamy wskaźnik ryzyka i pewność",
          onClick = onOpenOnboarding
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}

@Composable
private fun SystemRoleItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  iconBg: Color,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(iconBg),
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

    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = AppTextSecondary,
      modifier = Modifier.size(20.dp)
    )
  }
}
