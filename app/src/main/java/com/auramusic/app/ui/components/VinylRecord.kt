package com.auramusic.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberCyanGlow
import com.auramusic.app.ui.theme.NeonPurple
import com.auramusic.app.ui.theme.VinylBody
import com.auramusic.app.ui.theme.VinylCenterRing
import com.auramusic.app.ui.theme.VinylGroove

@Composable
fun VinylRecord(
    isPlaying: Boolean,
    coverUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 16.dp, shape = CircleShape, spotColor = CyberCyanGlow)
            .rotate(currentRotation),
        contentAlignment = Alignment.Center
    ) {
        // Base Vinyl Body
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            val radius = size.toPx() / 2f

            // Vinyl outer body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(VinylBody, CyberBackgroundDark),
                    center = center,
                    radius = radius
                ),
                radius = radius
            )

            // Grooves
            val grooveCount = 8
            for (i in 1..grooveCount) {
                val r = radius * (0.45f + 0.5f * (i.toFloat() / grooveCount))
                drawCircle(
                    color = VinylGroove,
                    radius = r,
                    style = Stroke(width = 1.5f)
                )
            }

            // Neon outer edge accent
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(CyberCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.3f), CyberCyan.copy(alpha = 0.5f)),
                    center = center
                ),
                radius = radius - 2f,
                style = Stroke(width = 2.5f)
            )
        }

        // Center Album Art / Disc Label
        val centerSize = size * 0.42f
        Box(
            modifier = Modifier
                .size(centerSize)
                .clip(CircleShape)
                .background(VinylCenterRing)
                .border(2.dp, CyberCyan.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(centerSize * 0.5f)
                )
            }

            // Spindle Center Hole
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(CyberBackgroundDark)
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
        }
    }
}
