package com.auramusic.app.network

import android.util.Log
import com.auramusic.app.data.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object LyricsEngine {

    private const val TAG = "LyricsEngine"

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Identified User-Agent required by LRCLIB to prevent Cloudflare blocks
    private const val LRCLIB_USER_AGENT = "AuraMusic/1.0 (https://github.com/auramusic/app)"
    private const val YOUTUBE_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    private const val YOUTUBE_NEXT_URL = "https://music.youtube.com/youtubei/v1/next"
    private const val YOUTUBE_BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse"

    private val LRC_PATTERN = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)")

    // In-memory cache: "cacheKey" -> List<LyricLine>
    private val lyricsCache = ConcurrentHashMap<String, List<LyricLine>>()

    fun clearCache() {
        lyricsCache.clear()
    }

    suspend fun translateLyrics(lines: List<LyricLine>, targetLangCode: String = "es"): List<LyricLine> = withContext(Dispatchers.IO) {
        if (lines.isEmpty()) return@withContext lines
        if (lines.any { it.translatedText.isNotBlank() }) return@withContext lines
        try {
            val separator = " ||| "
            val combinedText = lines.joinToString("\n") { it.text }
            val urlBuilder = "https://translate.googleapis.com/translate_a/single".toHttpUrlOrNull()?.newBuilder()
                ?: return@withContext lines
            urlBuilder.addQueryParameter("client", "gtx")
            urlBuilder.addQueryParameter("sl", "auto")
            urlBuilder.addQueryParameter("tl", targetLangCode)
            urlBuilder.addQueryParameter("dt", "t")
            urlBuilder.addQueryParameter("q", combinedText)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", YOUTUBE_USER_AGENT)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext lines
                val body = response.body?.string() ?: return@withContext lines
                val rootArray = JSONArray(body)
                val sentences = rootArray.optJSONArray(0) ?: return@withContext lines
                val sb = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val part = sentences.optJSONArray(i)?.optString(0) ?: ""
                    sb.append(part)
                }
                val translatedLines = sb.toString().split("\n")
                return@withContext lines.mapIndexed { idx, line ->
                    val trans = translatedLines.getOrNull(idx)?.trim() ?: ""
                    if (trans.isNotBlank() && !trans.equals(line.text.trim(), ignoreCase = true)) {
                        line.copy(translatedText = trans)
                    } else {
                        line
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Translation error: ${e.message}")
            lines
        }
    }

    suspend fun fetchSyncedLyrics(
        trackName: String,
        artistName: String,
        durationSec: Long,
        videoId: String = ""
    ): List<LyricLine> = withContext(Dispatchers.IO) {
        if (trackName.isBlank()) return@withContext emptyList()

        val (extractedArtist, extractedTitle) = parseArtistAndTitle(trackName, artistName)
        val cleanRaw = cleanTitle(trackName)
        val cacheKey = if (videoId.isNotBlank()) "vid:$videoId" else "$extractedTitle|$extractedArtist".lowercase()

        // 1. Check in-memory cache
        lyricsCache[cacheKey]?.let {
            Log.d(TAG, "Cache hit for $cacheKey (${it.size} lines)")
            return@withContext it
        }

        Log.d(TAG, "Fetching lyrics for '$cleanRaw' by '$extractedArtist' (videoId: $videoId)")

        // 2. Fetch from LRCLIB and YouTube Music concurrently for speed & resilience
        val lrclibDeferred = async(Dispatchers.IO) {
            try {
                if (extractedArtist.isNotBlank() && extractedTitle.isNotBlank()) {
                    val direct = fetchFromGetEndpoint(extractedTitle, extractedArtist, durationSec)
                    if (direct.isNotEmpty()) return@async direct
                }
                val query = if (extractedArtist.isNotBlank()) "$extractedArtist $cleanRaw" else cleanRaw
                fetchFromSearchEndpoint(query)
            } catch (e: Exception) {
                Log.w(TAG, "LRCLIB fetch error: ${e.message}")
                emptyList()
            }
        }

        val ytMusicDeferred = async(Dispatchers.IO) {
            try {
                if (videoId.isNotBlank()) {
                    fetchFromYouTubeMusicLyrics(videoId, durationSec)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.w(TAG, "YouTube Music lyrics error: ${e.message}")
                emptyList()
            }
        }

        val lrclibResult = lrclibDeferred.await()
        if (lrclibResult.isNotEmpty()) {
            Log.d(TAG, "Resolved ${lrclibResult.size} lines from LRCLIB")
            lyricsCache[cacheKey] = lrclibResult
            return@withContext lrclibResult
        }

        val ytResult = ytMusicDeferred.await()
        if (ytResult.isNotEmpty()) {
            Log.d(TAG, "Resolved ${ytResult.size} lines from YouTube Music official lyrics")
            lyricsCache[cacheKey] = ytResult
            return@withContext ytResult
        }

        // 3. Fallback: If videoId was empty or not matched, search YouTube for the videoId and fetch its lyrics
        if (videoId.isBlank()) {
            try {
                val searchFallback = YouTubeMusicEngine.searchTracks("$cleanRaw $extractedArtist")
                val candidateTrack = searchFallback.firstOrNull { !it.isArtist && !it.isAlbum && it.id.isNotBlank() }
                if (candidateTrack != null) {
                    val fallbackLyrics = fetchFromYouTubeMusicLyrics(candidateTrack.id, durationSec)
                    if (fallbackLyrics.isNotEmpty()) {
                        Log.d(TAG, "Resolved ${fallbackLyrics.size} lines from YouTube fallback search")
                        lyricsCache[cacheKey] = fallbackLyrics
                        return@withContext fallbackLyrics
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback search error: ${e.message}")
            }
        }

        Log.w(TAG, "No lyrics found for '$trackName'")
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
                .header("User-Agent", LRCLIB_USER_AGENT)
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
                return parsePlain(plainText, durationSec)
            }
        } catch (e: Exception) {
            // Silently handled by caller
        }
        return emptyList()
    }

    private fun fetchFromSearchEndpoint(query: String): List<LyricLine> {
        try {
            val urlBuilder = "https://lrclib.net/api/search".toHttpUrlOrNull()?.newBuilder() ?: return emptyList()
            urlBuilder.addQueryParameter("q", query)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", LRCLIB_USER_AGENT)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string() ?: return emptyList()
            val jsonArray = JSONArray(body)

            // Priority 1: Pick first item with synced lyrics
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val syncedText = item.optString("syncedLyrics")
                if (syncedText.isNotBlank()) {
                    return parseLrc(syncedText)
                }
            }

            // Priority 2: Pick first item with plain lyrics
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue
                val plainText = item.optString("plainLyrics")
                if (plainText.isNotBlank()) {
                    return parsePlain(plainText, 0)
                }
            }
        } catch (e: Exception) {
            // Silently handled by caller
        }
        return emptyList()
    }

    private fun fetchFromYouTubeMusicLyrics(videoId: String, durationSec: Long): List<LyricLine> {
        if (videoId.isBlank()) return emptyList()
        try {
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"videoId":"$videoId"}"""
            val req = Request.Builder()
                .url(YOUTUBE_NEXT_URL)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", YOUTUBE_USER_AGENT)
                .header("Content-Type", "application/json")
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            val root = JSONObject(body)

            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs") ?: return emptyList()

            var lyricsBrowseId = ""
            for (i in 0 until tabs.length()) {
                val tab = tabs.optJSONObject(i)?.optJSONObject("tabRenderer") ?: continue
                val ep = tab.optJSONObject("endpoint")?.optJSONObject("browseEndpoint")
                val bid = ep?.optString("browseId") ?: ""
                if (bid.startsWith("MPLY")) {
                    lyricsBrowseId = bid
                    break
                }
            }

            if (lyricsBrowseId.isBlank()) return emptyList()

            val browsePayload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"browseId":"$lyricsBrowseId"}"""
            val browseReq = Request.Builder()
                .url(YOUTUBE_BROWSE_URL)
                .post(browsePayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", YOUTUBE_USER_AGENT)
                .header("Content-Type", "application/json")
                .build()

            val browseResp = client.newCall(browseReq).execute()
            if (!browseResp.isSuccessful) return emptyList()
            val browseBody = browseResp.body?.string() ?: return emptyList()
            val browseRoot = JSONObject(browseBody)

            val shelf = browseRoot.optJSONObject("contents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")?.optJSONObject(0)
                ?.optJSONObject("musicDescriptionShelfRenderer")

            val runs = shelf?.optJSONObject("description")?.optJSONArray("runs")
            val text = if (runs != null && runs.length() > 0) {
                (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text") }
            } else ""

            if (text.isNotBlank()) {
                return parsePlain(text, durationSec)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching YouTube Music lyrics: ${e.message}")
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

    private fun parsePlain(plainText: String, durationSec: Long = 0): List<LyricLine> {
        val rawLines = plainText.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (rawLines.isEmpty()) return emptyList()

        val intervalMs = if (durationSec > 0) {
            val totalMs = durationSec * 1000L
            val step = totalMs / (rawLines.size + 1)
            step.coerceIn(2500L, 7000L)
        } else {
            3500L
        }

        return rawLines.mapIndexed { index, text ->
            LyricLine(timestampMs = index * intervalMs, text = text)
        }
    }

    fun parseArtistAndTitle(rawTitle: String, rawArtist: String): Pair<String, String> {
        val cleanT = cleanTitle(rawTitle)
        val cleanA = cleanArtist(rawArtist)

        // Check if raw title follows "Artist - Title" format
        val separators = listOf(" - ", " – ", " — ", " : ", " | ")
        for (sep in separators) {
            if (cleanT.contains(sep)) {
                val parts = cleanT.split(sep, limit = 2)
                val possibleArtist = parts[0].trim()
                val possibleTitle = parts[1].trim()
                if (possibleArtist.isNotBlank() && possibleTitle.isNotBlank()) {
                    return Pair(possibleArtist, possibleTitle)
                }
            }
        }

        // If artist seems to be an arbitrary channel or uploader
        if (cleanA.isBlank() || isGenericArtist(cleanA)) {
            return Pair("", cleanT)
        }

        return Pair(cleanA, cleanT)
    }

    private fun isGenericArtist(artist: String): Boolean {
        val lower = artist.lowercase()
        return lower.endsWith("vevo") || lower.contains("official") || lower.contains("records") ||
               lower.contains("channel") || lower.contains("topic") ||
               lower.matches(Regex(".*\\d+[a-z]?"))
    }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\[\\s*(?:letra|lyrics?|video|audio|oficial|official|hd|4k|remastered|en vivo|live)[^\\]]*\\]"), "")
            .replace(Regex("(?i)\\(\\s*(?:letra|lyrics?|video|audio|oficial|official|hd|4k|remastered|video oficial|audio oficial|en vivo|live)[^)]*\\)"), "")
            .replace(Regex("(?i)\\s*(?:feat\\.|ft\\.|featuring).*"), "")
            .replace(Regex("(?i)\\[[^\\]]*\\]"), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist
            .replace(" - Topic", "")
            .replace(Regex("(?i)\\s*VEVO$"), "")
            .trim()
    }
}
