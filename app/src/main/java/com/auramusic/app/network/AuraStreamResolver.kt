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

    @Synchronized
    fun init() {
        if (!isInitialized) {
            NewPipe.init(AuraDownloaderImpl.instance)
            isInitialized = true
        }
    }

    suspend fun resolveAudioStream(videoIdOrUrl: String): String? = withContext(Dispatchers.IO) {
        init()
        val key = videoIdOrUrl.substringAfterLast("v=").substringBefore("&")

        // 1. Check in-memory stream cache for instantaneous playback
        val cached = streamCache[key]
        if (cached != null) {
            val (url, timestamp) = cached
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                return@withContext url
            } else {
                streamCache.remove(key)
            }
        }

        try {
            val url = if (videoIdOrUrl.startsWith("http")) videoIdOrUrl else "https://www.youtube.com/watch?v=$videoIdOrUrl"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)
            
            // 1. Pick the best audio-only stream (M4A or Opus)
            val audioStreams = info.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val best = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams[0]
                if (!best.content.isNullOrBlank()) {
                    val directUrl = best.content
                    streamCache[key] = Pair(directUrl, System.currentTimeMillis())
                    return@withContext directUrl
                }
            }

            // 2. Fallback to video streams containing audio track
            val videoStreams = info.videoStreams
            if (!videoStreams.isNullOrEmpty()) {
                val videoWithAudio = videoStreams.firstOrNull { !it.content.isNullOrBlank() }
                if (videoWithAudio != null) {
                    val directUrl = videoWithAudio.content
                    streamCache[key] = Pair(directUrl, System.currentTimeMillis())
                    return@withContext directUrl
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isCached(videoId: String): Boolean {
        val key = videoId.substringAfterLast("v=").substringBefore("&")
        val cached = streamCache[key] ?: return false
        return (System.currentTimeMillis() - cached.second) < CACHE_EXPIRATION_MS
    }
}
