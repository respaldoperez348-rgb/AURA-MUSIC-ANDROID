package com.auramusic.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.models.AccentColor
import com.auramusic.app.data.models.AppTheme
import com.auramusic.app.data.models.AudioQualityOption
import com.auramusic.app.data.models.UserSettings
import com.auramusic.app.data.repository.UserProfile
import com.auramusic.app.ui.components.AuraAppLogoBadge
import com.auramusic.app.ui.components.AuraCover
import com.auramusic.app.ui.theme.toColor

enum class SettingsSubscreen {
    MAIN,
    THEME,
    ACCENT_COLOR,
    AUDIO_QUALITY,
    NOTIFICATIONS,
    ABOUT,
    ABOUT_US,
    APP_ICON
}

data class AppIconStyle(
    val id: String,
    val title: String,
    val subtitle: String,
)

@Composable
fun SettingsScreen(
    currentUser: UserProfile?,
    currentSettings: UserSettings,
    selectedAppIcon: String,
    onAppIconSelected: (String) -> Unit,
    onSettingsChanged: (UserSettings) -> Unit,
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentSubscreen by remember { mutableStateOf(SettingsSubscreen.MAIN) }

    when (currentSubscreen) {
        SettingsSubscreen.MAIN -> {
            SettingsMainView(
                currentUser = currentUser,
                settings = currentSettings,
                selectedAppIcon = selectedAppIcon,
                onBackClick = onBackClick,
                onAccountClick = onAccountClick,
                onNavigate = { currentSubscreen = it },
                onToggleContinuous = {
                    onSettingsChanged(currentSettings.copy(continuousPlayback = it))
                },
                modifier = modifier
            )
        }
        SettingsSubscreen.THEME -> {
            ThemeSelectorView(
                currentTheme = currentSettings.theme,
                onThemeSelected = {
                    onSettingsChanged(currentSettings.copy(theme = it))
                },
                onBack = { currentSubscreen = SettingsSubscreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubscreen.ACCENT_COLOR -> {
            AccentColorSelectorView(
                currentAccent = currentSettings.accentColor,
                onAccentSelected = {
                    onSettingsChanged(currentSettings.copy(accentColor = it))
                },
                onBack = { currentSubscreen = SettingsSubscreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubscreen.AUDIO_QUALITY -> {
            AudioQualitySelectorView(
                currentQuality = currentSettings.audioQuality,
                onQualitySelected = {
                    onSettingsChanged(currentSettings.copy(audioQuality = it))
                },
                onBack = { currentSubscreen = SettingsSubscreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubscreen.NOTIFICATIONS -> {
            NotificationsSettingsView(
                settings = currentSettings,
                onSettingsChanged = onSettingsChanged,
                onBack = { currentSubscreen = SettingsSubscreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubscreen.ABOUT -> {
            AboutScreenView(
                selectedAppIcon = selectedAppIcon,
                onBack = { currentSubscreen = SettingsSubscreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubscreen.ABOUT_US -> {
            AboutUsScreenView(
                selectedAppIcon = selectedAppIcon,
                onBack = { currentSubscreen = SettingsSubscreen.MAIN },
                modifier = modifier
            )
        }
        SettingsSubscreen.APP_ICON -> {
            AppIconSelectorView(
                currentIcon = selectedAppIcon,
                onIconSelected = onAppIconSelected,
                onBack = { currentSubscreen = SettingsSubscreen.MAIN },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SettingsMainView(
    currentUser: UserProfile?,
    settings: UserSettings,
    selectedAppIcon: String,
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    onNavigate: (SettingsSubscreen) -> Unit,
    onToggleContinuous: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Configuración",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Sección CUENTA
            SettingsSectionHeader("CUENTA")
            SettingsCard(
                title = if (currentUser != null) currentUser.name else "Tu cuenta",
                subtitle = if (currentUser != null) currentUser.email else "Conectar cuenta de Google o registrarte",
                onClick = onAccountClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección APARIENCIA
            SettingsSectionHeader("APARIENCIA")
            SettingsCard(
                title = "Tema de la aplicación",
                subtitle = settings.theme.title,
                onClick = { onNavigate(SettingsSubscreen.THEME) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard(
                title = "Color de acento",
                subtitle = settings.accentColor.displayName,
                trailingCircleColor = MaterialTheme.colorScheme.primary,
                onClick = { onNavigate(SettingsSubscreen.ACCENT_COLOR) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard(
                title = "Icono de la app",
                subtitle = "Estilo actual: $selectedAppIcon",
                onClick = { onNavigate(SettingsSubscreen.APP_ICON) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección REPRODUCCIÓN
            SettingsSectionHeader("REPRODUCCIÓN")
            SettingsCard(
                title = "Calidad de audio",
                subtitle = when (settings.audioQuality) {
                    AudioQualityOption.AUTO -> "Automática"
                    AudioQualityOption.STANDARD -> "Estándar"
                    AudioQualityOption.HIGH -> "Alta (Máxima fidelidad)"
                },
                onClick = { onNavigate(SettingsSubscreen.AUDIO_QUALITY) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsToggleCard(
                title = "Reproducción continua (Auto-Radio)",
                checked = settings.continuousPlayback,
                onCheckedChange = onToggleContinuous
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección OTROS
            SettingsSectionHeader("OTROS")
            SettingsCard(
                title = "Notificaciones",
                subtitle = if (settings.playbackNotifications) "Activadas" else "Desactivadas",
                onClick = { onNavigate(SettingsSubscreen.NOTIFICATIONS) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard(
                title = "Sobre nosotros",
                subtitle = "Nuestra misión y tecnología",
                onClick = { onNavigate(SettingsSubscreen.ABOUT_US) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard(
                title = "Acerca de AURA Music",
                subtitle = "Versión v3.0.0",
                onClick = { onNavigate(SettingsSubscreen.ABOUT) }
            )
        }
    }
}

@Composable
private fun AboutUsScreenView(
    selectedAppIcon: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repoUrl = "https://github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID"

    val openRepo = {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))
            context.startActivity(intent)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SubscreenHeader(title = "Sobre nosotros", onBack = onBack)
        }

        // Tarjeta Principal: Logo + AURA Music + [v3.0.0] [UNIVERSAL]
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraAppLogoBadge(iconStyle = selectedAppIcon, size = 64.dp)

                    Spacer(modifier = Modifier.width(18.dp))

                    Column {
                        Text(
                            text = "AURA Music",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "3.0.0",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "UNIVERSAL",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sección: Comunidad e información
        item {
            Text(
                text = "Comunidad e información",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, top = 6.dp)
            )
        }

        // Tarjeta del Creador: David Tineo (Made by)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { openRepo() },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "David Tineo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "David Tineo",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 21.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Made by • Creador",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "GitHub",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Tarjeta de Enlaces: Ver repositorio, Arquitectura y Licencia
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // 1. Ver repositorio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openRepo() }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Ver repositorio",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "respaldoperez348-rgb/AURA-MUSIC-ANDROID",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // 2. Motor y Arquitectura
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openRepo() }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "AURA Audio Engine",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "AURA Music está construido con AndroidX Media3 & Innertube bajo licencia GPL-3.0",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // 3. GNU Licencia Pública General v3.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openRepo() }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "GNU Licencia Pública General v3.0",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Gratis, de código abierto. Puedes usarlo, estudiarlo, compartirlo y mejorarlo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIconSelectorView(
    currentIcon: String,
    onIconSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icons = listOf(
        AppIconStyle("VIOLET", "AURA Violeta (Clásico)", "Fondo violeta con monograma blanco"),
        AppIconStyle("BLACK", "Midnight Black (Estudio)", "Fondo negro carbono con anillo claro"),
        AppIconStyle("WHITE", "Snow White (Luminoso)", "Fondo blanco con monograma violeta"),
        AppIconStyle("CYAN", "Electric Cyan (Neón)", "Fondo esmeralda cian con anillo claro"),
        AppIconStyle("ORANGE", "Sunset Orange (Cálido)", "Fondo naranja intenso con anillo claro"),
        AppIconStyle("GOLD", "Oro Imperial (Edición Especial)", "Fondo dorado con monograma oscuro")
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        item {
            SubscreenHeader(title = "Icono de la app", onBack = onBack)
            Text(
                text = "Cambia el icono de AURA Music tanto dentro de la aplicación como en la pantalla de inicio de tu teléfono.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 18.dp)
            )
        }

        items(icons) { iconStyle ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onIconSelected(iconStyle.id) },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AuraAppLogoBadge(
                            iconStyle = iconStyle.id,
                            size = 52.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = iconStyle.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = iconStyle.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    RadioButton(
                        selected = currentIcon.equals(iconStyle.id, ignoreCase = true),
                        onClick = { onIconSelected(iconStyle.id) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectorView(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themes = AppTheme.values().toList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SubscreenHeader(title = "Tema", onBack = onBack)

            Text(
                text = "Elige la atmósfera visual de AURA Music",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(themes) { themeItem ->
            val previewBg = when (themeItem) {
                AppTheme.LIGHT -> Color(0xFFF5F5F7)
                AppTheme.DARK -> Color(0xFF080B11)
                AppTheme.AMOLED -> Color(0xFF000000)
                AppTheme.MIDNIGHT_VIOLET -> Color(0xFF17112A)
                AppTheme.CYBER_CARBON -> Color(0xFF0E1D26)
                AppTheme.WARM_ESPRESSO -> Color(0xFF1C1613)
                AppTheme.AUTO -> Color(0xFF334155)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onThemeSelected(themeItem) },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(previewBg)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = themeItem.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = themeItem.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RadioButton(
                        selected = currentTheme == themeItem,
                        onClick = { onThemeSelected(themeItem) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentColorSelectorView(
    currentAccent: AccentColor,
    onAccentSelected: (AccentColor) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AccentColor.values().toList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        item {
            SubscreenHeader(title = "Color de acento", onBack = onBack)

            Text(
                text = "Personaliza los botones, ondas e indicadores de la interfaz (12 paletas)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                colors.chunked(3).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowColors.forEach { colorItem ->
                            ColorCircleOption(
                                color = Color(colorItem.hex),
                                label = colorItem.displayName,
                                isSelected = currentAccent == colorItem,
                                onClick = { onAccentSelected(colorItem) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Vista previa",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraCover(
                        thumbnailUrl = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
                        id = "4NRXx6U8ABQ",
                        title = "Blinding Lights",
                        artist = "The Weeknd",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blinding Lights",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "The Weeknd",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioQualitySelectorView(
    currentQuality: AudioQualityOption,
    onQualitySelected: (AudioQualityOption) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        SubscreenHeader(title = "Calidad de audio", onBack = onBack)

        Text(
            text = "Selecciona la calidad de reproducción",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        OptionSelectCard(
            title = "Automática",
            subtitle = "Se adapta a tu conexión",
            isSelected = currentQuality == AudioQualityOption.AUTO,
            onClick = { onQualitySelected(AudioQualityOption.AUTO) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OptionSelectCard(
            title = "Estándar",
            subtitle = "Menor consumo de datos",
            isSelected = currentQuality == AudioQualityOption.STANDARD,
            onClick = { onQualitySelected(AudioQualityOption.STANDARD) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OptionSelectCard(
            title = "Alta",
            subtitle = "Mayor fidelidad cuando esté disponible",
            isSelected = currentQuality == AudioQualityOption.HIGH,
            onClick = { onQualitySelected(AudioQualityOption.HIGH) }
        )
    }
}

@Composable
private fun NotificationsSettingsView(
    settings: UserSettings,
    onSettingsChanged: (UserSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        SubscreenHeader(title = "Notificaciones", onBack = onBack)

        Text(
            text = "Configura los avisos de la aplicación",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        SettingsToggleCard(
            title = "Controles de reproducción",
            checked = settings.playbackNotifications,
            onCheckedChange = { onSettingsChanged(settings.copy(playbackNotifications = it)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggleCard(
            title = "Novedades musicales",
            checked = settings.newMusicNotifications,
            onCheckedChange = { onSettingsChanged(settings.copy(newMusicNotifications = it)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        SettingsToggleCard(
            title = "Actualizaciones de la app",
            checked = settings.appUpdateNotifications,
            onCheckedChange = { onSettingsChanged(settings.copy(appUpdateNotifications = it)) }
        )
    }
}

@Composable
private fun AboutScreenView(
    selectedAppIcon: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubscreenHeader(title = "Acerca de AURA Music", onBack = onBack)

        Spacer(modifier = Modifier.height(30.dp))

        AuraAppLogoBadge(iconStyle = selectedAppIcon, size = 82.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AURA Music",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Reproductor musical",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(36.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Creador", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("David Tineo", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)

                Spacer(modifier = Modifier.height(14.dp))

                Text("Versión", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("v3.0.0 (UNIVERSAL)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(14.dp))

                Text("Repositorio oficial", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)

                Spacer(modifier = Modifier.height(14.dp))

                Text("Diseño", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("7 Temas • 12 Colores de Acento", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun SubscreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Atrás",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    trailingCircleColor: Color? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (trailingCircleColor != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(trailingCircleColor)
                )
            } else {
                Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun OptionSelectCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun ColorCircleOption(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
