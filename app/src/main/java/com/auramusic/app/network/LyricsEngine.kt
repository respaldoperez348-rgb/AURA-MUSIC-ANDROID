package com.auramusic.app.network

import com.auramusic.app.data.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object LyricsEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val LRC_PATTERN = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)")

    suspend fun fetchSyncedLyrics(trackName: String, artistName: String, durationSec: Long): List<LyricLine> = withContext(Dispatchers.IO) {
        if (trackName.isBlank()) return@withContext emptyList()

        try {
            val urlBuilder = "https://lrclib.net/api/get".toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext emptyList()

            urlBuilder.addQueryParameter("track_name", cleanTitle(trackName))
            if (artistName.isNotBlank() && artistName != "Unknown Artist") {
                urlBuilder.addQueryParameter("artist_name", cleanArtist(artistName))
            }
            if (durationSec > 0) {
                urlBuilder.addQueryParameter("duration", durationSec.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "AuraMusic/1.0 (Android)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext getFallbackLyrics(trackName)

            val body = response.body?.string() ?: return@withContext getFallbackLyrics(trackName)
            val json = JSONObject(body)

            val syncedText = json.optString("syncedLyrics")
            if (syncedText.isNotBlank()) {
                parseLrc(syncedText)
            } else {
                val plainText = json.optString("plainLyrics")
                if (plainText.isNotBlank()) parsePlain(plainText) else getFallbackLyrics(trackName)
            }
        } catch (e: Exception) {
            getFallbackLyrics(trackName)
        }
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val splitLines = lrcText.split("\n")

        for (line in splitLines) {
            val matcher = LRC_PATTERN.matcher(line.trim())
            if (matcher.matches()) {
                val minStr = matcher.group(1) ?: "0"
                val secStr = matcher.group(2) ?: "0"
                val content = matcher.group(3)?.trim() ?: ""

                val minutes = minStr.toLongOrNull() ?: 0L
                val seconds = (secStr.toDoubleOrNull() ?: 0.0)
                val totalMs = (minutes * 60 * 1000) + (seconds * 1000).toLong()

                if (content.isNotBlank()) {
                    lines.add(LyricLine(timestampMs = totalMs, text = content))
                }
            }
        }

        return lines.sortedBy { it.timestampMs }
    }

    private fun parsePlain(plainText: String): List<LyricLine> {
        return plainText.split("\n")
            .filter { it.isNotBlank() }
            .mapIndexed { index, text ->
                LyricLine(timestampMs = index * 4000L, text = text.trim())
            }
    }

    private fun cleanTitle(title: String): String {
        return title.replace(Regex("(?i)\\s*[\\[(](?:official|audio|video|lyrics?|hd|4k|remastered)[^\\])]*[\\])]"), "")
            .replace(Regex("(?i)feat\\..*"), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist.replace(" - Topic", "").trim()
    }

    private fun getFallbackLyrics(trackName: String): List<LyricLine> {
        return listOf(
            LyricLine(0L, "♪ Instrumental Intro ♪"),
            LyricLine(12000L, "Cruising down the neon highway"),
            LyricLine(24000L, "Reflections in the obsidian rain"),
            LyricLine(38000L, "Frequency pulses through the wire"),
            LyricLine(52000L, "Aura synthesizers echoing the night"),
            LyricLine(68000L, "Lost in the rhythm of the cyber lights"),
            LyricLine(85000L, "♪ Deep Bass Solo ♪"),
            LyricLine(110000L, "Holding on to the resonance"),
            LyricLine(135000L, "Till the morning breaks the glass")
        )
    }
}
