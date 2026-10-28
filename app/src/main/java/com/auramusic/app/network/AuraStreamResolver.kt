package com.auramusic.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

object AuraStreamResolver {

    private var isInitialized = false

    @Synchronized
    fun init() {
        if (!isInitialized) {
            NewPipe.init(AuraDownloaderImpl.instance)
            isInitialized = true
        }
    }

    suspend fun resolveAudioStream(videoIdOrUrl: String): String? = withContext(Dispatchers.IO) {
        init()
        try {
            val url = if (videoIdOrUrl.startsWith("http")) videoIdOrUrl else "https://www.youtube.com/watch?v=$videoIdOrUrl"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)
            
            // 1. Pick the best audio-only stream (M4A or Opus)
            val audioStreams = info.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val best = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams[0]
                if (!best.content.isNullOrBlank()) {
                    return@withContext best.content
                }
            }

            // 2. Fallback to video streams containing audio track
            val videoStreams = info.videoStreams
            if (!videoStreams.isNullOrEmpty()) {
                val videoWithAudio = videoStreams.firstOrNull { !it.content.isNullOrBlank() }
                if (videoWithAudio != null) {
                    return@withContext videoWithAudio.content
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
