package com.auramusic.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AuraPalette(
    val background: Color,
    val backgroundDark: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceActive: Color,
    val cyan: Color,
    val cyanGlow: Color,
    val neonPurple: Color,
    val cyberPink: Color,
    val cyberGreen: Color,
    val cyberAmber: Color,
    val cyberRed: Color,
    val glassBorder: Color,
    val glassBorderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val vinylBody: Color,
    val vinylGroove: Color,
    val vinylCenterRing: Color
)

// 1. Cyber Neon (Default Cyberpunk Aesthetic)
val CyberNeonPalette = AuraPalette(
    background = Color(0xFF0D1117),
    backgroundDark = Color(0xFF070A0E),
    surface = Color(0x14FFFFFF),
    surfaceCard = Color(0x1A1E2638),
    surfaceActive = Color(0x28FFFFFF),
    cyan = Color(0xFF00FFCC),
    cyanGlow = Color(0x4000FFCC),
    neonPurple = Color(0xFFBD93F9),
    cyberPink = Color(0xFFFF79C6),
    cyberGreen = Color(0xFF50FA7B),
    cyberAmber = Color(0xFFFFB86C),
    cyberRed = Color(0xFFFF5555),
    glassBorder = Color(0x3300FFCC),
    glassBorderSubtle = Color(0x1FFFFFFF),
    textPrimary = Color(0xFFF0F6FC),
    textSecondary = Color(0xFF8B949E),
    textMuted = Color(0xFF5A6270),
    vinylBody = Color(0xFF12151B),
    vinylGroove = Color(0xFF1C222D),
    vinylCenterRing = Color(0xFF2A3140)
)

// 2. Synthwave (Retro 80s Sunset: Neon Orange, Magenta, Deep Violet)
val SynthwavePalette = AuraPalette(
    background = Color(0xFF10051D),
    backgroundDark = Color(0xFF0A0314),
    surface = Color(0x1E3A1A5E),
    surfaceCard = Color(0x2E2A1244),
    surfaceActive = Color(0x3E4A1A6E),
    cyan = Color(0xFFFF8C00),
    cyanGlow = Color(0x40FF8C00),
    neonPurple = Color(0xFFBD93F9),
    cyberPink = Color(0xFFE024C3),
    cyberGreen = Color(0xFF00FFA3),
    cyberAmber = Color(0xFFFFB86C),
    cyberRed = Color(0xFFFF2A6D),
    glassBorder = Color(0x33FF8C00),
    glassBorderSubtle = Color(0x1FFFFFFF),
    textPrimary = Color(0xFFF5F3FF),
    textSecondary = Color(0xFFA78BFA),
    textMuted = Color(0xFF7C3AED),
    vinylBody = Color(0xFF150A21),
    vinylGroove = Color(0xFF221133),
    vinylCenterRing = Color(0xFF3B1754)
)

// 3. Matrix 99 (Hacker Phosphor Green & Deep Terminal Black)
val MatrixPalette = AuraPalette(
    background = Color(0xFF030D06),
    backgroundDark = Color(0xFF010603),
    surface = Color(0x180D381D),
    surfaceCard = Color(0x28082412),
    surfaceActive = Color(0x380F4A24),
    cyan = Color(0xFF00FF66),
    cyanGlow = Color(0x4000FF66),
    neonPurple = Color(0xFF10B981),
    cyberPink = Color(0xFF34D399),
    cyberGreen = Color(0xFF00FF66),
    cyberAmber = Color(0xFFA7F3D0),
    cyberRed = Color(0xFFEF4444),
    glassBorder = Color(0x3300FF66),
    glassBorderSubtle = Color(0x1F00FF66),
    textPrimary = Color(0xFFECFDF5),
    textSecondary = Color(0xFF6EE7B7),
    textMuted = Color(0xFF047857),
    vinylBody = Color(0xFF06150A),
    vinylGroove = Color(0xFF0B2412),
    vinylCenterRing = Color(0xFF133E1F)
)

// 4. OLED Black (Pure Minimalist Stealth: Pure Black & Crisp Silver/White)
val OledPalette = AuraPalette(
    background = Color(0xFF000000),
    backgroundDark = Color(0xFF000000),
    surface = Color(0xFF141414),
    surfaceCard = Color(0xFF101010),
    surfaceActive = Color(0xFF222222),
    cyan = Color(0xFFFFFFFF),
    cyanGlow = Color(0x33FFFFFF),
    neonPurple = Color(0xFF94A3B8),
    cyberPink = Color(0xFFCBD5E1),
    cyberGreen = Color(0xFF4ADE80),
    cyberAmber = Color(0xFFFBBF24),
    cyberRed = Color(0xFFF87171),
    glassBorder = Color(0x33FFFFFF),
    glassBorderSubtle = Color(0x1AFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    vinylBody = Color(0xFF0A0A0A),
    vinylGroove = Color(0xFF141414),
    vinylCenterRing = Color(0xFF262626)
)

val LocalAuraPalette = staticCompositionLocalOf { CyberNeonPalette }

// Composable accessors for backward compatibility and automatic dynamic theme propagation
val CyberBackground: Color @Composable get() = LocalAuraPalette.current.background
val CyberBackgroundDark: Color @Composable get() = LocalAuraPalette.current.backgroundDark
val CyberSurface: Color @Composable get() = LocalAuraPalette.current.surface
val CyberSurfaceCard: Color @Composable get() = LocalAuraPalette.current.surfaceCard
val CyberSurfaceActive: Color @Composable get() = LocalAuraPalette.current.surfaceActive

val CyberCyan: Color @Composable get() = LocalAuraPalette.current.cyan
val CyberCyanGlow: Color @Composable get() = LocalAuraPalette.current.cyanGlow
val NeonPurple: Color @Composable get() = LocalAuraPalette.current.neonPurple
val CyberPink: Color @Composable get() = LocalAuraPalette.current.cyberPink
val CyberGreen: Color @Composable get() = LocalAuraPalette.current.cyberGreen
val CyberAmber: Color @Composable get() = LocalAuraPalette.current.cyberAmber
val CyberRed: Color @Composable get() = LocalAuraPalette.current.cyberRed

val GlassBorder: Color @Composable get() = LocalAuraPalette.current.glassBorder
val GlassBorderSubtle: Color @Composable get() = LocalAuraPalette.current.glassBorderSubtle

val TextPrimary: Color @Composable get() = LocalAuraPalette.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAuraPalette.current.textSecondary
val TextMuted: Color @Composable get() = LocalAuraPalette.current.textMuted

val VinylBody: Color @Composable get() = LocalAuraPalette.current.vinylBody
val VinylGroove: Color @Composable get() = LocalAuraPalette.current.vinylGroove
val VinylCenterRing: Color @Composable get() = LocalAuraPalette.current.vinylCenterRing
