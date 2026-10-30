package com.auramusic.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AuraMusicTheme(
    themeName: String = "Studio Carbon",
    themeMode: String = "Oscuro",
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode.lowercase()) {
        "claro", "light" -> false
        "sistema", "system" -> systemInDark
        else -> true // "oscuro" / "dark" by default for studio fidelity
    }

    val palette = if (isDark) {
        when (themeName) {
            "Champagne Hi-Fi", "Synthwave" -> SynthwavePalette
            "Olive Vinyl", "Matrix 99" -> MatrixPalette
            "Leica Monochrome", "OLED Black" -> OledPalette
            else -> CyberNeonPalette
        }
    } else {
        when (themeName) {
            "Champagne Hi-Fi", "Synthwave" -> SynthwavePaletteLight
            "Olive Vinyl", "Matrix 99" -> MatrixPaletteLight
            "Leica Monochrome", "OLED Black" -> OledPaletteLight
            else -> CyberNeonPaletteLight
        }
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
            primary = palette.cyan,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = palette.cyanGlow,
            onPrimaryContainer = palette.cyan,
            secondary = palette.neonPurple,
            onSecondary = androidx.compose.ui.graphics.Color.White,
            tertiary = palette.cyberPink,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surfaceCard,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.glassBorder
        )
    }

    CompositionLocalProvider(LocalAuraPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
