package com.auramusic.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.auramusic.app.playback.AuraAudioEffects
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberPink

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 24
) {
    val eqBands by AuraAudioEffects.bands.collectAsState()
    val bassBoost by AuraAudioEffects.bassBoostStrength.collectAsState()
    val eqEnabled by AuraAudioEffects.isEnabled.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")

    // Animate phase offsets for waveform
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2*PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    val pink = CyberPink
    val cyan = CyberCyan

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val width = size.width
        val height = size.height
        val totalSpacing = width * 0.25f
        val barWidth = (width - totalSpacing) / barCount
        val gap = totalSpacing / (barCount - 1)

        val gradient = Brush.verticalGradient(
            colors = listOf(pink, cyan)
        )

        val bassMultiplier = if (eqEnabled) (1f + (bassBoost / 2200f)) else 1f

        for (i in 0 until barCount) {
            val factor = if (isPlaying) {
                val bandIdx = if (eqBands.isNotEmpty()) (i * eqBands.size) / barCount else 0
                val bandGainMb = if (eqEnabled && eqBands.isNotEmpty()) {
                    eqBands.getOrNull(bandIdx)?.levelMb?.toFloat() ?: 0f
                } else 0f
                val eqBoost = (1f + (bandGainMb / 2400f)).coerceIn(0.65f, 1.45f)
                val subBoost = if (i < barCount / 3) bassMultiplier else 1f

                // Multi-harmonic reactive wave coupled to active DSP EQ & BassBoost profile
                val wave1 = Math.sin(phase.toDouble() * 1.4 + i * 0.45).toFloat()
                val wave2 = Math.cos(phase.toDouble() * 2.3 - i * 0.7).toFloat() * 0.45f
                val combined = Math.abs(wave1 + wave2) * 0.7f * eqBoost * subBoost
                (0.16f + combined).coerceIn(0.15f, 1.0f)
            } else {
                0.08f
            }

            val barHeight = height * factor
            val x = i * (barWidth + gap)
            val y = height - barHeight

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
