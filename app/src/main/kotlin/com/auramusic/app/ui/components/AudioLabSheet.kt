package com.auramusic.app.ui.components

import android.media.audiofx.PresetReverb
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.i18n.LocalAuraStrings
import com.auramusic.app.playback.AudioLabController
import com.auramusic.app.playback.EqPreset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioLabBottomSheet(
    audioLab: AudioLabController,
    onSetSpeedAndPitch: (Float, Float) -> Unit,
    onStartSleepTimer: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAuraStrings.current
    val state by audioLab.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.audioLab,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = state.eqEnabled,
                    onCheckedChange = { audioLab.setEqEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets del Ecualizador
            Text(
                text = strings.equalizer,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EqPreset.entries.forEach { preset ->
                    val selected = state.eqEnabled && state.selectedPreset == preset
                    FilterChip(
                        selected = selected,
                        onClick = { audioLab.applyPreset(preset) },
                        label = { Text(preset.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bandas de 5 frecuencias
            val minF = state.minBandLevel.toFloat()
            val maxF = state.maxBandLevel.toFloat().coerceAtLeast(minF + 100f)
            state.bandLevels.forEachIndexed { idx, level ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.bandFreqLabels.getOrNull(idx) ?: "B${idx + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(54.dp)
                    )
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { audioLab.setBandLevel(idx, it.roundToInt().toShort()) },
                        valueRange = minF..maxF,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "${level / 100} dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // BassBoost & Reverb
            Text(
                text = "BassBoost (${state.bassBoostStrength / 10}%)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = state.bassBoostStrength.toFloat(),
                onValueChange = { audioLab.setBassBoost(it.roundToInt()) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            val reverbOptions = listOf(
                PresetReverb.PRESET_NONE to "Sin Reverb",
                PresetReverb.PRESET_SMALLROOM to "Sala",
                PresetReverb.PRESET_LARGEHALL to "Estadio",
                PresetReverb.PRESET_PLATE to "Estudio"
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                reverbOptions.forEach { (presetId, label) ->
                    FilterChip(
                        selected = state.reverbPreset == presetId,
                        onClick = { audioLab.setReverbPreset(presetId) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Velocidad y Tono
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.speedAndPitch} (${String.format("%.2fx", state.playbackSpeed)})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        onSetSpeedAndPitch(0.84f, 0.88f)
                        audioLab.setReverbPreset(PresetReverb.PRESET_LARGEHALL)
                    }) {
                        Text("Slowed+Reverb", fontSize = 11.sp)
                    }
                    TextButton(onClick = {
                        onSetSpeedAndPitch(1.0f, 1.0f)
                        audioLab.setReverbPreset(PresetReverb.PRESET_NONE)
                    }) {
                        Text("Reset", fontSize = 11.sp)
                    }
                }
            }

            Slider(
                value = state.playbackSpeed,
                onValueChange = { onSetSpeedAndPitch(it, state.playbackPitch) },
                valueRange = 0.6f..1.6f
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Temporizador de Sueño (Sleep Timer)
            val sleepLabel = when {
                state.sleepTimerStopAfterSong -> "${strings.sleepTimer}: Al terminar"
                state.sleepTimerRemainingMs > 0L -> {
                    val mins = (state.sleepTimerRemainingMs / 60_000L) + 1
                    "${strings.sleepTimer}: ${mins} min"
                }
                else -> strings.sleepTimer
            }
            Text(
                text = sleepLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0 to "Off", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "60 min").forEach { (mins, lbl) ->
                    val isSelected = if (mins == 0) {
                        state.sleepTimerRemainingMs == 0L && !state.sleepTimerStopAfterSong
                    } else false
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onStartSleepTimer(mins, false) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStartSleepTimer(0, true) },
                    color = if (state.sleepTimerStopAfterSong) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Fin de canción",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
