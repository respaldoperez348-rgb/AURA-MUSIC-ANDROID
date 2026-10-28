package com.auramusic.app.viewmodel

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auramusic.app.data.model.LyricLine
import com.auramusic.app.data.model.Song
import com.auramusic.app.network.LyricsEngine
import com.auramusic.app.network.YouTubeMusicEngine
import com.auramusic.app.playback.AuraPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val lyrics: List<LyricLine> = emptyList(),
    val currentTab: MainTab = MainTab.STUDIO,
    val isPlayerExpanded: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var searchJob: Job? = null
    private var lyricsJob: Job? = null

    init {
        initializeMediaController()
        startProgressUpdater()
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val initial = YouTubeMusicEngine.searchTracks("")
            val favs = initial.filter { it.isFavorite }
            _uiState.update {
                it.copy(
                    searchResults = initial,
                    queue = if (it.queue.isEmpty()) initial else it.queue,
                    favorites = favs
                )
            }
            loadLyricsForSong(_uiState.value.currentSong)
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

    fun playSong(song: Song) {
        val mediaItem = MediaItem.Builder()
            .setUri(song.mediaUri)
            .setMediaId(song.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .build()
            )
            .build()

        mediaController?.run {
            setMediaItem(mediaItem)
            prepare()
            play()
        }

        _uiState.update { state ->
            val updatedQueue = if (state.queue.none { it.id == song.id }) {
                listOf(song) + state.queue
            } else {
                state.queue
            }
            state.copy(
                currentSong = song,
                queue = updatedQueue,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = song.durationMs
            )
        }

        loadLyricsForSong(song)
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

    fun toggleFavorite() {
        _uiState.update { state ->
            val isNowFav = !state.currentSong.isFavorite
            val updatedSong = state.currentSong.copy(isFavorite = isNowFav)

            val updatedFavorites = if (isNowFav) {
                if (state.favorites.none { it.id == updatedSong.id }) {
                    listOf(updatedSong) + state.favorites
                } else state.favorites
            } else {
                state.favorites.filter { it.id != updatedSong.id }
            }

            state.copy(
                currentSong = updatedSong,
                favorites = updatedFavorites,
                queue = state.queue.map { if (it.id == updatedSong.id) updatedSong else it },
                searchResults = state.searchResults.map { if (it.id == updatedSong.id) updatedSong else it }
            )
        }
    }

    fun playAllFavorites() {
        val favs = _uiState.value.favorites
        if (favs.isNotEmpty()) {
            _uiState.update { it.copy(queue = favs, currentIndex = 0) }
            playSong(favs[0])
        }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _uiState.update { it.copy(isRepeat = !it.isRepeat) }
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
