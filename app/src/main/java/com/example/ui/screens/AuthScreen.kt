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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CzyToTyLogo
import com.example.ui.components.LogoVariant
import com.example.ui.theme.AppBorder
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppGreen
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurpleLight
import com.example.ui.theme.AppRedText
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

/**
 * Ekran logowania przez Supabase Auth (Google OAuth z PKCE).
 *
 * Filozofia CzyToTy?:
 * - GUEST MODE: Dostępny od ręki jednym tapnięciem ("Kontynuuj bez konta").
 * - ACCOUNT: Umożliwia synchronizację, budowanie reputacji społeczności i profil.
 */
@Composable
fun AuthScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onSignInWithGoogle: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppDarkBackground)
    ) {
        // Przycisk zamknięcia (jeśli otwarty jako modal z profilu)
        if (onDismiss != null) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Zamknij",
                    tint = AppTextSecondary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. LOGO MARKI CzyToTy?
            CzyToTyLogo(
                size = 80.dp,
                variant = LogoVariant.DARK
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TYPOGRAFIA DWUKOLOROWA CzyTo + Ty?
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CzyTo",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Ty?",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppPurpleLight,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "WIESZ, ZANIM ODBIERZESZ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = AppPurple
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Zaloguj się, aby synchronizować ustawienia i budować reputację społeczności.",
                fontSize = 14.5.sp,
                color = AppTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 2. KARTA KORZYŚCI ZALOGOWANEGO UŻYTKOWNIKA
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppSurfaceVariant),
                border = BorderStroke(1.dp, AppBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    BenefitRow(
                        icon = Icons.Default.Shield,
                        title = "Buduj reputację numerów",
                        desc = "Twoje głosy zyskują wyższy Trust Score w ochronie obywatelskiej"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BenefitRow(
                        icon = Icons.Default.Sync,
                        title = "Synchronizacja chmurowa",
                        desc = "Bezpieczny backup reguł blokowania i przypiętych kontaktów"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BenefitRow(
                        icon = Icons.Default.Lock,
                        title = "Prywatność i brak haseł",
                        desc = "Bezpieczna autoryzacja Google PKCE bez przechowywania haseł"
                    )
                }
            }

            // Komunikat błędu (jeśli wystąpił)
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E151A)),
                    border = BorderStroke(1.dp, Color(0xFF6B202D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        fontSize = 13.sp,
                        color = AppRedText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. PRZYCISK GŁÓWNY: KONTYNUUJ Z GOOGLE
            Button(
                onClick = onSignInWithGoogle,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E1E28)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color(0xFF7B4DFF),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Łączenie z Google...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E28)
                    )
                } else {
                    // Stylizowana litera G (Google)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA4335)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Kontynuuj z Google",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F1F24)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. PRZYCISK TRYBU GOŚCIA: KONTYNUUJ BEZ KONTA
            OutlinedButton(
                onClick = onContinueAsGuest,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("guest_mode_button")
            ) {
                Text(
                    text = "Kontynuuj bez konta",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. STOPKA PRAWNA
            Text(
                text = "Logując się, akceptujesz Regulamin i Politykę prywatności.",
                fontSize = 11.5.sp,
                color = AppTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "CzyToTy? nie prosi o dostęp do kontaktów Google, poczty Gmail ani dysku.",
                fontSize = 10.5.sp,
                color = Color(0xFF53566A),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppPurpleContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppPurple,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.5.sp,
                color = AppTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
