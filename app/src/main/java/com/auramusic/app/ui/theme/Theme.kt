package com.auramusic.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AuraMusicTheme(
    themeName: String = "Studio Carbon",
    content: @Composable () -> Unit
) {
    val palette = when (themeName) {
        "Champagne Hi-Fi", "Synthwave" -> SynthwavePalette
        "Olive Vinyl", "Matrix 99" -> MatrixPalette
        "Leica Monochrome", "OLED Black" -> OledPalette
        else -> CyberNeonPalette
    }

    val colorScheme = darkColorScheme(
        primary = palette.cyan,
        onPrimary = palette.backgroundDark,
        primaryContainer = palette.cyanGlow,
        onPrimaryContainer = palette.cyan,
        secondary = palette.neonPurple,
        onSecondary = palette.backgroundDark,
        tertiary = palette.cyberPink,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surfaceCard,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surface,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.glassBorder
    )

    CompositionLocalProvider(LocalAuraPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
