package com.auramusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AuraAppLogoBadge(
    iconStyle: String = "VIOLET",
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val (bgColor, strokeColor, ringColor) = when (iconStyle.uppercase()) {
        "BLACK" -> Triple(Color(0xFF0F1117), Color(0xFFFFFFFF), Color(0xFFC4B5FD))
        "WHITE" -> Triple(Color(0xFFF8FAFC), Color(0xFF6D28D9), Color(0xFF7C3AED))
        "CYAN" -> Triple(Color(0xFF0D9488), Color(0xFFFFFFFF), Color(0xFF99F6E4))
        "ORANGE" -> Triple(Color(0xFFF97316), Color(0xFFFFFFFF), Color(0xFFFED7AA))
        "GOLD" -> Triple(Color(0xFFD4AF37), Color(0xFF0F1117), Color(0xFF1E1B4B))
        else -> Triple(Color(0xFF7C3AED), Color(0xFFFFFFFF), Color(0xFFDDD6FE))
    }

    val corner = (size.value * 0.28f).dp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = ringColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(corner)
            )
            .padding(size * 0.14f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = (w * 0.085f).coerceAtLeast(2.2f)

            // Anillo exterior (A)
            drawArc(
                color = ringColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(strokeW / 2f, strokeW / 2f),
                size = Size(w - strokeW, h - strokeW),
                style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round)
            )

            // Letra A geométrica en el centro
            val path = Path().apply {
                moveTo(w * 0.31f, h * 0.72f)
                lineTo(w * 0.50f, h * 0.26f)
                lineTo(w * 0.69f, h * 0.72f)
            }
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawLine(
                color = strokeColor,
                start = Offset(w * 0.37f, h * 0.57f),
                end = Offset(w * 0.63f, h * 0.57f),
                strokeWidth = strokeW * 0.9f,
                cap = StrokeCap.Round
            )
        }
    }
}
