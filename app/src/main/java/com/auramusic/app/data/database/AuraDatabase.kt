package com.auramusic.app.data.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.auramusic.app.data.model.Album
import com.auramusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Playlist(
    val id: String,
    val title: String,
    val description: String = "",
    val coverUrl: String = "",
    val trackCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class DownloadedTrack(
    val song: Song,
    val localFilePath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)

data class ListeningStats(
    val totalPlays: Int = 0,
    val totalMinutes: Long = 0L,
    val topArtist: String = "—",
    val topSongTitle: String = "—"
)

class AuraDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "aura_music.db"
        const val DATABASE_VERSION = 3

        // Table Names
        const val TABLE_FAVORITES = "favorites"
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_SONGS = "playlist_songs"
        const val TABLE_DOWNLOADS = "downloads"
        const val TABLE_HISTORY = "history"
        const val TABLE_SEARCH_HISTORY = "search_history"
        const val TABLE_FOLLOWED_ARTISTS = "followed_artists"
        const val TABLE_SAVED_ALBUMS = "saved_albums"

        @Volatile
        private var instance: AuraDatabase? = null

        fun getInstance(context: Context): AuraDatabase {
            return instance ?: synchronized(this) {
                instance ?: AuraDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Favorites Table
        db.execSQL("""
            CREATE TABLE $TABLE_FAVORITES (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                durationMs INTEGER,
                coverUrl TEXT,
                mediaUri TEXT,
                addedAt INTEGER
            )
        """.trimIndent())

        // Playlists Table
        db.execSQL("""
            CREATE TABLE $TABLE_PLAYLISTS (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT,
                coverUrl TEXT,
                createdAt INTEGER
            )
        """.trimIndent())

        // Playlist Songs Cross-Reference Table
        db.execSQL("""
            CREATE TABLE $TABLE_PLAYLIST_SONGS (
                playlistId TEXT NOT NULL,
                songId TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                durationMs INTEGER,
                coverUrl TEXT,
                mediaUri TEXT,
                sortOrder INTEGER,
                PRIMARY KEY (playlistId, songId),
                FOREIGN KEY (playlistId) REFERENCES $TABLE_PLAYLISTS(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // Offline Downloads Table
        db.execSQL("""
            CREATE TABLE $TABLE_DOWNLOADS (
                songId TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                durationMs INTEGER,
                coverUrl TEXT,
                mediaUri TEXT,
                localFilePath TEXT NOT NULL,
                fileSize INTEGER,
                downloadedAt INTEGER
            )
        """.trimIndent())

        // Listening History Table
        db.execSQL("""
            CREATE TABLE $TABLE_HISTORY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                songId TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                durationMs INTEGER,
                coverUrl TEXT,
                mediaUri TEXT,
                playedAt INTEGER
            )
        """.trimIndent())

        // Search History Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                query TEXT UNIQUE,
                timestamp INTEGER
            )
        """.trimIndent())

        // Followed Artists Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_FOLLOWED_ARTISTS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                coverUrl TEXT,
                subscribers TEXT,
                followedAt INTEGER
            )
        """.trimIndent())

        // Saved Albums Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_SAVED_ALBUMS (
                browseId TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artistOrYear TEXT,
                coverUrl TEXT,
                savedAt INTEGER
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_SEARCH_HISTORY (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    query TEXT UNIQUE,
                    timestamp INTEGER
                )
            """.trimIndent())
        }
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_FOLLOWED_ARTISTS (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    coverUrl TEXT,
                    subscribers TEXT,
                    followedAt INTEGER
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_SAVED_ALBUMS (
                    browseId TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    artistOrYear TEXT,
                    coverUrl TEXT,
                    savedAt INTEGER
                )
            """.trimIndent())
        }
    }

    // ==========================================
    // FAVORITES OPERATIONS
    // ==========================================

    private fun getDownloadMap(db: SQLiteDatabase): Map<String, Pair<String, String>> {
        val map = mutableMapOf<String, Pair<String, String>>()
        try {
            val cursor = db.rawQuery(
                "SELECT songId, title, artist, coverUrl, localFilePath FROM $TABLE_DOWNLOADS",
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    val songId = it.getString(0) ?: ""
                    val title = it.getString(1)?.trim()?.lowercase() ?: ""
                    val artist = it.getString(2)?.trim()?.lowercase() ?: ""
                    val coverUrl = it.getString(3) ?: ""
                    val localPath = it.getString(4) ?: ""

                    if (localPath.isNotBlank() && java.io.File(localPath).exists()) {
                        val pair = Pair(coverUrl, localPath)
                        if (songId.isNotBlank()) map[songId] = pair
                        if (title.isNotBlank()) map["$title|$artist"] = pair
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    suspend fun getFavorites(): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val downloadMap = getDownloadMap(db)
        val cursor: Cursor = db.rawQuery(
            "SELECT id, title, artist, album, durationMs, coverUrl, mediaUri FROM $TABLE_FAVORITES ORDER BY addedAt DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                val title = it.getString(1)
                val artist = it.getString(2)
                val album = it.getString(3) ?: ""
                val durationMs = it.getLong(4)
                var coverUrl = it.getString(5) ?: ""
                var mediaUri = it.getString(6) ?: ""

                val dl = downloadMap[id] ?: downloadMap["${title.trim().lowercase()}|${artist.trim().lowercase()}"]
                if (dl != null) {
                    if (mediaUri.isBlank() || mediaUri.startsWith("http")) {
                        mediaUri = dl.second
                    }
                    if (coverUrl.isBlank() || coverUrl.startsWith("http")) {
                        if (dl.first.isNotBlank() && java.io.File(dl.first).exists()) {
                            coverUrl = dl.first
                        }
                    }
                }

                list.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        coverUrl = coverUrl,
                        mediaUri = mediaUri,
                        isFavorite = true
                    )
                )
            }
        }
        list
    }

    suspend fun updateFavoriteMediaAndCover(songId: String, coverUrl: String, mediaUri: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (coverUrl.isNotBlank()) put("coverUrl", coverUrl)
            if (mediaUri.isNotBlank()) put("mediaUri", mediaUri)
        }
        db.update(TABLE_FAVORITES, values, "id = ?", arrayOf(songId))
    }

    suspend fun isFavorite(songId: String): Boolean = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_FAVORITES WHERE id = ?", arrayOf(songId))
        val exists = cursor.count > 0
        cursor.close()
        exists
    }

    suspend fun addFavorite(song: Song) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("durationMs", song.durationMs)
            put("coverUrl", song.coverUrl)
            put("mediaUri", song.mediaUri)
            put("addedAt", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_FAVORITES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun removeFavorite(songId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_FAVORITES, "id = ?", arrayOf(songId))
    }

    suspend fun toggleFavorite(song: Song): Boolean = withContext(Dispatchers.IO) {
        if (isFavorite(song.id)) {
            removeFavorite(song.id)
            false
        } else {
            addFavorite(song)
            true
        }
    }

    // ==========================================
    // PLAYLIST OPERATIONS
    // ==========================================

    suspend fun createPlaylist(title: String, description: String = ""): Playlist = withContext(Dispatchers.IO) {
        val id = "pl_${System.currentTimeMillis()}"
        val createdAt = System.currentTimeMillis()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("description", description)
            put("coverUrl", "")
            put("createdAt", createdAt)
        }
        db.insert(TABLE_PLAYLISTS, null, values)
        Playlist(id, title, description, "", 0, createdAt)
    }

    suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Playlist>()
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT p.id, p.title, p.description, p.coverUrl, p.createdAt, COUNT(ps.songId) as trackCount
            FROM $TABLE_PLAYLISTS p
            LEFT JOIN $TABLE_PLAYLIST_SONGS ps ON p.id = ps.playlistId
            GROUP BY p.id
            ORDER BY p.createdAt DESC
        """.trimIndent(), null)

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Playlist(
                        id = it.getString(0),
                        title = it.getString(1),
                        description = it.getString(2) ?: "",
                        coverUrl = it.getString(3) ?: "",
                        createdAt = it.getLong(4),
                        trackCount = it.getInt(5)
                    )
                )
            }
        }
        list
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("playlistId", playlistId)
            put("songId", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("durationMs", song.durationMs)
            put("coverUrl", song.coverUrl)
            put("mediaUri", song.mediaUri)
            put("sortOrder", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_PLAYLIST_SONGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getSongsInPlaylist(playlistId: String): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val downloadMap = getDownloadMap(db)
        val cursor = db.rawQuery("""
            SELECT songId, title, artist, album, durationMs, coverUrl, mediaUri
            FROM $TABLE_PLAYLIST_SONGS
            WHERE playlistId = ?
            ORDER BY sortOrder ASC
        """.trimIndent(), arrayOf(playlistId))

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                val title = it.getString(1)
                val artist = it.getString(2)
                val album = it.getString(3) ?: ""
                val durationMs = it.getLong(4)
                var coverUrl = it.getString(5) ?: ""
                var mediaUri = it.getString(6) ?: ""

                val dl = downloadMap[id] ?: downloadMap["${title.trim().lowercase()}|${artist.trim().lowercase()}"]
                if (dl != null) {
                    if (mediaUri.isBlank() || mediaUri.startsWith("http")) {
                        mediaUri = dl.second
                    }
                    if (coverUrl.isBlank() || coverUrl.startsWith("http")) {
                        if (dl.first.isNotBlank() && java.io.File(dl.first).exists()) {
                            coverUrl = dl.first
                        }
                    }
                }

                list.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        coverUrl = coverUrl,
                        mediaUri = mediaUri
                    )
                )
            }
        }
        list
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_PLAYLIST_SONGS, "playlistId = ? AND songId = ?", arrayOf(playlistId, songId))
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_PLAYLIST_SONGS, "playlistId = ?", arrayOf(playlistId))
        db.delete(TABLE_PLAYLISTS, "id = ?", arrayOf(playlistId))
    }

    // ==========================================
    // DOWNLOAD OPERATIONS
    // ==========================================

    suspend fun saveDownload(song: Song, localPath: String, fileSize: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("songId", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("durationMs", song.durationMs)
            put("coverUrl", song.coverUrl)
            put("mediaUri", song.mediaUri)
            put("localFilePath", localPath)
            put("fileSize", fileSize)
            put("downloadedAt", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_DOWNLOADS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun getDownloadedTracks(): List<DownloadedTrack> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DownloadedTrack>()
        val db = readableDatabase
        val cursor = db.rawQuery("""
            SELECT songId, title, artist, album, durationMs, coverUrl, mediaUri, localFilePath, fileSize, downloadedAt
            FROM $TABLE_DOWNLOADS
            ORDER BY downloadedAt DESC
        """.trimIndent(), null)

        cursor.use {
            while (it.moveToNext()) {
                val song = Song(
                    id = it.getString(0),
                    title = it.getString(1),
                    artist = it.getString(2),
                    album = it.getString(3) ?: "",
                    durationMs = it.getLong(4),
                    coverUrl = it.getString(5) ?: "",
                    mediaUri = it.getString(7) // local file URI takes precedence!
                )
                list.add(
                    DownloadedTrack(
                        song = song,
                        localFilePath = it.getString(7),
                        fileSize = it.getLong(8),
                        downloadedAt = it.getLong(9)
                    )
                )
            }
        }
        list
    }

    suspend fun isDownloaded(songId: String, title: String? = null, artist: String? = null): Boolean = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            db.rawQuery("SELECT localFilePath FROM $TABLE_DOWNLOADS WHERE songId = ? OR (LOWER(TRIM(title)) = LOWER(TRIM(?)) AND LOWER(TRIM(artist)) = LOWER(TRIM(?)))", arrayOf(songId, title, artist))
        } else {
            db.rawQuery("SELECT localFilePath FROM $TABLE_DOWNLOADS WHERE songId = ?", arrayOf(songId))
        }
        var exists = false
        cursor.use {
            if (it.moveToFirst()) {
                val path = it.getString(0)
                exists = java.io.File(path).let { f -> f.exists() && f.length() > 0 }
            }
        }
        exists
    }

    suspend fun removeDownload(songId: String) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_DOWNLOADS, "songId = ?", arrayOf(songId))
    }

    // ==========================================
    // HISTORY OPERATIONS
    // ==========================================

    suspend fun addToHistory(song: Song) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("songId", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("durationMs", song.durationMs)
            put("coverUrl", song.coverUrl)
            put("mediaUri", song.mediaUri)
            put("playedAt", System.currentTimeMillis())
        }
        db.insert(TABLE_HISTORY, null, values)

        // Keep only top 50 recent items
        db.execSQL("""
            DELETE FROM $TABLE_HISTORY WHERE id NOT IN (
                SELECT id FROM $TABLE_HISTORY ORDER BY playedAt DESC LIMIT 50
            )
        """.trimIndent())
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_HISTORY, null, null)
    }

    suspend fun getTopArtistsFromHistoryAndFavorites(): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery(
            """
            SELECT artist, coverUrl, COUNT(*) as playCount 
            FROM (
                SELECT artist, coverUrl FROM $TABLE_HISTORY WHERE artist != '' AND artist != 'Unknown Artist'
                UNION ALL
                SELECT artist, coverUrl FROM $TABLE_FAVORITES WHERE artist != '' AND artist != 'Unknown Artist'
            )
            GROUP BY LOWER(artist)
            ORDER BY playCount DESC
            LIMIT 10
            """.trimIndent(),
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val artistName = it.getString(0)
                val cover = it.getString(1) ?: ""
                list.add(
                    Song(
                        id = artistName,
                        title = artistName,
                        artist = "Artista",
                        coverUrl = cover,
                        durationMs = 0L,
                        mediaUri = "",
                        isArtist = true
                    )
                )
            }
        }
        list
    }

    // ==========================================
    // SEARCH HISTORY OPERATIONS
    // ==========================================

    suspend fun insertSearchHistory(query: String) = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("query", cleanQuery)
            put("timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_SEARCH_HISTORY, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        // Keep top 25 recent searches
        try {
            db.execSQL("""
                DELETE FROM $TABLE_SEARCH_HISTORY WHERE id NOT IN (
                    SELECT id FROM $TABLE_SEARCH_HISTORY ORDER BY timestamp DESC LIMIT 25
                )
            """.trimIndent())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getSearchHistory(): List<String> = withContext(Dispatchers.IO) {
        val list = mutableListOf<String>()
        try {
            val db = readableDatabase
            val cursor: Cursor = db.rawQuery(
                "SELECT query FROM $TABLE_SEARCH_HISTORY ORDER BY timestamp DESC LIMIT 20",
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    val q = it.getString(0)
                    if (!q.isNullOrBlank()) list.add(q)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun deleteSearchHistoryItem(query: String) = withContext(Dispatchers.IO) {
        try {
            val db = writableDatabase
            db.delete(TABLE_SEARCH_HISTORY, "query = ?", arrayOf(query))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        try {
            val db = writableDatabase
            db.delete(TABLE_SEARCH_HISTORY, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // AURA WRAPPED STATS & BACKUP (v2.0)
    // ==========================================

    suspend fun getListeningStats(): ListeningStats = withContext(Dispatchers.IO) {
        val db = readableDatabase
        var totalPlays = 0
        var totalDurationMs = 0L
        var topArtist = "—"
        var topSong = "—"

        try {
            db.rawQuery("SELECT COUNT(*), COALESCE(SUM(durationMs), 0) FROM $TABLE_HISTORY", null).use { c ->
                if (c.moveToFirst()) {
                    totalPlays = c.getInt(0)
                    totalDurationMs = c.getLong(1)
                }
            }
            db.rawQuery(
                "SELECT artist, COUNT(*) as cnt FROM $TABLE_HISTORY WHERE artist != '' AND artist != 'Unknown Artist' GROUP BY LOWER(artist) ORDER BY cnt DESC LIMIT 1",
                null
            ).use { c ->
                if (c.moveToFirst()) {
                    topArtist = c.getString(0) ?: "—"
                }
            }
            db.rawQuery(
                "SELECT title, COUNT(*) as cnt FROM $TABLE_HISTORY WHERE title != '' GROUP BY LOWER(title) ORDER BY cnt DESC LIMIT 1",
                null
            ).use { c ->
                if (c.moveToFirst()) {
                    topSong = c.getString(0) ?: "—"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ListeningStats(
            totalPlays = totalPlays,
            totalMinutes = (totalDurationMs / 60000L).coerceAtLeast(if (totalPlays > 0) totalPlays * 3L else 0L),
            topArtist = topArtist,
            topSongTitle = topSong
        )
    }

    // ==========================================
    // FOLLOWED ARTISTS & SAVED ALBUMS
    // ==========================================

    suspend fun getFollowedArtists(): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT id, name, coverUrl, subscribers FROM $TABLE_FOLLOWED_ARTISTS ORDER BY followedAt DESC",
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    val id = it.getString(0) ?: ""
                    val name = it.getString(1) ?: ""
                    val coverUrl = it.getString(2) ?: ""
                    val subs = it.getString(3) ?: "Artista"
                    list.add(
                        Song(
                            id = id.ifBlank { name },
                            title = name,
                            artist = subs.ifBlank { "Artista" },
                            coverUrl = coverUrl,
                            durationMs = 0L,
                            mediaUri = "",
                            isArtist = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun isArtistFollowed(artistId: String, artistName: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT 1 FROM $TABLE_FOLLOWED_ARTISTS WHERE id = ? OR LOWER(name) = LOWER(?)",
                arrayOf(artistId, artistName.ifBlank { artistId })
            )
            val exists = cursor.count > 0
            cursor.close()
            exists
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleFollowArtist(
        artistId: String,
        artistName: String,
        coverUrl: String,
        subscribers: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cleanId = artistId.ifBlank { artistName }
        if (isArtistFollowed(cleanId, artistName)) {
            db.delete(
                TABLE_FOLLOWED_ARTISTS,
                "id = ? OR LOWER(name) = LOWER(?)",
                arrayOf(cleanId, artistName)
            )
            false
        } else {
            val values = ContentValues().apply {
                put("id", cleanId)
                put("name", artistName)
                put("coverUrl", coverUrl)
                put("subscribers", subscribers)
                put("followedAt", System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_FOLLOWED_ARTISTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            true
        }
    }

    suspend fun getSavedAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Album>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT browseId, title, artistOrYear, coverUrl FROM $TABLE_SAVED_ALBUMS ORDER BY savedAt DESC",
                null
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(
                        Album(
                            browseId = it.getString(0) ?: "",
                            title = it.getString(1) ?: "",
                            year = it.getString(2) ?: "",
                            coverUrl = it.getString(3) ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun isAlbumSaved(browseId: String): Boolean = withContext(Dispatchers.IO) {
        if (browseId.isBlank()) return@withContext false
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT 1 FROM $TABLE_SAVED_ALBUMS WHERE browseId = ?",
                arrayOf(browseId)
            )
            val exists = cursor.count > 0
            cursor.close()
            exists
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleSaveAlbum(album: Album): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val id = album.browseId.ifBlank { album.title }
        if (isAlbumSaved(id)) {
            db.delete(TABLE_SAVED_ALBUMS, "browseId = ?", arrayOf(id))
            false
        } else {
            val values = ContentValues().apply {
                put("browseId", id)
                put("title", album.title)
                put("artistOrYear", album.year)
                put("coverUrl", album.coverUrl)
                put("savedAt", System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_SAVED_ALBUMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            true
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = org.json.JSONObject()
        root.put("version", "2.1.3")
        root.put("exportedAt", System.currentTimeMillis())

        val favArray = org.json.JSONArray()
        getFavorites().forEach { s ->
            val obj = org.json.JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album)
                put("durationMs", s.durationMs)
                put("coverUrl", s.coverUrl)
                put("mediaUri", s.mediaUri)
            }
            favArray.put(obj)
        }
        root.put("favorites", favArray)

        val artistsArray = org.json.JSONArray()
        getFollowedArtists().forEach { a ->
            val obj = org.json.JSONObject().apply {
                put("id", a.id)
                put("name", a.title)
                put("coverUrl", a.coverUrl)
                put("subscribers", a.artist)
            }
            artistsArray.put(obj)
        }
        root.put("followedArtists", artistsArray)

        val albumsArray = org.json.JSONArray()
        getSavedAlbums().forEach { alb ->
            val obj = org.json.JSONObject().apply {
                put("browseId", alb.browseId)
                put("title", alb.title)
                put("year", alb.year)
                put("coverUrl", alb.coverUrl)
            }
            albumsArray.put(obj)
        }
        root.put("savedAlbums", albumsArray)

        val playlistsArray = org.json.JSONArray()
        getPlaylists().forEach { pl ->
            val plObj = org.json.JSONObject().apply {
                put("id", pl.id)
                put("title", pl.title)
                put("description", pl.description)
            }
            val songsArray = org.json.JSONArray()
            getSongsInPlaylist(pl.id).forEach { s ->
                val sObj = org.json.JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("artist", s.artist)
                    put("album", s.album)
                    put("durationMs", s.durationMs)
                    put("coverUrl", s.coverUrl)
                    put("mediaUri", s.mediaUri)
                }
                songsArray.put(sObj)
            }
            plObj.put("songs", songsArray)
            playlistsArray.put(plObj)
        }
        root.put("playlists", playlistsArray)
        root.toString(2)
    }

    suspend fun importBackupJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = org.json.JSONObject(jsonStr)
            val favArray = root.optJSONArray("favorites")
            if (favArray != null) {
                for (i in 0 until favArray.length()) {
                    val o = favArray.optJSONObject(i) ?: continue
                    addFavorite(
                        Song(
                            id = o.optString("id"),
                            title = o.optString("title"),
                            artist = o.optString("artist"),
                            album = o.optString("album"),
                            durationMs = o.optLong("durationMs", 210000L),
                            coverUrl = o.optString("coverUrl"),
                            mediaUri = o.optString("mediaUri"),
                            isFavorite = true
                        )
                    )
                }
            }
            val artistsArray = root.optJSONArray("followedArtists")
            if (artistsArray != null) {
                for (i in 0 until artistsArray.length()) {
                    val o = artistsArray.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    val name = o.optString("name")
                    if (name.isNotBlank() && !isArtistFollowed(id, name)) {
                        toggleFollowArtist(id, name, o.optString("coverUrl"), o.optString("subscribers"))
                    }
                }
            }
            val albumsArray = root.optJSONArray("savedAlbums")
            if (albumsArray != null) {
                for (i in 0 until albumsArray.length()) {
                    val o = albumsArray.optJSONObject(i) ?: continue
                    val bId = o.optString("browseId")
                    val title = o.optString("title")
                    if (title.isNotBlank() && !isAlbumSaved(bId.ifBlank { title })) {
                        toggleSaveAlbum(
                            Album(
                                browseId = bId.ifBlank { title },
                                title = title,
                                year = o.optString("year"),
                                coverUrl = o.optString("coverUrl")
                            )
                        )
                    }
                }
            }
            val playlistsArray = root.optJSONArray("playlists")
            if (playlistsArray != null) {
                for (i in 0 until playlistsArray.length()) {
                    val plObj = playlistsArray.optJSONObject(i) ?: continue
                    val title = plObj.optString("title")
                    if (title.isBlank()) continue
                    val created = createPlaylist(title, plObj.optString("description"))
                    val songsArr = plObj.optJSONArray("songs") ?: continue
                    for (j in 0 until songsArr.length()) {
                        val sObj = songsArr.optJSONObject(j) ?: continue
                        addSongToPlaylist(
                            created.id,
                            Song(
                                id = sObj.optString("id"),
                                title = sObj.optString("title"),
                                artist = sObj.optString("artist"),
                                album = sObj.optString("album"),
                                durationMs = sObj.optLong("durationMs", 210000L),
                                coverUrl = sObj.optString("coverUrl"),
                                mediaUri = sObj.optString("mediaUri")
                            )
                        )
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
