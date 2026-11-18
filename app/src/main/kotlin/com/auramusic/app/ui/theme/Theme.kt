package com.auramusic.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.auramusic.app.data.models.AccentColor
import com.auramusic.app.data.models.AppTheme

fun AccentColor.toColor(isDark: Boolean = false): Color = when (this) {
    AccentColor.VIOLET -> AuraViolet
    AccentColor.GOLD -> AuraGold
    AccentColor.CYAN -> AuraCyan
    AccentColor.BLUE -> AuraBlue
    AccentColor.TEAL -> AuraTeal
    AccentColor.ORANGE -> AuraOrange
    AccentColor.CRIMSON -> AuraCrimson
    AccentColor.SAKURA -> AuraSakura
    AccentColor.LAVENDER -> AuraLavender
    AccentColor.LIME -> AuraLime
    AccentColor.COPPER -> AuraCopper
    AccentColor.GRAPHITE -> if (isDark) AuraGraphiteDark else AuraGraphiteLight
}

@Composable
fun AuraMusicTheme(
    appTheme: AppTheme = AppTheme.LIGHT,
    accentColor: AccentColor = AccentColor.VIOLET,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.AUTO -> systemDark
        else -> true
    }

    val primaryColor = accentColor.toColor(isDark = isDark)
    val onPrimaryColor = when (accentColor) {
        AccentColor.GOLD, AccentColor.LIME -> Color(0xFF0A0D12)
        AccentColor.GRAPHITE -> if (isDark) Color(0xFF0A0D12) else Color.White
        else -> Color.White
    }

    val (bg, surf, surfSubtle) = when (appTheme) {
        AppTheme.LIGHT -> Triple(LightBackground, LightSurface, LightSurfaceSubtle)
        AppTheme.AMOLED -> Triple(AmoledBackground, AmoledSurface, AmoledSurfaceSubtle)
        AppTheme.MIDNIGHT_VIOLET -> Triple(MidnightVioletBg, MidnightVioletSurface, MidnightVioletSubtle)
        AppTheme.CYBER_CARBON -> Triple(CyberCarbonBg, CyberCarbonSurface, CyberCarbonSubtle)
        AppTheme.WARM_ESPRESSO -> Triple(WarmEspressoBg, WarmEspressoSurface, WarmEspressoSubtle)
        AppTheme.DARK -> Triple(DarkBackground, DarkSurface, DarkSurfaceSubtle)
        AppTheme.AUTO -> if (systemDark) {
            Triple(DarkBackground, DarkSurface, DarkSurfaceSubtle)
        } else {
            Triple(LightBackground, LightSurface, LightSurfaceSubtle)
        }
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = primaryColor,
            background = bg,
            onBackground = DarkTextPrimary,
            surface = surf,
            onSurface = DarkTextPrimary,
            surfaceVariant = surfSubtle,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkBorder
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = primaryColor,
            background = bg,
            onBackground = LightTextPrimary,
            surface = surf,
            onSurface = LightTextPrimary,
            surfaceVariant = surfSubtle,
            onSurfaceVariant = LightTextSecondary,
            outline = LightBorder
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = bg.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = bg.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            val insetsController = WindowCompat.getInsetsController(window, view)
            // En temas oscuros (isDark = true), los iconos de hora/batería/wifi y botones inferiores salen en BLANCO nítido
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            content()
        }
    }
}
