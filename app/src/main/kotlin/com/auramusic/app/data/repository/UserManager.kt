package com.auramusic.app.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auramusic.app.MainActivity
import com.auramusic.app.R
import com.auramusic.app.data.models.AccentColor
import com.auramusic.app.data.models.Album
import com.auramusic.app.data.models.AppTheme
import com.auramusic.app.data.models.Artist
import com.auramusic.app.data.models.AudioQualityOption
import com.auramusic.app.data.models.Song
import com.auramusic.app.data.models.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "aura_user_prefs")

data class UserProfile(
    val name: String,
    val email: String,
    val avatarUrl: String = "",
    val isGoogleAccount: Boolean = false,
)

data class UserPlaylist(
    val id: String,
    val title: String,
    val songs: List<Song> = emptyList(),
)

private class AuraLocalDb(context: Context) : SQLiteOpenHelper(context, "aura_library.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorites (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                artistId TEXT,
                album TEXT,
                albumId TEXT,
                thumbnailUrl TEXT NOT NULL,
                durationSeconds INTEGER DEFAULT 0,
                addedAt INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS followed_artists (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                thumbnailUrl TEXT NOT NULL,
                monthlyListeners TEXT,
                addedAt INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_albums (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                year TEXT,
                thumbnailUrl TEXT NOT NULL,
                songCount INTEGER DEFAULT 0,
                addedAt INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS downloads (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                thumbnailUrl TEXT NOT NULL,
                durationSeconds INTEGER DEFAULT 0,
                localFilePath TEXT,
                downloadedAt INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS playlists (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                songsJson TEXT NOT NULL,
                createdAt INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS play_history (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                artistId TEXT,
                album TEXT,
                albumId TEXT,
                thumbnailUrl TEXT NOT NULL,
                durationSeconds INTEGER DEFAULT 0,
                playCount INTEGER DEFAULT 1,
                lastPlayedAt INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }
}

class UserManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dbHelper = AuraLocalDb(context)

    companion object {
        private const val DOWNLOAD_CHANNEL_ID = "aura_downloads_channel"
    }

    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
    private val KEY_USER_AVATAR = stringPreferencesKey("user_avatar")
    private val KEY_IS_GOOGLE = stringPreferencesKey("is_google")
    private val KEY_YT_COOKIE = stringPreferencesKey("yt_cookie")
    private val KEY_APP_ICON = stringPreferencesKey("app_icon")
    private val KEY_APP_THEME = stringPreferencesKey("app_theme")
    private val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
    private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _favorites = MutableStateFlow<List<Song>>(emptyList())
    val favorites: StateFlow<List<Song>> = _favorites.asStateFlow()

    private val _followedArtists = MutableStateFlow<List<Artist>>(emptyList())
    val followedArtists: StateFlow<List<Artist>> = _followedArtists.asStateFlow()

    private val _savedAlbums = MutableStateFlow<List<Album>>(emptyList())
    val savedAlbums: StateFlow<List<Album>> = _savedAlbums.asStateFlow()

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val userPlaylists: StateFlow<List<UserPlaylist>> = _userPlaylists.asStateFlow()

    private val _mostListenedSongs = MutableStateFlow<List<Song>>(emptyList())
    val mostListenedSongs: StateFlow<List<Song>> = _mostListenedSongs.asStateFlow()

    private val _currentAppIcon = MutableStateFlow("VIOLET")
    val currentAppIcon: StateFlow<String> = _currentAppIcon.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        ensureDownloadNotificationChannel()
        scope.launch {
            val prefs = context.dataStore.data.first()
            val name = prefs[KEY_USER_NAME]
            val email = prefs[KEY_USER_EMAIL]
            val avatar = prefs[KEY_USER_AVATAR].orEmpty()
            val isGoogle = prefs[KEY_IS_GOOGLE] == "true"
            val cookie = prefs[KEY_YT_COOKIE].orEmpty()
            val icon = prefs[KEY_APP_ICON] ?: "VIOLET"
            _currentAppIcon.value = icon

            val themeStr = prefs[KEY_APP_THEME] ?: AppTheme.LIGHT.name
            val accentStr = prefs[KEY_ACCENT_COLOR] ?: AccentColor.VIOLET.name
            val qualityStr = prefs[KEY_AUDIO_QUALITY] ?: AudioQualityOption.HIGH.name

            _userSettings.value = UserSettings(
                theme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.LIGHT),
                accentColor = runCatching { AccentColor.valueOf(accentStr) }.getOrDefault(AccentColor.VIOLET),
                audioQuality = runCatching { AudioQualityOption.valueOf(qualityStr) }.getOrDefault(AudioQualityOption.HIGH)
            )

            if (cookie.isNotBlank()) {
                YouTubeAuthManager.setCookie(cookie)
            }

            if (!name.isNullOrBlank() && !email.isNullOrBlank()) {
                _currentUser.value = UserProfile(name, email, avatar, isGoogle)
            }

            reloadFromDb()
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
        scope.launch {
            delay(3200)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        _userSettings.value = newSettings
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_APP_THEME] = newSettings.theme.name
                prefs[KEY_ACCENT_COLOR] = newSettings.accentColor.name
                prefs[KEY_AUDIO_QUALITY] = newSettings.audioQuality.name
            }
        }
    }

    private fun ensureDownloadNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Descargas de AURA Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones de progreso y finalización de descargas de canciones"
                setShowBadge(true)
            }
            nm?.createNotificationChannel(channel)
        }
    }

    private fun postDownloadNotification(
        notificationId: Int,
        title: String,
        subtitle: String,
        progress: Int = -1,
        ongoing: Boolean = false
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(
                    if (ongoing) android.R.drawable.stat_sys_download
                    else android.R.drawable.stat_sys_download_done
                )
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            if (progress in 0..99) {
                builder.setProgress(100, progress, progress == 0)
            } else {
                builder.setProgress(0, 0, false)
            }

            nm.notify(notificationId, builder.build())
        } catch (_: Exception) {}
    }

    private fun reloadFromDb() {
        val db = dbHelper.readableDatabase

        // Favoritos
        val favList = mutableListOf<Song>()
        db.rawQuery("SELECT id, title, artist, artistId, album, albumId, thumbnailUrl, durationSeconds FROM favorites ORDER BY addedAt DESC", null).use { c ->
            while (c.moveToNext()) {
                favList.add(
                    Song(
                        id = c.getString(0),
                        title = c.getString(1),
                        artist = c.getString(2),
                        artistId = c.getString(3),
                        album = c.getString(4),
                        albumId = c.getString(5),
                        thumbnailUrl = c.getString(6),
                        durationSeconds = c.getLong(7),
                        isFavorite = true
                    )
                )
            }
        }
        _favorites.value = favList
        val favIds = favList.map { it.id }.toSet()

        // Artistas seguidos
        val artistList = mutableListOf<Artist>()
        db.rawQuery("SELECT id, name, thumbnailUrl, monthlyListeners FROM followed_artists ORDER BY addedAt DESC", null).use { c ->
            while (c.moveToNext()) {
                artistList.add(
                    Artist(
                        id = c.getString(0),
                        name = c.getString(1),
                        thumbnailUrl = c.getString(2),
                        monthlyListeners = c.getString(3),
                        isFollowing = true
                    )
                )
            }
        }
        _followedArtists.value = artistList

        // Álbumes guardados
        val albumList = mutableListOf<Album>()
        db.rawQuery("SELECT id, title, artist, year, thumbnailUrl, songCount FROM saved_albums ORDER BY addedAt DESC", null).use { c ->
            while (c.moveToNext()) {
                albumList.add(
                    Album(
                        id = c.getString(0),
                        title = c.getString(1),
                        artist = c.getString(2),
                        year = c.getString(3),
                        thumbnailUrl = c.getString(4),
                        songCount = c.getInt(5)
                    )
                )
            }
        }
        _savedAlbums.value = albumList

        // Descargas
        val dlList = mutableListOf<Song>()
        db.rawQuery("SELECT id, title, artist, album, thumbnailUrl, durationSeconds, localFilePath FROM downloads ORDER BY downloadedAt DESC", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                dlList.add(
                    Song(
                        id = id,
                        title = c.getString(1),
                        artist = c.getString(2),
                        album = c.getString(3),
                        thumbnailUrl = c.getString(4),
                        durationSeconds = c.getLong(5),
                        localFilePath = c.getString(6),
                        isFavorite = favIds.contains(id)
                    )
                )
            }
        }
        _downloadedSongs.value = dlList

        // Playlists
        val plList = mutableListOf<UserPlaylist>()
        db.rawQuery("SELECT id, title, songsJson FROM playlists ORDER BY createdAt DESC", null).use { c ->
            while (c.moveToNext()) {
                val plId = c.getString(0)
                val plTitle = c.getString(1)
                val jsonStr = c.getString(2)
                val songs = parseSongsJson(jsonStr, favIds)
                plList.add(UserPlaylist(id = plId, title = plTitle, songs = songs))
            }
        }
        _userPlaylists.value = plList

        // Tus más escuchados
        val historyList = mutableListOf<Song>()
        db.rawQuery(
            "SELECT id, title, artist, artistId, album, albumId, thumbnailUrl, durationSeconds FROM play_history ORDER BY playCount DESC, lastPlayedAt DESC LIMIT 15",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                historyList.add(
                    Song(
                        id = id,
                        title = c.getString(1),
                        artist = c.getString(2),
                        artistId = c.getString(3),
                        album = c.getString(4),
                        albumId = c.getString(5),
                        thumbnailUrl = c.getString(6),
                        durationSeconds = c.getLong(7),
                        isFavorite = favIds.contains(id)
                    )
                )
            }
        }
        _mostListenedSongs.value = historyList
    }

    fun login(name: String, email: String, isGoogle: Boolean = false, avatarUrl: String = "", cookie: String = "") {
        val user = UserProfile(name, email, avatarUrl, isGoogle)
        _currentUser.value = user
        if (cookie.isNotBlank()) {
            YouTubeAuthManager.setCookie(cookie)
        }
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_NAME] = name
                prefs[KEY_USER_EMAIL] = email
                prefs[KEY_USER_AVATAR] = avatarUrl
                prefs[KEY_IS_GOOGLE] = if (isGoogle) "true" else "false"
                if (cookie.isNotBlank()) {
                    prefs[KEY_YT_COOKIE] = cookie
                }
            }
        }
    }

    fun loginWithGoogleCookie(cookie: String, onComplete: () -> Unit = {}) {
        scope.launch {
            YouTubeAuthManager.setCookie(cookie)
            val (name, handle, avatar) = YouTubeAuthManager.fetchAccountDetailsFromCookie(cookie)
            login(
                name = name,
                email = handle,
                isGoogle = true,
                avatarUrl = avatar,
                cookie = cookie
            )
            showToast("✓ Cuenta de Google vinculada: $name")
            onComplete()
        }
    }

    fun logout() {
        _currentUser.value = null
        YouTubeAuthManager.clearCookie()
        scope.launch {
            val db = dbHelper.writableDatabase
            db.delete("favorites", null, null)
            db.delete("followed_artists", null, null)
            db.delete("saved_albums", null, null)
            db.delete("downloads", null, null)
            db.delete("playlists", null, null)
            db.delete("play_history", null, null)
            reloadFromDb()

            context.dataStore.edit { prefs ->
                prefs.remove(KEY_USER_NAME)
                prefs.remove(KEY_USER_EMAIL)
                prefs.remove(KEY_USER_AVATAR)
                prefs.remove(KEY_IS_GOOGLE)
                prefs.remove(KEY_YT_COOKIE)
            }
        }
    }

    fun recordSongPlay(song: Song) {
        scope.launch {
            val db = dbHelper.writableDatabase
            var currentCount = 0
            db.rawQuery("SELECT playCount FROM play_history WHERE id = ?", arrayOf(song.id)).use { c ->
                if (c.moveToFirst()) {
                    currentCount = c.getInt(0)
                }
            }
            val cv = ContentValues().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("artistId", song.artistId)
                put("album", song.album)
                put("albumId", song.albumId)
                put("thumbnailUrl", song.thumbnailUrl)
                put("durationSeconds", song.durationSeconds)
                put("playCount", currentCount + 1)
                put("lastPlayedAt", System.currentTimeMillis())
            }
            db.insertWithOnConflict("play_history", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            reloadFromDb()
        }
    }

    fun isSongFavorite(song: Song): Boolean {
        val cleanTitle = song.title.trim()
        val cleanArtist = song.artist.trim()
        return _favorites.value.any { fav ->
            fav.id == song.id ||
                (cleanTitle.isNotBlank() && fav.title.trim().equals(cleanTitle, ignoreCase = true) &&
                    fav.artist.trim().equals(cleanArtist, ignoreCase = true))
        }
    }

    fun toggleFavorite(song: Song): Boolean {
        val existingFav = _favorites.value.find { fav ->
            fav.id == song.id ||
                (fav.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                    fav.artist.trim().equals(song.artist.trim(), ignoreCase = true))
        }
        val currentlyFav = existingFav != null
        val newFavState = !currentlyFav

        scope.launch {
            val db = dbHelper.writableDatabase
            if (currentlyFav && existingFav != null) {
                db.delete("favorites", "id = ?", arrayOf(existingFav.id))
                showToast("Eliminado de Favoritos: ${song.title}")
            } else {
                val cv = ContentValues().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("artistId", song.artistId)
                    put("album", song.album)
                    put("albumId", song.albumId)
                    put("thumbnailUrl", song.thumbnailUrl)
                    put("durationSeconds", song.durationSeconds)
                    put("addedAt", System.currentTimeMillis())
                }
                db.insertWithOnConflict("favorites", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                showToast("♥ Añadido a Favoritos: ${song.title}")
            }
            reloadFromDb()
        }
        return newFavState
    }

    fun isFavorite(songId: String): Boolean {
        return _favorites.value.any { it.id == songId }
    }

    fun toggleFollowArtist(artist: Artist): Boolean {
        val currentlyFollowing = _followedArtists.value.any {
            it.id == artist.id || it.name.equals(artist.name, ignoreCase = true)
        }
        val newFollowState = !currentlyFollowing
        scope.launch {
            val db = dbHelper.writableDatabase
            if (currentlyFollowing) {
                db.delete("followed_artists", "id = ? OR LOWER(name) = LOWER(?)", arrayOf(artist.id, artist.name))
                showToast("Dejaste de seguir a ${artist.name}")
            } else {
                val cv = ContentValues().apply {
                    put("id", artist.id)
                    put("name", artist.name)
                    put("thumbnailUrl", artist.thumbnailUrl)
                    put("monthlyListeners", artist.monthlyListeners)
                    put("addedAt", System.currentTimeMillis())
                }
                db.insertWithOnConflict("followed_artists", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                showToast("✓ Siguiendo a ${artist.name}")
            }
            reloadFromDb()
        }
        return newFollowState
    }

    fun isFollowingArtist(artistId: String): Boolean {
        return _followedArtists.value.any { it.id == artistId }
    }

    fun isSongDownloaded(song: Song): Boolean {
        val cleanTitle = song.title.trim()
        val cleanArtist = song.artist.trim()
        return _downloadedSongs.value.any { dl ->
            dl.id == song.id ||
                (cleanTitle.isNotBlank() && dl.title.trim().equals(cleanTitle, ignoreCase = true) &&
                    dl.artist.trim().equals(cleanArtist, ignoreCase = true))
        }
    }

    fun removeDownload(song: Song) {
        scope.launch {
            val db = dbHelper.writableDatabase
            val existing = _downloadedSongs.value.find { dl ->
                dl.id == song.id ||
                    (dl.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                        dl.artist.trim().equals(song.artist.trim(), ignoreCase = true))
            }
            val targetId = existing?.id ?: song.id
            db.delete("downloads", "id = ?", arrayOf(targetId))
            runCatching {
                val file = java.io.File(java.io.File(context.filesDir, "downloads"), "$targetId.m4a")
                if (file.exists()) file.delete()
            }
            reloadFromDb()
            showToast("Descarga eliminada: ${song.title}")
        }
    }

    fun addDownload(song: Song) {
        val notifId = (song.id.hashCode() and 0x7FFFFFFF).coerceAtLeast(2000)
        showToast("⬇ Descargando en segundo plano: ${song.title}...")
        postDownloadNotification(
            notificationId = notifId,
            title = "Descargando: ${song.title}",
            subtitle = "${song.artist} • Iniciando descarga...",
            progress = 0,
            ongoing = true
        )

        scope.launch {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("thumbnailUrl", song.thumbnailUrl)
                put("durationSeconds", song.durationSeconds)
                put("localFilePath", song.localFilePath)
                put("downloadedAt", System.currentTimeMillis())
            }
            db.insertWithOnConflict("downloads", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            reloadFromDb()

            val downloadedPath = com.auramusic.app.AuraApplication.repository.downloadSongToDisk(song) { pct ->
                if (pct in 1..99) {
                    postDownloadNotification(
                        notificationId = notifId,
                        title = "Descargando: ${song.title}",
                        subtitle = "${song.artist} • $pct%",
                        progress = pct,
                        ongoing = true
                    )
                }
            }

            if (!downloadedPath.isNullOrBlank()) {
                val updateCv = ContentValues().apply {
                    put("localFilePath", downloadedPath)
                }
                db.update("downloads", updateCv, "id = ?", arrayOf(song.id))
                reloadFromDb()

                postDownloadNotification(
                    notificationId = notifId,
                    title = "✓ Descarga completada: ${song.title}",
                    subtitle = "${song.artist} • Guardada en Descargados sin conexión",
                    progress = -1,
                    ongoing = false
                )
                showToast("✓ Se descargó: ${song.title}")
            } else {
                postDownloadNotification(
                    notificationId = notifId,
                    title = "Descarga guardada en biblioteca: ${song.title}",
                    subtitle = song.artist,
                    progress = -1,
                    ongoing = false
                )
                showToast("✓ Añadida a Descargados: ${song.title}")
            }
        }
    }

    fun toggleSaveAlbum(album: Album): Boolean {
        val currentlySaved = _savedAlbums.value.any { it.id == album.id }
        val newSavedState = !currentlySaved
        scope.launch {
            val db = dbHelper.writableDatabase
            if (currentlySaved) {
                db.delete("saved_albums", "id = ?", arrayOf(album.id))
                showToast("Álbum quitado de tu biblioteca")
            } else {
                val cv = ContentValues().apply {
                    put("id", album.id)
                    put("title", album.title)
                    put("artist", album.artist)
                    put("year", album.year)
                    put("thumbnailUrl", album.thumbnailUrl)
                    put("songCount", album.songCount.coerceAtLeast(album.songs.size))
                    put("addedAt", System.currentTimeMillis())
                }
                db.insertWithOnConflict("saved_albums", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                showToast("✓ Álbum guardado: ${album.title}")
            }
            reloadFromDb()
        }
        return newSavedState
    }

    fun addToPlaylist(playlistTitle: String, song: Song) {
        scope.launch {
            val db = dbHelper.writableDatabase
            val existing = _userPlaylists.value.find { it.title.equals(playlistTitle.trim(), ignoreCase = true) }
            val plId = existing?.id ?: "pl_${System.currentTimeMillis()}"
            val updatedSongs = if (existing != null) {
                if (existing.songs.any { it.id == song.id }) existing.songs else existing.songs + song
            } else {
                listOf(song)
            }
            val cleanTitle = playlistTitle.trim().ifBlank { "Mi Playlist" }
            val cv = ContentValues().apply {
                put("id", plId)
                put("title", cleanTitle)
                put("songsJson", serializeSongsJson(updatedSongs))
                put("createdAt", System.currentTimeMillis())
            }
            db.insertWithOnConflict("playlists", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            reloadFromDb()
            showToast("✓ Añadida a playlist '$cleanTitle'")
        }
    }

    fun createPlaylist(playlistTitle: String) {
        val clean = playlistTitle.trim()
        if (clean.isBlank()) return
        scope.launch {
            val db = dbHelper.writableDatabase
            val plId = "pl_${System.currentTimeMillis()}"
            val cv = ContentValues().apply {
                put("id", plId)
                put("title", clean)
                put("songsJson", "[]")
                put("createdAt", System.currentTimeMillis())
            }
            db.insertWithOnConflict("playlists", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            reloadFromDb()
            showToast("✓ Playlist '$clean' creada")
        }
    }

    fun setAppIcon(iconName: String) {
        val normalized = iconName.uppercase()
        _currentAppIcon.value = normalized
        scope.launch {
            context.dataStore.edit { it[KEY_APP_ICON] = normalized }
        }
        applyLauncherIconAlias(normalized)
        showToast("✓ Icono de AURA Music actualizado")
    }

    private fun applyLauncherIconAlias(iconName: String) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            // El nombre de clase completo siempre reside en el namespace com.auramusic.app
            val baseNamespace = "com.auramusic.app"
            val aliases = mapOf(
                "VIOLET" to "$baseNamespace.LauncherViolet",
                "BLACK" to "$baseNamespace.LauncherBlack",
                "WHITE" to "$baseNamespace.LauncherWhite",
                "CYAN" to "$baseNamespace.LauncherCyan",
                "ORANGE" to "$baseNamespace.LauncherOrange",
                "GOLD" to "$baseNamespace.LauncherGold"
            )
            val targetAlias = aliases[iconName] ?: aliases["VIOLET"]!!

            // Primero habilitar el nuevo alias y luego deshabilitar los demás
            pm.setComponentEnabledSetting(
                ComponentName(pkg, targetAlias),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            aliases.values.filter { it != targetAlias }.forEach { aliasClass ->
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, aliasClass),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (_: Exception) {}
    }

    private fun serializeSongsJson(songs: List<Song>): String {
        val arr = JSONArray()
        songs.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("artist", s.artist)
                    put("album", s.album.orEmpty())
                    put("thumbnailUrl", s.thumbnailUrl)
                    put("durationSeconds", s.durationSeconds)
                }
            )
        }
        return arr.toString()
    }

    private fun parseSongsJson(jsonStr: String, favIds: Set<String>): List<Song> {
        val list = mutableListOf<Song>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                list.add(
                    Song(
                        id = id,
                        title = obj.optString("title"),
                        artist = obj.optString("artist"),
                        album = obj.optString("album").takeIf { it.isNotBlank() },
                        thumbnailUrl = obj.optString("thumbnailUrl"),
                        durationSeconds = obj.optLong("durationSeconds", 210L),
                        isFavorite = favIds.contains(id)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
