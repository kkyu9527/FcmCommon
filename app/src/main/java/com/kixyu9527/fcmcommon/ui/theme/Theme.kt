package com.kixyu9527.fcmcommon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    secondary = NightSecondary,
    tertiary = NightSurfaceAlt,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceAlt,
    onPrimary = Ink,
    onSecondary = Ink,
    onTertiary = NightText,
    onBackground = NightText,
    onSurface = NightText,
    onSurfaceVariant = NightText.copy(alpha = 0.72f),
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentMint,
    tertiary = AccentSky,
    background = Cloud,
    surface = CardWhite,
    surfaceVariant = Frost,
    onPrimary = CardWhite,
    onSecondary = CardWhite,
    onTertiary = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Ink.copy(alpha = 0.62f),
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
        content = content,
    )
}
