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

// 1. Cyber Neon -> Studio Obsidian (Default Human-Crafted Dark Editorial Aesthetic)
val CyberNeonPalette = AuraPalette(
    background = Color(0xFF0A0B0F),
    backgroundDark = Color(0xFF06070A),
    surface = Color(0xFF12141C),
    surfaceCard = Color(0xFF161922),
    surfaceActive = Color(0xFF222634),
    cyan = Color(0xFF2DD4BF),
    cyanGlow = Color(0x282DD4BF),
    neonPurple = Color(0xFF94A3B8),
    cyberPink = Color(0xFFF43F5E),
    cyberGreen = Color(0xFF34D399),
    cyberAmber = Color(0xFFFBBF24),
    cyberRed = Color(0xFFF87171),
    glassBorder = Color(0x14FFFFFF),
    glassBorderSubtle = Color(0x0AFFFFFF),
    textPrimary = Color(0xFFF3F4F6),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF64748B),
    vinylBody = Color(0xFF111318),
    vinylGroove = Color(0xFF1B1F2A),
    vinylCenterRing = Color(0xFF282E3D)
)

// 2. Synthwave (Warm Sunset Amber & Deep Espresso Plum)
val SynthwavePalette = AuraPalette(
    background = Color(0xFF0E0A14),
    backgroundDark = Color(0xFF08050C),
    surface = Color(0xFF171121),
    surfaceCard = Color(0xFF1C1528),
    surfaceActive = Color(0xFF2A203B),
    cyan = Color(0xFFF59E0B),
    cyanGlow = Color(0x28F59E0B),
    neonPurple = Color(0xFFC084FC),
    cyberPink = Color(0xFFF43F5E),
    cyberGreen = Color(0xFF34D399),
    cyberAmber = Color(0xFFFBBF24),
    cyberRed = Color(0xFFFB7185),
    glassBorder = Color(0x14FFFFFF),
    glassBorderSubtle = Color(0x0AFFFFFF),
    textPrimary = Color(0xFFFAF5FF),
    textSecondary = Color(0xFFB4A5C7),
    textMuted = Color(0xFF7E6E94),
    vinylBody = Color(0xFF140E1C),
    vinylGroove = Color(0xFF21172E),
    vinylCenterRing = Color(0xFF332447)
)

// 3. Matrix 99 (Forest Emerald & Deep Carbon)
val MatrixPalette = AuraPalette(
    background = Color(0xFF070C09),
    backgroundDark = Color(0xFF040705),
    surface = Color(0xFF0F1712),
    surfaceCard = Color(0xFF131D17),
    surfaceActive = Color(0xFF1C2B22),
    cyan = Color(0xFF10B981),
    cyanGlow = Color(0x2810B981),
    neonPurple = Color(0xFF6EE7B7),
    cyberPink = Color(0xFFF43F5E),
    cyberGreen = Color(0xFF10B981),
    cyberAmber = Color(0xFFFBBF24),
    cyberRed = Color(0xFFEF4444),
    glassBorder = Color(0x14FFFFFF),
    glassBorderSubtle = Color(0x0AFFFFFF),
    textPrimary = Color(0xFFECFDF5),
    textSecondary = Color(0xFF9CA3AF),
    textMuted = Color(0xFF6B7280),
    vinylBody = Color(0xFF0B120E),
    vinylGroove = Color(0xFF142019),
    vinylCenterRing = Color(0xFF1F3327)
)

// 4. OLED Black (Pure Minimalist Stealth: Pure Black & Crisp Silver/White)
val OledPalette = AuraPalette(
    background = Color(0xFF000000),
    backgroundDark = Color(0xFF000000),
    surface = Color(0xFF111113),
    surfaceCard = Color(0xFF141417),
    surfaceActive = Color(0xFF222226),
    cyan = Color(0xFFFFFFFF),
    cyanGlow = Color(0x24FFFFFF),
    neonPurple = Color(0xFFA1A1AA),
    cyberPink = Color(0xFFF43F5E),
    cyberGreen = Color(0xFF4ADE80),
    cyberAmber = Color(0xFFFBBF24),
    cyberRed = Color(0xFFF87171),
    glassBorder = Color(0x16FFFFFF),
    glassBorderSubtle = Color(0x0DFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF71717A),
    vinylBody = Color(0xFF0A0A0C),
    vinylGroove = Color(0xFF141417),
    vinylCenterRing = Color(0xFF27272A)
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
