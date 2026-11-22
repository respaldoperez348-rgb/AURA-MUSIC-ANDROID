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
        "BLACK" -> Triple(Color(0xFF090B0F), Color(0xFFFFFFFF), Color(0xFFF5A623))
        "WHITE" -> Triple(Color(0xFFF8FAFC), Color(0xFF090B0F), Color(0xFFF5A623))
        "CYAN" -> Triple(Color(0xFF0D9488), Color(0xFFFFFFFF), Color(0xFF99F6E4))
        "ORANGE" -> Triple(Color(0xFFF97316), Color(0xFFFFFFFF), Color(0xFFFED7AA))
        "GOLD" -> Triple(Color(0xFF13161D), Color(0xFFF5A623), Color(0xFFF5A623))
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

/**
 * Monograma minimalista en trazo doble de AURA con ondas acústicas opcionales
 * para las cabeceras y la pantalla de Bienvenida.
 */
@Composable
fun AuraMonogramGlyph(
    color: Color,
    size: Dp = 28.dp,
    withRings: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        if (withRings) {
            listOf(0.98f to 0.14f, 0.80f to 0.24f, 0.62f to 0.42f).forEach { (factor, alpha) ->
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = (w * 0.5f) * factor,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        val scale = if (withRings) 0.48f else 0.90f
        val ox = cx - (w * scale) / 2f
        val oy = cy - (h * scale) / 2f
        val sw = w * scale
        val sh = h * scale
        val strokeW = (sw * 0.085f).coerceAtLeast(1.8.dp.toPx())

        val outerA = Path().apply {
            moveTo(ox + sw * 0.20f, oy + sh * 0.86f)
            lineTo(ox + sw * 0.50f, oy + sh * 0.14f)
            lineTo(ox + sw * 0.80f, oy + sh * 0.86f)
        }
        drawPath(
            path = outerA,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawArc(
            color = color.copy(alpha = 0.78f),
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(ox + sw * 0.31f, oy + sh * 0.50f),
            size = Size(sw * 0.38f, sh * 0.24f),
            style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round)
        )
    }
}
