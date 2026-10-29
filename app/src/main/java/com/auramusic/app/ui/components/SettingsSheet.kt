package com.auramusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.locale.AuraStrings
import com.auramusic.app.ui.theme.CyberBackground
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
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
    currentTheme: String = "Cyber Neon",
    appLanguage: String = "Español",
    lyricsOffsetMs: Long = -200L,
    onQualitySelected: (String) -> Unit,
    onToggleLoudness: () -> Unit,
    onToggleAutoRadio: () -> Unit = {},
    onToggleSkipSilence: () -> Unit = {},
    onSelectTheme: (String) -> Unit = {},
    onSelectLanguage: (String) -> Unit = {},
    onSelectLyricsOffset: (Long) -> Unit = {},
    onClearCache: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
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
            // Subtle Top Drag Pill
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextMuted.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Clean Editorial Header
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
                        fontSize = 24.sp,
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AuraStrings.get("settings_subtitle", appLanguage),
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceCard)
                        .border(1.dp, GlassBorder, CircleShape)
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

            // 1. LANGUAGE SELECTOR (Clean 2x3 Editorial Grid)
            EditorialGroupHeader(title = AuraStrings.get("language_title", appLanguage))

            val languages = listOf(
                "Español" to "ES",
                "English" to "EN",
                "Português" to "PT",
                "Français" to "FR",
                "Deutsch" to "DE",
                "日本語" to "JA"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (lang, code) ->
                            val isSelected = appLanguage.startsWith(lang, ignoreCase = true)
                            LanguageOptionPill(
                                label = lang,
                                code = code,
                                isSelected = isSelected,
                                onClick = { onSelectLanguage(lang) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. APPEARANCE / STUDIO THEMES
            EditorialGroupHeader(title = AuraStrings.get("theme_title", appLanguage))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeSwatchCard(
                    title = "Obsidian",
                    color1 = Color(0xFF2DD4BF),
                    color2 = Color(0xFF161922),
                    isSelected = currentTheme == "Cyber Neon",
                    onClick = { onSelectTheme("Cyber Neon") },
                    modifier = Modifier.weight(1f)
                )
                ThemeSwatchCard(
                    title = "Synthwave",
                    color1 = Color(0xFFF59E0B),
                    color2 = Color(0xFF2A203B),
                    isSelected = currentTheme == "Synthwave",
                    onClick = { onSelectTheme("Synthwave") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeSwatchCard(
                    title = "Emerald",
                    color1 = Color(0xFF10B981),
                    color2 = Color(0xFF131D17),
                    isSelected = currentTheme == "Matrix 99",
                    onClick = { onSelectTheme("Matrix 99") },
                    modifier = Modifier.weight(1f)
                )
                ThemeSwatchCard(
                    title = "OLED Black",
                    color1 = Color(0xFFFFFFFF),
                    color2 = Color(0xFF000000),
                    isSelected = currentTheme == "OLED Black",
                    onClick = { onSelectTheme("OLED Black") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. STREAMING QUALITY (Single Grouped Surface Card)
            EditorialGroupHeader(title = AuraStrings.get("audio_quality", appLanguage))

            val qualityOptions = listOf(
                Triple(
                    "Hi-Res (320 kbps)",
                    AuraStrings.get("quality_high_title", appLanguage),
                    AuraStrings.get("quality_high_desc", appLanguage)
                ),
                Triple(
                    "Estándar (160 kbps)",
                    AuraStrings.get("quality_std_title", appLanguage),
                    AuraStrings.get("quality_std_desc", appLanguage)
                ),
                Triple(
                    "Ahorro de datos (96 kbps)",
                    AuraStrings.get("quality_low_title", appLanguage),
                    AuraStrings.get("quality_low_desc", appLanguage)
                )
            )

            GroupedSurfaceCard {
                qualityOptions.forEachIndexed { index, (internalKey, displayTitle, displayDesc) ->
                    val isSelected = audioQuality == internalKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQualitySelected(internalKey) }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayTitle,
                                color = if (isSelected) CyberCyan else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 14.sp,
                                letterSpacing = (-0.1).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = displayDesc,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CyberCyan else Color.Transparent)
                                .border(
                                    1.5.dp,
                                    if (isSelected) CyberCyan else TextMuted.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CyberBackgroundDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    if (index < qualityOptions.lastIndex) {
                        HorizontalDivider(
                            color = GlassBorderSubtle,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. SMART PLAYBACK (Single Grouped Surface Card with Switches)
            EditorialGroupHeader(title = AuraStrings.get("playback_section", appLanguage))

            GroupedSurfaceCard {
                SettingsToggleRow(
                    title = AuraStrings.get("normalizer_title", appLanguage),
                    subtitle = AuraStrings.get("normalizer_desc", appLanguage),
                    checked = loudnessNormalizer,
                    onCheckedChange = { onToggleLoudness() }
                )
                HorizontalDivider(
                    color = GlassBorderSubtle,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsToggleRow(
                    title = AuraStrings.get("autoradio_title", appLanguage),
                    subtitle = AuraStrings.get("autoradio_desc", appLanguage),
                    checked = autoRadioEnabled,
                    onCheckedChange = { onToggleAutoRadio() }
                )
                HorizontalDivider(
                    color = GlassBorderSubtle,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsToggleRow(
                    title = AuraStrings.get("skipsilence_title", appLanguage),
                    subtitle = AuraStrings.get("skipsilence_desc", appLanguage),
                    checked = skipSilenceEnabled,
                    onCheckedChange = { onToggleSkipSilence() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. LYRICS LATENCY COMPENSATION (Clean Segmented Control in 1 Card)
            EditorialGroupHeader(title = AuraStrings.get("lyrics_sync_title", appLanguage))

            GroupedSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AuraStrings.get("lyrics_sync_desc", appLanguage),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val offsets = listOf(-400L, -200L, 0L, 200L)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberBackgroundDark)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        offsets.forEach { offset ->
                            val isSelected = lyricsOffsetMs == offset
                            val label = if (offset > 0) "+${offset}ms" else "${offset}ms"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSelected) CyberSurfaceActive else Color.Transparent)
                                    .clickable { onSelectLyricsOffset(offset) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) CyberCyan else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. STORAGE & BACKUP (Single Grouped Surface Card)
            EditorialGroupHeader(title = AuraStrings.get("storage_section", appLanguage))

            GroupedSurfaceCard {
                // Clear Cache Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AuraStrings.get("clean_cache_title", appLanguage),
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AuraStrings.get("clean_cache_desc", appLanguage),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ActionPillButton(
                        text = AuraStrings.get("clean_cache_btn", appLanguage),
                        onClick = onClearCache
                    )
                }

                HorizontalDivider(
                    color = GlassBorderSubtle,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Backup Export & Restore Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AuraStrings.get("backup_title", appLanguage),
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AuraStrings.get("backup_desc", appLanguage),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ActionPillButton(
                            text = AuraStrings.get("export_btn", appLanguage),
                            onClick = onExportBackup
                        )
                        ActionPillButton(
                            text = AuraStrings.get("restore_btn", appLanguage),
                            isPrimary = true,
                            onClick = onImportBackup
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. ABOUT AURA (Refined Editorial Footer Card)
            EditorialGroupHeader(title = AuraStrings.get("about_title", appLanguage))

            GroupedSurfaceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AuraIcon(size = 34.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Aura Music",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = AuraStrings.get("app_version", appLanguage),
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            text = "2.0.0",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AuraStrings.get("creator_label", appLanguage),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = AuraStrings.get("creator_name", appLanguage),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = AuraStrings.get("about_desc", appLanguage),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = AuraStrings.get("open_source", appLanguage),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EditorialGroupHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun GroupedSurfaceCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = (-0.1).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberBackgroundDark,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CyberSurfaceActive,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ActionPillButton(
    text: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPrimary) CyberCyan else CyberSurfaceActive)
            .border(
                1.dp,
                if (isPrimary) CyberCyan else GlassBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isPrimary) CyberBackgroundDark else TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun LanguageOptionPill(
    label: String,
    code: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyberSurfaceActive else CyberSurfaceCard)
            .border(
                1.dp,
                if (isSelected) CyberCyan else GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = code,
            color = if (isSelected) CyberCyan else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ThemeSwatchCard(
    title: String,
    color1: Color,
    color2: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) CyberSurfaceActive else CyberSurfaceCard)
            .border(
                1.dp,
                if (isSelected) CyberCyan else GlassBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color1)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
