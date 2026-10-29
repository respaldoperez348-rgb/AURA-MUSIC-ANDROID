package com.auramusic.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.TextPrimary

@Composable
fun AuraIcon(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    isAnimated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aura_platter_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "platter_angle"
    )

    val primaryColor = TextPrimary
    val accentColor = CyberCyan

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(CyberSurfaceCard)
            .padding(size * 0.16f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val stroke = w * 0.095f

            // Precision 260-degree platter arc (rotates gently when playing)
            rotate(degrees = if (isAnimated) angle else 0f) {
                drawArc(
                    color = primaryColor.copy(alpha = 0.92f),
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(w - stroke, h - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            // Architectural 'A' / Tonearm Monogram
            val aPath = Path().apply {
                moveTo(w * 0.28f, h * 0.80f)
                lineTo(w * 0.50f, h * 0.24f)
                lineTo(w * 0.72f, h * 0.80f)
            }
            drawPath(
                path = aPath,
                color = primaryColor,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Crossbar
            drawLine(
                color = primaryColor,
                start = Offset(w * 0.36f, h * 0.62f),
                end = Offset(w * 0.64f, h * 0.62f),
                strokeWidth = stroke * 0.85f,
                cap = StrokeCap.Round
            )

            // Industrial Vermilion / Accent Stylus Pivot Dot
            drawCircle(
                color = accentColor,
                radius = w * 0.085f,
                center = Offset(w * 0.50f, h * 0.44f)
            )
        }
    }
}
