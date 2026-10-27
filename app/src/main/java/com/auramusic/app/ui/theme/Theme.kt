package com.auramusic.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CyberBackgroundDark,
    primaryContainer = CyberCyanGlow,
    onPrimaryContainer = CyberCyan,
    secondary = NeonPurple,
    onSecondary = CyberBackgroundDark,
    tertiary = CyberPink,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

@Composable
fun AuraMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
