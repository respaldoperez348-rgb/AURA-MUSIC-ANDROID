package com.auramusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun WaveformVisualizer(
    progress: Float, // 0.0 a 1.0
    modifier: Modifier = Modifier,
    songSeed: String = "",
    barCount: Int = 44,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
    onSeek: ((Float) -> Unit)? = null,
) {
    // Genera una silueta acústica única y determinista por canción
    val sampleHeights = remember(songSeed, barCount) {
        val base = floatArrayOf(
            0.30f, 0.45f, 0.72f, 0.90f, 0.62f, 0.42f, 0.80f, 1.0f, 0.76f, 0.50f,
            0.86f, 0.95f, 0.66f, 0.40f, 0.32f, 0.58f, 0.82f, 0.92f, 0.60f, 0.75f,
            0.85f, 0.52f, 0.38f, 0.68f, 0.90f, 1.0f, 0.72f, 0.46f, 0.62f, 0.82f
        )
        val seedHash = abs(songSeed.hashCode())
        FloatArray(barCount) { i ->
            val offset = (seedHash + i * 7) % base.size
            val jitter = ((seedHash shr (i % 16)) and 0x7) / 40f
            (base[offset] * 0.85f + jitter).coerceIn(0.24f, 1.0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(
                if (onSeek != null) {
                    Modifier
                        .pointerInput(onSeek) {
                            detectTapGestures { offset ->
                                val tappedProgress = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                onSeek(tappedProgress)
                            }
                        }
                        .pointerInput(onSeek) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                val draggedProgress = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                onSeek(draggedProgress)
                            }
                        }
                } else Modifier
            )
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.60f
        val gap = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)
        val clampedProgress = progress.coerceIn(0f, 1f)

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount.coerceAtLeast(1)
            val isPlayed = normalizedIndex <= clampedProgress
            val heightFactor = sampleHeights[i]
            val barHeight = maxHeight * heightFactor
            val startX = i * (barWidth + gap)
            val startY = (maxHeight - barHeight) / 2f

            drawRoundRect(
                color = if (isPlayed) activeColor else inactiveColor,
                topLeft = Offset(startX, startY),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }

        // Cabezal luminoso AURA en la posición actual
        val headX = (clampedProgress * totalWidth).coerceIn(barWidth, totalWidth - barWidth)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.92f),
            topLeft = Offset(headX - (barWidth * 0.7f), 2.dp.toPx()),
            size = Size(barWidth * 1.4f, maxHeight - 4.dp.toPx()),
            cornerRadius = CornerRadius(barWidth, barWidth)
        )
    }
}
