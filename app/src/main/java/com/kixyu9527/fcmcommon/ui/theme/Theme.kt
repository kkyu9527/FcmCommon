package com.kixyu9527.fcmcommon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NightClay,
    secondary = MossSoft,
    tertiary = SandDeep,
    background = NightForest,
    surface = NightSurface,
    surfaceVariant = NightSurfaceAlt,
    onPrimary = Ink,
    onSecondary = NightMist,
    onTertiary = Ink,
    onBackground = NightMist,
    onSurface = NightMist,
    onSurfaceVariant = SandDeep,
)

private val LightColorScheme = lightColorScheme(
    primary = Moss,
    secondary = Clay,
    tertiary = SandDeep,
    background = SandLight,
    surface = Mist,
    surfaceVariant = SandDeep,
    onPrimary = Mist,
    onSecondary = Mist,
    onTertiary = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Ink,
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
