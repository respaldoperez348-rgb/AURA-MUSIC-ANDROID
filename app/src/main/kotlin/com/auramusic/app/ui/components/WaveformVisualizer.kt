package com.auramusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun WaveformVisualizer(
    progress: Float, // 0.0 a 1.0
    modifier: Modifier = Modifier,
    barCount: Int = 40,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onSeek: ((Float) -> Unit)? = null,
) {
    // Alturas predefinidas proporcionales para simular la onda acústica de la canción
    val sampleHeights = floatArrayOf(
        0.3f, 0.45f, 0.7f, 0.9f, 0.6f, 0.4f, 0.8f, 1.0f, 0.75f, 0.5f,
        0.85f, 0.95f, 0.65f, 0.4f, 0.3f, 0.55f, 0.8f, 0.9f, 0.6f, 0.75f,
        0.85f, 0.5f, 0.4f, 0.65f, 0.9f, 1.0f, 0.7f, 0.45f, 0.6f, 0.8f,
        0.95f, 0.65f, 0.4f, 0.7f, 0.85f, 0.6f, 0.45f, 0.35f, 0.5f, 0.3f
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(
                if (onSeek != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val tappedProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek(tappedProgress)
                        }
                    }
                } else Modifier
            )
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.65f
        val gap = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

        for (i in 0 until barCount) {
            val normalizedIndex = (i.toFloat() / barCount)
            val isPlayed = normalizedIndex <= progress
            val heightFactor = sampleHeights[i % sampleHeights.size]
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
    }
}
