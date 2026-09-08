package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = md_theme_dark_primary,
  onPrimary = md_theme_dark_onPrimary,
  primaryContainer = md_theme_dark_primaryContainer,
  onPrimaryContainer = md_theme_dark_onPrimaryContainer,
  secondary = md_theme_dark_secondary,
  onSecondary = md_theme_dark_onSecondary,
  secondaryContainer = md_theme_dark_secondaryContainer,
  onSecondaryContainer = md_theme_dark_onSecondaryContainer,
  tertiary = md_theme_dark_tertiary,
  onTertiary = md_theme_dark_onTertiary,
  tertiaryContainer = md_theme_dark_tertiaryContainer,
  onTertiaryContainer = md_theme_dark_onTertiaryContainer,
  error = md_theme_dark_error,
  errorContainer = md_theme_dark_errorContainer,
  onError = md_theme_dark_onError,
  onErrorContainer = md_theme_dark_onErrorContainer,
  background = md_theme_dark_background,
  onBackground = md_theme_dark_onBackground,
  surface = md_theme_dark_surface,
  onSurface = md_theme_dark_onSurface,
  surfaceVariant = md_theme_dark_surfaceVariant,
  onSurfaceVariant = md_theme_dark_onSurfaceVariant,
  outline = md_theme_dark_outline,
)

private val LightColorScheme = lightColorScheme(
  primary = md_theme_light_primary,
  onPrimary = md_theme_light_onPrimary,
  primaryContainer = md_theme_light_primaryContainer,
  onPrimaryContainer = md_theme_light_onPrimaryContainer,
  secondary = md_theme_light_secondary,
  onSecondary = md_theme_light_onSecondary,
  secondaryContainer = md_theme_light_secondaryContainer,
  onSecondaryContainer = md_theme_light_onSecondaryContainer,
  tertiary = md_theme_light_tertiary,
  onTertiary = md_theme_light_onTertiary,
  tertiaryContainer = md_theme_light_tertiaryContainer,
  onTertiaryContainer = md_theme_light_onTertiaryContainer,
  error = md_theme_light_error,
  errorContainer = md_theme_light_errorContainer,
  onError = md_theme_light_onError,
  onErrorContainer = md_theme_light_onErrorContainer,
  background = md_theme_light_background,
  onBackground = md_theme_light_onBackground,
  surface = md_theme_light_surface,
  onSurface = md_theme_light_onSurface,
  surfaceVariant = md_theme_light_surfaceVariant,
  onSurfaceVariant = md_theme_light_onSurfaceVariant,
  outline = md_theme_light_outline,
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean? = null,
  dynamicColor: Boolean? = null,
  appearance: Appearance = rememberAppearance(),
  content: @Composable () -> Unit,
) {
  val dark = darkTheme ?: when (appearance.mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }
  val base = when {
    (dynamicColor ?: appearance.dynamic) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    dark -> DarkColorScheme
    else -> LightColorScheme
  }
  val colorScheme = if ((dynamicColor ?: appearance.dynamic) && Build.VERSION.SDK_INT >= 31) base else {
    val colors = when (appearance.accent) {
      Accent.LAVENDER -> if (dark) listOf(0xFFD0BCFF, 0xFF38205D, 0xFF493365, 0xFFF0E5FF) else listOf(0xFF67449B, 0xFFFFFFFF, 0xFFECDDFF, 0xFF291047)
      Accent.OCEAN -> if (dark) listOf(0xFFA4C9FF, 0xFF00315D, 0xFF24466A, 0xFFD6E8FF) else listOf(0xFF245D96, 0xFFFFFFFF, 0xFFD4E5FF, 0xFF07315A)
      Accent.MINT -> if (dark) listOf(0xFF8DD8C0, 0xFF00382B, 0xFF245345, 0xFFB2F1DC) else listOf(0xFF176B55, 0xFFFFFFFF, 0xFFB2F1DC, 0xFF00382B)
    }.map { Color(it) }
    base.copy(primary = colors[0], onPrimary = colors[1], primaryContainer = colors[2], onPrimaryContainer = colors[3])
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography
  ) {
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides colorScheme.onSurface,
      LocalAppearance provides appearance) { content() }
  }
}
