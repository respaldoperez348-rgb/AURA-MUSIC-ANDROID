package com.auramusic.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.auramusic.app.data.model.Song

private val YT_VIDEO_ID = Regex("^[a-zA-Z0-9_-]{11}$")
private val YT_VI_PATH = Regex("/vi/([a-zA-Z0-9_-]{11})/")
private val YT_WATCH_PARAM = Regex("[?&]v=([a-zA-Z0-9_-]{11})")

/** Arregla URLs de portada que llegan incompletas o con parámetros temporales inestables. */
internal fun normalizeCoverUrl(raw: String): String {
    var url = raw.trim()
    if (url.isEmpty()) return ""
    if (url.startsWith("//")) {
        url = "https:$url"
    } else if (url.startsWith("http://") &&
        (url.contains("ytimg.com") || url.contains("googleusercontent.com") || url.contains("ggpht.com"))
    ) {
        url = "https://" + url.removePrefix("http://")
    }
    if (url.contains("i.ytimg.com/vi/") && url.contains("?")) {
        url = url.substringBefore("?")
    }
    return url
}

internal fun extractYoutubeVideoId(song: Song): String {
    val cleanId = song.id.removePrefix("local_dl_").trim()
    if (YT_VIDEO_ID.matches(cleanId)) return cleanId
    YT_WATCH_PARAM.find(song.mediaUri)?.groupValues?.getOrNull(1)?.let { return it }
    YT_VI_PATH.find(song.coverUrl)?.groupValues?.getOrNull(1)?.let { return it }
    return ""
}

internal fun buildCoverCandidates(song: Song): List<String> {
    val list = mutableListOf<String>()
    val primary = normalizeCoverUrl(song.coverUrl)
    if (primary.isNotBlank()) {
        list.add(primary)
        if ((primary.contains("googleusercontent.com") || primary.contains("ggpht.com")) &&
            primary.contains("=w") && primary.contains("-h")
        ) {
            val compact = primary.replace(Regex("=w\\d+-h\\d+.*$"), "=w300-h300")
            if (compact != primary) list.add(compact)
        }
    }

    val ytId = extractYoutubeVideoId(song)
    if (ytId.isNotBlank() && !song.isArtist) {
        list.add("https://i.ytimg.com/vi/$ytId/hqdefault.jpg")
        list.add("https://i.ytimg.com/vi/$ytId/mqdefault.jpg")
        list.add("https://i.ytimg.com/vi/$ytId/0.jpg")
    }

    return list.distinct()
}

/**
 * Portada de canción/álbum/artista con cadena de respaldo automática:
 * 1) Portada principal normalizada
 * 2) Variante compacta de Google CDN (si aplica)
 * 3) Miniaturas oficiales de YouTube (hqdefault -> mqdefault -> 0.jpg)
 * 4) Placeholder limpio si todas fallan
 */
@Composable
fun AuraCover(
    song: Song,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit
) {
    val candidates = remember(song.id, song.coverUrl, song.mediaUri, song.isArtist, song.isAlbum) {
        buildCoverCandidates(song)
    }
    var index by remember(candidates) { mutableIntStateOf(0) }

    val currentUrl = candidates.getOrNull(index).orEmpty()

    if (currentUrl.isBlank()) {
        placeholder()
    } else {
        AsyncImage(
            model = currentUrl,
            contentDescription = song.title,
            modifier = modifier,
            contentScale = contentScale,
            onError = {
                if (index + 1 < candidates.size) {
                    index += 1
                } else {
                    index = candidates.size
                }
            }
        )
    }
}
