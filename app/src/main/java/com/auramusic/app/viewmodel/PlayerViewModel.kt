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
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.auramusic.app.data.database.AuraDatabase
import com.auramusic.app.data.database.DownloadedTrack
import com.auramusic.app.data.database.Playlist
import com.auramusic.app.data.model.LyricLine
import com.auramusic.app.data.model.Song
import com.auramusic.app.network.AuraDownloadManager
import com.auramusic.app.network.AuraStreamResolver
import com.auramusic.app.network.LyricsEngine
import com.auramusic.app.network.YouTubeMusicEngine
import com.auramusic.app.playback.AuraAudioEffects
import com.auramusic.app.playback.AuraPlaybackService
import com.auramusic.app.playback.SleepTimerManager
import com.auramusic.app.playback.SleepTimerState
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
import java.io.ByteArrayOutputStream

enum class MainTab {
    STUDIO, RADAR, VAULT
}

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
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val searchResults: List<Song> = emptyList(),
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
    val currentTheme: String = "Cyber Neon"
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AuraDatabase.getInstance(application)
    private val downloadManager = AuraDownloadManager(application, database, viewModelScope)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var searchJob: Job? = null
    private var lyricsJob: Job? = null

    init {
        AuraStreamResolver.init()
        initializeMediaController()
        startProgressUpdater()
        loadInitialData()
        observeDownloadProgress()
        observeSleepTimer()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load persistent data from SQLite Database
            val savedFavs = database.getFavorites()
            val savedDownloads = database.getDownloadedTracks()
            val savedPlaylists = database.getPlaylists()

            val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
            val savedPalette = prefs.getString("palette", "Cyber Neon") ?: "Cyber Neon"

            val initialTracks = YouTubeMusicEngine.searchTracks("")

            _uiState.update {
                it.copy(
                    searchResults = initialTracks,
                    queue = if (it.queue.isEmpty()) initialTracks else it.queue,
                    favorites = if (savedFavs.isNotEmpty()) savedFavs else initialTracks.filter { s -> s.isFavorite },
                    downloads = savedDownloads,
                    playlists = savedPlaylists,
                    currentTheme = savedPalette
                )
            }

            // Sync initial favorites into DB if first run
            if (savedFavs.isEmpty()) {
                initialTracks.filter { it.isFavorite }.forEach { database.addFavorite(it) }
            }

            scanLocalTracks()
            loadLyricsForSong(_uiState.value.currentSong)
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

    private fun loadLyricsForSong(song: Song) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            val lyrics = LyricsEngine.fetchSyncedLyrics(
                trackName = song.title,
                artistName = song.artist,
                durationSec = song.durationMs / 1000
            )
            _uiState.update { it.copy(lyrics = lyrics) }
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
            setupPlayerListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val duration = mediaController?.duration?.takeIf { it > 0 } ?: _uiState.value.durationMs
                    _uiState.update { it.copy(durationMs = duration) }
                } else if (playbackState == Player.STATE_ENDED) {
                    SleepTimerManager.notifyTrackEnded()
                    if (!_uiState.value.sleepTimer.isEndOfTrack) {
                        next()
                    }
                }
            }
        })
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _uiState.update {
                            it.copy(
                                currentPositionMs = controller.currentPosition,
                                durationMs = controller.duration.coerceAtLeast(1L)
                            )
                        }
                    }
                }
                delay(300)
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
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = YouTubeMusicEngine.searchTracks(query)
            _uiState.update {
                it.copy(
                    searchResults = results,
                    isSearching = false
                )
            }
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
            val context = getApplication<Application>()
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap ?: return@withContext null
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    fun playSong(song: Song) {
        // 1. Immediately reflect the selected track in the UI for instant response
        _uiState.update { state ->
            val updatedQueue = if (state.queue.none { it.id == song.id }) {
                listOf(song) + state.queue
            } else {
                state.queue
            }
            val newIndex = updatedQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            state.copy(
                currentSong = song,
                queue = updatedQueue,
                currentIndex = newIndex,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = song.durationMs,
                lyrics = emptyList() // Clear previous song lyrics immediately
            )
        }

        // 2. Fetch lyrics concurrently
        loadLyricsForSong(song)

        viewModelScope.launch {
            val downloadedList = database.getDownloadedTracks()
            val localTrack = downloadedList.find { it.song.id == song.id }

            // 3. Resolve audio stream & cover art bytes in parallel
            val streamDeferred = async(Dispatchers.IO) {
                if (localTrack != null) {
                    localTrack.localFilePath
                } else if (song.id.startsWith("local_")) {
                    song.mediaUri
                } else {
                    AuraStreamResolver.resolveAudioStream(song.id) ?: song.mediaUri
                }
            }

            val artworkDeferred = async(Dispatchers.IO) {
                fetchCoverBytes(song.coverUrl)
            }

            val playableUri = streamDeferred.await()
            val artworkBytes = artworkDeferred.await()

            // 4. Build MediaMetadata with Artwork URI and Artwork Data Bytes for system notification
            val metaBuilder = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)

            if (song.coverUrl.isNotBlank()) {
                metaBuilder.setArtworkUri(Uri.parse(song.coverUrl))
            }
            if (artworkBytes != null) {
                metaBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }

            val mediaItem = MediaItem.Builder()
                .setUri(playableUri)
                .setMediaId(song.id)
                .setMediaMetadata(metaBuilder.build())
                .build()

            mediaController?.run {
                setMediaItem(mediaItem)
                prepare()
                play()
            }

            database.addToHistory(song)

            // 5. Background prefetch for NEXT song in queue (zero-latency skipping)
            val q = _uiState.value.queue
            val idx = _uiState.value.currentIndex
            if (q.isNotEmpty()) {
                val nextIdx = (idx + 1) % q.size
                val nextSong = q[nextIdx]
                if (!nextSong.id.startsWith("local_") && !AuraStreamResolver.isCached(nextSong.id)) {
                    launch(Dispatchers.IO) {
                        AuraStreamResolver.resolveAudioStream(nextSong.id)
                    }
                }
            }
        }
    }

    fun next() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return
        val nextIdx = (state.currentIndex + 1) % state.queue.size
        val nextSong = state.queue[nextIdx]
        _uiState.update { it.copy(currentIndex = nextIdx) }
        playSong(nextSong)
    }

    fun previous() {
        val state = _uiState.value
        if (state.queue.isEmpty()) return
        val prevIdx = if (state.currentIndex - 1 < 0) state.queue.size - 1 else state.currentIndex - 1
        val prevSong = state.queue[prevIdx]
        _uiState.update { it.copy(currentIndex = prevIdx) }
        playSong(prevSong)
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

            val updatedCurrent = if (_uiState.value.currentSong.id == songToToggle.id) {
                _uiState.value.currentSong.copy(isFavorite = isNowFav)
            } else _uiState.value.currentSong

            _uiState.update { state ->
                state.copy(
                    currentSong = updatedCurrent,
                    favorites = updatedFavorites,
                    queue = state.queue.map { if (it.id == songToToggle.id) it.copy(isFavorite = isNowFav) else it },
                    searchResults = state.searchResults.map { if (it.id == songToToggle.id) it.copy(isFavorite = isNowFav) else it }
                )
            }
        }
    }

    fun playAllFavorites() {
        val favs = _uiState.value.favorites
        if (favs.isNotEmpty()) {
            _uiState.update { it.copy(queue = favs, currentIndex = 0) }
            playSong(favs[0])
        }
    }

    fun scanLocalTracks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningLocal = true) }
            val tracks = com.auramusic.app.data.local.LocalAudioScanner.scanDeviceAudio(getApplication())
            _uiState.update { it.copy(localTracks = tracks, isScanningLocal = false) }
        }
    }

    fun playAllLocalTracks() {
        val locals = _uiState.value.localTracks
        if (locals.isNotEmpty()) {
            _uiState.update { it.copy(queue = locals, currentIndex = 0) }
            playSong(locals[0])
        }
    }

    fun playAllDownloads() {
        val dls = _uiState.value.downloads.map { it.song }
        if (dls.isNotEmpty()) {
            _uiState.update { it.copy(queue = dls, currentIndex = 0) }
            playSong(dls[0])
        }
    }

    // ==========================================
    // DOWNLOAD OPERATIONS
    // ==========================================

    fun startDownload(song: Song) {
        downloadManager.startDownload(song)
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
        if (songs.isEmpty()) return
        val queueToPlay = if (shuffle) songs.shuffled() else songs
        val songToStart = queueToPlay.getOrElse(startIndex) { queueToPlay[0] }
        _uiState.update {
            it.copy(
                queue = queueToPlay,
                currentIndex = startIndex,
                isShuffle = shuffle
            )
        }
        playSong(songToStart)
    }

    fun setShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun setAudioQuality(quality: String) {
        _uiState.update { it.copy(audioQuality = quality) }
    }

    fun toggleLoudnessNormalizer() {
        _uiState.update { it.copy(loudnessNormalizer = !it.loudnessNormalizer) }
    }

    fun setThemePalette(paletteName: String) {
        val prefs = getApplication<Application>().getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("palette", paletteName).apply()
        _uiState.update { it.copy(currentTheme = paletteName) }
    }

    fun addToQueueNext(song: Song) {
        _uiState.update { state ->
            val newQueue = state.queue.toMutableList()
            val insertIdx = (state.currentIndex + 1).coerceAtMost(newQueue.size)
            newQueue.add(insertIdx, song)
            state.copy(queue = newQueue)
        }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _uiState.update { it.copy(isRepeat = !it.isRepeat) }
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
            state.copy(queue = updated)
        }
    }

    fun clearQueue() {
        _uiState.update { state ->
            state.copy(queue = listOf(state.currentSong), currentIndex = 0)
        }
    }

    fun playQueueItem(index: Int) {
        val state = _uiState.value
        if (index in state.queue.indices) {
            _uiState.update { it.copy(currentIndex = index) }
            playSong(state.queue[index])
        }
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
