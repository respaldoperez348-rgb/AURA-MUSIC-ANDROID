package com.auramusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.auramusic.app.AuraApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val YT_VIDEO_ID = Regex("^[a-zA-Z0-9_-]{11}$")
private val YT_VI_PATH = Regex("/vi/([a-zA-Z0-9_-]{11})/")

object DynamicCoverResolver {
    private val memoryCache = ConcurrentHashMap<String, String>()
    private val failedCache = ConcurrentHashMap.newKeySet<String>()
    private val networkSemaphore = kotlinx.coroutines.sync.Semaphore(2)

    suspend fun resolve(title: String, artist: String, isArtist: Boolean = false): String? = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        if (cleanTitle.isBlank()) return@withContext null
        val cacheKey = "${isArtist}_${cleanTitle.lowercase()}_${cleanArtist.lowercase()}"
        memoryCache[cacheKey]?.let { return@withContext it }
        if (failedCache.contains(cacheKey)) return@withContext null

        networkSemaphore.acquire()
        try {
            memoryCache[cacheKey]?.let { return@withContext it }
            if (isArtist) {
                val artists = AuraApplication.repository.searchArtists(cleanTitle)
                val match = artists.firstOrNull { it.thumbnailUrl.isNotBlank() }
                if (match != null) {
                    val url = normalizeCoverUrl(match.thumbnailUrl)
                    memoryCache[cacheKey] = url
                    return@withContext url
                }
            }
            val q = "$cleanTitle $cleanArtist".trim()
            val songs = AuraApplication.repository.searchSongs(q)
            val match = songs.firstOrNull { it.thumbnailUrl.isNotBlank() }
            if (match != null) {
                val url = normalizeCoverUrl(match.thumbnailUrl)
                memoryCache[cacheKey] = url
                return@withContext url
            }
            failedCache.add(cacheKey)
        } catch (_: Exception) {
            failedCache.add(cacheKey)
        } finally {
            networkSemaphore.release()
        }
        null
    }
}

internal fun normalizeCoverUrl(raw: String): String {
    var url = raw.trim()
    if (url.isEmpty()) return ""
    if (url.startsWith("//")) {
        url = "https:$url"
    } else if (url.startsWith("http://")) {
        url = "https://" + url.removePrefix("http://")
    }
    if (url.contains("i.ytimg.com/vi/") && url.contains("?")) {
        url = url.substringBefore("?")
    }
    return url
}

internal fun buildCoverCandidates(thumbnailUrl: String, id: String, isArtist: Boolean = false): List<String> {
    val list = mutableListOf<String>()
    val primary = normalizeCoverUrl(thumbnailUrl)
    if (primary.isNotBlank()) {
        if ((primary.contains("googleusercontent.com") || primary.contains("ggpht.com")) &&
            primary.contains("=w") && primary.contains("-h")
        ) {
            val compact = primary.replace(Regex("=w\\d+-h\\d+.*$"), "=w360-h360")
            list.add(compact)
        }
        list.add(primary)
    }

    val ytId = when {
        YT_VIDEO_ID.matches(id.trim()) -> id.trim()
        else -> YT_VI_PATH.find(thumbnailUrl)?.groupValues?.getOrNull(1).orEmpty()
    }

    if (ytId.isNotBlank() && !isArtist) {
        list.add("https://i.ytimg.com/vi/$ytId/hqdefault.jpg")
        list.add("https://i.ytimg.com/vi/$ytId/mqdefault.jpg")
        list.add("https://i.ytimg.com/vi/$ytId/0.jpg")
    }

    return list.distinct()
}

@Composable
fun AuraCover(
    thumbnailUrl: String,
    id: String = "",
    title: String = "",
    artist: String = "",
    isArtist: Boolean = false,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val candidates = remember(id, thumbnailUrl, isArtist) {
        buildCoverCandidates(thumbnailUrl, id, isArtist)
    }
    var index by remember(candidates) { mutableIntStateOf(0) }
    var resolvedDynamicUrl by remember(id, title, artist) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(id, title, artist, thumbnailUrl) {
        if (candidates.isEmpty() && title.isNotBlank()) {
            val res = DynamicCoverResolver.resolve(title, artist, isArtist)
            if (!res.isNullOrBlank()) {
                resolvedDynamicUrl = res
            }
        }
    }

    val currentUrl = resolvedDynamicUrl ?: candidates.getOrNull(index).orEmpty()

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isArtist) Icons.Default.Person else Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(24.dp)
        )

        if (currentUrl.isNotBlank()) {
            AsyncImage(
                model = currentUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = {
                    if (resolvedDynamicUrl != null) {
                        resolvedDynamicUrl = null
                    } else if (index + 1 < candidates.size) {
                        index += 1
                    } else if (resolvedDynamicUrl == null && title.isNotBlank()) {
                        scope.launch {
                            val res = DynamicCoverResolver.resolve(title, artist, isArtist)
                            if (!res.isNullOrBlank()) {
                                resolvedDynamicUrl = res
                            } else {
                                index = candidates.size
                            }
                        }
                    } else {
                        index = candidates.size
                    }
                }
            )
        }
    }
}
