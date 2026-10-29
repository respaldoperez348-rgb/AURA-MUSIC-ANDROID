package com.auramusic.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.GlassBorder

@Composable
fun AuraIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    isAnimated: Boolean = false
) {
    val barHeights = listOf(6f, 11f, 16f, 21f, 28f, 21f, 16f, 11f, 6f)
    val scale = size / 36.dp

    val infiniteTransition = rememberInfiniteTransition(label = "aura_icon_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp * scale))
            .background(Color(0xFF161922))
            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp * scale)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.2.dp * scale),
            verticalAlignment = Alignment.CenterVertically
        ) {
            barHeights.forEachIndexed { index, baseHeight ->
                val t = kotlin.math.abs(index - 4) / 4f
                val barColor = lerp(CyberCyan, Color.White.copy(alpha = 0.85f), 1f - t)
                val dynamicHeight = if (isAnimated) {
                    val factor = if (index % 2 == 0) pulse else (2f - pulse)
                    (baseHeight * factor).coerceIn(4f, 30f)
                } else {
                    baseHeight
                }

                Box(
                    modifier = Modifier
                        .width(2.2.dp * scale)
                        .height((dynamicHeight * scale).dp)
                        .clip(RoundedCornerShape(1.2.dp * scale))
                        .background(barColor)
                )
            }
        }
    }
}
