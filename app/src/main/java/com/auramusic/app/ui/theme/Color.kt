package com.auramusic.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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

// =========================================================================
// DARK PALETTES (High-Fidelity Studio Hardware Finishes)
// =========================================================================

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

// =========================================================================
// LIGHT PALETTES (Clean Daylight Studio Edition)
// =========================================================================

// 1. Studio Carbon (Light — Architectural Porcelain & Vermilion Accent)
val CyberNeonPaletteLight = AuraPalette(
    background = Color(0xFFF6F7FA),
    backgroundDark = Color(0xFFFFFFFF),
    surface = Color(0xFFECEEF2),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceActive = Color(0xFFDFE3EB),
    cyan = Color(0xFFE85D2A),
    cyanGlow = Color(0x22E85D2A),
    neonPurple = Color(0xFFD97757),
    cyberPink = Color(0xFFE14D43),
    cyberGreen = Color(0xFF2E7D47),
    cyberAmber = Color(0xFFD99B26),
    cyberRed = Color(0xFFD94A4A),
    glassBorder = Color(0x18000000),
    glassBorderSubtle = Color(0x0A000000),
    textPrimary = Color(0xFF121418),
    textSecondary = Color(0xFF5A606D),
    textMuted = Color(0xFF8F96A3),
    vinylBody = Color(0xFF22242B),
    vinylGroove = Color(0xFF2E313A),
    vinylCenterRing = Color(0xFFE85D2A)
)

// 2. Champagne Hi-Fi (Light — Warm Brushed Titanium & Antique Brass Gold)
val SynthwavePaletteLight = AuraPalette(
    background = Color(0xFFFAF8F5),
    backgroundDark = Color(0xFFFFFFFF),
    surface = Color(0xFFF2ECE3),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceActive = Color(0xFFE8DFD3),
    cyan = Color(0xFFB88E3E),
    cyanGlow = Color(0x22B88E3E),
    neonPurple = Color(0xFF9E772F),
    cyberPink = Color(0xFFC95B3E),
    cyberGreen = Color(0xFF3B7A4C),
    cyberAmber = Color(0xFFB88E3E),
    cyberRed = Color(0xFFD94A4A),
    glassBorder = Color(0x18000000),
    glassBorderSubtle = Color(0x0A000000),
    textPrimary = Color(0xFF1C1814),
    textSecondary = Color(0xFF6B6356),
    textMuted = Color(0xFF999081),
    vinylBody = Color(0xFF24201B),
    vinylGroove = Color(0xFF332D27),
    vinylCenterRing = Color(0xFFB88E3E)
)

// 3. Olive Vinyl (Light — Natural Stone & Forest Sage)
val MatrixPaletteLight = AuraPalette(
    background = Color(0xFFF5F7F5),
    backgroundDark = Color(0xFFFFFFFF),
    surface = Color(0xFFECF1EC),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceActive = Color(0xFFDFE7DF),
    cyan = Color(0xFF4A7C4F),
    cyanGlow = Color(0x224A7C4F),
    neonPurple = Color(0xFF3E6942),
    cyberPink = Color(0xFFC75A45),
    cyberGreen = Color(0xFF4A7C4F),
    cyberAmber = Color(0xFFC2903E),
    cyberRed = Color(0xFFD94A4A),
    glassBorder = Color(0x18000000),
    glassBorderSubtle = Color(0x0A000000),
    textPrimary = Color(0xFF131914),
    textSecondary = Color(0xFF536355),
    textMuted = Color(0xFF829484),
    vinylBody = Color(0xFF1D241E),
    vinylGroove = Color(0xFF29332A),
    vinylCenterRing = Color(0xFF4A7C4F)
)

// 4. Leica Monochrome (Light — Stark Minimalist Silver & Red Dot)
val OledPaletteLight = AuraPalette(
    background = Color(0xFFFAFAFA),
    backgroundDark = Color(0xFFFFFFFF),
    surface = Color(0xFFEEEEEE),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceActive = Color(0xFFE0E0E0),
    cyan = Color(0xFF1A1A1A),
    cyanGlow = Color(0x1A000000),
    neonPurple = Color(0xFF333333),
    cyberPink = Color(0xFFD32F2F),
    cyberGreen = Color(0xFF2E7D32),
    cyberAmber = Color(0xFFB8860B),
    cyberRed = Color(0xFFD32F2F),
    glassBorder = Color(0x1A000000),
    glassBorderSubtle = Color(0x0E000000),
    textPrimary = Color(0xFF0A0A0A),
    textSecondary = Color(0xFF555555),
    textMuted = Color(0xFF888888),
    vinylBody = Color(0xFF161616),
    vinylGroove = Color(0xFF252525),
    vinylCenterRing = Color(0xFFD32F2F)
)

val LocalAuraPalette = compositionLocalOf { CyberNeonPalette }

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
