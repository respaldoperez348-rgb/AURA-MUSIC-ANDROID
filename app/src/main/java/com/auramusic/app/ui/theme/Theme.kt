package com.auramusic.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberNeonColorScheme = darkColorScheme(
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

private val SynthwaveColorScheme = darkColorScheme(
    primary = Color(0xFFFF8C00),
    onPrimary = Color(0xFF10051D),
    primaryContainer = Color(0x40FF8C00),
    onPrimaryContainer = Color(0xFFFF8C00),
    secondary = NeonPurple,
    onSecondary = Color(0xFF10051D),
    tertiary = Color(0xFFE024C3),
    background = Color(0xFF10051D),
    onBackground = TextPrimary,
    surface = Color(0x2E2A1244),
    onSurface = TextPrimary,
    surfaceVariant = Color(0x1E3A1A5E),
    onSurfaceVariant = TextSecondary,
    outline = Color(0x40FF8C00)
)

private val MatrixColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),
    onPrimary = Color(0xFF020D06),
    primaryContainer = Color(0x4000FF66),
    onPrimaryContainer = Color(0xFF00FF66),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF020D06),
    tertiary = Color(0xFF059669),
    background = Color(0xFF030D06),
    onBackground = TextPrimary,
    surface = Color(0x28082412),
    onSurface = TextPrimary,
    surfaceVariant = Color(0x180D381D),
    onSurfaceVariant = TextSecondary,
    outline = Color(0x4000FF66)
)

private val OledColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0x33FFFFFF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFFCBD5E1),
    background = Color(0xFF000000),
    onBackground = TextPrimary,
    surface = Color(0xFF121212),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = TextSecondary,
    outline = Color(0x33FFFFFF)
)

@Composable
fun AuraMusicTheme(
    themeName: String = "Cyber Neon",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Synthwave" -> SynthwaveColorScheme
        "Matrix 99" -> MatrixColorScheme
        "OLED Black" -> OledColorScheme
        else -> CyberNeonColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
