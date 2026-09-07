package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AppDarkBackground
import com.example.ui.theme.AppPurple
import com.example.ui.theme.AppPurpleContainer
import com.example.ui.theme.AppPurpleGlow
import com.example.ui.theme.AppPurpleLight
import com.example.ui.theme.AppPurplePrimary
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary

enum class LogoVariant {
    DARK,
    LIGHT,
    MONO,
    BW
}

/**
 * "Serduszko produktu" - Logo aplikacji CzyToTy?
 * Znak zapytania uformowany z dymka SMS oraz słuchawki telefonu.
 */
@Composable
fun CzyToTyLogo(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    variant: LogoVariant = LogoVariant.DARK
) {
    val resId = when (variant) {
        LogoVariant.DARK -> R.drawable.ic_czytoty_logo
        LogoVariant.LIGHT -> R.drawable.ic_czytoty_logo_light
        LogoVariant.MONO -> R.drawable.ic_czytoty_logo_mono
        LogoVariant.BW -> R.drawable.ic_czytoty_logo_bw
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (variant == LogoVariant.DARK) 8.dp else 0.dp,
                shape = RoundedCornerShape(size * 0.22f),
                ambientColor = AppPurplePrimary,
                spotColor = AppPurpleLight
            )
            .clip(RoundedCornerShape(size * 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = "CzyToTy? Logo",
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Nagłówek marki CzyToTy? z dwutonową typografią ("CzyTo" na biało, "Ty?" w fiolecie)
 * oraz hasłem: "WIESZ, ZANIM ODBIERZESZ".
 */
@Composable
fun CzyToTyBrandHeader(
    modifier: Modifier = Modifier,
    subtitle: String = "WIESZ, ZANIM ODBIERZESZ",
    showLogo: Boolean = true,
    logoSize: Dp = 46.dp,
    onLogoClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLogo) {
            CzyToTyLogo(
                size = logoSize,
                variant = LogoVariant.DARK,
                modifier = Modifier
                    .clickable(enabled = onLogoClick != null) { onLogoClick?.invoke() }
                    .testTag("brand_logo")
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            // Nazwa "CzyToTy?" z wyróżnionym fioletem na "Ty?"
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppTextPrimary, fontWeight = FontWeight.ExtraBold)) {
                        append("CzyTo")
                    }
                    withStyle(SpanStyle(color = AppPurple, fontWeight = FontWeight.ExtraBold)) {
                        append("Ty?")
                    }
                },
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = AppPurpleLight
            )

            Text(
                text = "Telefon • Wiadomości • Ochrona",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = AppTextSecondary
            )
        }
    }
}

/**
 * 4 filary produktu z dolnego paska załączonej grafiki:
 * 1. Ochrona ("Ostrzeżenia przed spamem")
 * 2. Telefon ("Sprawdź, kto dzwoni")
 * 3. Wiadomości ("Wykrywanie podejrzanych SMS")
 * 4. Dla każdego ("Prosto, po ludzku")
 */
@Composable
fun CzyToTyPillarsSection(
    modifier: Modifier = Modifier,
    onPillarClick: (Int) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppSurfaceVariant)
            .border(BorderStroke(1.dp, Color(0x26A875FF)), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        // Górny wiersz: Ochrona & Telefon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PillarCard(
                iconRes = R.drawable.ic_protection_shield,
                title = "Ochrona",
                subtitle = "Ostrzeżenia przed spamem",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onPillarClick(0) }
            )
            PillarCard(
                iconRes = R.drawable.ic_phone_lookup,
                title = "Telefon",
                subtitle = "Sprawdź, kto dzwoni",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onPillarClick(1) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dolny wiersz: Wiadomości & Dla każdego
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PillarCard(
                iconRes = R.drawable.ic_messages_sms,
                title = "Wiadomości",
                subtitle = "Wykrywanie podejrzanych SMS",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onPillarClick(2) }
            )
            PillarCard(
                iconRes = R.drawable.ic_everyone_people,
                title = "Dla każdego",
                subtitle = "Prosto, po ludzku",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = { onPillarClick(3) }
            )
        }
    }
}

@Composable
private fun PillarCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, Color(0x1FA875FF)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppPurpleContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = AppPurpleLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = AppTextSecondary,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
