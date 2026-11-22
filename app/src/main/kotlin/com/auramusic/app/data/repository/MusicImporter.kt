package com.auramusic.app.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.auramusic.app.data.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class ImportedPlaylistResult(
    val playlistTitle: String,
    val songs: List<Song>,
    val sourceLabel: String,
)

/**
 * Motor de importación de música para AURA Music:
 * - Enlaces públicos de Spotify, Apple Music, Deezer y YouTube Music (sin pedir contraseñas)
 * - Escáner de música local del teléfono (MediaStore)
 * - Importación y exportación CSV (compatible con TuneMyMusic)
 */
object MusicImporter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val MOBILE_UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

    private val SPOTIFY_REGEX = Regex("(?:open\\.spotify\\.com/(?:intl-[a-zA-Z-]+/)?(?:embed/)?|spotify:)(playlist|album)[/:]([A-Za-z0-9]{22})")
    private val DEEZER_REGEX = Regex("deezer\\.com/(?:[a-z]{2}/)?(playlist|album)/(\\d+)")
    private val APPLE_REGEX = Regex("music\\.apple\\.com/[a-z]{2}/(playlist|album)/(?:[^/?#]+/)?(pl\\.[A-Za-z0-9-]+|\\d+)")
    private val YT_LIST_REGEX = Regex("[?&]list=([A-Za-z0-9_-]+)")

    fun requiredAudioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasAudioPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, requiredAudioPermission()) == PackageManager.PERMISSION_GRANTED

    /**
     * Escanea los archivos de audio reales guardados en el dispositivo.
     */
    suspend fun scanLocalMusic(context: Context): List<Song> = withContext(Dispatchers.IO) {
        if (!hasAudioPermission(context)) return@withContext emptyList()
        val songs = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: continue
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == MediaStore.UNKNOWN_STRING) "Artista desconocido" else rawArtist
                    val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                    val durSec = (cursor.getLong(durCol) / 1000L).coerceAtLeast(1L)
                    val artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        cursor.getLong(albumIdCol)
                    ).toString()

                    songs.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            thumbnailUrl = artUri,
                            durationSeconds = durSec,
                            localFilePath = uri
                        )
                    )
                }
            }
        }.onFailure { Timber.w(it, "Error escaneando música local") }
        songs
    }

    /**
     * Importa una playlist o álbum desde un enlace público de Spotify, Deezer, Apple Music o YouTube Music.
     */
    suspend fun importFromLink(
        rawLink: String,
        repository: MusicRepository,
        onProgress: (processed: Int, total: Int) -> Unit,
    ): ImportedPlaylistResult = withContext(Dispatchers.IO) {
        val link = Regex("https?://\\S+").find(rawLink.trim())?.value?.trimEnd('.', ',', ')', '"', '\'')
            ?: throw IllegalArgumentException("Pega un enlace válido de Spotify, Deezer, Apple Music o YouTube Music.")

        // YouTube Music playlist directa
        val ytMatch = YT_LIST_REGEX.find(link)
        if ((link.contains("youtube.com") || link.contains("youtu.be")) && ytMatch != null) {
            val listId = ytMatch.groupValues[1]
            val album = repository.getAlbumDetails(if (listId.startsWith("VL")) listId else "VL$listId")
            val songs = album?.songs.orEmpty()
            if (songs.isEmpty()) throw IllegalStateException("No se pudieron leer canciones de esta lista de YouTube.")
            onProgress(songs.size, songs.size)
            return@withContext ImportedPlaylistResult(album?.title ?: "YouTube Playlist", songs, "YouTube Music")
        }

        // Deezer API pública
        val deezerMatch = DEEZER_REGEX.find(link)
        if (deezerMatch != null) {
            val (type, id) = deezerMatch.destructured
            val apiUrl = "https://api.deezer.com/$type/$id"
            val body = httpGetText(apiUrl) ?: throw IllegalStateException("No se pudo conectar con Deezer.")
            val json = JSONObject(body)
            val title = json.optString("title").ifBlank { "Deezer $type" }
            val tracksArr = json.optJSONObject("tracks")?.optJSONArray("data") ?: JSONArray()
            val rawPairs = (0 until tracksArr.length()).mapNotNull { i ->
                val obj = tracksArr.optJSONObject(i) ?: return@mapNotNull null
                val t = obj.optString("title")
                val a = obj.optJSONObject("artist")?.optString("name").orEmpty()
                if (t.isBlank()) null else t to a
            }
            val matched = matchTracksWithYouTube(rawPairs, repository, onProgress)
            return@withContext ImportedPlaylistResult(title, matched, "Deezer")
        }

        // Spotify Embed pública (sin login)
        val spotifyMatch = SPOTIFY_REGEX.find(link)
        if (spotifyMatch != null) {
            val (type, id) = spotifyMatch.destructured
            val html = httpGetText("https://open.spotify.com/embed/$type/$id")
                ?: throw IllegalStateException("No se pudo leer el enlace de Spotify.")
            val nextData = Regex("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.get(1)
                ?: throw IllegalStateException("Spotify no devolvió datos públicos para este enlace.")
            val entity = JSONObject(nextData)
                .optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("state")
                ?.optJSONObject("data")?.optJSONObject("entity")
                ?: throw IllegalStateException("Esta playlist de Spotify es privada o no existe.")
            val title = entity.optString("name").ifBlank { entity.optString("title").ifBlank { "Spotify $type" } }
            val trackList = entity.optJSONArray("trackList") ?: JSONArray()
            val rawPairs = (0 until trackList.length()).mapNotNull { i ->
                val obj = trackList.optJSONObject(i) ?: return@mapNotNull null
                val t = obj.optString("title")
                val a = obj.optString("subtitle").replace('\u00A0', ' ')
                if (t.isBlank()) null else t to a
            }
            val matched = matchTracksWithYouTube(rawPairs, repository, onProgress)
            return@withContext ImportedPlaylistResult(title, matched, "Spotify")
        }

        // Apple Music público
        val appleMatch = APPLE_REGEX.find(link)
        if (appleMatch != null) {
            val html = httpGetText(link.substringBefore('#'))
                ?: throw IllegalStateException("No se pudo leer el enlace de Apple Music.")
            val jsonText = Regex("<script[^>]*id=\"serialized-server-data\"[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.get(1)
                ?: throw IllegalStateException("Apple Music no devolvió datos públicos.")
            val rootArr = JSONArray(jsonText)
            val pageData = rootArr.optJSONObject(0)?.optJSONObject("data")
            val sections = pageData?.optJSONArray("sections") ?: JSONArray()
            var playlistTitle = "Apple Music"
            val rawPairs = mutableListOf<Pair<String, String>>()
            for (s in 0 until sections.length()) {
                val sec = sections.optJSONObject(s) ?: continue
                val kind = sec.optString("itemKind")
                val items = sec.optJSONArray("items") ?: continue
                if (kind == "containerDetailHeaderLockup" && items.length() > 0) {
                    playlistTitle = items.optJSONObject(0)?.optString("title").orEmpty().ifBlank { playlistTitle }
                } else if (kind == "trackLockup") {
                    for (k in 0 until items.length()) {
                        val tr = items.optJSONObject(k) ?: continue
                        val t = tr.optString("title")
                        val a = tr.optString("artistName")
                        if (t.isNotBlank()) rawPairs.add(t to a)
                    }
                }
            }
            val matched = matchTracksWithYouTube(rawPairs, repository, onProgress)
            return@withContext ImportedPlaylistResult(playlistTitle, matched, "Apple Music")
        }

        throw IllegalArgumentException("Usa un enlace de Spotify, Deezer, Apple Music o YouTube Music.")
    }

    suspend fun importFromCsv(
        context: Context,
        uri: Uri,
        repository: MusicRepository,
        onProgress: (processed: Int, total: Int) -> Unit,
    ): ImportedPlaylistResult = withContext(Dispatchers.IO) {
        val rawPairs = mutableListOf<Pair<String, String>>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                val header = reader.readLine() ?: return@use
                val cols = parseCsvLine(header).map { it.lowercase().trim() }
                val titleIdx = cols.indexOfFirst { it.contains("title") || it.contains("track") || it.contains("song") || it.contains("título") }.coerceAtLeast(0)
                val artistIdx = cols.indexOfFirst { it.contains("artist") || it.contains("artista") }.let { if (it >= 0) it else 1 }

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = parseCsvLine(line.orEmpty())
                    val t = parts.getOrNull(titleIdx)?.trim().orEmpty()
                    val a = parts.getOrNull(artistIdx)?.trim().orEmpty()
                    if (t.isNotBlank()) rawPairs.add(t to a)
                }
            }
        }
        if (rawPairs.isEmpty()) throw IllegalStateException("El archivo CSV no contiene canciones válidas.")
        val matched = matchTracksWithYouTube(rawPairs, repository, onProgress)
        ImportedPlaylistResult("CSV Importado", matched, "CSV")
    }

    suspend fun exportLibraryToCsv(
        context: Context,
        uri: Uri,
        favorites: List<Song>,
        playlists: List<UserPlaylist>,
    ): Int = withContext(Dispatchers.IO) {
        val allSongs = (favorites + playlists.flatMap { it.songs }).distinctBy { it.id }
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write("Title,Artist,Album,Playlist,YouTubeId\n")
            favorites.forEach { s ->
                writer.write("${csvEscape(s.title)},${csvEscape(s.artist)},${csvEscape(s.album.orEmpty())},Favorito,${csvEscape(s.id)}\n")
            }
            playlists.forEach { pl ->
                pl.songs.forEach { s ->
                    writer.write("${csvEscape(s.title)},${csvEscape(s.artist)},${csvEscape(s.album.orEmpty())},${csvEscape(pl.title)},${csvEscape(s.id)}\n")
                }
            }
        }
        allSongs.size
    }

    private suspend fun matchTracksWithYouTube(
        rawPairs: List<Pair<String, String>>,
        repository: MusicRepository,
        onProgress: (processed: Int, total: Int) -> Unit,
    ): List<Song> {
        val matched = mutableListOf<Song>()
        val total = rawPairs.size
        rawPairs.forEachIndexed { idx, (title, artist) ->
            val query = "$title $artist".trim()
            val found = runCatching { repository.searchSongs(query).firstOrNull() }.getOrNull()
            if (found != null) {
                matched.add(found)
            }
            onProgress(idx + 1, total)
        }
        return matched.distinctBy { it.id }
    }

    private fun httpGetText(url: String): String? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_UA)
            .header("Accept-Language", "en-US,en;q=0.9")
            .get()
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString())
        return result
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
