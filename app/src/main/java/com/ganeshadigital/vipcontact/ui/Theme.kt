package com.ganeshadigital.vipcontact.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDEA8),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF1F2E57),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2FF),
    onSecondaryContainer = Color(0xFF0B1A3F),
    tertiary = Color(0xFF1B6B3A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB7F2C6),
    onTertiaryContainer = Color(0xFF00210D),
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF1E6D6),
    onSurfaceVariant = Color(0xFF4F4539),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFBA3B),
    onPrimary = Color(0xFF462B00),
    primaryContainer = Color(0xFF643F00),
    onPrimaryContainer = Color(0xFFFFDEA8),
    secondary = Color(0xFF2B3B66),
    onSecondary = Color(0xFFEFF1FF),
    secondaryContainer = Color(0xFF2B3B66),
    onSecondaryContainer = Color(0xFFDCE2FF),
    tertiary = Color(0xFF9BD6AB),
    onTertiary = Color(0xFF00391A),
    tertiaryContainer = Color(0xFF00522A),
    onTertiaryContainer = Color(0xFFB7F2C6),
    background = Color(0xFF14161C),
    onBackground = Color(0xFFEAE1D9),
    surface = Color(0xFF14161C),
    onSurface = Color(0xFFEAE1D9),
    surfaceVariant = Color(0xFF2C2A30),
    onSurfaceVariant = Color(0xFFD3C4B4),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun VipTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
