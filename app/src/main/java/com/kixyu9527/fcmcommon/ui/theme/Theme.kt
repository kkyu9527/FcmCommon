package com.kixyu9527.fcmcommon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    secondary = NightSecondary,
    tertiary = NightSurfaceAlt,
    primaryContainer = NightSurfaceAlt,
    secondaryContainer = NightSurfaceAlt,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceAlt,
    onPrimary = NightBackground,
    onSecondary = NightBackground,
    onTertiary = NightText,
    onPrimaryContainer = NightText,
    onSecondaryContainer = NightText,
    onBackground = NightText,
    onSurface = NightText,
    onSurfaceVariant = NightText.copy(alpha = 0.72f),
    surfaceTint = NightPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentMint,
    tertiary = AccentSky,
    primaryContainer = AccentBlue.copy(alpha = 0.16f),
    secondaryContainer = AccentMint.copy(alpha = 0.16f),
    background = Cloud,
    surface = CardWhite,
    surfaceVariant = Frost,
    onPrimary = CardWhite,
    onSecondary = CardWhite,
    onTertiary = Ink,
    onPrimaryContainer = Ink,
    onSecondaryContainer = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Ink.copy(alpha = 0.62f),
    surfaceTint = AccentBlue,
)

@Composable
fun FcmCommonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onBackground,
            content = content,
        )
    }
}
