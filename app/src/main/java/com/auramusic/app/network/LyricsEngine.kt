package com.auramusic.app.network

import com.auramusic.app.data.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object LyricsEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private val LRC_PATTERN = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)")

    // In-memory cache: "title|artist" -> List<LyricLine>
    private val lyricsCache = ConcurrentHashMap<String, List<LyricLine>>()

    suspend fun fetchSyncedLyrics(
        trackName: String,
        artistName: String,
        durationSec: Long
    ): List<LyricLine> = withContext(Dispatchers.IO) {
        if (trackName.isBlank()) return@withContext emptyList()

        val cleanedTitle = cleanTitle(trackName)
        val cleanedArtist = cleanArtist(artistName)
        val cacheKey = "$cleanedTitle|$cleanedArtist"

        // 1. Check in-memory cache
        lyricsCache[cacheKey]?.let { return@withContext it }

        // 2. Try direct LRCLIB /api/get (exact match)
        val directLyrics = fetchFromGetEndpoint(cleanedTitle, cleanedArtist, durationSec)
        if (directLyrics.isNotEmpty()) {
            lyricsCache[cacheKey] = directLyrics
            return@withContext directLyrics
        }

        // 3. Fallback: Try LRCLIB /api/search (fuzzy search)
        val searchQuery = if (cleanedArtist.isNotBlank() && !cleanedTitle.contains(cleanedArtist, ignoreCase = true)) {
            "$cleanedTitle $cleanedArtist"
        } else {
            cleanedTitle
        }

        val searchLyrics = fetchFromSearchEndpoint(searchQuery)
        if (searchLyrics.isNotEmpty()) {
            lyricsCache[cacheKey] = searchLyrics
            return@withContext searchLyrics
        }

        // 4. No lyrics found - return empty list (never return fake/dummy lyrics!)
        emptyList()
    }

    private fun fetchFromGetEndpoint(trackName: String, artistName: String, durationSec: Long): List<LyricLine> {
        try {
            val urlBuilder = "https://lrclib.net/api/get".toHttpUrlOrNull()?.newBuilder() ?: return emptyList()
            urlBuilder.addQueryParameter("track_name", trackName)
            if (artistName.isNotBlank() && artistName != "Unknown Artist") {
                urlBuilder.addQueryParameter("artist_name", artistName)
            }
            if (durationSec > 0) {
                urlBuilder.addQueryParameter("duration", durationSec.toString())
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", BROWSER_USER_AGENT)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val json = JSONObject(body)

            val syncedText = json.optString("syncedLyrics")
            if (syncedText.isNotBlank()) {
                return parseLrc(syncedText)
            }

            val plainText = json.optString("plainLyrics")
            if (plainText.isNotBlank()) {
                return parsePlain(plainText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    private fun fetchFromSearchEndpoint(query: String): List<LyricLine> {
        try {
            val urlBuilder = "https://lrclib.net/api/search".toHttpUrlOrNull()?.newBuilder() ?: return emptyList()
            urlBuilder.addQueryParameter("q", query)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", BROWSER_USER_AGENT)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val jsonArray = JSONArray(body)

            // Find best matching item that contains synced or plain lyrics
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val syncedText = item.optString("syncedLyrics")
                if (syncedText.isNotBlank()) {
                    return parseLrc(syncedText)
                }
            }

            // Fallback to plain lyrics if no synced lyrics in any item
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val plainText = item.optString("plainLyrics")
                if (plainText.isNotBlank()) {
                    return parsePlain(plainText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
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
        return title
            .replace(Regex("(?i)\\s*[\\[(](?:official|audio|video|lyrics?|hd|4k|remastered|oficial|video oficial|audio oficial|letra)[^\\])]*[\\])]"), "")
            .replace(Regex("(?i)\\s*(?:feat\\.|ft\\.|featuring).*"), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist
            .replace(" - Topic", "")
            .replace(Regex("(?i)\\s*VEVO$"), "")
            .trim()
    }
}
