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

// 1. Studio Carbon (Default — Braun / Teenage Engineering Matte Carbon & Analog Vermilion)
val CyberNeonPalette = AuraPalette(
    background = Color(0xFF0C0C0E),
    backgroundDark = Color(0xFF070709),
    surface = Color(0xFF141417),
    surfaceCard = Color(0xFF1A1A1E),
    surfaceActive = Color(0xFF25252B),
    cyan = Color(0xFFE85D2A),
    cyanGlow = Color(0x24E85D2A),
    neonPurple = Color(0xFFD97757),
    cyberPink = Color(0xFFE14D43),
    cyberGreen = Color(0xFF5E9E72),
    cyberAmber = Color(0xFFD99B26),
    cyberRed = Color(0xFFD94A4A),
    glassBorder = Color(0x12FFFFFF),
    glassBorderSubtle = Color(0x08FFFFFF),
    textPrimary = Color(0xFFF4F3EF),
    textSecondary = Color(0xFF9E9D99),
    textMuted = Color(0xFF666560),
    vinylBody = Color(0xFF111113),
    vinylGroove = Color(0xFF1B1B20),
    vinylCenterRing = Color(0xFF292930)
)

// 2. Champagne Hi-Fi (Bang & Olufsen Brushed Titanium & Warm Brass Gold)
val SynthwavePalette = AuraPalette(
    background = Color(0xFF0E0F12),
    backgroundDark = Color(0xFF08090B),
    surface = Color(0xFF16181D),
    surfaceCard = Color(0xFF1C1E24),
    surfaceActive = Color(0xFF272A32),
    cyan = Color(0xFFD4AF6A),
    cyanGlow = Color(0x24D4AF6A),
    neonPurple = Color(0xFFB89B64),
    cyberPink = Color(0xFFD96B52),
    cyberGreen = Color(0xFF65A378),
    cyberAmber = Color(0xFFD4AF6A),
    cyberRed = Color(0xFFD9534F),
    glassBorder = Color(0x12FFFFFF),
    glassBorderSubtle = Color(0x08FFFFFF),
    textPrimary = Color(0xFFF5F3EE),
    textSecondary = Color(0xFFA3A19A),
    textMuted = Color(0xFF6E6C66),
    vinylBody = Color(0xFF121317),
    vinylGroove = Color(0xFF1C1E24),
    vinylCenterRing = Color(0xFF2B2E37)
)

// 3. Olive Vinyl (Organic Slate & Matte Sage Green)
val MatrixPalette = AuraPalette(
    background = Color(0xFF0A0D0B),
    backgroundDark = Color(0xFF060807),
    surface = Color(0xFF121714),
    surfaceCard = Color(0xFF171E1A),
    surfaceActive = Color(0xFF212B25),
    cyan = Color(0xFF7FA982),
    cyanGlow = Color(0x247FA982),
    neonPurple = Color(0xFF9ABCA0),
    cyberPink = Color(0xFFD96B52),
    cyberGreen = Color(0xFF7FA982),
    cyberAmber = Color(0xFFD4A359),
    cyberRed = Color(0xFFD9534F),
    glassBorder = Color(0x12FFFFFF),
    glassBorderSubtle = Color(0x08FFFFFF),
    textPrimary = Color(0xFFF0F4F1),
    textSecondary = Color(0xFF99A39B),
    textMuted = Color(0xFF636E66),
    vinylBody = Color(0xFF0E1310),
    vinylGroove = Color(0xFF17201B),
    vinylCenterRing = Color(0xFF243029)
)

// 4. Leica Monochrome (Pure OLED Black & High-Contrast Silver/White)
val OledPalette = AuraPalette(
    background = Color(0xFF000000),
    backgroundDark = Color(0xFF000000),
    surface = Color(0xFF0F0F11),
    surfaceCard = Color(0xFF151517),
    surfaceActive = Color(0xFF222225),
    cyan = Color(0xFFFFFFFF),
    cyanGlow = Color(0x20FFFFFF),
    neonPurple = Color(0xFFA1A1AA),
    cyberPink = Color(0xFFE14D43),
    cyberGreen = Color(0xFF65A378),
    cyberAmber = Color(0xFFD4AF6A),
    cyberRed = Color(0xFFD9534F),
    glassBorder = Color(0x14FFFFFF),
    glassBorderSubtle = Color(0x0AFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF6E6E77),
    vinylBody = Color(0xFF0A0A0C),
    vinylGroove = Color(0xFF151518),
    vinylCenterRing = Color(0xFF26262B)
)

val LocalAuraPalette = staticCompositionLocalOf { CyberNeonPalette }

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
