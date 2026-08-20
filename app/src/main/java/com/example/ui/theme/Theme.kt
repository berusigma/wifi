package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F24),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = NeonRed,
    onSecondary = Color(0xFF400010),
    secondaryContainer = Color(0xFF8F0028),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = NeonAmber,
    onTertiary = Color(0xFF422C00),
    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberDarkCard,
    onSurface = TextPrimary,
    surfaceVariant = CyberDarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = CyberDarkCardBorder,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
