package com.auramusic.app.data.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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

class AuraDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "aura_music.db"
        const val DATABASE_VERSION = 2

        // Table Names
        const val TABLE_FAVORITES = "favorites"
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_SONGS = "playlist_songs"
        const val TABLE_DOWNLOADS = "downloads"
        const val TABLE_HISTORY = "history"
        const val TABLE_SEARCH_HISTORY = "search_history"

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
    }

    // ==========================================
    // FAVORITES OPERATIONS
    // ==========================================

    suspend fun getFavorites(): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT id, title, artist, album, durationMs, coverUrl, mediaUri FROM $TABLE_FAVORITES ORDER BY addedAt DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Song(
                        id = it.getString(0),
                        title = it.getString(1),
                        artist = it.getString(2),
                        album = it.getString(3) ?: "",
                        durationMs = it.getLong(4),
                        coverUrl = it.getString(5) ?: "",
                        mediaUri = it.getString(6) ?: "",
                        isFavorite = true
                    )
                )
            }
        }
        list
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
        val cursor = db.rawQuery("""
            SELECT songId, title, artist, album, durationMs, coverUrl, mediaUri
            FROM $TABLE_PLAYLIST_SONGS
            WHERE playlistId = ?
            ORDER BY sortOrder ASC
        """.trimIndent(), arrayOf(playlistId))

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Song(
                        id = it.getString(0),
                        title = it.getString(1),
                        artist = it.getString(2),
                        album = it.getString(3) ?: "",
                        durationMs = it.getLong(4),
                        coverUrl = it.getString(5) ?: "",
                        mediaUri = it.getString(6) ?: ""
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

    suspend fun isDownloaded(songId: String): Boolean = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_DOWNLOADS WHERE songId = ?", arrayOf(songId))
        val exists = cursor.count > 0
        cursor.close()
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
}
