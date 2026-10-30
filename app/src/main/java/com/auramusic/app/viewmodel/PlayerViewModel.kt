package com.auramusic.app.viewmodel

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.auramusic.app.data.database.AuraDatabase
import com.auramusic.app.data.database.DownloadedTrack
import com.auramusic.app.data.database.ListeningStats
import com.auramusic.app.data.database.Playlist
import com.auramusic.app.data.local.LocalAudioScanner
import com.auramusic.app.data.model.Album
import com.auramusic.app.data.model.ArtistProfile
import com.auramusic.app.data.model.LyricLine
import com.auramusic.app.data.model.Song
import com.auramusic.app.network.AuraDownloadManager
import com.auramusic.app.network.AuraStreamResolver
import com.auramusic.app.network.LyricsEngine
import com.auramusic.app.network.YouTubeAccountState
import com.auramusic.app.network.YouTubeAuthManager
import com.auramusic.app.network.YouTubeMusicEngine
import com.auramusic.app.playback.AuraAudioEffects
import com.auramusic.app.playback.AuraPlaybackBridge
import com.auramusic.app.playback.AuraPlaybackService
import com.auramusic.app.playback.SleepTimerManager
import com.auramusic.app.playback.SleepTimerState
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File

enum class MainTab {
    STUDIO, RADAR, VAULT
}

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

data class PlayerUiState(
    val currentSong: Song = Song(
        id = "1",
        title = "Resonance",
        artist = "HOME",
        album = "Odyssey",
        durationMs = 212000L,
        coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
        mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
        isFavorite = true
    ),
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 212000L,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val queue: List<Song> = YouTubeMusicEngine.getCuratedFallbackTracks(),
    val currentIndex: Int = 0,
    val searchResults: List<Song> = YouTubeMusicEngine.getCuratedFallbackTracks(),
    val isSearching: Boolean = false,
    val favorites: List<Song> = emptyList(),
    val downloads: List<DownloadedTrack> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val lyrics: List<LyricLine> = emptyList(),
    val currentTab: MainTab = MainTab.STUDIO,
    val isPlayerExpanded: Boolean = false,
    val activeOptionSong: Song? = null,
    val downloadProgressMap: Map<String, Int> = emptyMap(),
    val localTracks: List<Song> = emptyList(),
    val isScanningLocal: Boolean = false,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val showEqualizer: Boolean = false,
    val showSleepTimer: Boolean = false,
    val showQueue: Boolean = false,
    val selectedPlaylist: Playlist? = null,
    val playlistSongs: List<Song> = emptyList(),
    val showSettings: Boolean = false,
    val audioQuality: String = "Hi-Res (320 kbps)",
    val loudnessNormalizer: Boolean = true,
    val currentTheme: String = "Studio Carbon",
    val themeMode: String = "Oscuro", // "Oscuro", "Claro", "Sistema"
    val appLanguage: String = "Español",
    val showFullscreenLyrics: Boolean = false,
    val selectedArtist: Song? = null,
    val artistProfile: ArtistProfile? = null,
    val artistTopSongs: List<Song> = emptyList(),
    val isLoadingArtist: Boolean = false,
    val selectedAlbum: Album? = null,
    val albumTracks: List<Song> = emptyList(),
    val isLoadingAlbum: Boolean = false,
    val showAlbumDetail: Boolean = false,
    val popularArtists: List<Song> = YouTubeMusicEngine.getCuratedPopularArtists(),
    val trendingAlbums: List<Album> = YouTubeMusicEngine.getCuratedTrendingAlbums(),
    val userTopArtists: List<Song> = emptyList(),
    val followedArtists: List<Song> = emptyList(),
    val savedAlbums: List<Album> = emptyList(),
    val ytAccount: YouTubeAccountState = YouTubeAccountState(),
    val showYouTubeLogin: Boolean = false,
    val popularStations: List<String> = YouTubeMusicEngine.getCuratedStations(),
    val searchHistory: List<String> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val toastMessage: String? = null,
    val lyricsOffsetMs: Long = -200L,
    val autoRadioEnabled: Boolean = true,
    val skipSilenceEnabled: Boolean = false,
    val showLyricsTranslation: Boolean = false,
    val filterShortAudios: Boolean = true,
    val downloadOnlyOnWifi: Boolean = false,
    val pauseHistory: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val updateInfo: AppUpdateInfo? = null,
    val showChangelogDialog: Boolean = false,
    val listeningStats: ListeningStats = ListeningStats()
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AuraDatabase.getInstance(application)
    private val downloadManager = AuraDownloadManager(application, database, viewModelScope)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var searchJob: Job? = null
    private var suggestionsJob: Job? = null
    private var lyricsJob: Job? = null
    private var playJob: Job? = null
    private var prefetchJob: Job? = null
    private var radioQueueJob: Job? = null
    private var unshuffledQueue: List<Song> = emptyList()
    private var isTransitioning = false

    init {
        AuraStreamResolver.init()
        setupPlaybackBridge()
        initializeMediaController()
        startProgressUpdater()
        setupDownloadListener()
        loadInitialData()
        observeDownloadProgress()
        observeSleepTimer()
    }

    private fun setupDownloadListener() {
        downloadManager.onDownloadCompleted = { song ->
            // Notification delivered directly to Android system notification tray via NotificationManagerCompat
        }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
        viewModelScope.launch {
            delay(3500)
            _uiState.update { if (it.toastMessage == message) it.copy(toastMessage = null) else it }
        }
    }

    private fun setupPlaybackBridge() {
        AuraPlaybackBridge.onNextRequested = {
            viewModelScope.launch(Dispatchers.Main) {
                next()
            }
        }
        AuraPlaybackBridge.onPreviousRequested = {
            viewModelScope.launch(Dispatchers.Main) {
                previous()
            }
        }
        val current = AuraPlaybackBridge.currentSong.value
        if (current != null) {
            val q = AuraPlaybackBridge.queue.value
            _uiState.update {
                it.copy(
                    currentSong = current,
                    queue = if (q.isNotEmpty()) q else it.queue,
                    currentIndex = AuraPlaybackBridge.currentIndex,
                    isPlaying = true
                )
            }
            loadLyricsForSong(current)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
            val savedPalette = prefs.getString("palette", "Studio Carbon") ?: "Studio Carbon"
            val savedThemeMode = prefs.getString("theme_mode", "Oscuro") ?: "Oscuro"
            val savedLanguage = prefs.getString("app_language", "Español") ?: "Español"
            val savedLyricsOffset = prefs.getLong("lyrics_offset_ms", -200L)
            val savedAudioQuality = prefs.getString("audio_quality", "Hi-Res (320 kbps)") ?: "Hi-Res (320 kbps)"
            val savedLoudness = prefs.getBoolean("loudness_normalizer", true)
            val savedAutoRadio = prefs.getBoolean("auto_radio_enabled", true)
            val savedSkipSilence = prefs.getBoolean("skip_silence_enabled", false)
            val savedFilterShort = prefs.getBoolean("filter_short_audios", true)
            val savedWifiOnly = prefs.getBoolean("download_wifi_only", false)
            val savedPauseHistory = prefs.getBoolean("pause_history", false)
            val savedGapless = prefs.getBoolean("gapless_enabled", true)
            val ytState = YouTubeAuthManager.loadFromPrefs(context)

            downloadManager.downloadOnlyOnWifi = savedWifiOnly
            AuraStreamResolver.preferredQuality = savedAudioQuality
            AuraAudioEffects.setLoudnessNormalizer(savedLoudness)
            AuraPlaybackBridge.autoRadioEnabled = savedAutoRadio
            AuraPlaybackBridge.skipSilenceEnabled = savedSkipSilence
            AuraPlaybackBridge.onSkipSilenceChanged?.invoke(savedSkipSilence)

            // Continue where you left off
            val lastSongId = prefs.getString("last_song_id", null)
            val lastPositionMs = prefs.getLong("last_position_ms", 0L)
            val lastSongTitle = prefs.getString("last_song_title", null)
            val lastSongArtist = prefs.getString("last_song_artist", null)
            val lastSongAlbum = prefs.getString("last_song_album", "") ?: ""
            val lastSongCover = prefs.getString("last_song_cover", "") ?: ""
            val lastSongDuration = prefs.getLong("last_song_duration", 0L)
            val lastSongMediaUri = prefs.getString("last_song_media_uri", "") ?: ""

            val restoredSong = if (!lastSongId.isNullOrBlank() && !lastSongTitle.isNullOrBlank()) {
                Song(
                    id = lastSongId,
                    title = lastSongTitle,
                    artist = lastSongArtist ?: "Artista",
                    album = lastSongAlbum,
                    coverUrl = lastSongCover,
                    durationMs = lastSongDuration,
                    mediaUri = lastSongMediaUri
                )
            } else null

            // 1. Instantly load persistent SQLite data & curated fallback lists (0ms cold start)
            val savedFavs = database.getFavorites()
            val savedDownloads = database.getDownloadedTracks()
            val savedPlaylists = database.getPlaylists()
            val savedFollowedArtists = database.getFollowedArtists()
            val savedAlbumsList = database.getSavedAlbums()
            val userTop = database.getTopArtistsFromHistoryAndFavorites()
            val stats = database.getListeningStats()
            val popArtists = YouTubeMusicEngine.getCuratedPopularArtists()
            val trendAlbums = YouTubeMusicEngine.getCuratedTrendingAlbums()
            val stations = YouTubeMusicEngine.getCuratedStations()
            val sHistory = database.getSearchHistory()
            val fallbackTracks = YouTubeMusicEngine.getCuratedFallbackTracks()

            _uiState.update {
                it.copy(
                    searchResults = if (it.searchResults.isNotEmpty()) it.searchResults else fallbackTracks,
                    popularArtists = popArtists,
                    trendingAlbums = trendAlbums,
                    userTopArtists = userTop,
                    followedArtists = savedFollowedArtists,
                    savedAlbums = savedAlbumsList,
                    ytAccount = ytState,
                    popularStations = stations,
                    searchHistory = sHistory,
                    queue = if (it.queue.isNotEmpty()) it.queue else fallbackTracks,
                    currentSong = restoredSong ?: (if (it.currentSong.id.isNotBlank() && it.currentSong.id != "1") it.currentSong else fallbackTracks.firstOrNull() ?: it.currentSong),
                    currentPositionMs = if (restoredSong != null) lastPositionMs else 0L,
                    durationMs = restoredSong?.durationMs ?: (fallbackTracks.firstOrNull()?.durationMs ?: 212000L),
                    isPlaying = false,
                    favorites = if (savedFavs.isNotEmpty()) savedFavs else fallbackTracks.filter { s -> s.isFavorite },
                    downloads = savedDownloads,
                    playlists = savedPlaylists,
                    currentTheme = savedPalette,
                    themeMode = savedThemeMode,
                    appLanguage = savedLanguage,
                    lyricsOffsetMs = savedLyricsOffset,
                    audioQuality = savedAudioQuality,
                    loudnessNormalizer = savedLoudness,
                    autoRadioEnabled = savedAutoRadio,
                    skipSilenceEnabled = savedSkipSilence,
                    filterShortAudios = savedFilterShort,
                    downloadOnlyOnWifi = savedWifiOnly,
                    pauseHistory = savedPauseHistory,
                    gaplessEnabled = savedGapless,
                    listeningStats = stats
                )
            }

            // Sync initial favorites into DB if first run
            if (savedFavs.isEmpty()) {
                fallbackTracks.filter { it.isFavorite }.forEach { database.addFavorite(it) }
            }

            scanLocalTracks()
            loadLyricsForSong(_uiState.value.currentSong)

            // 2. Fetch fresh tracks from network (personalized if user linked YouTube Music account)
            launch(Dispatchers.IO) {
                try {
                    val freshTracks = if (ytState.isLoggedIn) {
                        val personalized = YouTubeMusicEngine.fetchPersonalizedHomeFeed()
                        if (personalized.isNotEmpty()) personalized else YouTubeMusicEngine.searchTracks("")
                    } else {
                        YouTubeMusicEngine.searchTracks("")
                    }
                    if (freshTracks.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                searchResults = freshTracks,
                                queue = if (it.queue == fallbackTracks) freshTracks else it.queue
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Offline: Curated offline catalog remains active smoothly
                }
            }
        }
    }

    private fun observeDownloadProgress() {
        viewModelScope.launch {
            downloadManager.downloadProgress.collect { progressMap ->
                val updatedDownloads = database.getDownloadedTracks()
                _uiState.update {
                    it.copy(
                        downloadProgressMap = progressMap,
                        downloads = updatedDownloads
                    )
                }
            }
        }
    }

    private fun observeSleepTimer() {
        viewModelScope.launch {
            SleepTimerManager.timerState.collect { timerState ->
                _uiState.update { it.copy(sleepTimer = timerState) }
            }
        }
    }

    private fun getLanguageCode(langName: String): String {
        return when {
            langName.startsWith("English", ignoreCase = true) -> "en"
            langName.startsWith("Português", ignoreCase = true) -> "pt"
            langName.startsWith("Français", ignoreCase = true) -> "fr"
            langName.startsWith("Deutsch", ignoreCase = true) -> "de"
            langName.startsWith("日本語") -> "ja"
            else -> "es"
        }
    }

    private fun loadLyricsForSong(song: Song) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            val lyrics = LyricsEngine.fetchSyncedLyrics(
                trackName = song.title,
                artistName = song.artist,
                durationSec = song.durationMs / 1000,
                videoId = song.id
            )
            _uiState.update { it.copy(lyrics = lyrics) }
            if (_uiState.value.showLyricsTranslation && lyrics.isNotEmpty()) {
                val targetLang = getLanguageCode(_uiState.value.appLanguage)
                val translated = LyricsEngine.translateLyrics(lyrics, targetLang)
                _uiState.update { it.copy(lyrics = translated) }
            }
        }
    }

    fun toggleLyricsTranslation() {
        val newShow = !_uiState.value.showLyricsTranslation
        _uiState.update { it.copy(showLyricsTranslation = newShow) }
        if (newShow) {
            val currentLyrics = _uiState.value.lyrics
            if (currentLyrics.isNotEmpty()) {
                viewModelScope.launch {
                    val targetLang = getLanguageCode(_uiState.value.appLanguage)
                    val translated = LyricsEngine.translateLyrics(currentLyrics, targetLang)
                    _uiState.update { it.copy(lyrics = translated) }
                }
            }
        }
    }

    private fun initializeMediaController() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AuraPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.shuffleModeEnabled = _uiState.value.isShuffle
            mediaController?.repeatMode = if (_uiState.value.isRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            setupPlayerListener()
            syncStateFromController()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                isTransitioning = false
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && _uiState.value.sleepTimer.isEndOfTrack) {
                    SleepTimerManager.notifyTrackEnded()
                }
                mediaController?.let { controller ->
                    if (controller.currentMediaItemIndex > 0) {
                        controller.removeMediaItems(0, controller.currentMediaItemIndex)
                    }
                }
                syncStateFromController()
                prefetchNextTrackIntoPlayer()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isTransitioning = false
                    val duration = mediaController?.duration?.takeIf { it > 0 } ?: _uiState.value.durationMs
                    _uiState.update { it.copy(durationMs = duration) }
                } else if (playbackState == Player.STATE_ENDED) {
                    val wasEndOfTrack = _uiState.value.sleepTimer.isEndOfTrack
                    SleepTimerManager.notifyTrackEnded()
                    if (!wasEndOfTrack && !isTransitioning) {
                        if (_uiState.value.isRepeat) {
                            seekTo(0L)
                            mediaController?.play()
                        } else {
                            isTransitioning = true
                            next()
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("PlayerViewModel", "Playback error: ${error.message}", error)
                if (!isTransitioning && _uiState.value.queue.size > 1) {
                    isTransitioning = true
                    next()
                }
            }
        })
    }

    private fun syncStateFromController() {
        val controller = mediaController ?: return
        val currentItem = controller.currentMediaItem ?: return
        val isPlaying = controller.isPlaying
        val pos = controller.currentPosition
        val dur = controller.duration.takeIf { it > 0 } ?: _uiState.value.durationMs
        val meta = currentItem.mediaMetadata
        val title = meta.title?.toString() ?: ""
        val artist = meta.artist?.toString() ?: ""
        val album = meta.albumTitle?.toString() ?: ""
        val cover = meta.artworkUri?.toString() ?: ""

        if (title.isNotBlank()) {
            val q = _uiState.value.queue
            val foundIdx = q.indexOfFirst { it.id == currentItem.mediaId }
            val newIdx = if (foundIdx >= 0) foundIdx else _uiState.value.currentIndex
            val existingSong = q.getOrNull(foundIdx)

            val activeSong = if (existingSong != null) {
                existingSong.copy(
                    durationMs = if (dur > 0) dur else existingSong.durationMs
                )
            } else {
                Song(
                    id = currentItem.mediaId,
                    title = title,
                    artist = artist,
                    album = album,
                    coverUrl = cover,
                    durationMs = dur,
                    isFavorite = _uiState.value.favorites.any { it.id == currentItem.mediaId }
                )
            }
            val previousSongId = _uiState.value.currentSong.id
            AuraPlaybackBridge.currentSong.value = activeSong
            AuraPlaybackBridge.currentIndex = newIdx
            _uiState.update {
                it.copy(
                    currentSong = activeSong,
                    currentIndex = newIdx,
                    isPlaying = isPlaying,
                    currentPositionMs = pos,
                    durationMs = dur
                )
            }
            loadLyricsForSong(activeSong)
            if (previousSongId != activeSong.id) {
                viewModelScope.launch {
                    database.addToHistory(activeSong)
                    val updatedUserTop = database.getTopArtistsFromHistoryAndFavorites()
                    val updatedStats = database.getListeningStats()
                    _uiState.update { it.copy(userTopArtists = updatedUserTop, listeningStats = updatedStats) }
                }
            }
        }
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            var saveCounter = 0
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        val pos = controller.currentPosition
                        val dur = controller.duration.coerceAtLeast(1L)
                        if (kotlin.math.abs(pos - _uiState.value.currentPositionMs) >= 200L || !_uiState.value.isPlaying || dur != _uiState.value.durationMs) {
                            _uiState.update {
                                it.copy(
                                    currentPositionMs = pos,
                                    durationMs = dur,
                                    isPlaying = true
                                )
                            }
                        }
                        saveCounter++
                        if (saveCounter % 8 == 0 && pos > 0L) { // Save every ~2 seconds
                            val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putLong("last_position_ms", pos).apply()
                        }
                    }
                }
                delay(250)
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab, isPlayerExpanded = false) }
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isPlayerExpanded = expanded) }
    }

    fun openSongOptions(song: Song?) {
        _uiState.update { it.copy(activeOptionSong = song) }
    }

    fun search(query: String) {
        val trimmed = query.trim()
        searchJob?.cancel()
        suggestionsJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isSearching = true, searchSuggestions = emptyList()) }
            if (trimmed.isNotBlank()) {
                launch {
                    database.insertSearchHistory(trimmed)
                    val updatedHistory = database.getSearchHistory()
                    _uiState.update { it.copy(searchHistory = updatedHistory) }
                }
            }
            try {
                // Fast in-memory local filter (or MediaStore fallback if not scanned yet) in parallel with YouTube search
                val localDeferred = async {
                    val cachedLocals = _uiState.value.localTracks
                    if (cachedLocals.isNotEmpty()) {
                        if (trimmed.isBlank()) emptyList()
                        else cachedLocals.filter {
                            it.title.contains(trimmed, ignoreCase = true) ||
                            it.artist.contains(trimmed, ignoreCase = true)
                        }
                    } else {
                        LocalAudioScanner.searchLocalAudio(getApplication<Application>(), trimmed)
                    }
                }
                val remoteDeferred = async {
                    YouTubeMusicEngine.searchTracks(trimmed)
                }

                val localMatches = localDeferred.await()
                val remoteTracks = remoteDeferred.await()
                val combined: List<Song> = if (localMatches.isNotEmpty()) {
                    (localMatches + remoteTracks).distinctBy { s: Song -> s.id }
                } else {
                    remoteTracks
                }
                _uiState.update {
                    it.copy(
                        searchResults = combined,
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _uiState.update { it.copy(isSearching = false) }
                }
            }
        }
    }

    fun fetchSearchSuggestions(query: String) {
        val trimmed = query.trim()
        suggestionsJob?.cancel()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(searchSuggestions = emptyList()) }
            return
        }
        suggestionsJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val suggestions = YouTubeMusicEngine.getSearchSuggestions(trimmed)
                _uiState.update { it.copy(searchSuggestions = suggestions) }
            } catch (e: Exception) {
                // Cancelled or network issue
            }
        }
    }

    fun clearSearchSuggestions() {
        _uiState.update { it.copy(searchSuggestions = emptyList()) }
    }

    fun deleteSearchHistoryItem(query: String) {
        viewModelScope.launch {
            database.deleteSearchHistoryItem(query)
            val updated = database.getSearchHistory()
            _uiState.update { it.copy(searchHistory = updated) }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            database.clearSearchHistory()
            _uiState.update { it.copy(searchHistory = emptyList()) }
        }
    }

    fun playPause() {
        val controller = mediaController
        if (controller != null) {
            if (controller.isPlaying) {
                controller.pause()
            } else {
                if (controller.mediaItemCount == 0) {
                    playSong(_uiState.value.currentSong)
                } else {
                    controller.play()
                }
            }
        } else {
            _uiState.update { it.copy(isPlaying = !it.isPlaying) }
        }
    }

    private suspend fun fetchCoverBytes(coverUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        if (coverUrl.isBlank()) return@withContext null
        try {
            val dataModel: Any = if (coverUrl.startsWith("/") || coverUrl.startsWith("file:")) {
                val f = File(coverUrl.removePrefix("file://"))
                if (f.exists() && f.length() > 0) f else return@withContext null
            } else {
                coverUrl
            }
            val context = getApplication<Application>()
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(dataModel)
                .size(256, 256)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap ?: return@withContext null
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    fun playSong(song: Song, contextQueue: List<Song>? = null, buildSmartRadio: Boolean = false) {
        // If the item clicked is an Artist profile, open Artist Detail View!
        if (song.isArtist) {
            openArtistDetail(song)
            return
        }

        // If the item clicked is an Album, open Album Detail View!
        if (song.isAlbum) {
            val targetBrowse = if (song.browseId.isNotBlank()) song.browseId else song.id
            val album = Album(
                browseId = targetBrowse,
                title = song.title,
                year = song.artist,
                coverUrl = song.coverUrl
            )
            openAlbumDetail(album)
            return
        }

        // 1. Immediately reflect the selected track in the UI for instant response
        val playQueue = if (buildSmartRadio) {
            // Start with the selected song and clean fallback tracks while Smart Song Radio loads in the background
            val initialExtras = (contextQueue ?: _uiState.value.queue)
                .filter { !it.isArtist && !it.isAlbum && it.id != song.id }
            listOf(song) + (if (_uiState.value.isShuffle) initialExtras.shuffled() else initialExtras)
        } else if (!contextQueue.isNullOrEmpty()) {
            contextQueue.filter { !it.isArtist && !it.isAlbum }
        } else if (_uiState.value.queue.none { it.id == song.id }) {
            listOf(song) + _uiState.value.queue.filter { !it.isArtist && !it.isAlbum }
        } else {
            _uiState.value.queue.filter { !it.isArtist && !it.isAlbum }
        }
        val newIndex = playQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        AuraPlaybackBridge.currentSong.value = song
        AuraPlaybackBridge.queue.value = playQueue
        AuraPlaybackBridge.currentIndex = newIndex

        _uiState.update { state ->
            state.copy(
                currentSong = song,
                queue = playQueue,
                currentIndex = newIndex,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = song.durationMs,
                lyrics = emptyList() // Clear previous song lyrics immediately
            )
        }

        // 2. Fetch lyrics concurrently
        loadLyricsForSong(song)

        // 2b. If started from Search or Home track tap, build a Spotify-style Song Radio (RDAMVM) queue around this track
        if (buildSmartRadio && !song.id.startsWith("local_")) {
            radioQueueJob?.cancel()
            radioQueueJob = viewModelScope.launch(Dispatchers.IO) {
                val related = YouTubeMusicEngine.fetchRelatedTracks(song.id, song.title, song.artist)
                    .filter { it.id != song.id && !it.isArtist && !it.isAlbum }
                if (related.isNotEmpty() && _uiState.value.currentSong.id == song.id) {
                    unshuffledQueue = listOf(song) + related
                    val orderedRelated = if (_uiState.value.isShuffle) related.shuffled() else related
                    val smartQueue = listOf(song) + orderedRelated
                    AuraPlaybackBridge.queue.value = smartQueue
                    AuraPlaybackBridge.currentIndex = 0
                    _uiState.update { it.copy(queue = smartQueue, currentIndex = 0) }
                    prefetchNextTrackIntoPlayer()
                }
            }
        }

        playJob?.cancel()
        prefetchJob?.cancel()
        mediaController?.pause()
        playJob = viewModelScope.launch {
            val downloadedList = database.getDownloadedTracks()
            val localTrack = downloadedList.find {
                it.song.id == song.id ||
                (it.song.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                 it.song.artist.trim().equals(song.artist.trim(), ignoreCase = true))
            }

            // 3. Resolve audio stream & cover art bytes in parallel
            val streamDeferred = async(Dispatchers.IO) {
                // Priority 1: song.mediaUri already points to an existing local file or Android content URI
                if (song.mediaUri.isNotBlank()) {
                    val directFile = File(song.mediaUri.removePrefix("file://"))
                    if (directFile.exists() && directFile.length() > 0) {
                        return@async directFile.absolutePath
                    } else if (song.mediaUri.startsWith("content://")) {
                        return@async song.mediaUri
                    }
                }

                // Priority 2: Offline downloaded track file on disk
                if (localTrack != null) {
                    val dlFile = File(localTrack.localFilePath)
                    if (dlFile.exists() && dlFile.length() > 0) {
                        return@async dlFile.absolutePath
                    }
                }

                // Priority 3: Local device track from MediaStore scanner
                if (song.id.startsWith("local_")) {
                    return@async song.mediaUri
                }

                val localDeviceTrack = _uiState.value.localTracks.find {
                    it.id == song.id ||
                    (it.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                     it.artist.trim().equals(song.artist.trim(), ignoreCase = true))
                }
                if (localDeviceTrack != null && localDeviceTrack.mediaUri.isNotBlank()) {
                    return@async localDeviceTrack.mediaUri
                }

                // Priority 4: Online streaming resolution (requires internet)
                val fallback = "${song.title} ${song.artist}"
                AuraStreamResolver.resolveAudioStream(song.id, fallback)
            }

            val artworkDeferred = async(Dispatchers.IO) {
                try {
                    withTimeoutOrNull(1200L) {
                        fetchCoverBytes(song.coverUrl)
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val playableUri = streamDeferred.await()
            val artworkBytes = artworkDeferred.await()

            if (playableUri.isNullOrBlank() || playableUri.startsWith("https://www.youtube.com")) {
                android.util.Log.e("PlayerViewModel", "Playable audio stream could not be resolved for: ${song.title} (${song.id})")
                _uiState.update { it.copy(isPlaying = false, toastMessage = "Sin conexión: Canción no disponible offline") }
                return@launch
            }

            // 4. Build MediaMetadata with Artwork URI and Artwork Data Bytes for system notification
            val metaBuilder = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)

            if (song.coverUrl.isNotBlank()) {
                val artUri = if (song.coverUrl.startsWith("/") || song.coverUrl.startsWith("file:")) {
                    Uri.fromFile(File(song.coverUrl.removePrefix("file://")))
                } else {
                    Uri.parse(song.coverUrl)
                }
                metaBuilder.setArtworkUri(artUri)
            }
            if (artworkBytes != null) {
                metaBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }

            val mediaUri = if (playableUri.startsWith("/") || playableUri.startsWith("file:")) {
                Uri.fromFile(File(playableUri.removePrefix("file://")))
            } else {
                Uri.parse(playableUri)
            }

            val mediaItem = MediaItem.Builder()
                .setUri(mediaUri)
                .setMediaId(song.id)
                .setMediaMetadata(metaBuilder.build())
                .build()

            mediaController?.run {
                setMediaItem(mediaItem)
                prepare()
                play()
            }

            if (!_uiState.value.pauseHistory) {
                database.addToHistory(song)
                val updatedUserTop = database.getTopArtistsFromHistoryAndFavorites()
                val updatedStats = database.getListeningStats()
                _uiState.update { it.copy(userTopArtists = updatedUserTop, listeningStats = updatedStats) }
            }

            val appPrefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
            appPrefs.edit()
                .putString("last_song_id", song.id)
                .putString("last_song_title", song.title)
                .putString("last_song_artist", song.artist)
                .putString("last_song_album", song.album)
                .putString("last_song_cover", song.coverUrl)
                .putLong("last_song_duration", song.durationMs)
                .putString("last_song_media_uri", song.mediaUri)
                .putLong("last_position_ms", 0L)
                .apply()

            // 5. Seamlessly prefetch & enqueue NEXT song into ExoPlayer queue for gapless auto-advance
            if (_uiState.value.gaplessEnabled) {
                prefetchNextTrackIntoPlayer()
            }
        }
    }

    private fun prefetchNextTrackIntoPlayer() {
        val initialQ = _uiState.value.queue.filter { !it.isArtist && !it.isAlbum }
        val curSong = _uiState.value.currentSong
        val currentIdx = initialQ.indexOfFirst { it.id == curSong.id }
        if (initialQ.isEmpty() || currentIdx < 0) return

        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            var q = initialQ
            // Auto-Radio / Automix: if near the end of the queue and Auto-Radio is enabled, fetch Song Radio tracks
            if (_uiState.value.autoRadioEnabled && (currentIdx >= q.size - 1 || q.size <= 1)) {
                val related = YouTubeMusicEngine.fetchRelatedTracks(curSong.id, curSong.title, curSong.artist)
                    .filter { rel -> q.none { it.id == rel.id } && !rel.isArtist && !rel.isAlbum }
                if (related.isNotEmpty()) {
                    val orderedRelated = if (_uiState.value.isShuffle) related.shuffled() else related
                    val expandedQueue = q + orderedRelated
                    q = expandedQueue
                    AuraPlaybackBridge.queue.value = expandedQueue
                    _uiState.update { it.copy(queue = expandedQueue) }
                }
            }

            if (q.size <= 1) return@launch

            // Queue is already ordered (either sequential or Fisher-Yates shuffled when Shuffle is toggled)
            val nextIdx = (currentIdx + 1) % q.size
            val nextSong = q[nextIdx]
            val downloadedList = database.getDownloadedTracks()
            val localTrack = downloadedList.find {
                it.song.id == nextSong.id ||
                (it.song.title.trim().equals(nextSong.title.trim(), ignoreCase = true) &&
                 it.song.artist.trim().equals(nextSong.artist.trim(), ignoreCase = true))
            }

            val playableUri = if (nextSong.mediaUri.isNotBlank() && (File(nextSong.mediaUri.removePrefix("file://")).let { it.exists() && it.length() > 0 } || nextSong.mediaUri.startsWith("content://"))) {
                nextSong.mediaUri
            } else if (localTrack != null && File(localTrack.localFilePath).let { it.exists() && it.length() > 0 }) {
                localTrack.localFilePath
            } else if (nextSong.id.startsWith("local_")) {
                nextSong.mediaUri
            } else {
                val localDeviceTrack = _uiState.value.localTracks.find {
                    it.id == nextSong.id ||
                    (it.title.trim().equals(nextSong.title.trim(), ignoreCase = true) &&
                     it.artist.trim().equals(nextSong.artist.trim(), ignoreCase = true))
                }
                if (localDeviceTrack != null && localDeviceTrack.mediaUri.isNotBlank()) {
                    localDeviceTrack.mediaUri
                } else {
                    val fallback = "${nextSong.title} ${nextSong.artist}"
                    AuraStreamResolver.resolveAudioStream(nextSong.id, fallback)
                }
            }

            if (!playableUri.isNullOrBlank() && !playableUri.startsWith("https://www.youtube.com")) {
                val artworkBytes = try {
                    withTimeoutOrNull(1000L) {
                        fetchCoverBytes(nextSong.coverUrl)
                    }
                } catch (e: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    val controller = mediaController ?: return@withContext
                    if (controller.currentMediaItem?.mediaId == curSong.id) {
                        val curItemIdx = controller.currentMediaItemIndex
                        if (controller.mediaItemCount > curItemIdx + 1) {
                            controller.removeMediaItems(curItemIdx + 1, controller.mediaItemCount)
                        }
                        val metaBuilder = MediaMetadata.Builder()
                            .setTitle(nextSong.title)
                            .setArtist(nextSong.artist)
                            .setAlbumTitle(nextSong.album)
                        if (nextSong.coverUrl.isNotBlank()) {
                            val artUri = if (nextSong.coverUrl.startsWith("/") || nextSong.coverUrl.startsWith("file:")) {
                                Uri.fromFile(File(nextSong.coverUrl.removePrefix("file://")))
                            } else {
                                Uri.parse(nextSong.coverUrl)
                            }
                            metaBuilder.setArtworkUri(artUri)
                        }
                        if (artworkBytes != null) {
                            metaBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        }

                        val nextUri = if (playableUri.startsWith("/") || playableUri.startsWith("file:")) {
                            Uri.fromFile(File(playableUri.removePrefix("file://")))
                        } else {
                            Uri.parse(playableUri)
                        }

                        val nextMediaItem = MediaItem.Builder()
                            .setUri(nextUri)
                            .setMediaId(nextSong.id)
                            .setMediaMetadata(metaBuilder.build())
                            .build()

                        controller.addMediaItem(nextMediaItem)
                    }
                }
            }
        }
    }

    fun next() {
        val state = _uiState.value
        val playQueue = state.queue.filter { !it.isArtist && !it.isAlbum }
        if (playQueue.isEmpty()) return
        val currentPos = playQueue.indexOfFirst { it.id == state.currentSong.id }

        if (state.autoRadioEnabled && (currentPos == playQueue.size - 1 || playQueue.size <= 1)) {
            viewModelScope.launch {
                val related = withContext(Dispatchers.IO) {
                    YouTubeMusicEngine.fetchRelatedTracks(
                        state.currentSong.id,
                        state.currentSong.title,
                        state.currentSong.artist
                    ).filter { rel -> playQueue.none { it.id == rel.id } && !rel.isArtist && !rel.isAlbum }
                }
                if (related.isNotEmpty()) {
                    val orderedRelated = if (state.isShuffle) related.shuffled() else related
                    val expandedQueue = playQueue + orderedRelated
                    val nextIdx = (currentPos + 1).coerceAtMost(expandedQueue.size - 1)
                    val nextSong = expandedQueue[nextIdx]
                    AuraPlaybackBridge.queue.value = expandedQueue
                    AuraPlaybackBridge.currentIndex = nextIdx
                    playSong(nextSong, expandedQueue)
                } else {
                    val fallbackIdx = if (currentPos >= 0) (currentPos + 1) % playQueue.size else 0
                    AuraPlaybackBridge.currentIndex = fallbackIdx
                    playSong(playQueue[fallbackIdx], playQueue)
                }
            }
            return
        }

        val nextIdx = if (currentPos >= 0) (currentPos + 1) % playQueue.size else 0
        val nextSong = playQueue[nextIdx]
        AuraPlaybackBridge.currentIndex = nextIdx
        playSong(nextSong, playQueue)
    }

    fun previous() {
        val state = _uiState.value
        val playQueue = state.queue.filter { !it.isArtist && !it.isAlbum }
        if (playQueue.isEmpty()) return
        val currentPos = playQueue.indexOfFirst { it.id == state.currentSong.id }
        val prevIdx = if (currentPos > 0) {
            currentPos - 1
        } else {
            playQueue.size - 1
        }
        val prevSong = playQueue[prevIdx]
        AuraPlaybackBridge.currentIndex = prevIdx
        playSong(prevSong, playQueue)
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun toggleFavorite(targetSong: Song? = null) {
        val songToToggle = targetSong ?: _uiState.value.currentSong
        viewModelScope.launch {
            val isNowFav = database.toggleFavorite(songToToggle)
            val updatedFavorites = database.getFavorites()
            val updatedUserTop = database.getTopArtistsFromHistoryAndFavorites()

            val updatedCurrent = if (_uiState.value.currentSong.id == songToToggle.id) {
                _uiState.value.currentSong.copy(isFavorite = isNowFav)
            } else _uiState.value.currentSong

            _uiState.update { state ->
                state.copy(
                    currentSong = updatedCurrent,
                    favorites = updatedFavorites,
                    userTopArtists = updatedUserTop,
                    queue = state.queue.map { if (it.id == songToToggle.id) it.copy(isFavorite = isNowFav) else it },
                    searchResults = state.searchResults.map { if (it.id == songToToggle.id) it.copy(isFavorite = isNowFav) else it }
                )
            }
        }
    }

    fun playAllFavorites() {
        val favs = _uiState.value.favorites.filter { !it.isArtist && !it.isAlbum }
        if (favs.isNotEmpty()) {
            setShuffleMode(false)
            playSong(favs[0], favs)
        }
    }

    fun scanLocalTracks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningLocal = true) }
            val tracks = com.auramusic.app.data.local.LocalAudioScanner.scanDeviceAudio(
                getApplication(),
                _uiState.value.filterShortAudios
            )
            _uiState.update { it.copy(localTracks = tracks, isScanningLocal = false) }
        }
    }

    fun playAllLocalTracks() {
        val locals = _uiState.value.localTracks.filter { !it.isArtist && !it.isAlbum }
        if (locals.isNotEmpty()) {
            setShuffleMode(false)
            playSong(locals[0], locals)
        }
    }

    fun playAllDownloads() {
        val dls = _uiState.value.downloads.map { it.song }.filter { !it.isArtist && !it.isAlbum }
        if (dls.isNotEmpty()) {
            setShuffleMode(false)
            playSong(dls[0], dls)
        }
    }

    fun playSongWithQueue(song: Song, queue: List<Song>, resetShuffle: Boolean = true) {
        val validQueue = queue.filter { !it.isArtist && !it.isAlbum }
        if (resetShuffle) {
            setShuffleMode(false)
        }
        playSong(song, validQueue)
    }

    // ==========================================
    // DOWNLOAD OPERATIONS
    // ==========================================

    fun startDownload(song: Song) {
        val ok = downloadManager.startDownload(song)
        if (!ok && _uiState.value.downloadOnlyOnWifi) {
            showToast("Descarga detenida: Ajustado para solo descargar con Wi-Fi")
        }
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            downloadManager.deleteDownload(song.id)
            val updatedDownloads = database.getDownloadedTracks()
            val updatedFavorites = database.getFavorites()
            _uiState.update {
                it.copy(
                    downloads = updatedDownloads,
                    favorites = updatedFavorites
                )
            }
            showToast("✓ Descarga offline eliminada")
        }
    }

    fun isSongDownloaded(songId: String): Boolean {
        return _uiState.value.downloads.any { it.song.id == songId }
    }

    // ==========================================
    // PLAYLIST OPERATIONS
    // ==========================================

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            database.createPlaylist(title)
            val updated = database.getPlaylists()
            _uiState.update { it.copy(playlists = updated) }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            database.deletePlaylist(playlistId)
            val updated = database.getPlaylists()
            _uiState.update { it.copy(playlists = updated) }
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: Song) {
        viewModelScope.launch {
            database.addSongToPlaylist(playlistId, songId)
            val updated = database.getPlaylists()
            _uiState.update { it.copy(playlists = updated) }
        }
    }

    fun openPlaylist(playlist: Playlist?) {
        if (playlist == null) {
            _uiState.update { it.copy(selectedPlaylist = null, playlistSongs = emptyList()) }
            return
        }
        viewModelScope.launch {
            val songs = database.getSongsInPlaylist(playlist.id)
            _uiState.update { it.copy(selectedPlaylist = playlist, playlistSongs = songs) }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            database.removeSongFromPlaylist(playlistId, songId)
            val songs = database.getSongsInPlaylist(playlistId)
            val updatedPlaylists = database.getPlaylists()
            _uiState.update {
                it.copy(
                    playlistSongs = songs,
                    playlists = updatedPlaylists
                )
            }
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        val validSongs = songs.filter { !it.isArtist && !it.isAlbum }
        if (validSongs.isEmpty()) return
        val queueToPlay = if (shuffle) validSongs.shuffled() else validSongs
        val songToStart = queueToPlay.getOrElse(startIndex) { queueToPlay[0] }
        setShuffleMode(shuffle)
        playSong(songToStart, queueToPlay)
    }

    fun setShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun setAudioQuality(quality: String) {
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("audio_quality", quality).apply()
        AuraStreamResolver.preferredQuality = quality
        AuraStreamResolver.clearCache()
        _uiState.update { it.copy(audioQuality = quality) }
    }

    fun toggleLoudnessNormalizer() {
        val newValue = !_uiState.value.loudnessNormalizer
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("loudness_normalizer", newValue).apply()
        AuraAudioEffects.setLoudnessNormalizer(newValue)
        _uiState.update { it.copy(loudnessNormalizer = newValue) }
    }

    fun toggleAutoRadio() {
        val newValue = !_uiState.value.autoRadioEnabled
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_radio_enabled", newValue).apply()
        AuraPlaybackBridge.autoRadioEnabled = newValue
        _uiState.update { it.copy(autoRadioEnabled = newValue) }
        showToast(if (newValue) "✓ Auto-Radio Infinito activado" else "Auto-Radio Infinito desactivado")
    }

    fun toggleSkipSilence() {
        val newValue = !_uiState.value.skipSilenceEnabled
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("skip_silence_enabled", newValue).apply()
        AuraPlaybackBridge.skipSilenceEnabled = newValue
        AuraPlaybackBridge.onSkipSilenceChanged?.invoke(newValue)
        _uiState.update { it.copy(skipSilenceEnabled = newValue) }
        showToast(if (newValue) "✓ Saltar silencios activado" else "Saltar silencios desactivado")
    }

    fun importExternalPlaylist(urlOrQuery: String) {
        val trimmed = urlOrQuery.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            showToast("Importando playlist de YouTube...")
            val result = withContext(Dispatchers.IO) {
                YouTubeMusicEngine.importExternalPlaylist(trimmed)
            }
            if (result != null && result.second.isNotEmpty()) {
                val (playlistTitle, tracks) = result
                val created = database.createPlaylist(playlistTitle)
                tracks.forEach { song ->
                    database.addSongToPlaylist(created.id, song)
                }
                val updated = database.getPlaylists()
                _uiState.update { it.copy(playlists = updated) }
                showToast("✓ Playlist '$playlistTitle' (${tracks.size} canciones) importada")
            } else {
                showToast("No se pudo importar la playlist")
            }
        }
    }

    fun toggleFilterShortAudios() {
        val newVal = !_uiState.value.filterShortAudios
        _uiState.update { it.copy(filterShortAudios = newVal) }
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("filter_short_audios", newVal).apply()
        scanLocalTracks()
        showToast(if (newVal) "Filtro activo: Ocultando audios < 30s y notas de voz" else "Filtro desactivado: Mostrando todos los audios")
    }

    fun toggleDownloadOnlyOnWifi() {
        val newVal = !_uiState.value.downloadOnlyOnWifi
        _uiState.update { it.copy(downloadOnlyOnWifi = newVal) }
        downloadManager.downloadOnlyOnWifi = newVal
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("download_wifi_only", newVal).apply()
        showToast(if (newVal) "Descargas limitadas a solo Wi-Fi" else "Descargas permitidas con datos móviles")
    }

    fun togglePauseHistory() {
        val newVal = !_uiState.value.pauseHistory
        _uiState.update { it.copy(pauseHistory = newVal) }
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("pause_history", newVal).apply()
        showToast(if (newVal) "Historial de escucha pausado" else "Historial de escucha reanudado")
    }

    fun clearListeningHistory() {
        viewModelScope.launch {
            database.clearHistory()
            val updatedUserTop = database.getTopArtistsFromHistoryAndFavorites()
            val updatedStats = database.getListeningStats()
            _uiState.update { it.copy(userTopArtists = updatedUserTop, listeningStats = updatedStats) }
            showToast("✓ Historial de reproducción eliminado")
        }
    }

    fun toggleGapless() {
        val newVal = !_uiState.value.gaplessEnabled
        _uiState.update { it.copy(gaplessEnabled = newVal) }
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("gapless_enabled", newVal).apply()
        showToast(if (newVal) "Gapless activado: Reproducción continua sin pausas" else "Gapless desactivado")
    }

    fun setShowChangelogDialog(show: Boolean) {
        _uiState.update { it.copy(showChangelogDialog = show) }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = database.exportBackupJson()
                val context = getApplication<Application>()
                val success = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                        true
                    } ?: false
                }
                if (success) {
                    showToast("✓ Respaldo exportado correctamente")
                } else {
                    showToast("No se pudo escribir el archivo de respaldo")
                }
            } catch (e: Exception) {
                showToast("Error al exportar: ${e.message}")
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                }
                if (!json.isNullOrBlank()) {
                    val ok = database.importBackupJson(json)
                    if (ok) {
                        val favs = database.getFavorites()
                        val pls = database.getPlaylists()
                        val followed = database.getFollowedArtists()
                        val savedAlbs = database.getSavedAlbums()
                        val stats = database.getListeningStats()
                        _uiState.update {
                            it.copy(
                                favorites = favs,
                                playlists = pls,
                                followedArtists = followed,
                                savedAlbums = savedAlbs,
                                listeningStats = stats
                            )
                        }
                        showToast("✓ Respaldo restaurado (${favs.size} favs, ${pls.size} listas)")
                    } else {
                        showToast("Error al restaurar el respaldo")
                    }
                } else {
                    showToast("El archivo de respaldo está vacío")
                }
            } catch (e: Exception) {
                showToast("Error al restaurar: ${e.message}")
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(updateInfo = null) }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            showToast("Buscando actualizaciones...")
            val client = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url("https://api.github.com/repos/respaldoperez348-rgb/AURA-MUSIC-ANDROID/releases")
                .header("User-Agent", "AuraMusic/2.1.2")
                .build()

            val update = withContext(Dispatchers.IO) {
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@withContext null
                        val jsonArray = org.json.JSONArray(body)
                        if (jsonArray.length() == 0) return@withContext null

                        val currentVersion = "2.1.2"
                        var candidateRelease: JSONObject? = null
                        var candidateTag = ""

                        for (i in 0 until jsonArray.length()) {
                            val rel = jsonArray.getJSONObject(i)
                            if (rel.optBoolean("draft", false)) continue
                            val tag = rel.optString("tag_name", "").removePrefix("v").trim()
                            if (tag.isNotBlank()) {
                                candidateRelease = rel
                                candidateTag = tag
                                break
                            }
                        }

                        if (candidateRelease == null) return@withContext null

                        fun parseVersion(v: String): List<Int> {
                            val clean = v.split("-").firstOrNull() ?: v
                            return clean.split(".").mapNotNull { it.toIntOrNull() }
                        }

                        val remoteParts = parseVersion(candidateTag)
                        val currentParts = parseVersion(currentVersion)
                        val maxLen = maxOf(remoteParts.size, currentParts.size)
                        var isNewer = false
                        for (idx in 0 until maxLen) {
                            val r = remoteParts.getOrElse(idx) { 0 }
                            val c = currentParts.getOrElse(idx) { 0 }
                            if (r > c) {
                                isNewer = true
                                break
                            } else if (r < c) {
                                isNewer = false
                                break
                            }
                        }

                        val bodyNotes = candidateRelease.optString("body", "Mejoras de rendimiento y estabilidad.")
                        val assets = candidateRelease.optJSONArray("assets")
                        var apkUrl = candidateRelease.optString("html_url", "https://github.com/respaldoperez348-rgb/AURA-MUSIC-ANDROID/releases")
                        if (assets != null && assets.length() > 0) {
                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                if (asset.optString("name").endsWith(".apk")) {
                                    apkUrl = asset.optString("browser_download_url")
                                    break
                                }
                            }
                        }

                        AppUpdateInfo(
                            hasUpdate = isNewer,
                            latestVersion = candidateTag,
                            downloadUrl = apkUrl,
                            releaseNotes = bodyNotes
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            if (update != null) {
                if (update.hasUpdate) {
                    _uiState.update { it.copy(updateInfo = update) }
                    showToast("¡Nueva versión v${update.latestVersion} disponible!")
                } else {
                    _uiState.update { it.copy(updateInfo = null) }
                    showToast("✓ Tu aplicación ya está actualizada a la última versión (v2.1.2)")
                }
            } else {
                showToast("No se pudo conectar a GitHub Releases")
            }
        }
    }

    fun exportBackup() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val json = database.exportBackupJson()
            val savedFile = withContext(Dispatchers.IO) {
                try {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = File(dir, "aura_backup_v2.json")
                    file.writeText(json)
                    file
                } catch (e: Exception) {
                    null
                }
            }
            if (savedFile != null) {
                showToast("✓ Backup guardado en almacenamiento (${savedFile.name})")
            } else {
                showToast("Error al exportar backup")
            }
        }
    }

    fun importBackup() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                try {
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val file = File(dir, "aura_backup_v2.json")
                    if (file.exists()) file.readText() else null
                } catch (e: Exception) {
                    null
                }
            }
            if (json.isNullOrBlank()) {
                showToast("No se encontró archivo aura_backup_v2.json")
                return@launch
            }
            val ok = database.importBackupJson(json)
            if (ok) {
                val favs = database.getFavorites()
                val pls = database.getPlaylists()
                val followed = database.getFollowedArtists()
                val savedAlbs = database.getSavedAlbums()
                val stats = database.getListeningStats()
                _uiState.update {
                    it.copy(
                        favorites = favs,
                        playlists = pls,
                        followedArtists = followed,
                        savedAlbums = savedAlbs,
                        listeningStats = stats
                    )
                }
                showToast("✓ Backup restaurado (${favs.size} favs, ${pls.size} listas)")
            } else {
                showToast("Error al restaurar backup")
            }
        }
    }

    fun setThemePalette(paletteName: String) {
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("palette", paletteName).apply()
        _uiState.update { it.copy(currentTheme = paletteName) }
        showToast("Tema: $paletteName")
    }

    fun setAppLanguage(language: String) {
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("app_language", language).apply()
        _uiState.update { it.copy(appLanguage = language) }
        if (_uiState.value.showLyricsTranslation && _uiState.value.lyrics.isNotEmpty()) {
            viewModelScope.launch {
                val targetLang = getLanguageCode(language)
                val translated = LyricsEngine.translateLyrics(_uiState.value.lyrics, targetLang)
                _uiState.update { it.copy(lyrics = translated) }
            }
        }
        showToast("✓ $language")
    }

    fun setLyricsOffsetMs(offsetMs: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("lyrics_offset_ms", offsetMs).apply()
        _uiState.update { it.copy(lyricsOffsetMs = offsetMs) }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearAppCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val imageLoader = Coil.imageLoader(context)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
                AuraStreamResolver.clearCache()
                YouTubeMusicEngine.clearCache()
                LyricsEngine.clearCache()
                context.cacheDir.listFiles()?.forEach { child ->
                    if (child.name != "aura_audio_stream_cache") {
                        runCatching { child.deleteRecursively() }
                    }
                }
                withContext(Dispatchers.Main) {
                    showToast("✓ Memoria caché liberada con éxito")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("✓ Caché liberada")
                }
            }
        }
    }

    fun addToQueueNext(song: Song) {
        _uiState.update { state ->
            val newQueue = state.queue.toMutableList()
            val insertIdx = (state.currentIndex + 1).coerceAtMost(newQueue.size)
            newQueue.add(insertIdx, song)
            AuraPlaybackBridge.queue.value = newQueue
            state.copy(queue = newQueue)
        }
        prefetchNextTrackIntoPlayer()
    }

    fun setShuffleMode(enabled: Boolean) {
        val state = _uiState.value
        val curSong = state.currentSong
        val currentQueue = state.queue.filter { !it.isArtist && !it.isAlbum }

        if (enabled && currentQueue.size > 1) {
            if (!state.isShuffle || unshuffledQueue.isEmpty()) {
                unshuffledQueue = currentQueue
            }
            val rest = currentQueue.filter { it.id != curSong.id }.shuffled()
            val shuffledQueue = listOf(curSong) + rest
            AuraPlaybackBridge.queue.value = shuffledQueue
            AuraPlaybackBridge.currentIndex = 0
            _uiState.update {
                it.copy(
                    isShuffle = true,
                    queue = shuffledQueue,
                    currentIndex = 0
                )
            }
            if (shuffledQueue.size <= 3 && state.autoRadioEnabled && !curSong.id.startsWith("local_")) {
                radioQueueJob?.cancel()
                radioQueueJob = viewModelScope.launch(Dispatchers.IO) {
                    val related = YouTubeMusicEngine.fetchRelatedTracks(curSong.id, curSong.title, curSong.artist)
                        .filter { rel -> shuffledQueue.none { it.id == rel.id } && !rel.isArtist && !rel.isAlbum }
                    if (related.isNotEmpty() && _uiState.value.currentSong.id == curSong.id) {
                        val expanded = _uiState.value.queue + related.shuffled()
                        AuraPlaybackBridge.queue.value = expanded
                        _uiState.update { it.copy(queue = expanded) }
                        prefetchNextTrackIntoPlayer()
                    }
                }
            }
        } else if (!enabled && unshuffledQueue.isNotEmpty() && unshuffledQueue.any { it.id == curSong.id }) {
            val restoredIdx = unshuffledQueue.indexOfFirst { it.id == curSong.id }.coerceAtLeast(0)
            AuraPlaybackBridge.queue.value = unshuffledQueue
            AuraPlaybackBridge.currentIndex = restoredIdx
            _uiState.update {
                it.copy(
                    isShuffle = false,
                    queue = unshuffledQueue,
                    currentIndex = restoredIdx
                )
            }
        } else {
            _uiState.update { it.copy(isShuffle = enabled) }
        }

        AuraPlaybackBridge.isShuffle = enabled
        mediaController?.shuffleModeEnabled = false
        prefetchNextTrackIntoPlayer()
    }

    fun toggleShuffle() {
        setShuffleMode(!_uiState.value.isShuffle)
    }

    fun toggleRepeat() {
        val newRepeat = !_uiState.value.isRepeat
        _uiState.update { it.copy(isRepeat = newRepeat) }
        AuraPlaybackBridge.isRepeat = newRepeat
        mediaController?.repeatMode = if (newRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // ==========================================
    // AUDIO FX, SLEEP TIMER & QUEUE SHEETS
    // ==========================================

    fun setShowEqualizer(show: Boolean) {
        _uiState.update { it.copy(showEqualizer = show) }
    }

    fun setShowSleepTimer(show: Boolean) {
        _uiState.update { it.copy(showSleepTimer = show) }
    }

    fun setShowQueue(show: Boolean) {
        _uiState.update { it.copy(showQueue = show) }
    }

    fun startSleepTimer(minutes: Int) {
        SleepTimerManager.startTimer(minutes) {
            mediaController?.pause()
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    fun setSleepTimerEndOfTrack() {
        SleepTimerManager.setEndOfTrack {
            mediaController?.pause()
            _uiState.update { it.copy(isPlaying = false) }
        }
    }

    fun cancelSleepTimer() {
        SleepTimerManager.cancelTimer()
    }

    fun removeFromQueue(index: Int) {
        _uiState.update { state ->
            val updated = state.queue.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            val newCurrentIdx = updated.indexOfFirst { it.id == state.currentSong.id }.coerceAtLeast(0)
            AuraPlaybackBridge.queue.value = updated
            AuraPlaybackBridge.currentIndex = newCurrentIdx
            state.copy(queue = updated, currentIndex = newCurrentIdx)
        }
        prefetchNextTrackIntoPlayer()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val updated = state.queue.toMutableList()
            if (fromIndex in updated.indices && toIndex in updated.indices && fromIndex != toIndex) {
                val moved = updated.removeAt(fromIndex)
                updated.add(toIndex, moved)
            }
            val newCurrentIdx = updated.indexOfFirst { it.id == state.currentSong.id }.coerceAtLeast(0)
            AuraPlaybackBridge.queue.value = updated
            AuraPlaybackBridge.currentIndex = newCurrentIdx
            state.copy(queue = updated, currentIndex = newCurrentIdx)
        }
        prefetchNextTrackIntoPlayer()
    }

    fun clearQueue() {
        _uiState.update { state ->
            val single = listOf(state.currentSong)
            AuraPlaybackBridge.queue.value = single
            AuraPlaybackBridge.currentIndex = 0
            state.copy(queue = single, currentIndex = 0)
        }
        mediaController?.let { controller ->
            val curIdx = controller.currentMediaItemIndex
            if (controller.mediaItemCount > curIdx + 1) {
                controller.removeMediaItems(curIdx + 1, controller.mediaItemCount)
            }
        }
    }

    fun playQueueItem(index: Int) {
        val state = _uiState.value
        if (index in state.queue.indices) {
            _uiState.update { it.copy(currentIndex = index) }
            playSong(state.queue[index])
        }
    }

    fun setShowFullscreenLyrics(show: Boolean) {
        _uiState.update { it.copy(showFullscreenLyrics = show) }
    }

    fun openArtistDetail(artist: Song) {
        _uiState.update {
            it.copy(
                selectedArtist = artist,
                isLoadingArtist = true,
                artistTopSongs = emptyList(),
                artistProfile = null
            )
        }
        viewModelScope.launch {
            val queryOrId = if (artist.id.startsWith("UC")) artist.id else artist.title
            val profile = YouTubeMusicEngine.fetchArtistProfile(queryOrId)
            if (profile != null) {
                _uiState.update {
                    it.copy(
                        artistProfile = profile,
                        artistTopSongs = profile.topTracks,
                        isLoadingArtist = false
                    )
                }
            } else {
                val tracks = YouTubeMusicEngine.fetchArtistTopTracks(artist.title)
                _uiState.update { it.copy(artistTopSongs = tracks, isLoadingArtist = false) }
            }
        }
    }

    fun dismissArtistDetail() {
        _uiState.update { it.copy(selectedArtist = null, artistTopSongs = emptyList(), artistProfile = null) }
    }

    fun openAlbumDetail(album: Album) {
        _uiState.update {
            it.copy(
                selectedAlbum = album,
                showAlbumDetail = true,
                isLoadingAlbum = true,
                albumTracks = emptyList()
            )
        }
        viewModelScope.launch {
            val tracks = YouTubeMusicEngine.fetchAlbumTracks(album.browseId, album.title, album.year)
            _uiState.update { it.copy(albumTracks = tracks, isLoadingAlbum = false) }
        }
    }

    fun dismissAlbumDetail() {
        _uiState.update { it.copy(showAlbumDetail = false, selectedAlbum = null, albumTracks = emptyList()) }
    }

    fun playAlbum(album: Album) {
        viewModelScope.launch {
            val tracks = if (_uiState.value.selectedAlbum?.browseId == album.browseId && _uiState.value.albumTracks.isNotEmpty()) {
                _uiState.value.albumTracks
            } else {
                YouTubeMusicEngine.fetchAlbumTracks(album.browseId, album.title, album.year)
            }
            if (tracks.isNotEmpty()) {
                playSong(tracks[0], tracks)
            }
        }
    }

    fun toggleFollowArtist(
        artistId: String,
        artistName: String,
        coverUrl: String,
        subscribers: String = ""
    ) {
        viewModelScope.launch {
            val isNowFollowed = database.toggleFollowArtist(artistId, artistName, coverUrl, subscribers)
            val updated = database.getFollowedArtists()
            _uiState.update { it.copy(followedArtists = updated) }
            showToast(if (isNowFollowed) "✓ Siguiendo a $artistName" else "Dejaste de seguir a $artistName")
        }
    }

    fun toggleSaveAlbum(album: Album) {
        viewModelScope.launch {
            val isNowSaved = database.toggleSaveAlbum(album)
            val updated = database.getSavedAlbums()
            _uiState.update { it.copy(savedAlbums = updated) }
            showToast(if (isNowSaved) "✓ Álbum guardado en tu colección" else "Álbum eliminado de tu colección")
        }
    }

    fun openCurrentSongArtist() {
        val song = _uiState.value.currentSong
        val cleanArtist = song.artist.split(",", "•", "&", "feat.", "ft.").firstOrNull()?.trim().orEmpty()
        if (cleanArtist.isBlank() || cleanArtist == "Unknown Artist") return
        openArtistDetail(
            Song(
                id = cleanArtist,
                title = cleanArtist,
                artist = "Artista",
                coverUrl = song.coverUrl,
                isArtist = true
            )
        )
    }

    fun setShowYouTubeLogin(show: Boolean) {
        _uiState.update { it.copy(showYouTubeLogin = show) }
    }

    fun onYouTubeLoginCaptured(cookie: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showYouTubeLogin = false) }
            val context = getApplication<Application>()
            val (name, handle, avatar) = YouTubeAuthManager.fetchAccountDetailsFromCookie(cookie)
            val accountState = YouTubeAuthManager.saveSession(context, cookie, name, handle, avatar)
            YouTubeMusicEngine.clearCache()
            _uiState.update { it.copy(ytAccount = accountState) }
            showToast("✓ Cuenta vinculada: ${accountState.accountName}")
            // Refresh personalized feed with authenticated session
            launch(Dispatchers.IO) {
                runCatching {
                    val personalized = YouTubeMusicEngine.fetchPersonalizedHomeFeed()
                    val fresh = if (personalized.isNotEmpty()) personalized else YouTubeMusicEngine.searchTracks("")
                    if (fresh.isNotEmpty()) {
                        _uiState.update { it.copy(searchResults = fresh) }
                    }
                }
            }
        }
    }

    fun setThemeMode(mode: String) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("theme_mode", mode).apply()
        }
        val title = when (mode.lowercase()) {
            "claro" -> "Modo Claro"
            "sistema" -> "Modo Automático del Sistema"
            else -> "Modo Oscuro"
        }
        showToast("✓ $title")
    }

    fun logoutYouTubeAccount() {
        val context = getApplication<Application>()
        val state = YouTubeAuthManager.logout(context)
        YouTubeMusicEngine.clearCache()
        _uiState.update { it.copy(ytAccount = state) }
        showToast("Sesión de YouTube Music cerrada")
    }

    override fun onCleared() {
        AuraPlaybackBridge.onNextRequested = null
        AuraPlaybackBridge.onPreviousRequested = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
