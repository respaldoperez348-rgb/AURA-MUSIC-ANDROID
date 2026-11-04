package com.auramusic.app.network

import android.util.Log
import com.auramusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SpotifyImportResult(
    val playlistTitle: String,
    val author: String,
    val coverUrl: String,
    val tracks: List<Song>
)

object SpotifyImporter {
    private const val TAG = "SpotifyImporter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    suspend fun importFromSpotifyUrl(url: String): SpotifyImportResult? = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        val type = when {
            cleanUrl.contains("/playlist/") -> "playlist"
            cleanUrl.contains("/album/") -> "album"
            cleanUrl.contains("/track/") -> "track"
            else -> return@withContext null
        }

        val id = cleanUrl.substringAfter("/$type/").substringBefore("?").substringBefore("/")
        if (id.isBlank()) return@withContext null

        try {
            // 1. Fetch metadata via Spotify oEmbed
            var playlistTitle = "Spotify Playlist"
            var author = "Spotify"
            var coverUrl = ""

            try {
                val oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/$type/$id"
                val oembedReq = Request.Builder().url(oembedUrl).header("User-Agent", DESKTOP_UA).build()
                client.newCall(oembedReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        playlistTitle = json.optString("title", playlistTitle)
                        author = json.optString("author_name", author)
                        coverUrl = json.optString("thumbnail_url", "")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "oEmbed error: ${e.message}")
            }

            // 2. Fetch tracks via Spotify Embed endpoint (Contains full tracklist without API keys)
            val embedUrl = "https://open.spotify.com/embed/$type/$id"
            val embedReq = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", DESKTOP_UA)
                .build()

            client.newCall(embedReq).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: return@withContext null

                val (tracks, resolvedCover) = parseTracksFromEmbedHtml(html, id, coverUrl)
                val finalCover = coverUrl.ifBlank { resolvedCover }

                if (tracks.isNotEmpty()) {
                    return@withContext SpotifyImportResult(
                        playlistTitle = playlistTitle,
                        author = author,
                        coverUrl = finalCover,
                        tracks = tracks
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Import error: ${e.message}")
        }
        null
    }

    private fun parseTracksFromEmbedHtml(html: String, parentId: String, fallbackCover: String): Pair<List<Song>, String> {
        val tracks = mutableListOf<Song>()
        var entityCover = fallbackCover

        // Approach A: Parse __NEXT_DATA__ JSON script tag
        val nextDataPattern = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">([^<]+)</script>")
        val matcher = nextDataPattern.matcher(html)
        if (matcher.find()) {
            val jsonText = matcher.group(1) ?: ""
            try {
                val root = JSONObject(jsonText)
                val entity = root.optJSONObject("props")
                    ?.optJSONObject("pageProps")
                    ?.optJSONObject("state")
                    ?.optJSONObject("data")
                    ?.optJSONObject("entity")

                // Extract entity cover if available
                if (entityCover.isBlank() && entity != null) {
                    val coverSources = entity.optJSONObject("coverArt")?.optJSONArray("sources")
                    if (coverSources != null && coverSources.length() > 0) {
                        entityCover = coverSources.getJSONObject(0).optString("url", "")
                    }
                    if (entityCover.isBlank()) {
                        val images = entity.optJSONArray("images")
                        if (images != null && images.length() > 0) {
                            entityCover = images.getJSONObject(0).optString("url", "")
                        }
                    }
                }

                val trackList = entity?.optJSONArray("trackList")
                if (trackList != null && trackList.length() > 0) {
                    for (i in 0 until trackList.length()) {
                        val item = trackList.getJSONObject(i)
                        val title = item.optString("title", item.optString("name", "")).trim()
                        val artist = item.optString("subtitle", "").trim()
                        val duration = item.optLong("duration", 0L)
                        
                        // Extract track-specific cover or fallback to playlist cover
                        var trackCover = ""
                        val itemSources = item.optJSONObject("coverArt")?.optJSONArray("sources")
                        if (itemSources != null && itemSources.length() > 0) {
                            trackCover = itemSources.getJSONObject(0).optString("url", "")
                        }
                        if (trackCover.isBlank()) {
                            val albumObj = item.optJSONObject("album")
                            val albumImages = albumObj?.optJSONArray("images")
                            if (albumImages != null && albumImages.length() > 0) {
                                trackCover = albumImages.getJSONObject(0).optString("url", "")
                            }
                        }
                        if (trackCover.isBlank()) {
                            trackCover = entityCover
                        }

                        if (title.isNotBlank()) {
                            tracks.add(
                                Song(
                                    id = "spot_${parentId}_$i",
                                    title = title,
                                    artist = if (artist.isNotBlank()) artist else "Spotify Artist",
                                    album = "",
                                    durationMs = duration,
                                    coverUrl = trackCover
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "__NEXT_DATA__ parse error: ${e.message}")
            }
        }

        // Approach B: Regex fallback for track entries in Spotify HTML
        if (tracks.isEmpty()) {
            val trackRegex = Pattern.compile("\"title\":\"([^\"]+)\",\"subtitle\":\"([^\"]+)\"")
            val m = trackRegex.matcher(html)
            var idx = 0
            while (m.find()) {
                val title = m.group(1)?.trim() ?: ""
                val artist = m.group(2)?.trim() ?: ""
                if (title.isNotBlank()) {
                    tracks.add(
                        Song(
                            id = "spot_${parentId}_${idx++}",
                            title = title,
                            artist = artist,
                            album = "",
                            durationMs = 0L,
                            coverUrl = entityCover
                        )
                    )
                }
            }
        }

        return Pair(tracks, entityCover)
    }
}

