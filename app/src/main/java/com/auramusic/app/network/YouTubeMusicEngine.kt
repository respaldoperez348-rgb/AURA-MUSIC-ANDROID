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
        .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
    private val searchCache = java.util.concurrent.ConcurrentHashMap<String, List<Song>>()

    suspend fun searchTracks(query: String): List<Song> = withContext(Dispatchers.IO) {
        val effectiveQuery = if (query.isBlank()) "Cyberpunk Synthwave Top Hits" else query.trim()
        val cacheKey = effectiveQuery.lowercase()
        searchCache[cacheKey]?.let { return@withContext it }

        try {
            val escapedQuery = effectiveQuery.replace("\\", "\\\\").replace("\"", "\\\"")
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"US"}},"query":"$escapedQuery"}"""

            val requestBody = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(SEARCH_URL)
                .post(requestBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: return@withContext getCuratedFallbackTracks(effectiveQuery)
            val parsed = parseSearchResponse(rawBody)
            val result = if (parsed.isNotEmpty()) parsed else getCuratedFallbackTracks(effectiveQuery)
            if (result.isNotEmpty()) searchCache[cacheKey] = result
            result
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
                        candidate != "Playlist" &&
                        candidate != "Episode" &&
                        candidate != "Podcast" &&
                        !candidate.endsWith("plays") &&
                        !candidate.endsWith("views")
                    ) {
                        artist = candidate
                        break
                    }
                }
            }

            // 1. Video ID from overlay or flex columns or navigationEndpoint
            var videoId = ""
            val overlayPlay = col.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")

            val overlayWatch = overlayPlay?.optJSONObject("watchEndpoint")
            if (overlayWatch != null) {
                videoId = overlayWatch.optString("videoId")
            }

            if (videoId.isBlank() && titleRuns != null && titleRuns.length() > 0) {
                val nav = titleRuns.getJSONObject(0).optJSONObject("navigationEndpoint")
                val w = nav?.optJSONObject("watchEndpoint")
                if (w != null) videoId = w.optString("videoId")
            }

            if (videoId.isBlank()) {
                val nav = col.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")
                if (nav != null) videoId = nav.optString("videoId")
            }

            if (videoId.isBlank()) {
                val plItem = col.optJSONObject("playlistItemData")
                if (plItem != null) videoId = plItem.optString("videoId")
            }

            // 2. Detect Artist / Album / Playlist identifiers
            var isArtistItem = false
            val navEndpoint = col.optJSONObject("navigationEndpoint")
            var browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId") ?: ""
            if (browseId.isBlank() && titleRuns != null && titleRuns.length() > 0) {
                browseId = titleRuns.getJSONObject(0).optJSONObject("navigationEndpoint")
                    ?.optJSONObject("browseEndpoint")?.optString("browseId") ?: ""
            }

            val playlistId = overlayPlay?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId") ?: ""

            if (browseId.startsWith("UC") || browseId.contains("artist", ignoreCase = true) ||
                artist.equals("Artist", ignoreCase = true) || artist.equals("Artista", ignoreCase = true)) {
                isArtistItem = true
            }

            // 3. If videoId is blank but it's an Album/Single (MPREb_) or Playlist (PL...), resolve first playable track!
            if (videoId.isBlank() && !isArtistItem) {
                val targetBrowse = if (browseId.startsWith("MPREb_") || browseId.startsWith("VL")) {
                    browseId
                } else if (playlistId.isNotBlank()) {
                    if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
                } else {
                    ""
                }

                if (targetBrowse.isNotBlank()) {
                    videoId = resolveFirstTrackFromBrowse(targetBrowse) ?: ""
                }
            }

            // 4. Thumbnail
            var thumb = ""
            val thumbnails = col.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")

            if (thumbnails != null && thumbnails.length() > 0) {
                thumb = thumbnails.getJSONObject(thumbnails.length() - 1).optString("url")
            }

            if (thumb.startsWith("//")) {
                thumb = "https:$thumb"
            }
            if (thumb.isBlank() && videoId.isNotBlank()) {
                thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            }
            if (thumb.contains("=w") && thumb.contains("-h")) {
                thumb = thumb.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
            }

            // If it's not an artist and still has no videoId, we drop the unplayable item instead of creating bogus hash URLs
            if (videoId.isBlank() && !isArtistItem) {
                return null
            }

            val finalId = if (isArtistItem) (if (browseId.isNotBlank()) browseId else title) else videoId
            val mediaUrl = if (isArtistItem) "" else "https://www.youtube.com/watch?v=$finalId"

            return Song(
                id = finalId,
                title = title,
                artist = if (isArtistItem) "Artist" else artist,
                coverUrl = thumb,
                durationMs = if (isArtistItem) 0L else 210000L,
                mediaUri = mediaUrl,
                isArtist = isArtistItem
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun resolveFirstTrackFromBrowse(browseId: String): String? {
        try {
            val actualBrowseId = if (browseId.startsWith("PL") && !browseId.startsWith("VL")) "VL$browseId" else browseId
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"US"}},"browseId":"$actualBrowseId"}"""

            val request = Request.Builder()
                .url(BROWSE_URL)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return null
                val match = Regex("\"videoId\":\\s*\"([a-zA-Z0-9_-]{11})\"").find(body)
                return match?.groupValues?.get(1)
            }
        } catch (e: Exception) {
            return null
        }
    }

    fun searchFirstPlayableVideoId(query: String): String? {
        try {
            val escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"US"}},"query":"$escapedQuery"}"""

            val request = Request.Builder()
                .url(SEARCH_URL)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return null
                val match = Regex("\"videoId\":\\s*\"([a-zA-Z0-9_-]{11})\"").find(body)
                return match?.groupValues?.get(1)
            }
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

    suspend fun fetchArtistTopTracks(artistName: String): List<Song> = withContext(Dispatchers.IO) {
        val query = "$artistName Top Hits"
        searchTracks(query).filter { !it.isArtist }
    }
}
