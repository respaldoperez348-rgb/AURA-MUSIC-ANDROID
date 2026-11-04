package com.auramusic.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.auramusic.app.data.model.Song

private val YT_VIDEO_ID = Regex("^[a-zA-Z0-9_-]{11}$")

/** Arregla URLs de portada que llegan incompletas o con http:// */
internal fun normalizeCoverUrl(raw: String): String {
    val url = raw.trim()
    if (url.isEmpty()) return ""
    return when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") &&
            (url.contains("ytimg.com") || url.contains("googleusercontent.com") || url.contains("ggpht.com")) ->
            "https://" + url.removePrefix("http://")
        else -> url
    }
}

/** Miniatura estándar de YouTube, disponible para cualquier video aunque la portada oficial falle. */
internal fun youtubeThumbFallback(song: Song): String {
    if (song.isArtist || song.isAlbum) return ""
    return if (YT_VIDEO_ID.matches(song.id)) "https://i.ytimg.com/vi/${song.id}/hqdefault.jpg" else ""
}

/**
 * Portada de una canción con respaldo:
 * 1) usa la portada recibida; 2) si falla al cargar, prueba la miniatura de YouTube;
 * 3) si tampoco hay, muestra [placeholder] (ícono) en lugar de un cuadro vacío.
 */
@Composable
fun AuraCover(
    song: Song,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit
) {
    val primary = remember(song.coverUrl) { normalizeCoverUrl(song.coverUrl) }
    val fallback = remember(song.id, song.isArtist, song.isAlbum) { youtubeThumbFallback(song) }
    var current by remember(primary, fallback) { mutableStateOf(primary.ifBlank { fallback }) }
    var failed by remember(primary, fallback) { mutableStateOf(false) }

    if (current.isBlank() || failed) {
        placeholder()
    } else {
        AsyncImage(
            model = current,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            onError = {
                if (fallback.isNotBlank() && current != fallback) {
                    current = fallback
                } else {
                    failed = true
                }
            }
        )
    }
}
