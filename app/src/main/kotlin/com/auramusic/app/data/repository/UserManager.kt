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
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auramusic.app.MainActivity
import com.auramusic.app.R
import com.auramusic.app.data.models.AccentColor
import com.auramusic.app.data.models.Album
import com.auramusic.app.data.models.AppLanguageOption
import com.auramusic.app.data.models.AppTheme
import com.auramusic.app.data.models.Artist
import com.auramusic.app.data.models.ArtistFeedMode
import com.auramusic.app.data.models.AudioQualityOption
import com.auramusic.app.data.models.ListeningStats
import com.auramusic.app.data.models.PlayerBackdropMode
import com.auramusic.app.data.models.PlayerStyleMode
import com.auramusic.app.data.models.Song
import com.auramusic.app.data.models.UserSettings
import com.auramusic.app.i18n.AppLanguage
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

private class AuraLocalDb(context: Context) : SQLiteOpenHelper(context, "aura_library.db", null, 3) {
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
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cached_lyrics (
                songId TEXT PRIMARY KEY,
                lyricsJson TEXT NOT NULL,
                updatedAt INTEGER DEFAULT 0
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
        private const val DOWNLOAD_PROGRESS_CHANNEL_ID = "aura_downloads_progress_v2"
        private const val DOWNLOAD_COMPLETE_CHANNEL_ID = "aura_downloads_complete_sound_v3"
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
    private val KEY_BACKDROP_MODE = stringPreferencesKey("player_backdrop_mode")
    private val KEY_PLAYER_STYLE = stringPreferencesKey("player_style_mode")
    private val KEY_DYNAMIC_SYSTEM_BG = stringPreferencesKey("dynamic_system_bg")
    private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_DEFAULT_SHUFFLE = stringPreferencesKey("default_shuffle")
    private val KEY_CONTINUOUS = stringPreferencesKey("continuous_playback")
    private val KEY_OFFLINE_MODE = stringPreferencesKey("offline_mode")
    private val KEY_CROSSFADE = stringPreferencesKey("crossfade_seconds")
    private val KEY_ARTIST_FEED_MODE = stringPreferencesKey("artist_feed_mode")
    private val KEY_FAST_PLAYBACK_START = stringPreferencesKey("fast_playback_start")
    private val KEY_HAS_ENTERED_APP = stringPreferencesKey("has_entered_app")
    private val KEY_UI_V32_MIGRATED = stringPreferencesKey("ui_v32_obsidian_amber_migrated")

    private val syncPrefs = context.getSharedPreferences("aura_fast_boot", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(
        run {
            val savedName = syncPrefs.getString("user_name", null)
            val savedEmail = syncPrefs.getString("user_email", null)
            if (!savedName.isNullOrBlank() && !savedEmail.isNullOrBlank()) {
                UserProfile(
                    name = savedName,
                    email = savedEmail,
                    avatarUrl = syncPrefs.getString("user_avatar", "").orEmpty(),
                    isGoogleAccount = syncPrefs.getBoolean("is_google", false)
                )
            } else null
        }
    )
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _hasEnteredApp = MutableStateFlow(
        syncPrefs.getBoolean("has_entered_app", true)
    )
    val hasEnteredApp: StateFlow<Boolean> = _hasEnteredApp.asStateFlow()

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

    private val _currentAppIcon = MutableStateFlow("GOLD")
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
            val needsV32Defaults = prefs[KEY_UI_V32_MIGRATED] != "true"

            val icon = if (needsV32Defaults) "GOLD" else (prefs[KEY_APP_ICON] ?: "GOLD")
            _currentAppIcon.value = icon
            if (needsV32Defaults) {
                applyLauncherIconAlias(icon)
            }

            val themeStr = if (needsV32Defaults) AppTheme.DARK.name else (prefs[KEY_APP_THEME] ?: AppTheme.DARK.name)
            val accentStr = if (needsV32Defaults) AccentColor.GOLD.name else (prefs[KEY_ACCENT_COLOR] ?: AccentColor.GOLD.name)
            val qualityStr = prefs[KEY_AUDIO_QUALITY] ?: AudioQualityOption.HIGH.name
            val backdropStr = if (needsV32Defaults) PlayerBackdropMode.SOFT_BLUR.name else (prefs[KEY_BACKDROP_MODE] ?: PlayerBackdropMode.SOFT_BLUR.name)
            val styleStr = prefs[KEY_PLAYER_STYLE] ?: PlayerStyleMode.STUDIO_HALO.name
            val dynamicBg = prefs[KEY_DYNAMIC_SYSTEM_BG]?.toBooleanStrictOrNull() ?: true
            val langStr = prefs[KEY_LANGUAGE] ?: AppLanguageOption.SYSTEM.name
            val defaultShuffle = prefs[KEY_DEFAULT_SHUFFLE]?.toBooleanStrictOrNull() ?: false
            val continuous = prefs[KEY_CONTINUOUS]?.toBooleanStrictOrNull() ?: true
            val offline = prefs[KEY_OFFLINE_MODE]?.toBooleanStrictOrNull() ?: false
            val crossfade = prefs[KEY_CROSSFADE]?.toIntOrNull() ?: 0
            val artistFeedStr = prefs[KEY_ARTIST_FEED_MODE] ?: ArtistFeedMode.MORE_ARTISTS.name
            val fastStart = prefs[KEY_FAST_PLAYBACK_START]?.toBooleanStrictOrNull() ?: true
            val entered = prefs[KEY_HAS_ENTERED_APP] == "true"

            if (needsV32Defaults) {
                context.dataStore.edit { editable ->
                    editable[KEY_APP_ICON] = "GOLD"
                    editable[KEY_APP_THEME] = AppTheme.DARK.name
                    editable[KEY_ACCENT_COLOR] = AccentColor.GOLD.name
                    editable[KEY_BACKDROP_MODE] = PlayerBackdropMode.SOFT_BLUR.name
                    editable[KEY_UI_V32_MIGRATED] = "true"
                }
            }

            val loadedLanguage = runCatching { AppLanguageOption.valueOf(langStr) }.getOrDefault(AppLanguageOption.SYSTEM)
            AppLanguage.applyLanguage(context, loadedLanguage)

            _userSettings.value = UserSettings(
                theme = runCatching { AppTheme.valueOf(themeStr) }.getOrDefault(AppTheme.DARK),
                accentColor = runCatching { AccentColor.valueOf(accentStr) }.getOrDefault(AccentColor.GOLD),
                audioQuality = runCatching { AudioQualityOption.valueOf(qualityStr) }.getOrDefault(AudioQualityOption.HIGH),
                defaultShuffle = defaultShuffle,
                continuousPlayback = continuous,
                playerBackdropMode = runCatching { PlayerBackdropMode.valueOf(backdropStr) }.getOrDefault(PlayerBackdropMode.SOFT_BLUR),
                playerStyleMode = runCatching { PlayerStyleMode.valueOf(styleStr) }.getOrDefault(PlayerStyleMode.STUDIO_HALO),
                dynamicSystemBackdrop = dynamicBg,
                language = loadedLanguage,
                offlineMode = offline,
                crossfadeSeconds = crossfade,
                artistFeedMode = runCatching { ArtistFeedMode.valueOf(artistFeedStr) }.getOrDefault(ArtistFeedMode.MORE_ARTISTS),
                fastPlaybackStart = fastStart
            )

            if (cookie.isNotBlank()) {
                YouTubeAuthManager.setCookie(cookie)
            }

            if (!name.isNullOrBlank() && !email.isNullOrBlank()) {
                _currentUser.value = UserProfile(name, email, avatar, isGoogle)
                _hasEnteredApp.value = true
                syncPrefs.edit()
                    .putString("user_name", name)
                    .putString("user_email", email)
                    .putString("user_avatar", avatar)
                    .putBoolean("is_google", isGoogle)
                    .putBoolean("has_entered_app", true)
                    .apply()
            } else {
                val effectiveEntered = if (prefs[KEY_HAS_ENTERED_APP] == null) true else entered
                _hasEnteredApp.value = effectiveEntered
                syncPrefs.edit().putBoolean("has_entered_app", effectiveEntered).apply()
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
        val prevLang = _userSettings.value.language
        _userSettings.value = newSettings
        if (prevLang != newSettings.language) {
            AppLanguage.applyLanguage(context, newSettings.language)
        }
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_APP_THEME] = newSettings.theme.name
                prefs[KEY_ACCENT_COLOR] = newSettings.accentColor.name
                prefs[KEY_AUDIO_QUALITY] = newSettings.audioQuality.name
                prefs[KEY_BACKDROP_MODE] = newSettings.playerBackdropMode.name
                prefs[KEY_PLAYER_STYLE] = newSettings.playerStyleMode.name
                prefs[KEY_DYNAMIC_SYSTEM_BG] = newSettings.dynamicSystemBackdrop.toString()
                prefs[KEY_LANGUAGE] = newSettings.language.name
                prefs[KEY_DEFAULT_SHUFFLE] = newSettings.defaultShuffle.toString()
                prefs[KEY_CONTINUOUS] = newSettings.continuousPlayback.toString()
                prefs[KEY_OFFLINE_MODE] = newSettings.offlineMode.toString()
                prefs[KEY_CROSSFADE] = newSettings.crossfadeSeconds.toString()
                prefs[KEY_ARTIST_FEED_MODE] = newSettings.artistFeedMode.name
                prefs[KEY_FAST_PLAYBACK_START] = newSettings.fastPlaybackStart.toString()
            }
        }
    }

    private fun ensureDownloadNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val progressChannel = NotificationChannel(
                DOWNLOAD_PROGRESS_CHANNEL_ID,
                "Progreso de descargas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Barra de progreso silenciosa mientras se descarga una canción"
                setShowBadge(false)
            }

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val completeChannel = NotificationChannel(
                DOWNLOAD_COMPLETE_CHANNEL_ID,
                "Descarga completada (con sonido)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso sonoro al finalizar la descarga de una canción"
                setShowBadge(true)
                enableVibration(true)
                setSound(soundUri, audioAttrs)
            }
            nm?.createNotificationChannel(progressChannel)
            nm?.createNotificationChannel(completeChannel)
        }
    }

    private fun playDownloadCompleteSound() {
        runCatching {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
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
            val targetNotifId = if (ongoing) notificationId else notificationId + 1
            val pendingIntent = PendingIntent.getActivity(
                context,
                targetNotifId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val channelId = if (ongoing) DOWNLOAD_PROGRESS_CHANNEL_ID else DOWNLOAD_COMPLETE_CHANNEL_ID
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(
                    if (ongoing) android.R.drawable.stat_sys_download
                    else android.R.drawable.stat_sys_download_done
                )
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .setOnlyAlertOnce(ongoing)
                .setPriority(
                    if (ongoing) NotificationCompat.PRIORITY_LOW
                    else NotificationCompat.PRIORITY_HIGH
                )

            if (ongoing && progress in 0..99) {
                builder.setProgress(100, progress, progress == 0)
            } else {
                nm.cancel(notificationId)
                builder.setProgress(0, 0, false)
                builder.setDefaults(NotificationCompat.DEFAULT_ALL)
                builder.setSound(soundUri)
            }

            nm.notify(targetNotifId, builder.build())
            if (!ongoing) {
                playDownloadCompleteSound()
            }
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

    fun enterAsGuest() {
        _hasEnteredApp.value = true
        syncPrefs.edit().putBoolean("has_entered_app", true).apply()
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_HAS_ENTERED_APP] = "true"
            }
        }
    }

    fun login(name: String, email: String, isGoogle: Boolean = false, avatarUrl: String = "", cookie: String = "") {
        val user = UserProfile(name, email, avatarUrl, isGoogle)
        _currentUser.value = user
        _hasEnteredApp.value = true
        syncPrefs.edit()
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("user_avatar", avatarUrl)
            .putBoolean("is_google", isGoogle)
            .putBoolean("has_entered_app", true)
            .apply()
        if (cookie.isNotBlank()) {
            YouTubeAuthManager.setCookie(cookie)
        }
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_NAME] = name
                prefs[KEY_USER_EMAIL] = email
                prefs[KEY_USER_AVATAR] = avatarUrl
                prefs[KEY_IS_GOOGLE] = if (isGoogle) "true" else "false"
                prefs[KEY_HAS_ENTERED_APP] = "true"
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
        _hasEnteredApp.value = false
        syncPrefs.edit().clear().putBoolean("has_entered_app", false).apply()
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
                prefs.remove(KEY_HAS_ENTERED_APP)
            }
        }
    }

    private fun reloadPlayHistoryOnly() {
        val db = dbHelper.readableDatabase
        val favIds = _favorites.value.map { it.id }.toSet()
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
            reloadPlayHistoryOnly()
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

    fun deletePlaylist(playlistId: String) {
        scope.launch {
            val db = dbHelper.writableDatabase
            val target = _userPlaylists.value.find { it.id == playlistId }
            db.delete("playlists", "id = ?", arrayOf(playlistId))
            reloadFromDb()
            showToast("Playlist eliminada: ${target?.title.orEmpty()}")
        }
    }

    fun renamePlaylist(playlistId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank()) return
        scope.launch {
            val db = dbHelper.writableDatabase
            val cv = ContentValues().apply {
                put("title", clean)
            }
            db.update("playlists", cv, "id = ?", arrayOf(playlistId))
            reloadFromDb()
            showToast("✓ Playlist renombrada a '$clean'")
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        scope.launch {
            val db = dbHelper.writableDatabase
            val existing = _userPlaylists.value.find { it.id == playlistId } ?: return@launch
            val updatedSongs = existing.songs.filterNot { it.id == songId }
            val cv = ContentValues().apply {
                put("songsJson", serializeSongsJson(updatedSongs))
            }
            db.update("playlists", cv, "id = ?", arrayOf(playlistId))
            reloadFromDb()
            showToast("Canción quitada de '${existing.title}'")
        }
    }

    fun getCachedLyrics(songId: String): List<com.auramusic.app.data.models.LyricLine>? {
        return try {
            val db = dbHelper.readableDatabase
            db.rawQuery("SELECT lyricsJson FROM cached_lyrics WHERE songId = ?", arrayOf(songId)).use { c ->
                if (!c.moveToFirst()) return null
                val arr = JSONArray(c.getString(0))
                val list = mutableListOf<com.auramusic.app.data.models.LyricLine>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        com.auramusic.app.data.models.LyricLine(
                            timeMs = obj.optLong("t", 0L),
                            text = obj.optString("l", "")
                        )
                    )
                }
                list.takeIf { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun saveCachedLyrics(songId: String, lines: List<com.auramusic.app.data.models.LyricLine>) {
        if (lines.isEmpty()) return
        scope.launch {
            runCatching {
                val arr = JSONArray()
                lines.forEach { line ->
                    arr.put(
                        JSONObject().apply {
                            put("t", line.timeMs)
                            put("l", line.text)
                        }
                    )
                }
                val cv = ContentValues().apply {
                    put("songId", songId)
                    put("lyricsJson", arr.toString())
                    put("updatedAt", System.currentTimeMillis())
                }
                dbHelper.writableDatabase.insertWithOnConflict(
                    "cached_lyrics",
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        }
    }

    fun importSongsToPlaylist(playlistTitle: String, songs: List<Song>) {
        if (songs.isEmpty()) return
        val cleanTitle = playlistTitle.trim().ifBlank { "Importada" }
        scope.launch {
            val db = dbHelper.writableDatabase
            val existing = _userPlaylists.value.find { it.title.equals(cleanTitle, ignoreCase = true) }
            val plId = existing?.id ?: "pl_${System.currentTimeMillis()}"
            val merged = if (existing != null) {
                (existing.songs + songs).distinctBy { it.id }
            } else {
                songs.distinctBy { it.id }
            }
            val cv = ContentValues().apply {
                put("id", plId)
                put("title", cleanTitle)
                put("songsJson", serializeSongsJson(merged))
                put("createdAt", System.currentTimeMillis())
            }
            db.insertWithOnConflict("playlists", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            reloadFromDb()
            showToast("✓ Importadas ${songs.size} canciones en '$cleanTitle'")
        }
    }

    fun getListeningStats(): ListeningStats {
        val db = dbHelper.readableDatabase
        val songsWithCount = mutableListOf<Pair<Song, Int>>()
        val artistCounts = mutableMapOf<String, Int>()
        var totalPlays = 0
        var totalSeconds = 0L

        db.rawQuery(
            "SELECT id, title, artist, artistId, album, albumId, thumbnailUrl, durationSeconds, playCount FROM play_history ORDER BY playCount DESC, lastPlayedAt DESC LIMIT 50",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val count = c.getInt(8).coerceAtLeast(1)
                val dur = c.getLong(7).coerceAtLeast(180L)
                val artist = c.getString(2)
                val song = Song(
                    id = c.getString(0),
                    title = c.getString(1),
                    artist = artist,
                    artistId = c.getString(3),
                    album = c.getString(4),
                    albumId = c.getString(5),
                    thumbnailUrl = c.getString(6),
                    durationSeconds = dur
                )
                songsWithCount.add(song to count)
                totalPlays += count
                totalSeconds += dur * count
                val primaryArtist = artist.split(",", "&", "•").firstOrNull()?.trim().orEmpty().ifBlank { artist }
                artistCounts[primaryArtist] = (artistCounts[primaryArtist] ?: 0) + count
            }
        }

        return ListeningStats(
            totalPlays = totalPlays,
            totalMinutes = (totalSeconds / 60L).toInt(),
            topSongs = songsWithCount.take(10),
            topArtists = artistCounts.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        )
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
                    put("localFilePath", s.localFilePath.orEmpty())
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
                        localFilePath = obj.optString("localFilePath").takeIf { it.isNotBlank() },
                        isFavorite = favIds.contains(id)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
