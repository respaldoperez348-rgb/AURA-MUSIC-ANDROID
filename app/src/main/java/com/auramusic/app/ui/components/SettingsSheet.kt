package com.auramusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auramusic.app.data.locale.AuraStrings
import com.auramusic.app.network.YouTubeAccountState
import com.auramusic.app.ui.theme.CyberBackground
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberGreen
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.CyberSurface
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorderSubtle
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    audioQuality: String,
    loudnessNormalizer: Boolean,
    autoRadioEnabled: Boolean = true,
    skipSilenceEnabled: Boolean = false,
    currentTheme: String = "Studio Carbon",
    themeMode: String = "Oscuro",
    appLanguage: String = "Español",
    lyricsOffsetMs: Long = -200L,
    ytAccount: YouTubeAccountState = YouTubeAccountState(),
    onConnectYouTube: () -> Unit = {},
    onDisconnectYouTube: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onQualitySelected: (String) -> Unit,
    onToggleLoudness: () -> Unit,
    onToggleAutoRadio: () -> Unit = {},
    onToggleSkipSilence: () -> Unit = {},
    onSelectTheme: (String) -> Unit = {},
    onSelectThemeMode: (String) -> Unit = {},
    onSelectLanguage: (String) -> Unit = {},
    onSelectLyricsOffset: (Long) -> Unit = {},
    onClearCache: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    gaplessEnabled: Boolean = true,
    filterShortAudios: Boolean = true,
    downloadOnlyOnWifi: Boolean = false,
    pauseHistory: Boolean = false,
    onToggleGapless: () -> Unit = {},
    onToggleFilterShortAudios: () -> Unit = {},
    onToggleDownloadOnlyOnWifi: () -> Unit = {},
    onTogglePauseHistory: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onShowChangelog: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberBackground,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Subtle Top Drag Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Clean Senior Editorial Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AuraStrings.get("settings_title", appLanguage),
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 25.sp,
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AuraStrings.get("settings_subtitle", appLanguage),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceCard)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // 1. CUENTA DE YOUTUBE MUSIC / GOOGLE
            // ==========================================
            SettingsSectionLabel(text = AuraStrings.get("yt_account_section", appLanguage))
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .padding(16.dp)
            ) {
                if (ytAccount.isLoggedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurfaceCard),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ytAccount.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ytAccount.avatarUrl,
                                        contentDescription = ytAccount.accountName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ytAccount.accountName,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(CyberGreen)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ytAccount.accountHandle.ifBlank {
                                        AuraStrings.get("yt_connected_badge", appLanguage)
                                    },
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceCard)
                                .clickable { onDisconnectYouTube() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = AuraStrings.get("yt_disconnect_btn", appLanguage),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AuraStrings.get("yt_connect_title", appLanguage),
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = AuraStrings.get("yt_connect_desc", appLanguage),
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(TextPrimary)
                                .clickable { onConnectYouTube() }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = AuraStrings.get("yt_connect_btn", appLanguage),
                                color = CyberBackgroundDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 2. ACABADO VISUAL Y MODO DE TEMA
            // ==========================================
            SettingsSectionLabel(text = AuraStrings.get("theme_title", appLanguage))
            Spacer(modifier = Modifier.height(10.dp))

            // Selector de Modo de Tema (Oscuro / Claro / Sistema)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "Oscuro" to "🌙 Oscuro",
                    "Claro" to "☀️ Claro",
                    "Sistema" to "⚙️ Sistema"
                ).forEach { (modeKey, modeTitle) ->
                    val isSelected = themeMode.equals(modeKey, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberSurfaceActive else Color.Transparent)
                            .clickable { onSelectThemeMode(modeKey) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modeTitle,
                            color = if (isSelected) TextPrimary else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            data class HardwareFinish(
                val id: String,
                val legacyId: String,
                val label: String,
                val subtitle: String,
                val chassisColor: Color,
                val accentColor: Color
            )

            val finishes = listOf(
                HardwareFinish(
                    id = "Studio Carbon",
                    legacyId = "Cyber Neon",
                    label = "Studio Carbon",
                    subtitle = "Bermellón Analógico",
                    chassisColor = Color(0xFF141417),
                    accentColor = Color(0xFFE85D2A)
                ),
                HardwareFinish(
                    id = "Champagne Hi-Fi",
                    legacyId = "Synthwave",
                    label = "Champagne Hi-Fi",
                    subtitle = "Titanio & Latón",
                    chassisColor = Color(0xFF16181D),
                    accentColor = Color(0xFFD4AF6A)
                ),
                HardwareFinish(
                    id = "Olive Vinyl",
                    legacyId = "Matrix 99",
                    label = "Olive Vinyl",
                    subtitle = "Pizarra & Salvia",
                    chassisColor = Color(0xFF121714),
                    accentColor = Color(0xFF7FA982)
                ),
                HardwareFinish(
                    id = "Leica Monochrome",
                    legacyId = "OLED Black",
                    label = "Leica Monochrome",
                    subtitle = "OLED Negro Puro",
                    chassisColor = Color(0xFF050505),
                    accentColor = Color(0xFFFFFFFF)
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                finishes.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            val isSelected = currentTheme == item.id || currentTheme == item.legacyId
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) CyberSurfaceActive else CyberSurface)
                                    .clickable { onSelectTheme(item.id) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dual-tone physical swatch (Chassis + Accent dot)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(item.chassisColor)
                                        .border(1.dp, item.accentColor.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(item.accentColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = if (isSelected) CyberCyan else TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 3. IDIOMA DE LA INTERFAZ
            // ==========================================
            SettingsSectionLabel(text = AuraStrings.get("language_title", appLanguage))
            Spacer(modifier = Modifier.height(10.dp))

            val languages = listOf(
                "Español" to "ES",
                "English" to "EN",
                "Português" to "PT",
                "Français" to "FR",
                "Deutsch" to "DE",
                "日本語" to "JA"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.chunked(3).forEach { rowLangs ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowLangs.forEach { (langName, code) ->
                            val isSelected = appLanguage == langName
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) TextPrimary else CyberSurface)
                                    .clickable { onSelectLanguage(langName) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = code,
                                    color = if (isSelected) CyberBackgroundDark.copy(alpha = 0.65f) else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = langName,
                                    color = if (isSelected) CyberBackgroundDark else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 4. CADENA DE AUDIO, FIDELIDAD Y DSP
            // ==========================================
            SettingsSectionLabel(text = AuraStrings.get("audio_quality", appLanguage))
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
            ) {
                QualityOptionItem(
                    title = AuraStrings.get("quality_high_title", appLanguage),
                    subtitle = AuraStrings.get("quality_high_desc", appLanguage),
                    isSelected = audioQuality.contains("320"),
                    onClick = { onQualitySelected("Hi-Res (320 kbps)") }
                )
                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                QualityOptionItem(
                    title = AuraStrings.get("quality_std_title", appLanguage),
                    subtitle = AuraStrings.get("quality_std_desc", appLanguage),
                    isSelected = audioQuality.contains("160"),
                    onClick = { onQualitySelected("Estándar (160 kbps)") }
                )
                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                QualityOptionItem(
                    title = AuraStrings.get("quality_low_title", appLanguage),
                    subtitle = AuraStrings.get("quality_low_desc", appLanguage),
                    isSelected = audioQuality.contains("96"),
                    onClick = { onQualitySelected("Ahorro (96 kbps)") }
                )
                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                // Direct Equalizer DSP Launcher inside Audio Chain
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEqualizer() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AuraStrings.get("open_eq_title", appLanguage),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AuraStrings.get("open_eq_desc", appLanguage),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceCard)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AuraStrings.get("open_eq_btn", appLanguage),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 5. REPRODUCCIÓN INTELIGENTE
            // ==========================================
            SettingsSectionLabel(text = AuraStrings.get("playback_section", appLanguage))
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
            ) {
                SettingsToggleItem(
                    title = AuraStrings.get("normalizer_title", appLanguage),
                    subtitle = AuraStrings.get("normalizer_desc", appLanguage),
                    checked = loudnessNormalizer,
                    onToggle = onToggleLoudness
                )
                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggleItem(
                    title = AuraStrings.get("autoradio_title", appLanguage),
                    subtitle = AuraStrings.get("autoradio_desc", appLanguage),
                    checked = autoRadioEnabled,
                    onToggle = onToggleAutoRadio
                )
                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggleItem(
                    title = AuraStrings.get("skipsilence_title", appLanguage),
                    subtitle = AuraStrings.get("skipsilence_desc", appLanguage),
                    checked = skipSilenceEnabled,
                    onToggle = onToggleSkipSilence
                )
                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggleItem(
                    title = "Reproducción continua (Gapless)",
                    subtitle = "Transición fluida sin silencios ni pausas entre canciones",
                    checked = gaplessEnabled,
                    onToggle = onToggleGapless
                )
                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsToggleItem(
                    title = "Filtro de audios cortos & mensajería",
                    subtitle = "Ignora notas de voz (WhatsApp, Telegram) y audios menores a 30s",
                    checked = filterShortAudios,
                    onToggle = onToggleFilterShortAudios
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 6. CALIBRACIÓN DE LETRAS (LRC)
            // ==========================================
            SettingsSectionLabel(text = AuraStrings.get("lyrics_sync_title", appLanguage))
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AuraStrings.get("lyrics_sync_desc", appLanguage),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${if (lyricsOffsetMs > 0) "+" else ""}${lyricsOffsetMs} ms",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val steps = listOf(
                        "-100 ms" to (lyricsOffsetMs - 100L).coerceIn(-1500L, 1500L),
                        "0 ms" to 0L,
                        "+100 ms" to (lyricsOffsetMs + 100L).coerceIn(-1500L, 1500L)
                    )
                    steps.forEach { (label, targetValue) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceCard)
                                .clickable { onSelectLyricsOffset(targetValue) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 7. DATOS, CACHÉ Y COPIA DE SEGURIDAD
            // ==========================================
            SettingsSectionLabel(text = AuraStrings.get("storage_section", appLanguage))
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
            ) {
                SettingsToggleItem(
                    title = "Descargar solo con Wi-Fi",
                    subtitle = "Ahorra datos móviles al descargar música offline",
                    checked = downloadOnlyOnWifi,
                    onToggle = onToggleDownloadOnlyOnWifi
                )

                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AuraStrings.get("clean_cache_title", appLanguage),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AuraStrings.get("clean_cache_desc", appLanguage),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceCard)
                            .clickable { onClearCache() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = AuraStrings.get("clean_cache_btn", appLanguage),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = AuraStrings.get("backup_title", appLanguage),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AuraStrings.get("backup_desc", appLanguage),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceCard)
                                .clickable { onExportBackup() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AuraStrings.get("export_btn", appLanguage),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceCard)
                                .clickable { onImportBackup() }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AuraStrings.get("restore_btn", appLanguage),
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 8. PRIVACIDAD Y DATOS DE ESCUCHA
            // ==========================================
            SettingsSectionLabel(text = "PRIVACIDAD Y DATOS")
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
            ) {
                SettingsToggleItem(
                    title = "Pausar historial de reproducción",
                    subtitle = "No guardar canciones escuchadas ni actualizar estadísticas",
                    checked = pauseHistory,
                    onToggle = onTogglePauseHistory
                )

                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClearHistory() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Borrar historial de escucha",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Elimina el registro de reproducciones y resetea tus estadísticas",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceCard)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Borrar",
                            color = CyberPink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 9. COMUNIDAD Y CÓDIGO ABIERTO (GITHUB)
            // ==========================================
            SettingsSectionLabel(text = "COMUNIDAD Y CÓDIGO ABIERTO")
            Spacer(modifier = Modifier.height(10.dp))

            val currentAppVersion = com.auramusic.app.BuildConfig.VERSION_NAME
            val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
            val encodedDevice = java.net.URLEncoder.encode(deviceModel, "UTF-8")
            val reportIssueUrl = "https://github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID/issues/new?title=%5BBUG%5D+Reporte+de+error&body=%2A%2AVersi%C3%B3n+App%3A%2A%2A+$currentAppVersion%0A%2A%2ADispositivo%3A%2A%2A+$encodedDevice%0A%0A%2A%2ADescripci%C3%B3n+del+problema%3A%2A%2A%0A"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
            ) {
                // Ver código en GitHub
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenUrl("https://github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID") }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Repositorio en GitHub",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceCard)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Visitar ↗",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp)

                // Reportar un error
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenUrl(reportIssueUrl) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reportar un error",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Abre un ticket con versión ($currentAppVersion) y modelo prellenados",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "New Issue ↗",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp)

                // Buscar Actualizaciones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCheckForUpdates() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Buscar actualizaciones",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Comprobar si hay un APK más reciente en GitHub Releases",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceCard)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Comprobar",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp)

                // Novedades de la versión
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowChangelog() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Novedades de la versión",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Qué hay de nuevo en Aura Music v$currentAppVersion",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Ver",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // 10. VERIFICACIÓN E INTEGRIDAD DE LA APP
            // ==========================================
            SettingsSectionLabel(text = "ESTADO DEL SISTEMA")
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aura Music Estable v$currentAppVersion",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Arquitectura offline nativa con SQLite & ExoPlayer",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberGreen.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "✓ Verificada",
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // 11. ACERCA DE AURA
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Aura Music · Studio Edition",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${AuraStrings.get("creator_label", appLanguage)}: ${AuraStrings.get("creator_name", appLanguage)}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "v$currentAppVersion",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp
    )
}

@Composable
private fun QualityOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(CyberCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = CyberBackgroundDark,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberBackgroundDark,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CyberSurfaceCard,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
