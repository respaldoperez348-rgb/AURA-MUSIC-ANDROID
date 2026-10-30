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

    val palette = when (themeName) {
        "Champagne Hi-Fi", "Synthwave" -> SynthwavePalette
        "Olive Vinyl", "Matrix 99" -> MatrixPalette
        "Leica Monochrome", "OLED Black" -> OledPalette
        else -> CyberNeonPalette
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
            background = androidx.compose.ui.graphics.Color(0xFFF7F7F8),
            onBackground = androidx.compose.ui.graphics.Color(0xFF141416),
            surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            onSurface = androidx.compose.ui.graphics.Color(0xFF141416),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFEBECEF),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF555760),
            outline = androidx.compose.ui.graphics.Color(0x1F000000)
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
