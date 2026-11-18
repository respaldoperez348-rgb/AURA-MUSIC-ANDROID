package com.auramusic.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import com.auramusic.app.data.models.Album
import com.auramusic.app.data.models.Artist
import com.auramusic.app.data.models.LyricLine
import com.auramusic.app.data.models.Song
import com.auramusic.app.playback.StreamUrlCache
import com.auramusic.app.utils.InnerTubeXPlayer
import com.auramusic.app.utils.resize
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertubex.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MusicRepository(
    private val context: Context,
    private val streamUrlCache: StreamUrlCache = StreamUrlCache(),
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val searchCache = ConcurrentHashMap<String, List<Song>>()
    private val lyricsCache = ConcurrentHashMap<String, List<LyricLine>>()

    companion object {
        private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
        private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
        private const val NEXT_URL = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
        private val LRC_PATTERN = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)")

        fun getCuratedQuickPicks(): List<Song> = listOf(
            Song(
                id = "4NRXx6U8ABQ",
                title = "Blinding Lights",
                artist = "The Weeknd",
                artistId = "UClYV6hHlupm_S_ObS1W-DYw",
                album = "After Hours",
                albumId = "MPREb_TH6Wut5eTMQ",
                thumbnailUrl = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg",
                durationSeconds = 200
            ),
            Song(
                id = "34Na4j8AVgA",
                title = "Starboy",
                artist = "The Weeknd, Daft Punk",
                artistId = "UClYV6hHlupm_S_ObS1W-DYw",
                album = "Starboy",
                albumId = "MPREb_FWIMEPTHFsY",
                thumbnailUrl = "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg",
                durationSeconds = 230
            ),
            Song(
                id = "saGYMhApaH8",
                title = "Me Porto Bonito",
                artist = "Bad Bunny, Chencho Corleone",
                artistId = "UCiY3z8HAGD6BlSNKVn2kSvQ",
                album = "Un Verano Sin Ti",
                albumId = "MPREb_NMmY1n1NJFS",
                thumbnailUrl = "https://i.ytimg.com/vi/saGYMhApaH8/hqdefault.jpg",
                durationSeconds = 178
            ),
            Song(
                id = "4D7u5KF7SP8",
                title = "Get Lucky",
                artist = "Daft Punk, Pharrell Williams",
                artistId = "UCRr1xG_2WIDs18a6cIiCxeA",
                album = "Random Access Memories",
                albumId = "MPREb_K8qWMWVqXGi",
                thumbnailUrl = "https://i.ytimg.com/vi/4D7u5KF7SP8/hqdefault.jpg",
                durationSeconds = 248
            ),
            Song(
                id = "5g2hT4GmAGU",
                title = "DESPECHÁ",
                artist = "ROSALÍA",
                artistId = "UCeYz6rzUGhVwqxRM37FUo8w",
                album = "MOTOMAMI +",
                albumId = "MPREb_VdtEnJZ2vqp",
                thumbnailUrl = "https://i.ytimg.com/vi/5g2hT4GmAGU/hqdefault.jpg",
                durationSeconds = 157
            ),
            Song(
                id = "TUVcZfQe-Kw",
                title = "Levitating",
                artist = "Dua Lipa",
                artistId = "UCzVb0SIXp9q9PeKCcFjsBtA",
                album = "Future Nostalgia",
                thumbnailUrl = "https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg",
                durationSeconds = 203
            ),
            Song(
                id = "a5uQMwRMHcs",
                title = "Instant Crush",
                artist = "Daft Punk, Julian Casablancas",
                artistId = "UCRr1xG_2WIDs18a6cIiCxeA",
                album = "Random Access Memories",
                albumId = "MPREb_K8qWMWVqXGi",
                thumbnailUrl = "https://i.ytimg.com/vi/a5uQMwRMHcs/hqdefault.jpg",
                durationSeconds = 337
            ),
            Song(
                id = "HyHNuVaZJ-k",
                title = "Feel Good Inc.",
                artist = "Gorillaz",
                artistId = "UCNIV5B_aJnLrKDSnW_MOmcQ",
                album = "Demon Days",
                albumId = "MPREb_2az20wG4HxN",
                thumbnailUrl = "https://i.ytimg.com/vi/HyHNuVaZJ-k/hqdefault.jpg",
                durationSeconds = 223
            ),
            Song(
                id = "60ItHLz5WEA",
                title = "SICKO MODE",
                artist = "Travis Scott",
                artistId = "UCf_gP4AMRSgAfyzbkeS9k4g",
                album = "ASTROWORLD",
                albumId = "MPREb_w7oqKXSwULM",
                thumbnailUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg",
                durationSeconds = 312
            ),
            Song(
                id = "JWRlTezTF2k",
                title = "DANCE CRIP",
                artist = "Trueno",
                artistId = "UCEPYn7xbzvQwL9C21evFGDg",
                album = "BIEN O MAL",
                albumId = "MPREb_h2I0XmdWznM",
                thumbnailUrl = "https://i.ytimg.com/vi/JWRlTezTF2k/hqdefault.jpg",
                durationSeconds = 175
            ),
            Song(
                id = "sVx1mJDeUjY",
                title = "After Dark",
                artist = "Mr.Kitty",
                album = "Time",
                thumbnailUrl = "https://i.ytimg.com/vi/sVx1mJDeUjY/hqdefault.jpg",
                durationSeconds = 259
            ),
            Song(
                id = "8GW6sLrK40k",
                title = "Resonance",
                artist = "HOME",
                album = "Odyssey",
                thumbnailUrl = "https://i.ytimg.com/vi/8GW6sLrK40k/hqdefault.jpg",
                durationSeconds = 212
            )
        )

        fun getCuratedPopularArtists(): List<Artist> = listOf(
            Artist(
                id = "UClYV6hHlupm_S_ObS1W-DYw",
                name = "The Weeknd",
                thumbnailUrl = "https://lh3.googleusercontent.com/U-SAmNOu4TynE818gLCfKsuHZ0U5YNEtO9mrjSI9WCCKERs98LzrCal5kajBBTQNwdcisoB2Bn-pHp4=w544-h544-p-l90-rj",
                monthlyListeners = "115,4 M"
            ),
            Artist(
                id = "UCiY3z8HAGD6BlSNKVn2kSvQ",
                name = "Bad Bunny",
                thumbnailUrl = "https://yt3.googleusercontent.com/ploU_4iWpDoJlX3FOlwwd_yQcex0I8A0_665lePXAEBbNp1zn5g42eNwg5Q7lvYc2mG2--UNIYcIhww=w544-h544-p-l90-rj",
                monthlyListeners = "84,2 M"
            ),
            Artist(
                id = "UCRr1xG_2WIDs18a6cIiCxeA",
                name = "Daft Punk",
                thumbnailUrl = "https://lh3.googleusercontent.com/qLhu6Py_4_xoBsoubKQsXlhOQGqU9YU1ZRAbFusF0LlrPkXbbpu7bEh-k_ZtE4JwLgubvucAQqcK1hRk=w544-h544-p-l90-rj",
                monthlyListeners = "32,8 M"
            ),
            Artist(
                id = "UCzVb0SIXp9q9PeKCcFjsBtA",
                name = "Dua Lipa",
                thumbnailUrl = "https://lh3.googleusercontent.com/aFx8s1fTuelgxONGbezmTG0EKR8r82uB5H-Q6ZJtssyCWLJWF8GfZNr4tHo84sXdFCPBKrA4R6zXOss=w544-h544-p-l90-rj",
                monthlyListeners = "72,1 M"
            ),
            Artist(
                id = "UCeYz6rzUGhVwqxRM37FUo8w",
                name = "Rosalía",
                thumbnailUrl = "https://lh3.googleusercontent.com/gYQfHNwupieC5fsiFqa9P4bjogcTgDg2ChBB_9CnsYbaGy0ZsXgZ8zBufXmcy1zjggMB8poaof2PMA=w544-h544-p-l90-rj",
                monthlyListeners = "41,5 M"
            ),
            Artist(
                id = "UCONiUl5u7y2bMaVZJcuRDEQ",
                name = "Bizarrap",
                thumbnailUrl = "https://yt3.googleusercontent.com/vEkJjs-EZmAYP3ukHzky6kLxLaA1dGuGGUDvJcNQXHBaAXp4xajwptJlT20BXl-qsx18EcQLqJWiUCQ=w544-h544-p-l90-rj",
                monthlyListeners = "46,9 M"
            ),
            Artist(
                id = "UCf_gP4AMRSgAfyzbkeS9k4g",
                name = "Travis Scott",
                thumbnailUrl = "https://yt3.googleusercontent.com/r9k_FpAswxhQnl_cudiaT2ocWFccR6SzEFXgZ9a12iR5eDPSILlIL2EQewyQ-yYSt1JFyH1pqnoBXxs=w544-h544-p-l90-rj",
                monthlyListeners = "68,7 M"
            ),
            Artist(
                id = "UCNIV5B_aJnLrKDSnW_MOmcQ",
                name = "Gorillaz",
                thumbnailUrl = "https://lh3.googleusercontent.com/MQ8sTeoDhSNyEItlabM-Bl1kFgCa4r1Rog38K8nokZ8vAIAgoN0omxEk5G6lXBj5azaaUZAvH7xhuRA=w544-h544-p-l90-rj",
                monthlyListeners = "35,6 M"
            ),
            Artist(
                id = "UCprAFmT0C6O4X0ToEXpeFTQ",
                name = "Kendrick Lamar",
                thumbnailUrl = "https://yt3.googleusercontent.com/uB8Magh99SvDyT_mcDYeNYxlVZ_F9WN-cJtAFMHw_Q-_N_8y5-uZiay8-EZSKKloNoWxymBzVehSF4PN=w544-h544-p-l90-rj",
                monthlyListeners = "79,3 M"
            ),
            Artist(
                id = "UCIaFw5VBEK8qaW6nRpx_qnw",
                name = "Coldplay",
                thumbnailUrl = "https://lh3.googleusercontent.com/IOKuXtp8PCQ_Fc-vaRKm3sKIXBxFV51gZheLTH5br-YGnWHFQf_Jywcuk7wbprYRoEbQyS_XZY6-nMJX=w544-h544-p-l90-rj",
                monthlyListeners = "88,1 M"
            ),
            Artist(
                id = "UCedvOgsKFzcK3hA5taf3KoQ",
                name = "Eminem",
                thumbnailUrl = "https://lh3.googleusercontent.com/JFI6JZrS-Lco4UdpqDfHY5Wgwy51VXWxmNdI7bCBU5CDlIpN6WWyisZ7MGlpjbrxEGYMFpsqoR_UwcE=w544-h544-p-l90-rj",
                monthlyListeners = "77,9 M"
            ),
            Artist(
                id = "UCEPYn7xbzvQwL9C21evFGDg",
                name = "Trueno",
                thumbnailUrl = "https://yt3.googleusercontent.com/HupoNCBrq4WWflprOMqHDPxE9VSf8cGeSNmS8DZPlJQxGtj1Jju8rZf_HGPLk2d2_w1SZK-JwmTwew=w544-h544-p-l90-rj",
                monthlyListeners = "24,2 M"
            )
        )

        fun getCuratedFeaturedAlbums(): List<Album> = listOf(
            Album(
                id = "MPREb_TH6Wut5eTMQ",
                title = "After Hours",
                artist = "The Weeknd",
                year = "2020",
                thumbnailUrl = "https://yt3.googleusercontent.com/JDKz3Anlyo49xBhFcFx13QD_Tk4-kqdiYTo15gtkL93nE8biWyZ7o0BPyW6RnXVxcXaJ5DgU5nJ_0NjJ=w544-h544-l90-rj",
                songCount = 14
            ),
            Album(
                id = "MPREb_FWIMEPTHFsY",
                title = "Starboy",
                artist = "The Weeknd",
                year = "2016",
                thumbnailUrl = "https://yt3.googleusercontent.com/dcxXIIlest09vnvKznWM9VWQXu1EL7lKxBzXGzwgmVjmMNBm1dEWT_0qn1xrEZYyKF_qRE1TLq8P_JY_mQ=w544-h544-l90-rj",
                songCount = 18
            ),
            Album(
                id = "MPREb_NMmY1n1NJFS",
                title = "Un Verano Sin Ti",
                artist = "Bad Bunny",
                year = "2022",
                thumbnailUrl = "https://yt3.googleusercontent.com/0Ke36SSY53TQbmpqzKP4hMPoGiWYQiGI0KJ2CzcwjW4ore9vUMUmyzEi7irEU2MR3FCpNPdwIIuNslS6hg=w544-h544-l90-rj",
                songCount = 23
            ),
            Album(
                id = "MPREb_K8qWMWVqXGi",
                title = "Random Access Memories",
                artist = "Daft Punk",
                year = "2013",
                thumbnailUrl = "https://yt3.googleusercontent.com/N55arCGj69gtw6thXK8JUPisxoVYiwuIEQ7I6SGlkEyNcSJ7xIWPe76Vuu1SiUqRyx5w9qvR_zV8fV3CWQ=w544-h544-l90-rj",
                songCount = 13
            ),
            Album(
                id = "MPREb_VdtEnJZ2vqp",
                title = "MOTOMAMI",
                artist = "Rosalía",
                year = "2022",
                thumbnailUrl = "https://yt3.googleusercontent.com/NcPO6bbPE3euQSRNRwFC0FvRJ-TEzKRql9-FeyO6c0CP1Ow0YdPZhfDFgnEpro7N1rhZ3LxOxKUjqNo=w544-h544-l90-rj",
                songCount = 16
            ),
            Album(
                id = "MPREb_w7oqKXSwULM",
                title = "ASTROWORLD",
                artist = "Travis Scott",
                year = "2018",
                thumbnailUrl = "https://yt3.googleusercontent.com/PSIZ9cf9hpESZwcSz2ylS5I-zIREqCSagxV-X4CJqefrE0sRCktRtFw-a7PlkLygmg7k1nZREKCaSzY=w544-h544-l90-rj",
                songCount = 17
            ),
            Album(
                id = "MPREb_2az20wG4HxN",
                title = "Demon Days",
                artist = "Gorillaz",
                year = "2005",
                thumbnailUrl = "https://yt3.googleusercontent.com/_3LJKVh0dy-71CwQzWKt0q7JHI7M2rxIMGGAdT0wEhmGEEELRX98ACJFdKK5Fyl226nuNfQ0OAMc-JtP=w544-h544-l90-rj",
                songCount = 15
            ),
            Album(
                id = "MPREb_h2I0XmdWznM",
                title = "BIEN O MAL",
                artist = "Trueno",
                year = "2022",
                thumbnailUrl = "https://yt3.googleusercontent.com/G5eyMcGCE8xBp6Qq6NNHpL6GlbgtrqA5NrHWrm9849QlBFLyWCJOMY8e3bB9BDeuwZ692zKOVfE0cBID=w544-h544-l90-rj",
                songCount = 13
            )
        )
    }

    suspend fun fetchHomeFeed(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"browseId":"FEmusic_home"}"""
            val req = Request.Builder()
                .url(BROWSE_URL)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .apply { YouTubeAuthManager.applyAuthHeaders(this) }
                .build()

            val songs = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (!body.isNullOrBlank()) {
                    val root = JSONObject(body)
                    fun collectTracks(obj: Any?) {
                        when (obj) {
                            is JSONObject -> {
                                val col = obj.optJSONObject("musicResponsiveListItemRenderer")
                                if (col != null) {
                                    val parsed = extractSongFromListItem(col)
                                    if (parsed != null && seenIds.add(parsed.id)) {
                                        songs.add(parsed)
                                    }
                                }
                                val keys = obj.keys()
                                while (keys.hasNext()) {
                                    collectTracks(obj.opt(keys.next()))
                                }
                            }
                            is JSONArray -> {
                                for (i in 0 until obj.length()) {
                                    collectTracks(obj.opt(i))
                                }
                            }
                        }
                    }
                    collectTracks(root)
                }
            }

            if (songs.size >= 8) return@withContext songs
        } catch (e: Exception) {
            Timber.e(e, "Error en fetchHomeFeed")
        }

        val searched = searchSongs("Top Hits Éxitos Globales")
        if (searched.isNotEmpty()) searched else getCuratedQuickPicks()
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val effectiveQuery = query.trim().ifBlank { "Top Hits Éxitos Globales" }
        searchCache[effectiveQuery.lowercase()]?.let { return@withContext it }

        val combined = LinkedHashMap<String, Song>()

        // 1. Direct WEB_REMIX search (como en AURA-MUSIC-ANDROID)
        try {
            val escaped = effectiveQuery.replace("\\", "\\\\").replace("\"", "\\\"")
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"query":"$escaped"}"""
            val req = Request.Builder()
                .url(SEARCH_URL)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .apply { YouTubeAuthManager.applyAuthHeaders(this) }
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val rawBody = resp.body?.string()
                if (!rawBody.isNullOrBlank()) {
                    parseWebRemixSearchSongs(rawBody).forEach { song ->
                        combined[song.id] = song
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error en WEB_REMIX searchSongs")
        }

        // 2. Complemento con Innertube FILTER_SONG
        try {
            val result = YouTube.search(effectiveQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            result?.items?.filterIsInstance<SongItem>()?.forEach { songItem ->
                if (!combined.containsKey(songItem.id)) {
                    val cover = normalizeCover(songItem.thumbnail.resize(544, 544), songItem.id)
                    combined[songItem.id] = Song(
                        id = songItem.id,
                        title = songItem.title,
                        artist = songItem.artists.joinToString(", ") { it.name }.ifBlank { "Artista" },
                        artistId = songItem.artists.firstOrNull()?.id,
                        album = songItem.album?.name,
                        albumId = songItem.album?.id,
                        thumbnailUrl = cover,
                        durationSeconds = songItem.duration?.toLong() ?: 210L
                    )
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error en Innertube FILTER_SONG")
        }

        val finalResults = combined.values.toList()
        if (finalResults.isNotEmpty()) {
            searchCache[effectiveQuery.lowercase()] = finalResults
        }
        finalResults
    }

    suspend fun searchArtists(query: String): List<Artist> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext getCuratedPopularArtists()

        val foundArtists = LinkedHashMap<String, Artist>()

        try {
            coroutineScope {
                val webRemixDeferred = async {
                    try {
                        val escaped = trimmed.replace("\\", "\\\\").replace("\"", "\\\"")
                        val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"query":"$escaped"}"""
                        val req = Request.Builder()
                            .url(SEARCH_URL)
                            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                            .header("User-Agent", USER_AGENT)
                            .header("Content-Type", "application/json")
                            .build()
                        httpClient.newCall(req).execute().use { resp ->
                            resp.body?.string()?.let { parseWebRemixSearchArtists(it) } ?: emptyList()
                        }
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                val summaryDeferred = async { YouTube.searchSummary(trimmed).getOrNull() }
                val filterDeferred = async { YouTube.search(trimmed, YouTube.SearchFilter.FILTER_ARTIST).getOrNull() }

                webRemixDeferred.await().forEach { artist ->
                    foundArtists[artist.id] = artist
                }

                val summary = summaryDeferred.await()
                val filter = filterDeferred.await()

                summary?.summaries?.flatMap { it.items }?.filterIsInstance<ArtistItem>()?.forEach { item ->
                    if (!foundArtists.containsKey(item.id)) {
                        foundArtists[item.id] = Artist(
                            id = item.id,
                            name = item.title,
                            thumbnailUrl = normalizeCover((item.thumbnail ?: "").resize(544, 544), ""),
                            monthlyListeners = null
                        )
                    }
                }

                filter?.items?.filterIsInstance<ArtistItem>()?.forEach { item ->
                    if (!foundArtists.containsKey(item.id)) {
                        foundArtists[item.id] = Artist(
                            id = item.id,
                            name = item.title,
                            thumbnailUrl = normalizeCover((item.thumbnail ?: "").resize(544, 544), ""),
                            monthlyListeners = null
                        )
                    }
                }

                if (foundArtists.isEmpty()) {
                    summary?.summaries?.flatMap { it.items }?.filterIsInstance<SongItem>()?.forEach { song ->
                        val firstArtist = song.artists.firstOrNull()
                        if (firstArtist != null) {
                            val artId = firstArtist.id ?: "artist_${firstArtist.name.replace(" ", "_")}"
                            if (!foundArtists.containsKey(artId)) {
                                foundArtists[artId] = Artist(
                                    id = artId,
                                    name = firstArtist.name,
                                    thumbnailUrl = normalizeCover(song.thumbnail.resize(544, 544), song.id),
                                    monthlyListeners = null
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al buscar artistas para: $query")
        }

        foundArtists.values.toList()
    }

    suspend fun searchAlbums(query: String): List<Album> = withContext(Dispatchers.IO) {
        try {
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
            result?.items?.filterIsInstance<AlbumItem>()?.map { albumItem ->
                Album(
                    id = albumItem.id,
                    title = albumItem.title,
                    artist = albumItem.artists?.joinToString(", ") { it.name } ?: "",
                    year = albumItem.year?.toString(),
                    thumbnailUrl = normalizeCover(albumItem.thumbnail.resize(544, 544), "")
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Error al buscar álbumes para query: $query")
            emptyList()
        }
    }

    /**
     * Obtiene el perfil completo del artista (igual que fetchArtistProfile en AURA-MUSIC-ANDROID):
     * - Si no viene con un browseId UC..., lo resuelve automáticamente por nombre.
     * - Extrae foto Hero, oyentes/suscriptores, biografía, canciones populares, álbumes, sencillos y artistas relacionados.
     */
    suspend fun getArtistDetails(artistIdOrName: String, fallbackArtist: Artist? = null): Artist? = withContext(Dispatchers.IO) {
        try {
            var browseId = artistIdOrName.trim().removePrefix("artist_").replace("_", " ")
            val artistDisplayName = fallbackArtist?.name ?: browseId

            if (!browseId.startsWith("UC")) {
                val searchEscaped = artistDisplayName.replace("\\", "\\\\").replace("\"", "\\\"")
                val searchPayload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"query":"$searchEscaped"}"""
                val searchReq = Request.Builder()
                    .url(SEARCH_URL)
                    .post(searchPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("User-Agent", USER_AGENT)
                    .header("Content-Type", "application/json")
                    .build()

                httpClient.newCall(searchReq).execute().use { resp ->
                    val sBody = resp.body?.string().orEmpty()
                    val foundId = Regex("\"browseId\":\\s*\"(UC[a-zA-Z0-9_-]+)\"").find(sBody)?.groupValues?.get(1)
                    if (!foundId.isNullOrBlank()) {
                        browseId = foundId
                    }
                }
            }

            if (browseId.startsWith("UC")) {
                val browsePayload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"browseId":"$browseId"}"""
                val browseReq = Request.Builder()
                    .url(BROWSE_URL)
                    .post(browsePayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("User-Agent", USER_AGENT)
                    .header("Content-Type", "application/json")
                    .build()

                httpClient.newCall(browseReq).execute().use { resp ->
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = JSONObject(body)
                        val header = root.optJSONObject("header")?.optJSONObject("musicImmersiveHeaderRenderer")
                            ?: root.optJSONObject("header")?.optJSONObject("musicVisualHeaderRenderer")

                        val resolvedName = header?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: fallbackArtist?.name ?: artistDisplayName

                        val bio = header?.optJSONObject("description")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")

                        val heroThumbs = header?.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        val rawHero = if (heroThumbs != null && heroThumbs.length() > 0) {
                            heroThumbs.getJSONObject(heroThumbs.length() - 1).optString("url")
                        } else ""
                        val heroUrl = normalizeCover(rawHero, "").ifBlank { fallbackArtist?.thumbnailUrl.orEmpty() }

                        val subButton = header?.optJSONObject("subscriptionButton")?.optJSONObject("subscribeButtonRenderer")
                        val subscribers = subButton?.optJSONObject("subscriberCountText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: fallbackArtist?.monthlyListeners

                        val contents = root.optJSONObject("contents")
                            ?.optJSONObject("singleColumnBrowseResultsRenderer")
                            ?.optJSONArray("tabs")?.optJSONObject(0)
                            ?.optJSONObject("tabRenderer")?.optJSONObject("content")
                            ?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")

                        val topTracks = mutableListOf<Song>()
                        val albums = mutableListOf<Album>()
                        val singles = mutableListOf<Album>()
                        val relatedArtists = mutableListOf<Artist>()

                        if (contents != null) {
                            for (i in 0 until contents.length()) {
                                val sec = contents.optJSONObject(i) ?: continue

                                val shelf = sec.optJSONObject("musicShelfRenderer")
                                if (shelf != null) {
                                    val items = shelf.optJSONArray("contents") ?: continue
                                    for (j in 0 until items.length()) {
                                        val col = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                                        val song = extractSongFromListItem(col, heroUrl)
                                        if (song != null) topTracks.add(song)
                                    }
                                }

                                val carousel = sec.optJSONObject("musicCarouselShelfRenderer")
                                if (carousel != null) {
                                    val carTitle = carousel.optJSONObject("header")
                                        ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                                        ?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""

                                    val isAlbumSec = carTitle.contains("lbum", ignoreCase = true)
                                    val isSingleSec = carTitle.contains("single", ignoreCase = true) ||
                                        carTitle.contains("sencillo", ignoreCase = true) ||
                                        carTitle.contains("ep", ignoreCase = true)
                                    val isRelatedSec = carTitle.contains("fan", ignoreCase = true) ||
                                        carTitle.contains("similar", ignoreCase = true) ||
                                        carTitle.contains("relacionad", ignoreCase = true) ||
                                        carTitle.contains("like", ignoreCase = true)

                                    val cItems = carousel.optJSONArray("contents") ?: continue
                                    for (k in 0 until cItems.length()) {
                                        val twoRow = cItems.optJSONObject(k)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                                        val itemTitle = twoRow.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: ""
                                        val subRuns = twoRow.optJSONObject("subtitle")?.optJSONArray("runs")
                                        val subText = if (subRuns != null) {
                                            (0 until subRuns.length()).joinToString(" ") { subRuns.getJSONObject(it).optString("text") }
                                        } else ""
                                        val itemBrowseId = twoRow.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId") ?: ""
                                        val itemThumbs = twoRow.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                                            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                        val rawCover = if (itemThumbs != null && itemThumbs.length() > 0) {
                                            itemThumbs.getJSONObject(itemThumbs.length() - 1).optString("url")
                                        } else ""
                                        val itemCover = normalizeCover(rawCover, "")

                                        if (isAlbumSec || isSingleSec) {
                                            if (itemTitle.isNotBlank() && itemBrowseId.isNotBlank()) {
                                                val yearMatch = Regex("\\b(19|20)\\d{2}\\b").find(subText)?.value
                                                val albumObj = Album(
                                                    id = itemBrowseId,
                                                    title = itemTitle,
                                                    artist = resolvedName,
                                                    year = yearMatch ?: subText.takeIf { it.isNotBlank() },
                                                    thumbnailUrl = itemCover.ifBlank { heroUrl }
                                                )
                                                if (isAlbumSec) albums.add(albumObj) else singles.add(albumObj)
                                            }
                                        } else if (isRelatedSec) {
                                            if (itemTitle.isNotBlank() && itemCover.isNotBlank()) {
                                                relatedArtists.add(
                                                    Artist(
                                                        id = itemBrowseId.ifBlank { "artist_$itemTitle" },
                                                        name = itemTitle,
                                                        thumbnailUrl = itemCover
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val finalTracks = topTracks.ifEmpty { searchSongs("$resolvedName Top Hits") }
                        val finalAlbums = albums.ifEmpty { searchAlbums(resolvedName) }

                        return@withContext Artist(
                            id = browseId,
                            name = resolvedName,
                            thumbnailUrl = heroUrl.ifBlank { finalTracks.firstOrNull()?.thumbnailUrl.orEmpty() },
                            monthlyListeners = subscribers ?: fallbackArtist?.monthlyListeners ?: "45,2 M",
                            bio = bio,
                            topSongs = finalTracks,
                            albums = finalAlbums,
                            singles = singles,
                            relatedArtists = relatedArtists
                        )
                    }
                }
            }

            // Fallback por búsqueda directa
            val tracks = searchSongs(artistDisplayName)
            val albums = searchAlbums(artistDisplayName)
            Artist(
                id = browseId,
                name = artistDisplayName,
                thumbnailUrl = fallbackArtist?.thumbnailUrl?.takeIf { it.isNotBlank() }
                    ?: tracks.firstOrNull()?.thumbnailUrl.orEmpty(),
                monthlyListeners = fallbackArtist?.monthlyListeners ?: "38,5 M",
                topSongs = tracks,
                albums = albums
            )
        } catch (e: Exception) {
            Timber.e(e, "Error en getArtistDetails para $artistIdOrName")
            fallbackArtist
        }
    }

    /**
     * Obtiene todas las pistas reales de un álbum (igual que fetchAlbumTracks en AURA-MUSIC-ANDROID).
     */
    suspend fun getAlbumDetails(albumId: String, fallbackAlbum: Album? = null): Album? = withContext(Dispatchers.IO) {
        try {
            val tracks = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()
            var resolvedTitle = fallbackAlbum?.title.orEmpty()
            var resolvedArtist = fallbackAlbum?.artist.orEmpty()
            var resolvedCover = fallbackAlbum?.thumbnailUrl.orEmpty()
            var resolvedYear = fallbackAlbum?.year

            if (albumId.isNotBlank()) {
                val actualBrowseId = if (albumId.startsWith("PL") && !albumId.startsWith("VL")) "VL$albumId" else albumId
                val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"browseId":"$actualBrowseId"}"""
                val req = Request.Builder()
                    .url(BROWSE_URL)
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("User-Agent", USER_AGENT)
                    .header("Content-Type", "application/json")
                    .apply { YouTubeAuthManager.applyAuthHeaders(this) }
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = JSONObject(body)
                        val two = root.optJSONObject("contents")?.optJSONObject("twoColumnBrowseResultsRenderer")
                        val single = root.optJSONObject("contents")?.optJSONObject("singleColumnBrowseResultsRenderer")
                        val tabs = (two?.optJSONArray("tabs") ?: single?.optJSONArray("tabs"))
                            ?.optJSONObject(0)?.optJSONObject("tabRenderer")
                            ?.optJSONObject("content")?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")

                        val header = tabs?.optJSONObject(0)?.optJSONObject("musicResponsiveHeaderRenderer")
                            ?: root.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")
                            ?: root.optJSONObject("header")?.optJSONObject("musicResponsiveHeaderRenderer")

                        val headerTitle = header?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: header?.optJSONObject("title")?.optString("text").orEmpty()
                        if (headerTitle.isNotBlank()) resolvedTitle = headerTitle

                        val coverThumbs = header?.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            ?: header?.optJSONObject("thumbnail")?.optJSONObject("croppedSquareThumbnailRenderer")
                                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        if (coverThumbs != null && coverThumbs.length() > 0) {
                            val rawCover = coverThumbs.getJSONObject(coverThumbs.length() - 1).optString("url")
                            if (rawCover.isNotBlank()) resolvedCover = normalizeCover(rawCover, "")
                        }

                        val subRuns = header?.optJSONObject("subtitle")?.optJSONArray("runs")
                        if (subRuns != null) {
                            for (sIdx in 0 until subRuns.length()) {
                                val cand = subRuns.getJSONObject(sIdx).optString("text").trim()
                                if (cand.matches(Regex("^\\d{4}$"))) {
                                    resolvedYear = cand
                                } else if (cand.isNotBlank() && cand != "•" &&
                                    !cand.equals("Álbum", true) && !cand.equals("Album", true) &&
                                    !cand.equals("Single", true) && !cand.equals("EP", true) &&
                                    resolvedArtist.isBlank()
                                ) {
                                    resolvedArtist = cand
                                }
                            }
                        }

                        fun findTracksRecursive(obj: Any?) {
                            when (obj) {
                                is JSONObject -> {
                                    val col = obj.optJSONObject("musicResponsiveListItemRenderer")
                                    if (col != null) {
                                        val song = extractSongFromAlbumItem(col, resolvedArtist, resolvedTitle, resolvedCover)
                                        if (song != null && seenIds.add(song.id)) {
                                            tracks.add(song)
                                        }
                                    }
                                    val keys = obj.keys()
                                    while (keys.hasNext()) {
                                        findTracksRecursive(obj.opt(keys.next()))
                                    }
                                }
                                is JSONArray -> {
                                    for (idx in 0 until obj.length()) {
                                        findTracksRecursive(obj.opt(idx))
                                    }
                                }
                            }
                        }
                        findTracksRecursive(root)
                    }
                }
            }

            // Fallback a Innertube YouTube.album si aún está vacío
            if (tracks.isEmpty()) {
                val page = YouTube.album(albumId).getOrNull()
                if (page != null) {
                    if (resolvedTitle.isBlank()) resolvedTitle = page.album.title
                    if (resolvedArtist.isBlank()) resolvedArtist = page.album.artists?.joinToString(", ") { it.name }.orEmpty()
                    if (resolvedCover.isBlank()) resolvedCover = normalizeCover(page.album.thumbnail.resize(544, 544), "")
                    page.songs.forEach { songItem ->
                        if (seenIds.add(songItem.id)) {
                            tracks.add(
                                Song(
                                    id = songItem.id,
                                    title = songItem.title,
                                    artist = songItem.artists.joinToString(", ") { it.name }.ifBlank { resolvedArtist },
                                    artistId = songItem.artists.firstOrNull()?.id,
                                    album = resolvedTitle,
                                    albumId = albumId,
                                    thumbnailUrl = normalizeCover((songItem.thumbnail ?: page.album.thumbnail).resize(544, 544), songItem.id),
                                    durationSeconds = songItem.duration?.toLong() ?: 210L
                                )
                            )
                        }
                    }
                }
            }

            // Último fallback por búsqueda si es un ID personalizado
            if (tracks.isEmpty() && resolvedTitle.isNotBlank()) {
                val q = "$resolvedTitle $resolvedArtist".trim()
                tracks.addAll(searchSongs(q))
            }

            Album(
                id = albumId,
                title = resolvedTitle.ifBlank { "Álbum" },
                artist = resolvedArtist.ifBlank { "Artista" },
                year = resolvedYear ?: "2024",
                thumbnailUrl = resolvedCover.ifBlank { tracks.firstOrNull()?.thumbnailUrl.orEmpty() },
                songCount = tracks.size,
                songs = tracks
            )
        } catch (e: Exception) {
            Timber.e(e, "Error al obtener álbum $albumId")
            fallbackAlbum
        }
    }

    /**
     * Radio automática (RDAMVM<videoId>) como en AURA-MUSIC-ANDROID para reproducción continua.
     */
    suspend fun fetchRelatedTracks(videoId: String, currentTitle: String, currentArtist: String): List<Song> = withContext(Dispatchers.IO) {
        val related = mutableListOf<Song>()
        try {
            if (videoId.length == 11) {
                val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}},"videoId":"$videoId","playlistId":"RDAMVM$videoId","enablePersistentPlaylistPanel":true,"isAudioOnly":true}"""
                val req = Request.Builder()
                    .url(NEXT_URL)
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("User-Agent", USER_AGENT)
                    .header("Content-Type", "application/json")
                    .apply { YouTubeAuthManager.applyAuthHeaders(this) }
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    val body = resp.body?.string()
                    if (!body.isNullOrBlank() && resp.isSuccessful) {
                        val root = JSONObject(body)
                        val contents = root.optJSONObject("contents")
                            ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                            ?.optJSONObject("tabbedRenderer")
                            ?.optJSONObject("watchNextTabbedResultsRenderer")
                            ?.optJSONArray("tabs")?.optJSONObject(0)
                            ?.optJSONObject("tabRenderer")?.optJSONObject("content")
                            ?.optJSONObject("musicQueueRenderer")?.optJSONObject("content")
                            ?.optJSONObject("playlistPanelRenderer")?.optJSONArray("contents")

                        if (contents != null) {
                            for (i in 0 until contents.length()) {
                                val panelItem = contents.optJSONObject(i)?.optJSONObject("playlistPanelVideoRenderer") ?: continue
                                val vid = panelItem.optString("videoId")
                                if (vid.length != 11 || vid == videoId) continue
                                val title = panelItem.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text") ?: continue
                                val bylineRuns = panelItem.optJSONObject("longBylineText")?.optJSONArray("runs")
                                    ?: panelItem.optJSONObject("shortBylineText")?.optJSONArray("runs")
                                val artist = bylineRuns?.optJSONObject(0)?.optString("text") ?: currentArtist
                                val lengthStr = panelItem.optJSONObject("lengthText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                                    ?: panelItem.optJSONObject("lengthText")?.optString("simpleText").orEmpty()
                                val durSec = parseDurationSeconds(lengthStr) ?: 210L

                                val thumbs = panelItem.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                                val rawThumb = if (thumbs != null && thumbs.length() > 0) {
                                    thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                                } else ""

                                related.add(
                                    Song(
                                        id = vid,
                                        title = title,
                                        artist = artist,
                                        thumbnailUrl = normalizeCover(rawThumb, vid),
                                        durationSeconds = durSec
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error en fetchRelatedTracks")
        }

        if (related.isNotEmpty()) return@withContext related.distinctBy { it.id }.take(20)
        searchSongs("$currentArtist Top Hits").filter { it.id != videoId }.take(15)
    }

    /**
     * Letras sincronizadas desde LRCLIB (como en LyricsEngine.kt de AURA-MUSIC-ANDROID).
     */
    suspend fun fetchSyncedLyrics(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        lyricsCache[song.id]?.let { return@withContext it }

        try {
            val cleanTitle = song.title.replace(Regex("(?i)\\(.*?video.*?\\)|\\[.*?\\]|\\(official.*?\\)"), "").trim()
            val cleanArtist = song.artist.split(",", "&", "feat.", "ft.").firstOrNull()?.trim().orEmpty()
            val url = "https://lrclib.net/api/search".toHttpUrlOrNull()?.newBuilder()
                ?.addQueryParameter("track_name", cleanTitle)
                ?.addQueryParameter("artist_name", cleanArtist)
                ?.build() ?: return@withContext emptyList()

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "AuraMusic/3.0 (https://github.com/auramusic/app)")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return@withContext emptyList()
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val synced = item.optString("syncedLyrics")
                        if (synced.isNotBlank() && synced != "null") {
                            val parsed = parseLrcLyrics(synced)
                            if (parsed.isNotEmpty()) {
                                lyricsCache[song.id] = parsed
                                return@withContext parsed
                            }
                        }
                    }
                    val plain = arr.getJSONObject(0).optString("plainLyrics")
                    if (plain.isNotBlank() && plain != "null") {
                        val lines = plain.lines().map { it.trim() }.filter { it.isNotBlank() }
                        val stepMs = ((song.durationSeconds.coerceAtLeast(120L) * 1000L) / lines.size.coerceAtLeast(1))
                        val parsed = lines.mapIndexed { idx, txt -> LyricLine(idx * stepMs, txt) }
                        lyricsCache[song.id] = parsed
                        return@withContext parsed
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error obteniendo letras para ${song.title}")
        }
        emptyList()
    }

    private fun parseLrcLyrics(lrc: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        lrc.lines().forEach { line ->
            val matcher = LRC_PATTERN.matcher(line.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toDoubleOrNull() ?: 0.0
                val text = matcher.group(3)?.trim().orEmpty()
                if (text.isNotBlank()) {
                    val timeMs = (min * 60 * 1000L) + (sec * 1000.0).toLong()
                    result.add(LyricLine(timeMs, text))
                }
            }
        }
        return result.sortedBy { it.timeMs }
    }

    suspend fun resolveStreamUrl(songId: String): Pair<String, Map<String, String>>? = withContext(Dispatchers.IO) {
        // 1. Verificar si está descargada localmente en el dispositivo
        val localFile = File(File(context.filesDir, "downloads"), "$songId.m4a")
        if (localFile.exists() && localFile.length() > 10_240L) {
            return@withContext Pair(localFile.toURI().toString(), emptyMap())
        }

        streamUrlCache[songId]?.let { return@withContext Pair(it.url, it.requestHeaders) }

        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val playbackResult = InnerTubeXPlayer.playerResponseForPlayback(
                videoId = songId,
                connectivityManager = connectivityManager
            )
            val playbackData = playbackResult.getOrNull()
            val streamUrl = playbackData?.streamUrl

            if (streamUrl != null) {
                streamUrlCache.put(
                    mediaId = songId,
                    url = streamUrl,
                    requestHeaders = playbackData.streamHeaders,
                    clientName = playbackData.streamClient,
                    expiresInSeconds = playbackData.streamExpiresInSeconds
                )
                return@withContext Pair(streamUrl, playbackData.streamHeaders)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error al resolver stream con InnerTubeXPlayer para $songId")
        }

        try {
            val playerResponse = YouTube.player(videoId = songId, client = YouTubeClient.WEB_REMIX).getOrNull()
            val streamUrl = playerResponse?.streamingData?.adaptiveFormats
                ?.filter { it.isAudio }
                ?.maxByOrNull { it.bitrate }
                ?.url

            if (streamUrl != null) {
                val headers = mapOf("User-Agent" to USER_AGENT)
                streamUrlCache.put(
                    mediaId = songId,
                    url = streamUrl,
                    requestHeaders = headers,
                    expiresInSeconds = playerResponse.streamingData?.expiresInSeconds ?: (3600 * 5)
                )
                return@withContext Pair(streamUrl, headers)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error en fallback player para $songId")
        }

        null
    }

    /**
     * Autocompletado de sugerencias de canciones y artistas en tiempo real.
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        try {
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(body)
                if (jsonArray.length() > 1) {
                    val suggestionsArray = jsonArray.optJSONArray(1) ?: return@withContext emptyList()
                    val result = mutableListOf<String>()
                    for (i in 0 until suggestionsArray.length()) {
                        val s = suggestionsArray.optString(i)
                        if (s.isNotBlank() && !result.contains(s)) {
                            result.add(s)
                        }
                    }
                    return@withContext result.take(8)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error en getSearchSuggestions")
        }
        emptyList()
    }

    /**
     * Descarga real del archivo de audio al almacenamiento interno de la app con reporte de progreso (0..100%).
     */
    suspend fun downloadSongToDisk(song: Song, onProgress: (Int) -> Unit = {}): String? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = File(context.filesDir, "downloads").apply { mkdirs() }
            val targetFile = File(downloadsDir, "${song.id}.m4a")
            if (targetFile.exists() && targetFile.length() > 10_240L) {
                onProgress(100)
                return@withContext targetFile.absolutePath
            }

            val resolved = resolveStreamUrl(song.id) ?: return@withContext null
            val (streamUrl, headers) = resolved
            if (streamUrl.startsWith("file:")) {
                onProgress(100)
                return@withContext targetFile.absolutePath
            }

            val downloadClient = httpClient.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val reqBuilder = Request.Builder().url(streamUrl).get()
            headers.forEach { (k, v) -> reqBuilder.header(k, v) }
            if (!headers.containsKey("User-Agent")) {
                reqBuilder.header("User-Agent", USER_AGENT)
            }

            downloadClient.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body ?: return@withContext null
                val totalBytes = body.contentLength()
                val tempFile = File(downloadsDir, "${song.id}.tmp")
                var bytesCopied = 0L
                var lastReportedPercent = -1

                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            if (totalBytes > 0) {
                                val pct = ((bytesCopied * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                if (pct != lastReportedPercent && (pct - lastReportedPercent >= 5 || pct == 0)) {
                                    lastReportedPercent = pct
                                    onProgress(pct)
                                }
                            }
                        }
                    }
                }
                if (tempFile.length() > 10_240L) {
                    tempFile.renameTo(targetFile)
                    onProgress(100)
                    return@withContext targetFile.absolutePath
                } else {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error descargando canción ${song.title}")
        }
        null
    }

    private fun parseWebRemixSearchSongs(jsonStr: String): List<Song> {
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

                val cardShelf = sec.optJSONObject("musicCardShelfRenderer")
                if (cardShelf != null) {
                    val cardContents = cardShelf.optJSONArray("contents")
                    if (cardContents != null) {
                        for (c in 0 until cardContents.length()) {
                            val col = cardContents.optJSONObject(c)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            extractSongFromListItem(col)?.let { results.add(it) }
                        }
                    }
                }

                val shelf = sec.optJSONObject("musicShelfRenderer")
                if (shelf != null) {
                    val contents = shelf.optJSONArray("contents") ?: continue
                    for (k in 0 until contents.length()) {
                        val col = contents.optJSONObject(k)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                        extractSongFromListItem(col)?.let { results.add(it) }
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    private fun parseWebRemixSearchArtists(jsonStr: String): List<Artist> {
        val artists = mutableListOf<Artist>()
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

                val cardShelf = sec.optJSONObject("musicCardShelfRenderer")
                if (cardShelf != null) {
                    val titleRuns = cardShelf.optJSONObject("title")?.optJSONArray("runs")
                    val cardTitle = titleRuns?.optJSONObject(0)?.optString("text").orEmpty()
                    val browseId = titleRuns?.optJSONObject(0)?.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
                    val subRuns = cardShelf.optJSONObject("subtitle")?.optJSONArray("runs")
                    val subText = if (subRuns != null) {
                        (0 until subRuns.length()).joinToString("") { subRuns.getJSONObject(it).optString("text") }
                    } else ""
                    val cardThumbs = cardShelf.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val rawCover = if (cardThumbs != null && cardThumbs.length() > 0) {
                        cardThumbs.getJSONObject(cardThumbs.length() - 1).optString("url")
                    } else ""

                    if (cardTitle.isNotBlank() && (browseId.startsWith("UC") || subText.contains("Artista", true) || subText.contains("Artist", true))) {
                        artists.add(
                            Artist(
                                id = browseId.ifBlank { "artist_$cardTitle" },
                                name = cardTitle,
                                thumbnailUrl = normalizeCover(rawCover, "")
                            )
                        )
                    }
                }

                val shelf = sec.optJSONObject("musicShelfRenderer")
                if (shelf != null) {
                    val contents = shelf.optJSONArray("contents") ?: continue
                    for (k in 0 until contents.length()) {
                        val col = contents.optJSONObject(k)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                        val flexCols = col.optJSONArray("flexColumns") ?: continue
                        if (flexCols.length() < 2) continue
                        val flex0 = flexCols.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        val flex1 = flexCols.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        val title = flex0?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
                        val subRuns = flex1?.optJSONObject("text")?.optJSONArray("runs")
                        val subText = if (subRuns != null) {
                            (0 until subRuns.length()).joinToString("") { subRuns.getJSONObject(it).optString("text") }
                        } else ""
                        val browseId = col.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
                        if (title.isNotBlank() && (browseId.startsWith("UC") || subText.contains("Artista", true) || subText.contains("Artist", true))) {
                            val thumbs = col.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val rawCover = if (thumbs != null && thumbs.length() > 0) {
                                thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                            } else ""
                            artists.add(
                                Artist(
                                    id = browseId.ifBlank { "artist_$title" },
                                    name = title,
                                    thumbnailUrl = normalizeCover(rawCover, "")
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return artists
    }

    private fun extractSongFromListItem(col: JSONObject, fallbackCover: String = ""): Song? {
        try {
            val flexColumns = col.optJSONArray("flexColumns") ?: return null
            if (flexColumns.length() < 2) return null

            val flex0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            val flex1 = flexColumns.getJSONObject(1).optJSONObject("musicResponsiveListItemFlexColumnRenderer")

            val titleRuns = flex0?.optJSONObject("text")?.optJSONArray("runs")
                ?: flex0?.optJSONObject("title")?.optJSONArray("runs")
            val title = titleRuns?.optJSONObject(0)?.optString("text").orEmpty()
            if (title.isBlank()) return null

            var videoId = col.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId").orEmpty()

            if (videoId.isBlank()) {
                videoId = titleRuns?.optJSONObject(0)?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()
            }
            if (videoId.isBlank()) {
                videoId = col.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
            }
            if (videoId.length != 11) return null

            val artistRuns = flex1?.optJSONObject("text")?.optJSONArray("runs")
                ?: flex1?.optJSONObject("title")?.optJSONArray("runs")
            var artist = "Artista"
            var artistId: String? = null
            var durationSec: Long? = null

            if (artistRuns != null) {
                for (i in 0 until artistRuns.length()) {
                    val runObj = artistRuns.getJSONObject(i)
                    val candidate = runObj.optString("text").trim()
                    if (candidate.isEmpty() || candidate == "•" || candidate == "&" || candidate == ",") continue
                    val dur = parseDurationSeconds(candidate)
                    if (dur != null) {
                        durationSec = dur
                        continue
                    }
                    if (artist == "Artista" &&
                        !candidate.equals("Canción", true) &&
                        !candidate.equals("Song", true) &&
                        !candidate.equals("Video", true) &&
                        !candidate.equals("Vídeo", true) &&
                        !candidate.equals("Single", true) &&
                        !candidate.equals("Sencillo", true) &&
                        !candidate.contains("reproducciones", true) &&
                        !candidate.contains("views", true) &&
                        !candidate.contains("plays", true)
                    ) {
                        artist = candidate
                        artistId = runObj.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("browseEndpoint")?.optString("browseId")
                    }
                }
            }

            if (durationSec == null) {
                val fixed0 = col.optJSONArray("fixedColumns")?.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                val fixedText = fixed0?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: fixed0?.optJSONObject("text")?.optString("simpleText")
                if (!fixedText.isNullOrBlank()) {
                    durationSec = parseDurationSeconds(fixedText)
                }
            }

            if (durationSec != null && (durationSec > 600L || durationSec in 1L..35L)) return null

            val thumbnails = col.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            val rawThumb = if (thumbnails != null && thumbnails.length() > 0) {
                thumbnails.getJSONObject(thumbnails.length() - 1).optString("url")
            } else fallbackCover

            return Song(
                id = videoId,
                title = title,
                artist = artist,
                artistId = artistId,
                thumbnailUrl = normalizeCover(rawThumb, videoId),
                durationSeconds = durationSec ?: 210L
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun extractSongFromAlbumItem(
        col: JSONObject,
        defaultArtist: String,
        defaultAlbum: String,
        defaultCover: String
    ): Song? {
        try {
            val flexColumns = col.optJSONArray("flexColumns") ?: return null
            if (flexColumns.length() == 0) return null
            val flex0 = flexColumns.getJSONObject(0).optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            val titleRuns = flex0?.optJSONObject("text")?.optJSONArray("runs")
                ?: flex0?.optJSONObject("title")?.optJSONArray("runs")
            val title = titleRuns?.optJSONObject(0)?.optString("text")?.trim().orEmpty()
            if (title.isBlank()) return null

            var videoId = col.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId").orEmpty()
            if (videoId.isBlank()) {
                videoId = titleRuns?.optJSONObject(0)?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()
            }
            if (videoId.isBlank()) {
                videoId = col.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
            }
            if (videoId.length != 11) return null

            var durationSec = 210L
            val fixed0 = col.optJSONArray("fixedColumns")?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
            val fixedText = fixed0?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                ?: fixed0?.optJSONObject("text")?.optString("simpleText")
            if (!fixedText.isNullOrBlank()) {
                parseDurationSeconds(fixedText)?.let { durationSec = it }
            }

            return Song(
                id = videoId,
                title = title,
                artist = defaultArtist.ifBlank { "Artista" },
                album = defaultAlbum,
                thumbnailUrl = normalizeCover(defaultCover, videoId),
                durationSeconds = durationSec
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseDurationSeconds(text: String): Long? {
        val parts = text.trim().split(":")
        if (parts.size !in 2..3 || !parts.all { it.isNotEmpty() && it.all(Char::isDigit) }) return null
        return try {
            if (parts.size == 2) parts[0].toLong() * 60L + parts[1].toLong()
            else parts[0].toLong() * 3600L + parts[1].toLong() * 60L + parts[2].toLong()
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeCover(raw: String, videoId: String): String {
        var url = raw.trim()
        if (url.startsWith("//")) url = "https:$url"
        if (url.startsWith("http://")) url = "https://" + url.removePrefix("http://")
        if (url.contains("=w") && url.contains("-h")) {
            url = url.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
        } else if (url.contains("i.ytimg.com/vi/") && url.contains("?")) {
            url = url.substringBefore("?")
        }
        if (url.isBlank() && videoId.length == 11) {
            url = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }
        return url
    }
}
