package com.auramusic.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.auramusic.app.data.models.PlayerBackdropMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class CoverVisualData(
    val haloColor: Color,
    val secondaryColor: Color,
    val blurredBitmap: Bitmap? = null,
)

private val coverVisualCache = android.util.LruCache<String, CoverVisualData>(64)

@Composable
fun rememberCoverVisualData(
    songId: String,
    thumbnailUrl: String,
    fallbackColor: Color = MaterialTheme.colorScheme.primary,
    computeBlurBitmap: Boolean = true,
): CoverVisualData {
    val context = LocalContext.current
    val cacheKey = "$songId|$computeBlurBitmap"
    var visualData by remember(cacheKey) {
        val cached = coverVisualCache.get(cacheKey)
            ?: if (!computeBlurBitmap) coverVisualCache.get("$songId|true") else null
        mutableStateOf(
            cached ?: CoverVisualData(haloColor = fallbackColor, secondaryColor = fallbackColor)
        )
    }

    LaunchedEffect(cacheKey, thumbnailUrl) {
        if (thumbnailUrl.isBlank()) return@LaunchedEffect
        coverVisualCache.get(cacheKey)?.let {
            visualData = it
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(96, 96)
                    .allowHardware(false)
                    .build()
                val bitmap = context.imageLoader.execute(request).image?.toBitmap() ?: return@runCatching
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).maximumColorCount(8).generate()
                }
                val primarySwatch = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.dominantSwatch
                val secondarySwatch = palette.mutedSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.dominantSwatch

                val halo = primarySwatch?.rgb?.let { Color(it) } ?: fallbackColor
                val sec = secondarySwatch?.rgb?.let { Color(it) } ?: halo
                val blurred = if (computeBlurBitmap) {
                    withContext(Dispatchers.Default) { softBoxBlurBitmap(bitmap, targetWidth = 64, radius = 3) }
                } else null

                val computed = CoverVisualData(
                    haloColor = halo,
                    secondaryColor = sec,
                    blurredBitmap = blurred
                )
                coverVisualCache.put(cacheKey, computed)
                if (computeBlurBitmap) {
                    coverVisualCache.put("$songId|false", computed.copy(blurredBitmap = null))
                }
                visualData = computed
            }
        }
    }

    return visualData
}

/**
 * Fondo visual del reproductor con los 3 modos elegibles por el usuario:
 * - AURA_HALO (Identidad propia): fondo limpio + halo cromático respirando detrás de la carátula.
 * - SOFT_BLUR: carátula suavemente desenfocada en toda la pantalla con scrim legible.
 * - SOLID: fondo sólido limpio.
 */
@Composable
fun AuraPlayerBackdrop(
    mode: PlayerBackdropMode,
    visualData: CoverVisualData,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    haloCenterYFraction: Float = 0.34f,
) {
    val baseBg = MaterialTheme.colorScheme.background
    val isDark = baseBg.luminance() < 0.5f
    val animatedHalo by animateColorAsState(
        targetValue = visualData.haloColor,
        animationSpec = tween(durationMillis = 650),
        label = "auraHaloColor"
    )

    val effectivePulse = if (isPlaying) 1.03f else 0.94f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        when (mode) {
            PlayerBackdropMode.AURA_HALO -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val center = Offset(size.width * 0.5f, size.height * haloCenterYFraction)
                            val radius = (size.minDimension * 0.68f) * effectivePulse
                            val alphaCore = if (isDark) 0.42f else 0.24f
                            val alphaMid = if (isDark) 0.16f else 0.09f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0f to animatedHalo.copy(alpha = alphaCore),
                                        0.48f to animatedHalo.copy(alpha = alphaMid),
                                        1f to Color.Transparent
                                    ),
                                    center = center,
                                    radius = radius
                                ),
                                radius = radius,
                                center = center
                            )
                        }
                )
            }

            PlayerBackdropMode.SOFT_BLUR -> {
                val bmp = visualData.blurredBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color.Black.copy(alpha = 0.48f),
                                        Color.Black.copy(alpha = 0.58f),
                                        Color.Black.copy(alpha = 0.78f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.52f),
                                        Color.White.copy(alpha = 0.62f),
                                        Color.White.copy(alpha = 0.80f)
                                    )
                                }
                            )
                        )
                )
            }

            PlayerBackdropMode.SOLID -> {
                // Fondo limpio sin efectos
            }
        }
    }
}

/**
 * Baño ambiental sutil para el fondo del sistema/app (Inicio, Explore, Biblioteca, Buscador)
 * que cambia suavemente sus colores según la canción que se está reproduciendo.
 */
@Composable
fun AuraSystemAmbientBackground(
    haloColor: Color,
    secondaryColor: Color,
    isPlaying: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val baseBg = MaterialTheme.colorScheme.background
    val isDark = baseBg.luminance() < 0.5f
    val animPrimary by animateColorAsState(
        targetValue = if (enabled) haloColor else baseBg,
        animationSpec = tween(durationMillis = 700),
        label = "sysAmbientPrimary"
    )
    val animSecondary by animateColorAsState(
        targetValue = if (enabled) secondaryColor else baseBg,
        animationSpec = tween(durationMillis = 800),
        label = "sysAmbientSecondary"
    )
    val scale = if (isPlaying) 1.02f else 0.95f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
            .drawBehind {
                if (!enabled) return@drawBehind
                val topAlpha = if (isDark) 0.34f else 0.20f
                val bottomAlpha = if (isDark) 0.22f else 0.12f
                val topRadius = (size.width * 0.95f) * scale
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to animPrimary.copy(alpha = topAlpha),
                            0.55f to animPrimary.copy(alpha = topAlpha * 0.35f),
                            1f to Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.08f),
                        radius = topRadius
                    ),
                    radius = topRadius,
                    center = Offset(size.width * 0.5f, size.height * 0.08f)
                )

                val botRadius = (size.width * 0.85f) * scale
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to animSecondary.copy(alpha = bottomAlpha),
                            0.6f to animSecondary.copy(alpha = bottomAlpha * 0.25f),
                            1f to Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.92f),
                        radius = botRadius
                    ),
                    radius = botRadius,
                    center = Offset(size.width * 0.5f, size.height * 0.92f)
                )
            }
    )
}

private fun softBoxBlurBitmap(input: Bitmap, targetWidth: Int = 64, radius: Int = 3): Bitmap {
    val safeW = targetWidth.coerceAtLeast(16)
    val safeH = (input.height * safeW / input.width.toFloat().coerceAtLeast(1f)).roundToInt().coerceAtLeast(16)
    val scaled = Bitmap.createScaledBitmap(input, safeW, safeH, true)
    val pixels = IntArray(safeW * safeH)
    val scratch = IntArray(safeW * safeH)
    scaled.getPixels(pixels, 0, safeW, 0, 0, safeW, safeH)
    if (scaled !== input) scaled.recycle()

    repeat(2) {
        boxBlurPass(pixels, scratch, safeW, safeH, radius, horizontal = true)
        boxBlurPass(scratch, pixels, safeW, safeH, radius, horizontal = false)
    }
    return Bitmap.createBitmap(pixels, safeW, safeH, Bitmap.Config.ARGB_8888)
}

private fun boxBlurPass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
    val lines = if (horizontal) h else w
    val length = if (horizontal) w else h
    val step = if (horizontal) 1 else w
    val window = 2 * r + 1

    for (line in 0 until lines) {
        val start = if (horizontal) line * w else line
        var a = 0; var red = 0; var green = 0; var blue = 0
        for (i in -r..r) {
            val p = src[start + i.coerceIn(0, length - 1) * step]
            a += p ushr 24
            red += (p shr 16) and 0xFF
            green += (p shr 8) and 0xFF
            blue += p and 0xFF
        }
        for (i in 0 until length) {
            dst[start + i * step] =
                ((a / window) shl 24) or ((red / window) shl 16) or ((green / window) shl 8) or (blue / window)
            val leaving = src[start + (i - r).coerceAtLeast(0) * step]
            val entering = src[start + (i + r + 1).coerceAtMost(length - 1) * step]
            a += (entering ushr 24) - (leaving ushr 24)
            red += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
            green += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
            blue += (entering and 0xFF) - (leaving and 0xFF)
        }
    }
}
