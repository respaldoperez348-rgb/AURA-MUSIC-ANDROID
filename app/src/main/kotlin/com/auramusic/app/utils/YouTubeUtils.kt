package com.auramusic.app.utils

import kotlin.math.roundToInt

private val GOOGLEUSERCONTENT_SIZE_PATTERN =
    Regex("^(https://(?:lh3|yt3)\\.googleusercontent\\.com/[^?]*?)=w(\\d+)-h(\\d+)[^?]*(\\?.*)?$")
private val GGPHT_SIZE_PATTERN =
    Regex("^(https://yt3\\.ggpht\\.com/[^?=]+)=(?:s\\d+|w\\d+-h\\d+)[^?]*(\\?.*)?$")

/**
 * Escala las URLs de imágenes de YouTube Music y avatares de artistas
 * a la resolución solicitada en alta fidelidad (evita thumbnails pixelados).
 */
fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    GOOGLEUSERCONTENT_SIZE_PATTERN
        .matchEntire(this)
        ?.groupValues
        ?.let { group ->
            val originalWidth = group[2].toIntOrNull() ?: 1
            val originalHeight = group[3].toIntOrNull() ?: 1
            val query = group[4]
            val targetWidth = width ?: ((height!!.toDouble() * originalWidth) / originalHeight).roundToInt()
            val targetHeight = height ?: ((width!!.toDouble() * originalHeight) / originalWidth).roundToInt()
            return "${group[1]}=w${targetWidth.coerceAtLeast(1)}-h${targetHeight.coerceAtLeast(1)}-p-l90-rj$query"
        }

    GGPHT_SIZE_PATTERN.matchEntire(this)?.groupValues?.let { group ->
        val query = group[2]
        return if (width != null && height != null) {
            "${group[1]}=w$width-h$height-p-l90-rj$query"
        } else {
            "${group[1]}=s${width ?: height}$query"
        }
    }

    return this
}

/**
 * Formatea segundos en mm:ss (ej. 230 -> "3:50")
 */
fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
