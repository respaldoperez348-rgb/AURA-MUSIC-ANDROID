package com.auramusic.app.network

import com.auramusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YouTubeMusicEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"

    suspend fun searchTracks(query: String): List<Song> = withContext(Dispatchers.IO) {
        val effectiveQuery = if (query.isBlank()) "Cyberpunk Synthwave Top Hits" else query

        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20240101.01.00")
                        put("hl", "en")
                    })
                })
                put("query", effectiveQuery)
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(SEARCH_URL)
                .post(requestBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: return@withContext getCuratedFallbackTracks(effectiveQuery)
            val parsed = parseSearchResponse(rawBody)
            if (parsed.isNotEmpty()) parsed else getCuratedFallbackTracks(effectiveQuery)
        } catch (e: Exception) {
            e.printStackTrace()
            getCuratedFallbackTracks(effectiveQuery)
        }
    }

    private fun parseSearchResponse(jsonStr: String): List<Song> {
        val results = mutableListOf<Song>()
        try {
            val root = JSONObject(jsonStr)
            val tabs = root.optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs") ?: return emptyList()

            if (tabs.length() == 0) return emptyList()

            val sections = tabs.getJSONObject(0)
                .optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until sections.length()) {
                val sec = sections.optJSONObject(i) ?: continue

                // Check itemSectionRenderer
                val itemSec = sec.optJSONObject("itemSectionRenderer")
                if (itemSec != null) {
                    val contents = itemSec.optJSONArray("contents") ?: continue
                    for (j in 0 until contents.length()) {
                        val item = contents.optJSONObject(j) ?: continue
                        val col = item.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                        val song = extractSongFromListItem(col)
                        if (song != null) results.add(song)
                    }
                }

                // Check musicShelfRenderer directly
                val shelf = sec.optJSONObject("musicShelfRenderer")
                if (shelf != null) {
                    val contents = shelf.optJSONArray("contents") ?: continue
                    for (k in 0 until contents.length()) {
                        val it = contents.optJSONObject(k) ?: continue
                        val col = it.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                        val song = extractSongFromListItem(col)
                        if (song != null) results.add(song)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (results.isNotEmpty()) results else getCuratedFallbackTracks("")
    }

    private fun extractSongFromListItem(col: JSONObject): Song? {
        try {
            val flexColumns = col.optJSONArray("flexColumns") ?: return null
            if (flexColumns.length() < 2) return null

            val flex0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            val flex1 = flexColumns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")

            // Title is inside "text.runs" or "title.runs"
            val titleRuns = flex0?.optJSONObject("text")?.optJSONArray("runs")
                ?: flex0?.optJSONObject("title")?.optJSONArray("runs")

            val title = if (titleRuns != null && titleRuns.length() > 0) {
                titleRuns.getJSONObject(0).optString("text")
            } else ""

            if (title.isBlank()) return null

            // Artist is in flex1 runs (skip non-artist words like 'Song', '•', etc.)
            val artistRuns = flex1?.optJSONObject("text")?.optJSONArray("runs")
                ?: flex1?.optJSONObject("title")?.optJSONArray("runs")

            var artist = "Unknown Artist"
            if (artistRuns != null) {
                for (i in 0 until artistRuns.length()) {
                    val candidate = artistRuns.getJSONObject(i).optString("text").trim()
                    if (candidate.isNotEmpty() &&
                        candidate != "•" &&
                        candidate != "Song" &&
                        candidate != "Video" &&
                        candidate != "Single" &&
                        candidate != "Album" &&
                        !candidate.endsWith("plays") &&
                        !candidate.endsWith("views")
                    ) {
                        artist = candidate
                        break
                    }
                }
            }

            // Video ID from overlay or navigationEndpoint or playlistItemData
            var videoId = ""
            val overlay = col.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")

            if (overlay != null) {
                videoId = overlay.optString("videoId")
            }

            if (videoId.isBlank()) {
                val nav = col.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                if (nav != null) videoId = nav.optString("videoId")
            }

            if (videoId.isBlank()) {
                val plItem = col.optJSONObject("playlistItemData")
                if (plItem != null) videoId = plItem.optString("videoId")
            }

            // Thumbnail
            var thumb = ""
            val thumbnails = col.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")

            if (thumbnails != null && thumbnails.length() > 0) {
                thumb = thumbnails.getJSONObject(thumbnails.length() - 1).optString("url")
            }

            val finalId = if (videoId.isNotBlank()) videoId else title.hashCode().toString()
            val mediaUrl = "https://www.youtube.com/watch?v=$finalId"

            return Song(
                id = finalId,
                title = title,
                artist = artist,
                coverUrl = thumb,
                durationMs = 210000L,
                mediaUri = mediaUrl
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun getCuratedFallbackTracks(query: String): List<Song> {
        val all = listOf(
            Song(
                id = "1",
                title = "Resonance",
                artist = "HOME",
                album = "Odyssey",
                durationMs = 212000L,
                coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
                mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
                isFavorite = true
            ),
            Song(
                id = "2",
                title = "Cyber City Lights",
                artist = "Aura Synthesizer",
                album = "Neon Drift",
                durationMs = 184000L,
                coverUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=600&auto=format&fit=crop&q=80",
                mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
                isFavorite = false
            ),
            Song(
                id = "3",
                title = "Wayland Dreams",
                artist = "Quickshell Core",
                album = "Abyss Protocol",
                durationMs = 245000L,
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
                isFavorite = true
            ),
            Song(
                id = "4",
                title = "Midnight City Cyber",
                artist = "M83 Remix",
                album = "Hurry Up Synth",
                durationMs = 243000L,
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
                isFavorite = false
            ),
            Song(
                id = "5",
                title = "After Dark",
                artist = "Mr.Kitty",
                album = "Time",
                durationMs = 259000L,
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
                isFavorite = false
            )
        )

        val matches = all.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
        return if (matches.isNotEmpty()) matches else all
    }
}
