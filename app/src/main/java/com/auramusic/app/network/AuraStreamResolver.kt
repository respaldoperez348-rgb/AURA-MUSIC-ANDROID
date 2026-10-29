package com.auramusic.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.ConcurrentHashMap

object AuraStreamResolver {

    private var isInitialized = false

    // Cache: videoId -> Pair<StreamUrl, timestampMillis>
    // YouTube audio streams are valid for ~6 hours. We expire after 4 hours.
    private val streamCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private const val CACHE_EXPIRATION_MS = 4 * 60 * 60 * 1000L

    @Volatile
    var preferredQuality: String = "Hi-Res (320 kbps)"

    fun clearCache() {
        streamCache.clear()
    }

    @Synchronized
    fun init() {
        if (!isInitialized) {
            NewPipe.init(AuraDownloaderImpl.instance)
            isInitialized = true
        }
    }

    suspend fun resolveAudioStream(videoIdOrUrl: String, fallbackQuery: String? = null): String? = withContext(Dispatchers.IO) {
        init()
        var actualId = videoIdOrUrl.substringAfterLast("v=").substringBefore("&").trim()

        // 1. If it's an Album or Playlist browseId, resolve its first track
        if (actualId.startsWith("MPREb_") || actualId.startsWith("VL") || actualId.startsWith("PL")) {
            val resolved = YouTubeMusicEngine.resolveFirstTrackFromBrowse(actualId)
            if (!resolved.isNullOrBlank()) {
                actualId = resolved
            }
        }

        // 2. If it's not a standard 11-char YouTube ID, use fallback query search
        if (actualId.length != 11 && !fallbackQuery.isNullOrBlank()) {
            val searched = YouTubeMusicEngine.searchFirstPlayableVideoId(fallbackQuery)
            if (!searched.isNullOrBlank()) {
                actualId = searched
            }
        }

        if (actualId.isBlank()) return@withContext null

        // 3. Check in-memory stream cache
        val cached = streamCache[actualId]
        if (cached != null) {
            val (url, timestamp) = cached
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                return@withContext url
            } else {
                streamCache.remove(actualId)
            }
        }

        try {
            val url = if (actualId.startsWith("http")) actualId else "https://www.youtube.com/watch?v=$actualId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)

            // 4. Pick audio-only stream matching user quality preference (M4A or Opus)
            val audioStreams = info.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val chosen = when {
                    preferredQuality.contains("96") || preferredQuality.contains("Ahorro", ignoreCase = true) -> {
                        audioStreams.filter { !it.content.isNullOrBlank() }
                            .minByOrNull { it.averageBitrate.takeIf { b -> b > 0 } ?: Int.MAX_VALUE }
                    }
                    preferredQuality.contains("160") || preferredQuality.contains("Estándar", ignoreCase = true) -> {
                        audioStreams.filter { !it.content.isNullOrBlank() }
                            .minByOrNull { kotlin.math.abs(it.averageBitrate - 128) }
                    }
                    else -> {
                        audioStreams.filter { !it.content.isNullOrBlank() }
                            .maxByOrNull { it.averageBitrate }
                    }
                } ?: audioStreams.firstOrNull { !it.content.isNullOrBlank() }

                if (chosen != null && !chosen.content.isNullOrBlank()) {
                    val directUrl = chosen.content
                    streamCache[actualId] = Pair(directUrl, System.currentTimeMillis())
                    return@withContext directUrl
                }
            }

            // 5. Fallback to video streams containing audio track
            val videoStreams = info.videoStreams
            if (!videoStreams.isNullOrEmpty()) {
                val videoWithAudio = videoStreams.firstOrNull { !it.content.isNullOrBlank() }
                if (videoWithAudio != null) {
                    val directUrl = videoWithAudio.content
                    streamCache[actualId] = Pair(directUrl, System.currentTimeMillis())
                    return@withContext directUrl
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            // If primary resolve failed and fallback query is available, try searching once more
            if (!fallbackQuery.isNullOrBlank() && fallbackQuery != actualId) {
                try {
                    val altId = YouTubeMusicEngine.searchFirstPlayableVideoId(fallbackQuery)
                    if (!altId.isNullOrBlank() && altId != actualId) {
                        return@withContext resolveAudioStream(altId, null)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            null
        }
    }

    fun isCached(videoId: String): Boolean {
        val key = videoId.substringAfterLast("v=").substringBefore("&")
        val cached = streamCache[key] ?: return false
        return (System.currentTimeMillis() - cached.second) < CACHE_EXPIRATION_MS
    }
}
