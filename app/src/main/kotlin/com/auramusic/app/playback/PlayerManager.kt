package com.auramusic.app.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auramusic.app.AuraApplication
import com.auramusic.app.data.models.Song
import com.auramusic.app.data.repository.MusicRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val isLoading: Boolean = false,
)

@OptIn(UnstableApi::class)
class PlayerManager(
    private val context: Context,
    private val repository: MusicRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var exoPlayer: ExoPlayer? = null
    lateinit var forwardingPlayer: Player
        private set

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var unshuffledQueue: List<Song> = emptyList()
    private var retriedSongId: String? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        initPlayer()
    }

    private fun initPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying, isLoading = false) }
                if (isPlaying) {
                    startProgressUpdates()
                } else {
                    stopProgressUpdates()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        _playbackState.update { it.copy(isLoading = true) }
                    }
                    Player.STATE_READY -> {
                        retriedSongId = null
                        _playbackState.update {
                            it.copy(
                                durationMs = player.duration.coerceAtLeast(it.durationMs),
                                isLoading = false
                            )
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (_playbackState.value.isRepeatEnabled) {
                            player.seekTo(0L)
                            player.play()
                        } else {
                            playNext()
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.e(error, "ExoPlayer Error: ${error.errorCodeName}")
                val current = _playbackState.value.currentSong
                val lastPos = _playbackState.value.currentPositionMs
                if (current != null && retriedSongId != current.id) {
                    retriedSongId = current.id
                    playSong(current, _playbackState.value.queue, startPositionMs = lastPos)
                } else {
                    _playbackState.update { it.copy(isLoading = false, isPlaying = false) }
                }
            }
        })

        exoPlayer = player

        forwardingPlayer = object : ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun play() {
                resumeOrRestartPlayback()
            }

            override fun pause() {
                player.pause()
            }

            override fun seekToNext() {
                playNext()
            }

            override fun seekToNextMediaItem() {
                playNext()
            }

            override fun seekToPrevious() {
                playPrevious()
            }

            override fun seekToPreviousMediaItem() {
                playPrevious()
            }
        }

        bindMediaSessionService()
    }

    private fun bindMediaSessionService() {
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, AuraMusicService::class.java)
            )
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture?.addListener({
                runCatching {
                    mediaController = controllerFuture?.get()
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Timber.w(e, "No se pudo enlazar MediaController inicial")
        }
    }

    private fun ensureServiceStarted() {
        try {
            val serviceIntent = Intent(context, AuraMusicService::class.java)
            context.startService(serviceIntent)
            if (mediaController == null) {
                bindMediaSessionService()
            }
        } catch (_: Exception) {}
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.takeIf { d -> d > 0 } ?: it.durationMs
                        )
                    }
                }
                delay(280)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song), startPositionMs: Long = 0L) {
        playJob?.cancel()
        exoPlayer?.pause()
        ensureServiceStarted()

        val effectiveQueue = if (queue.any { it.id == song.id }) queue else listOf(song) + queue
        unshuffledQueue = effectiveQueue

        _playbackState.update {
            it.copy(
                currentSong = song,
                queue = effectiveQueue,
                currentPositionMs = startPositionMs,
                durationMs = song.durationSeconds.coerceAtLeast(180L) * 1000L,
                isPlaying = false,
                isLoading = true
            )
        }

        playJob = scope.launch {
            val streamData = repository.resolveStreamUrl(song.id)
            if (streamData != null) {
                val (streamUrl, requestHeaders) = streamData
                exoPlayer?.let { player ->
                    val metadata = MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album ?: "AURA Music")
                        .apply {
                            if (song.thumbnailUrl.isNotBlank()) {
                                setArtworkUri(Uri.parse(song.thumbnailUrl))
                            }
                        }
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setMediaId(song.id)
                        .setUri(streamUrl)
                        .setMediaMetadata(metadata)
                        .build()

                    val mediaSource = if (streamUrl.startsWith("file:")) {
                        DefaultMediaSourceFactory(context).createMediaSource(mediaItem)
                    } else {
                        val httpFactory = DefaultHttpDataSource.Factory()
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                            .setDefaultRequestProperties(requestHeaders)
                            .setAllowCrossProtocolRedirects(true)
                            .setConnectTimeoutMs(10_000)
                            .setReadTimeoutMs(15_000)

                        DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory))
                            .createMediaSource(mediaItem)
                    }

                    player.setMediaSource(mediaSource, startPositionMs.coerceAtLeast(0L))
                    player.prepare()
                    player.playWhenReady = true
                    player.play()
                }

                // Auto-Radio en segundo plano
                launch {
                    delay(800)
                    val curQueue = _playbackState.value.queue
                    val curIdx = curQueue.indexOfFirst { it.id == song.id }
                    if (curIdx >= curQueue.size - 2) {
                        val related = repository.fetchRelatedTracks(song.id, song.title, song.artist)
                        val existingIds = curQueue.map { it.id }.toSet()
                        val newTracks = related.filter { !existingIds.contains(it.id) }
                        if (newTracks.isNotEmpty()) {
                            _playbackState.update { state ->
                                state.copy(queue = state.queue + newTracks)
                            }
                        }
                    }
                }
            } else {
                Timber.e("No se pudo obtener URL de stream para: ${song.title}")
                _playbackState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun resumeOrRestartPlayback() {
        val player = exoPlayer ?: return
        val curSong = _playbackState.value.currentSong
        when {
            player.mediaItemCount == 0 && curSong != null -> {
                playSong(curSong, _playbackState.value.queue, _playbackState.value.currentPositionMs)
            }
            player.playbackState == Player.STATE_IDLE -> {
                if (curSong != null) {
                    playSong(curSong, _playbackState.value.queue, _playbackState.value.currentPositionMs)
                } else {
                    player.prepare()
                    player.playWhenReady = true
                    player.play()
                }
            }
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0L)
                player.playWhenReady = true
                player.play()
            }
            else -> {
                player.playWhenReady = true
                player.play()
            }
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            resumeOrRestartPlayback()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun playNext() {
        val currentQueue = _playbackState.value.queue
        val current = _playbackState.value.currentSong ?: return
        val currentIndex = currentQueue.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex + 1 < currentQueue.size) {
            val nextSong = currentQueue[currentIndex + 1]
            AuraApplication.userManager.recordSongPlay(nextSong)
            playSong(nextSong, currentQueue)
        } else if (currentQueue.isNotEmpty()) {
            val firstSong = currentQueue.first()
            playSong(firstSong, currentQueue)
        }
    }

    fun playPrevious() {
        val player = exoPlayer
        if (player != null && player.currentPosition > 4000L) {
            seekTo(0L)
            return
        }
        val currentQueue = _playbackState.value.queue
        val current = _playbackState.value.currentSong ?: return
        val currentIndex = currentQueue.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            val prevSong = currentQueue[currentIndex - 1]
            AuraApplication.userManager.recordSongPlay(prevSong)
            playSong(prevSong, currentQueue)
        } else {
            seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val state = _playbackState.value
        val curSong = state.currentSong ?: return
        val newShuffle = !state.isShuffleEnabled
        if (newShuffle && state.queue.size > 1) {
            unshuffledQueue = state.queue
            val rest = state.queue.filter { it.id != curSong.id }.shuffled()
            _playbackState.update {
                it.copy(
                    isShuffleEnabled = true,
                    queue = listOf(curSong) + rest
                )
            }
        } else {
            val restored = if (unshuffledQueue.isNotEmpty()) unshuffledQueue else state.queue
            _playbackState.update {
                it.copy(
                    isShuffleEnabled = false,
                    queue = restored
                )
            }
        }
    }

    fun toggleRepeat() {
        _playbackState.update { it.copy(isRepeatEnabled = !it.isRepeatEnabled) }
    }

    fun release() {
        stopProgressUpdates()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        exoPlayer?.release()
        exoPlayer = null
    }
}
