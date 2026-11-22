package com.auramusic.app.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
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
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Song> = emptyList(),
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val streamQualityLabel: String = "AUDIO • HQ",
    val lyricsOffsetMs: Long = 0L,
)

/**
 * Estado liviano sin currentPositionMs para evitar que las pantallas principales
 * (Inicio, Explorar, Biblioteca, Buscador, Artista, Álbum) se recompongan 4 veces por segundo.
 */
data class UiSongState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val queue: List<Song> = emptyList(),
)

@OptIn(UnstableApi::class)
class PlayerManager(
    private val context: Context,
    private val repository: MusicRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val persistPrefs = context.getSharedPreferences("aura_persistent_queue", Context.MODE_PRIVATE)
    private var lastPersistedTimeMs: Long = 0L
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false

    companion object {
        @Volatile
        private var sharedAudioCache: SimpleCache? = null
        private val cacheLock = Any()

        private fun getOrCreateAudioCache(context: Context): SimpleCache? {
            sharedAudioCache?.let { return it }
            synchronized(cacheLock) {
                sharedAudioCache?.let { return it }
                return runCatching {
                    val cacheDir = File(context.cacheDir, "aura_exo_audio_cache").apply { mkdirs() }
                    val evictor = LeastRecentlyUsedCacheEvictor(256L * 1024L * 1024L)
                    val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
                    SimpleCache(cacheDir, evictor, dbProvider).also { sharedAudioCache = it }
                }.getOrNull()
            }
        }
    }

    private val deckPlayers: Array<ExoPlayer?> = arrayOf(null, null)
    private var activeDeckIdx: Int = 0
    private val activePlayer: ExoPlayer?
        get() = deckPlayers[activeDeckIdx]
    private val forwardingListeners = CopyOnWriteArraySet<Player.Listener>()

    private lateinit var _forwardingPlayer: Player
    val forwardingPlayer: Player
        get() {
            ensurePlayersInitialized()
            return _forwardingPlayer
        }

    @Volatile
    private var playersInitialized: Boolean = false

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var crossfadeJob: Job? = null
    private var isCrossfading: Boolean = false
    private var retiringDeckIdx: Int = -1
    private var crossfadeTriggeredSongId: String? = null
    private var unshuffledQueue: List<Song> = emptyList()
    private var isUserCollectionQueue: Boolean = false
    private var retriedSongId: String? = null
    @Volatile
    private var isUiVisible: Boolean = true
    private var currentSpeed: Float = 1f
    private var currentPitch: Float = 1f

    val audioLab = AudioLabController()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _uiSongState = MutableStateFlow(UiSongState())
    val uiSongState: StateFlow<UiSongState> = _uiSongState.asStateFlow()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                if (!isCrossfading) {
                    deckPlayers.forEach { it?.pause() }
                    updatePlaybackAndUiState { it.copy(isPlaying = false) }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (!isCrossfading) {
                    activePlayer?.volume = 0.25f
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (!isCrossfading) {
                    activePlayer?.volume = 1.0f
                }
            }
        }
    }

    init {
        ioScope.launch {
            restorePersistedQueueState()
        }
        scope.launch {
            delay(350L)
            ensurePlayersInitialized()
        }
    }

    private fun updatePlaybackAndUiState(transform: (PlaybackState) -> PlaybackState) {
        _playbackState.update { prev ->
            val next = transform(prev)
            val nextUi = UiSongState(
                currentSong = next.currentSong,
                isPlaying = next.isPlaying,
                isLoading = next.isLoading,
                isShuffleEnabled = next.isShuffleEnabled,
                isRepeatEnabled = next.isRepeatEnabled,
                queue = next.queue
            )
            if (_uiSongState.value != nextUi) {
                _uiSongState.value = nextUi
            }
            next
        }
    }

    private fun requestAppAudioFocus(): Boolean {
        val am = audioManager ?: return true
        if (hasAudioFocus) return true
        return try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                    .also { audioFocusRequest = it }
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            true
        } catch (_: Exception) {
            true
        }
    }

    private fun abandonAppAudioFocus() {
        val am = audioManager ?: return
        hasAudioFocus = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {}
    }

    private fun buildDeckExoPlayer(deckIndex: Int): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(12_000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val fastLoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                8_000,
                32_000,
                300,
                600
            )
            .build()

        // IMPORTANTE: handleAudioFocus = false en cada deck individual para que Deck 0 y Deck 1
        // NUNCA se quiten el foco de audio entre sí al cambiar de canción o hacer crossfade.
        val player = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(fastLoadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (deckIndex != activeDeckIdx || deckIndex == retiringDeckIdx) return
                notifyForwardingListeners { it.onEvents(forwardingPlayer, events) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (deckIndex != activeDeckIdx || deckIndex == retiringDeckIdx) return
                notifyForwardingListeners { it.onMediaItemTransition(mediaItem, reason) }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                if (deckIndex != activeDeckIdx || deckIndex == retiringDeckIdx) return
                notifyForwardingListeners { it.onMediaMetadataChanged(mediaMetadata) }
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (deckIndex != activeDeckIdx || deckIndex == retiringDeckIdx) return
                notifyForwardingListeners { it.onTimelineChanged(timeline, reason) }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (deckIndex != activeDeckIdx || deckIndex == retiringDeckIdx) return
                notifyForwardingListeners { it.onPlayWhenReadyChanged(playWhenReady, reason) }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (deckIndex != activeDeckIdx || deckIndex == retiringDeckIdx) return
                updatePlaybackAndUiState {
                    it.copy(
                        isPlaying = isPlaying,
                        isLoading = if (isPlaying) false else it.isLoading
                    )
                }
                if (isPlaying) startProgressUpdates() else if (!isCrossfading) stopProgressUpdates()
                notifyForwardingListeners { it.onIsPlayingChanged(isPlaying) }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (deckIndex != activeDeckIdx || deckIndex == retiringDeckIdx) return
                notifyForwardingListeners { it.onPlaybackStateChanged(state) }
                when (state) {
                    Player.STATE_BUFFERING -> {
                        updatePlaybackAndUiState { it.copy(isLoading = true) }
                    }
                    Player.STATE_READY -> {
                        retriedSongId = null
                        audioLab.attachAudioSession(player.audioSessionId)
                        player.playbackParameters = PlaybackParameters(currentSpeed, currentPitch)
                        updatePlaybackAndUiState {
                            it.copy(
                                durationMs = player.duration.takeIf { d -> d > 0 } ?: it.durationMs,
                                isLoading = false,
                                isPlaying = player.playWhenReady
                            )
                        }
                        if (player.playWhenReady && !player.isPlaying) {
                            player.play()
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (isCrossfading) return
                        if (audioLab.state.value.sleepTimerStopAfterSong) {
                            audioLab.updateSleepTimerState(0L, false)
                            player.pause()
                            updatePlaybackAndUiState { it.copy(isPlaying = false, currentPositionMs = 0L) }
                        } else if (_playbackState.value.isRepeatEnabled) {
                            player.seekTo(0L)
                            player.playWhenReady = true
                            player.play()
                        } else {
                            playNext()
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (deckIndex != activeDeckIdx) return
                Timber.e(error, "ExoPlayer Error en deck $deckIndex: ${error.errorCodeName}")
                val current = _playbackState.value.currentSong
                val lastPos = _playbackState.value.currentPositionMs
                if (current != null) {
                    // Invalidar siempre la URL en caché para no reintentar una URL expirada o bloqueada
                    repository.invalidateStreamCache(current.id)
                }
                if (current != null && retriedSongId != current.id) {
                    retriedSongId = current.id
                    playSong(
                        song = current,
                        queue = _playbackState.value.queue,
                        startPositionMs = lastPos,
                        isQueueNavigation = true,
                        shuffleOrder = null,
                        isUserCollection = isUserCollectionQueue
                    )
                } else {
                    updatePlaybackAndUiState { it.copy(isLoading = false, isPlaying = false) }
                    // Si falló al cambiar de canción en una cola de varias pistas, pasar a la siguiente pista disponible
                    val queue = _playbackState.value.queue
                    if (current != null && queue.size > 1) {
                        val idx = queue.indexOfFirst { it.id == current.id }
                        if (idx != -1 && idx + 1 < queue.size) {
                            val fallbackNext = queue[idx + 1]
                            retriedSongId = current.id
                            playSong(
                                song = fallbackNext,
                                queue = queue,
                                startPositionMs = 0L,
                                isQueueNavigation = true,
                                shuffleOrder = null,
                                isUserCollection = isUserCollectionQueue
                            )
                        }
                    }
                }
            }
        })

        return player
    }

    private fun ensurePlayersInitialized() {
        if (playersInitialized) return
        synchronized(this) {
            if (playersInitialized) return
            initPlayers()
            playersInitialized = true
        }
    }

    private fun initPlayers() {
        val primary = buildDeckExoPlayer(0)
        val secondary = buildDeckExoPlayer(1)
        deckPlayers[0] = primary
        deckPlayers[1] = secondary
        activeDeckIdx = 0

        val initialShuffle = runCatching { AuraApplication.userManager.userSettings.value.defaultShuffle }.getOrDefault(false)
        updatePlaybackAndUiState { state ->
            if (state.currentSong == null) state.copy(isShuffleEnabled = initialShuffle) else state
        }

        _forwardingPlayer = object : ForwardingPlayer(primary) {
            override fun addListener(listener: Player.Listener) {
                forwardingListeners.add(listener)
            }

            override fun removeListener(listener: Player.Listener) {
                forwardingListeners.remove(listener)
            }

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

            override fun isPlaying(): Boolean = activePlayer?.isPlaying ?: false
            override fun getPlaybackState(): Int = activePlayer?.playbackState ?: Player.STATE_IDLE
            override fun getPlayWhenReady(): Boolean = activePlayer?.playWhenReady ?: false
            override fun getCurrentPosition(): Long = activePlayer?.currentPosition ?: 0L
            override fun getDuration(): Long = activePlayer?.duration ?: C.TIME_UNSET
            override fun getBufferedPosition(): Long = activePlayer?.bufferedPosition ?: 0L
            override fun getContentPosition(): Long = activePlayer?.contentPosition ?: 0L
            override fun getContentDuration(): Long = activePlayer?.contentDuration ?: C.TIME_UNSET
            override fun getContentBufferedPosition(): Long = activePlayer?.contentBufferedPosition ?: 0L
            override fun getCurrentMediaItem(): MediaItem? =
                _playbackState.value.currentSong?.let { buildSongMediaItem(it) } ?: activePlayer?.currentMediaItem
            override fun getMediaMetadata(): MediaMetadata =
                _playbackState.value.currentSong?.let { buildSongMediaMetadata(it) }
                    ?: activePlayer?.mediaMetadata
                    ?: MediaMetadata.EMPTY
            override fun getCurrentTimeline(): Timeline = activePlayer?.currentTimeline ?: primary.currentTimeline
            override fun getCurrentMediaItemIndex(): Int = activePlayer?.currentMediaItemIndex ?: 0
            override fun getCurrentPeriodIndex(): Int = activePlayer?.currentPeriodIndex ?: 0
            override fun getMediaItemCount(): Int = activePlayer?.mediaItemCount ?: 0
            override fun hasNextMediaItem(): Boolean = _playbackState.value.queue.size > 1
            override fun hasPreviousMediaItem(): Boolean = _playbackState.value.queue.isNotEmpty()
            override fun isCurrentMediaItemSeekable(): Boolean = activePlayer?.isCurrentMediaItemSeekable ?: true
            override fun isCurrentMediaItemLive(): Boolean = activePlayer?.isCurrentMediaItemLive ?: false
            override fun isCurrentMediaItemDynamic(): Boolean = activePlayer?.isCurrentMediaItemDynamic ?: false

            override fun play() {
                resumeOrRestartPlayback()
            }

            override fun pause() {
                deckPlayers.forEach { it?.pause() }
                updatePlaybackAndUiState { it.copy(isPlaying = false) }
            }

            override fun seekTo(positionMs: Long) {
                this@PlayerManager.seekTo(positionMs)
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
        ensurePlayersInitialized()
        try {
            val serviceIntent = Intent(context, AuraMusicService::class.java)
            context.startService(serviceIntent)
            if (mediaController == null) {
                bindMediaSessionService()
            }
        } catch (_: Exception) {}
    }

    private fun notifyForwardingListeners(action: (Player.Listener) -> Unit) {
        for (listener in forwardingListeners) {
            runCatching { action(listener) }
        }
    }

    private fun buildSongMediaMetadata(song: Song): MediaMetadata {
        return MediaMetadata.Builder()
            .setTitle(song.title)
            .setDisplayTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album ?: "AURA Music")
            .apply {
                if (song.thumbnailUrl.isNotBlank()) {
                    setArtworkUri(Uri.parse(song.thumbnailUrl))
                }
            }
            .build()
    }

    private fun buildSongMediaItem(song: Song, streamUrl: String? = null): MediaItem {
        val metadata = buildSongMediaMetadata(song)
        return MediaItem.Builder()
            .setMediaId(song.id)
            .apply {
                if (!streamUrl.isNullOrBlank()) {
                    setUri(Uri.parse(streamUrl))
                }
            }
            .setMediaMetadata(metadata)
            .build()
    }

    private fun syncNotificationMediaSession(song: Song) {
        val item = buildSongMediaItem(song)
        val metadata = item.mediaMetadata
        val player = activePlayer
        notifyForwardingListeners { listener ->
            listener.onMediaItemTransition(item, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
            listener.onMediaMetadataChanged(metadata)
            if (player != null) {
                listener.onTimelineChanged(player.currentTimeline, Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED)
                listener.onIsPlayingChanged(player.isPlaying)
                listener.onPlaybackStateChanged(player.playbackState)
            }
            listener.onEvents(
                forwardingPlayer,
                Player.Events(
                    androidx.media3.common.FlagSet.Builder()
                        .addAll(
                            Player.EVENT_MEDIA_ITEM_TRANSITION,
                            Player.EVENT_MEDIA_METADATA_CHANGED,
                            Player.EVENT_TIMELINE_CHANGED,
                            Player.EVENT_IS_PLAYING_CHANGED,
                            Player.EVENT_PLAYBACK_STATE_CHANGED
                        )
                        .build()
                )
            )
        }
    }

    private fun buildMediaSource(song: Song, streamUrl: String, requestHeaders: Map<String, String>): MediaSource {
        val mediaItem = buildSongMediaItem(song, streamUrl)

        return if (streamUrl.startsWith("file:") || streamUrl.startsWith("content:")) {
            DefaultMediaSourceFactory(DefaultDataSource.Factory(context))
                .createMediaSource(mediaItem)
        } else {
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .setDefaultRequestProperties(requestHeaders)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(8_000)
                .setReadTimeoutMs(12_000)

            val upstreamFactory = DefaultDataSource.Factory(context, httpFactory)
            val cache = getOrCreateAudioCache(context)
            val dataSourceFactory = if (cache != null) {
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } else {
                upstreamFactory
            }

            DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }
    }

    private fun stopStandbyDeck(standbyIdx: Int) {
        deckPlayers.getOrNull(standbyIdx)?.apply {
            runCatching {
                playWhenReady = false
                pause()
                if (standbyIdx != 0) {
                    stop()
                    clearMediaItems()
                    volume = 1f
                } else {
                    volume = 0f
                }
            }
        }
    }

    fun setUiVisible(visible: Boolean) {
        val wasVisible = isUiVisible
        isUiVisible = visible
        if (visible && !wasVisible) {
            val player = activePlayer
            if (player != null) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.takeIf { d -> d > 0 } ?: _playbackState.value.durationMs
                _playbackState.update { it.copy(currentPositionMs = pos, durationMs = dur) }
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = activePlayer
                if (player != null) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.takeIf { d -> d > 0 } ?: _playbackState.value.durationMs
                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastPersistedTimeMs >= 4_500L) {
                        lastPersistedTimeMs = nowMs
                        persistQueueStateAsync(pos)
                    }
                    if (isUiVisible) {
                        _playbackState.update { prev ->
                            if (abs(pos - prev.currentPositionMs) >= 180L || dur != prev.durationMs) {
                                prev.copy(currentPositionMs = pos, durationMs = dur)
                            } else {
                                prev
                            }
                        }
                    }

                    val crossfadeSec = AuraApplication.userManager.userSettings.value.crossfadeSeconds
                    val curSong = _playbackState.value.currentSong
                    if (crossfadeSec > 0 &&
                        !isCrossfading &&
                        curSong != null &&
                        player.isPlaying &&
                        dur > (crossfadeSec + 3) * 1000L &&
                        pos > 2500L &&
                        !_playbackState.value.isRepeatEnabled &&
                        !audioLab.state.value.sleepTimerStopAfterSong &&
                        crossfadeTriggeredSongId != curSong.id
                    ) {
                        val remaining = dur - pos
                        val triggerWindowMs = crossfadeSec * 1000L + 350L
                        if (remaining in 300L..triggerWindowMs) {
                            val queue = _playbackState.value.queue
                            val idx = queue.indexOfFirst { it.id == curSong.id }
                            val nextSong = when {
                                idx != -1 && idx + 1 < queue.size -> queue[idx + 1]
                                queue.size > 1 -> queue.first()
                                else -> null
                            }
                            if (nextSong != null) {
                                crossfadeTriggeredSongId = curSong.id
                                performDeckCrossfade(
                                    outgoingIdx = activeDeckIdx,
                                    nextSong = nextSong,
                                    queue = queue,
                                    fadeDurationMs = crossfadeSec * 1000L
                                )
                            }
                        }
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun syncShuffleFromSettings(enabled: Boolean) {
        val state = _playbackState.value
        if (state.isShuffleEnabled == enabled) return
        if (state.currentSong == null || state.queue.size <= 1) {
            updatePlaybackAndUiState { it.copy(isShuffleEnabled = enabled) }
        } else {
            toggleShuffle()
        }
    }

    /**
     * Reproduce una canción y su cola asociada.
     * - shuffleOrder = false: reproduce estrictamente en el orden original de la lista (1, 2, 3...) y desactiva aleatorio.
     * - shuffleOrder = true: activa aleatorio y mezcla el resto de la cola.
     * - shuffleOrder = null: mantiene el estado y orden actual de la cola (usado en Siguiente / Anterior).
     */
    fun playSong(
        song: Song,
        queue: List<Song> = listOf(song),
        startPositionMs: Long = 0L,
        isQueueNavigation: Boolean = false,
        shuffleOrder: Boolean? = false,
        isUserCollection: Boolean = false,
    ) {
        ensurePlayersInitialized()
        playJob?.cancel()
        crossfadeJob?.cancel()
        isCrossfading = false
        retiringDeckIdx = -1
        crossfadeTriggeredSongId = null

        if (!isQueueNavigation) {
            isUserCollectionQueue = isUserCollection
        }

        // En reproducción directa (sin crossfade en curso), usar siempre el Deck 0 primario
        // para que MediaSession y ForwardingPlayer estén 100% sincronizados.
        if (activeDeckIdx != 0) {
            stopStandbyDeck(activeDeckIdx)
            activeDeckIdx = 0
        } else {
            stopStandbyDeck(1)
        }

        activePlayer?.apply {
            runCatching {
                playWhenReady = false
                pause()
                stop()
                clearMediaItems()
                volume = 1f
            }
        }

        requestAppAudioFocus()
        ensureServiceStarted()

        val baseQueue = if (queue.any { it.id == song.id }) queue else listOf(song) + queue
        val shouldShuffle = when (shuffleOrder) {
            true -> true
            false -> false
            null -> _playbackState.value.isShuffleEnabled
        }

        val effectiveQueue = if (!isQueueNavigation) {
            unshuffledQueue = baseQueue
            if (shouldShuffle && baseQueue.size > 1) {
                listOf(song) + baseQueue.filter { it.id != song.id }.shuffled()
            } else {
                baseQueue
            }
        } else {
            baseQueue
        }

        updatePlaybackAndUiState {
            it.copy(
                currentSong = song,
                queue = effectiveQueue,
                isShuffleEnabled = shouldShuffle,
                currentPositionMs = startPositionMs,
                durationMs = song.durationSeconds.coerceAtLeast(180L) * 1000L,
                isPlaying = false,
                isLoading = true
            )
        }
        syncNotificationMediaSession(song)
        persistQueueStateAsync(startPositionMs)

        playJob = scope.launch {
            val offlineOnly = AuraApplication.userManager.userSettings.value.offlineMode
            var streamData = repository.resolveStreamWithInfo(
                songId = song.id,
                localFilePath = song.localFilePath,
                offlineOnly = offlineOnly
            )
            // Si falló la primera resolución online, invalidar caché y reintentar una vez
            if (streamData == null && !offlineOnly) {
                repository.invalidateStreamCache(song.id)
                streamData = repository.resolveStreamWithInfo(
                    songId = song.id,
                    localFilePath = song.localFilePath,
                    offlineOnly = false
                )
            }

            if (streamData != null) {
                updatePlaybackAndUiState { it.copy(streamQualityLabel = streamData.qualityLabel) }
                activePlayer?.let { player ->
                    val mediaSource = buildMediaSource(song, streamData.url, streamData.headers)
                    player.volume = 1.0f
                    player.setMediaSource(mediaSource, startPositionMs.coerceAtLeast(0L))
                    player.playbackParameters = PlaybackParameters(currentSpeed, currentPitch)
                    player.playWhenReady = true
                    player.prepare()
                    player.play()
                }
                syncNotificationMediaSession(song)
                prefetchAdjacentAndAutoRadio(song)
            } else {
                Timber.e("No se pudo obtener URL de stream para: ${song.title}")
                if (offlineOnly) {
                    AuraApplication.userManager.showToast("Modo Offline activo: esta canción no está descargada")
                }
                updatePlaybackAndUiState { it.copy(isLoading = false, isPlaying = false) }
            }
        }
    }

    private fun performDeckCrossfade(
        outgoingIdx: Int,
        nextSong: Song,
        queue: List<Song>,
        fadeDurationMs: Long,
    ) {
        val outgoingPlayer = deckPlayers[outgoingIdx] ?: return
        val incomingIdx = 1 - outgoingIdx
        val incomingPlayer = deckPlayers[incomingIdx] ?: return

        crossfadeJob?.cancel()
        playJob?.cancel()
        isCrossfading = true
        retiringDeckIdx = outgoingIdx
        requestAppAudioFocus()
        ensureServiceStarted()

        crossfadeJob = scope.launch {
            val offlineOnly = AuraApplication.userManager.userSettings.value.offlineMode
            val streamData = repository.resolveStreamWithInfo(
                songId = nextSong.id,
                localFilePath = nextSong.localFilePath,
                offlineOnly = offlineOnly
            )
            if (streamData == null) {
                isCrossfading = false
                retiringDeckIdx = -1
                playSong(
                    song = nextSong,
                    queue = queue,
                    isQueueNavigation = true,
                    shuffleOrder = null,
                    isUserCollection = isUserCollectionQueue
                )
                return@launch
            }

            val preparedOk = runCatching {
                incomingPlayer.playWhenReady = false
                incomingPlayer.pause()
                incomingPlayer.stop()
                incomingPlayer.clearMediaItems()
                val mediaSource = buildMediaSource(nextSong, streamData.url, streamData.headers)
                incomingPlayer.volume = 0.01f
                incomingPlayer.setMediaSource(mediaSource, 0L)
                incomingPlayer.playbackParameters = PlaybackParameters(currentSpeed, currentPitch)
                incomingPlayer.playWhenReady = true
                incomingPlayer.prepare()
                true
            }.getOrDefault(false)

            if (!preparedOk) {
                isCrossfading = false
                retiringDeckIdx = -1
                playSong(
                    song = nextSong,
                    queue = queue,
                    isQueueNavigation = true,
                    shuffleOrder = null,
                    isUserCollection = isUserCollectionQueue
                )
                return@launch
            }

            // Esperar a que el deck entrante esté realmente listo (STATE_READY) antes de atenuar el saliente
            var waitCycles = 0
            while (isActive &&
                waitCycles < 35 &&
                incomingPlayer.playbackState == Player.STATE_BUFFERING
            ) {
                delay(100L)
                waitCycles++
            }

            if (!isActive) return@launch
            if (incomingPlayer.playbackState == Player.STATE_IDLE) {
                // Si el stream en caché falló al preparar, invalidar y reproducir de forma directa
                repository.invalidateStreamCache(nextSong.id)
                isCrossfading = false
                retiringDeckIdx = -1
                playSong(
                    song = nextSong,
                    queue = queue,
                    isQueueNavigation = true,
                    shuffleOrder = null,
                    isUserCollection = isUserCollectionQueue
                )
                return@launch
            }

            incomingPlayer.playWhenReady = true
            incomingPlayer.play()

            activeDeckIdx = incomingIdx
            crossfadeTriggeredSongId = null
            AuraApplication.userManager.recordSongPlay(nextSong)
            updatePlaybackAndUiState {
                it.copy(
                    currentSong = nextSong,
                    queue = queue,
                    streamQualityLabel = streamData.qualityLabel,
                    currentPositionMs = 0L,
                    durationMs = nextSong.durationSeconds.coerceAtLeast(180L) * 1000L,
                    isPlaying = true,
                    isLoading = false
                )
            }
            syncNotificationMediaSession(nextSong)
            startProgressUpdates()

            val totalMs = fadeDurationMs.coerceIn(1000L, 8000L)
            val steps = 20
            val stepDelay = (totalMs / steps).coerceAtLeast(45L)

            for (step in 1..steps) {
                if (!isActive) break
                val fraction = step / steps.toFloat()
                val angle = fraction * (PI / 2.0)
                outgoingPlayer.volume = cos(angle).toFloat().coerceIn(0f, 1f)
                incomingPlayer.volume = sin(angle).toFloat().coerceIn(0.02f, 1f)
                delay(stepDelay)
            }

            incomingPlayer.volume = 1.0f
            if (!incomingPlayer.isPlaying && incomingPlayer.playWhenReady) {
                incomingPlayer.play()
            }
            stopStandbyDeck(outgoingIdx)
            retiringDeckIdx = -1
            isCrossfading = false
            audioLab.attachAudioSession(incomingPlayer.audioSessionId)
            prefetchAdjacentAndAutoRadio(nextSong)
        }
    }

    private fun prefetchAdjacentAndAutoRadio(song: Song) {
        scope.launch {
            delay(150L)
            val fastStart = runCatching { AuraApplication.userManager.userSettings.value.fastPlaybackStart }.getOrDefault(true)
            val curQueue = _playbackState.value.queue
            val curIdx = curQueue.indexOfFirst { it.id == song.id }
            if (fastStart && curIdx != -1) {
                if (curIdx + 1 < curQueue.size) {
                    launch { repository.prefetchStreamUrl(curQueue[curIdx + 1].id) }
                }
                if (curIdx + 2 < curQueue.size) {
                    launch { repository.prefetchStreamUrl(curQueue[curIdx + 2].id) }
                }
            }
            // No inyectar canciones aleatorias de radio mientras el usuario está escuchando su lista de Favoritos/Descargados/Álbum
            if (!isUserCollectionQueue &&
                AuraApplication.userManager.userSettings.value.continuousPlayback &&
                curIdx >= curQueue.size - 2
            ) {
                val related = repository.fetchRelatedTracks(song.id, song.title, song.artist)
                val existingIds = curQueue.map { it.id }.toSet()
                val newTracks = related.filter { !existingIds.contains(it.id) }
                if (newTracks.isNotEmpty()) {
                    updatePlaybackAndUiState { state ->
                        state.copy(queue = state.queue + newTracks)
                    }
                    if (fastStart) {
                        launch { repository.prefetchStreamUrl(newTracks.first().id) }
                    }
                }
            }
        }
    }

    private fun resumeOrRestartPlayback() {
        ensurePlayersInitialized()
        val player = activePlayer ?: return
        val curSong = _playbackState.value.currentSong
        requestAppAudioFocus()
        when {
            player.mediaItemCount == 0 && curSong != null -> {
                playSong(
                    song = curSong,
                    queue = _playbackState.value.queue,
                    startPositionMs = _playbackState.value.currentPositionMs,
                    isQueueNavigation = true,
                    shuffleOrder = null,
                    isUserCollection = isUserCollectionQueue
                )
            }
            player.playbackState == Player.STATE_IDLE -> {
                if (curSong != null) {
                    playSong(
                        song = curSong,
                        queue = _playbackState.value.queue,
                        startPositionMs = _playbackState.value.currentPositionMs,
                        isQueueNavigation = true,
                        shuffleOrder = null,
                        isUserCollection = isUserCollectionQueue
                    )
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
                updatePlaybackAndUiState { it.copy(isPlaying = true) }
            }
        }
    }

    fun togglePlayPause() {
        val player = activePlayer ?: return
        if (player.isPlaying) {
            deckPlayers.forEach { it?.pause() }
            updatePlaybackAndUiState { it.copy(isPlaying = false) }
        } else {
            resumeOrRestartPlayback()
        }
    }

    fun seekTo(positionMs: Long) {
        activePlayer?.seekTo(positionMs)
        _playbackState.update { it.copy(currentPositionMs = positionMs) }
    }

    fun playNext() {
        val currentQueue = _playbackState.value.queue
        val current = _playbackState.value.currentSong ?: return
        val currentIndex = currentQueue.indexOfFirst { it.id == current.id }
        val nextSong = when {
            currentIndex != -1 && currentIndex + 1 < currentQueue.size -> currentQueue[currentIndex + 1]
            currentQueue.isNotEmpty() -> currentQueue.first()
            else -> return
        }
        AuraApplication.userManager.recordSongPlay(nextSong)
        playSong(
            song = nextSong,
            queue = currentQueue,
            isQueueNavigation = true,
            shuffleOrder = null,
            isUserCollection = isUserCollectionQueue
        )
    }

    fun playPrevious() {
        val player = activePlayer
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
            playSong(
                song = prevSong,
                queue = currentQueue,
                isQueueNavigation = true,
                shuffleOrder = null,
                isUserCollection = isUserCollectionQueue
            )
        } else {
            seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val state = _playbackState.value
        val curSong = state.currentSong
        val newShuffle = !state.isShuffleEnabled
        if (curSong == null || state.queue.size <= 1) {
            updatePlaybackAndUiState { it.copy(isShuffleEnabled = newShuffle) }
            return
        }
        if (newShuffle) {
            if (unshuffledQueue.isEmpty()) {
                unshuffledQueue = state.queue
            }
            val rest = state.queue.filter { it.id != curSong.id }.shuffled()
            updatePlaybackAndUiState {
                it.copy(
                    isShuffleEnabled = true,
                    queue = listOf(curSong) + rest
                )
            }
        } else {
            val restored = if (unshuffledQueue.isNotEmpty()) unshuffledQueue else state.queue
            updatePlaybackAndUiState {
                it.copy(
                    isShuffleEnabled = false,
                    queue = restored
                )
            }
        }
    }

    fun toggleRepeat() {
        updatePlaybackAndUiState { it.copy(isRepeatEnabled = !it.isRepeatEnabled) }
    }

    /**
     * Inserta [song] inmediatamente después de la canción en curso ("Reproducir a continuación").
     */
    fun playNextInQueue(song: Song) {
        val state = _playbackState.value
        val cur = state.currentSong
        if (cur == null) {
            playSong(song = song, queue = listOf(song), shuffleOrder = false, isUserCollection = false)
            AuraApplication.userManager.recordSongPlay(song)
            return
        }
        if (cur.id == song.id) {
            AuraApplication.userManager.showToast("Ya se está reproduciendo: ${song.title}")
            return
        }
        val filtered = state.queue.filterNot { it.id == song.id }.toMutableList()
        val curIdx = filtered.indexOfFirst { it.id == cur.id }
        val insertIdx = if (curIdx != -1) curIdx + 1 else 0
        filtered.add(insertIdx, song)
        if (unshuffledQueue.isNotEmpty()) {
            unshuffledQueue = (unshuffledQueue.filterNot { it.id == song.id } + song)
        }
        updatePlaybackAndUiState { it.copy(queue = filtered) }
        persistQueueStateAsync()
        if (AuraApplication.userManager.userSettings.value.fastPlaybackStart) {
            ioScope.launch { repository.prefetchStreamUrl(song.id) }
        }
        AuraApplication.userManager.showToast("⏭ Se reproducirá a continuación: ${song.title}")
    }

    /**
     * Añade [song] al final de la cola de reproducción actual.
     */
    fun addToQueue(song: Song) {
        val state = _playbackState.value
        val cur = state.currentSong
        if (cur == null) {
            playSong(song = song, queue = listOf(song), shuffleOrder = false, isUserCollection = false)
            AuraApplication.userManager.recordSongPlay(song)
            return
        }
        if (state.queue.any { it.id == song.id }) {
            AuraApplication.userManager.showToast("Ya está en la cola: ${song.title}")
            return
        }
        val updated = state.queue + song
        if (unshuffledQueue.isNotEmpty()) {
            unshuffledQueue = unshuffledQueue + song
        }
        updatePlaybackAndUiState { it.copy(queue = updated) }
        persistQueueStateAsync()
        AuraApplication.userManager.showToast("➕ Añadida a la cola: ${song.title}")
    }

    /**
     * Elimina una canción de la cola activa.
     */
    fun removeFromQueue(songId: String) {
        val state = _playbackState.value
        if (state.currentSong?.id == songId) return
        val updated = state.queue.filterNot { it.id == songId }
        unshuffledQueue = unshuffledQueue.filterNot { it.id == songId }
        updatePlaybackAndUiState { it.copy(queue = updated) }
        persistQueueStateAsync()
    }

    /**
     * Mueve una canción hacia arriba o hacia abajo en la cola de reproducción.
     */
    fun moveQueueItem(songId: String, direction: Int) {
        val state = _playbackState.value
        val list = state.queue.toMutableList()
        val idx = list.indexOfFirst { it.id == songId }
        if (idx == -1) return
        val targetIdx = (idx + direction).coerceIn(0, list.lastIndex)
        if (targetIdx == idx || list[targetIdx].id == state.currentSong?.id) return
        val item = list.removeAt(idx)
        list.add(targetIdx, item)
        updatePlaybackAndUiState { it.copy(queue = list) }
        persistQueueStateAsync()
    }

    /**
     * Limpia las canciones siguientes de la cola, conservando únicamente la canción en curso.
     */
    fun clearUpcomingQueue() {
        val cur = _playbackState.value.currentSong ?: return
        val single = listOf(cur)
        unshuffledQueue = single
        updatePlaybackAndUiState { it.copy(queue = single) }
        persistQueueStateAsync()
        AuraApplication.userManager.showToast("Cola de reproducción limpiada")
    }

    private fun persistQueueStateAsync(overridePositionMs: Long? = null) {
        val state = _playbackState.value
        val cur = state.currentSong ?: return
        val pos = overridePositionMs ?: state.currentPositionMs
        val queueSnapshot = state.queue.take(80)
        val shuffle = state.isShuffleEnabled
        val isCollection = isUserCollectionQueue
        ioScope.launch {
            runCatching {
                val arr = JSONArray()
                queueSnapshot.forEach { s ->
                    arr.put(
                        JSONObject().apply {
                            put("id", s.id)
                            put("title", s.title)
                            put("artist", s.artist)
                            put("artistId", s.artistId.orEmpty())
                            put("album", s.album.orEmpty())
                            put("albumId", s.albumId.orEmpty())
                            put("thumbnailUrl", s.thumbnailUrl)
                            put("durationSeconds", s.durationSeconds)
                            put("localFilePath", s.localFilePath.orEmpty())
                        }
                    )
                }
                persistPrefs.edit()
                    .putString("current_song_id", cur.id)
                    .putLong("current_position_ms", pos.coerceAtLeast(0L))
                    .putBoolean("is_shuffle", shuffle)
                    .putBoolean("is_user_collection", isCollection)
                    .putString("queue_json", arr.toString())
                    .apply()
            }
        }
    }

    private fun restorePersistedQueueState() {
        runCatching {
            val savedId = persistPrefs.getString("current_song_id", null) ?: return
            val queueJson = persistPrefs.getString("queue_json", null) ?: return
            val savedPos = persistPrefs.getLong("current_position_ms", 0L).coerceAtLeast(0L)
            val savedShuffle = persistPrefs.getBoolean("is_shuffle", false)
            val savedIsCollection = persistPrefs.getBoolean("is_user_collection", false)

            val arr = JSONArray(queueJson)
            val restoredQueue = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isNotBlank()) {
                    restoredQueue.add(
                        Song(
                            id = id,
                            title = obj.optString("title", "Canción"),
                            artist = obj.optString("artist", "Artista"),
                            artistId = obj.optString("artistId").takeIf { it.isNotBlank() },
                            album = obj.optString("album").takeIf { it.isNotBlank() },
                            albumId = obj.optString("albumId").takeIf { it.isNotBlank() },
                            thumbnailUrl = obj.optString("thumbnailUrl", ""),
                            durationSeconds = obj.optLong("durationSeconds", 210L),
                            localFilePath = obj.optString("localFilePath").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            val current = restoredQueue.find { it.id == savedId } ?: restoredQueue.firstOrNull() ?: return
            if (_playbackState.value.currentSong != null) return
            unshuffledQueue = restoredQueue
            isUserCollectionQueue = savedIsCollection
            updatePlaybackAndUiState { state ->
                if (state.currentSong != null) {
                    state
                } else {
                    state.copy(
                        currentSong = current,
                        queue = restoredQueue,
                        currentPositionMs = savedPos,
                        durationMs = current.durationSeconds.coerceAtLeast(180L) * 1000L,
                        isShuffleEnabled = savedShuffle,
                        isPlaying = false,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        _playbackState.update {
            it.copy(lyricsOffsetMs = (it.lyricsOffsetMs + deltaMs).coerceIn(-10_000L, 10_000L))
        }
    }

    fun resetLyricsOffset() {
        _playbackState.update { it.copy(lyricsOffsetMs = 0L) }
    }

    fun setSpeedAndPitch(speed: Float, pitch: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        currentSpeed = clampedSpeed
        currentPitch = clampedPitch
        activePlayer?.playbackParameters = PlaybackParameters(clampedSpeed, clampedPitch)
        audioLab.updateSpeedAndPitchState(clampedSpeed, clampedPitch)
    }

    fun startSleepTimer(minutes: Int, stopAfterCurrentSong: Boolean = false) {
        sleepTimerJob?.cancel()
        if (stopAfterCurrentSong) {
            audioLab.updateSleepTimerState(0L, true)
            AuraApplication.userManager.showToast("La música se detendrá al terminar esta canción")
            return
        }
        if (minutes <= 0) {
            audioLab.updateSleepTimerState(0L, false)
            activePlayer?.volume = 1f
            return
        }
        val totalMs = minutes * 60_000L
        audioLab.updateSleepTimerState(totalMs, false)
        AuraApplication.userManager.showToast("Temporizador: $minutes min")
        sleepTimerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining = (remaining - 1000L).coerceAtLeast(0L)
                audioLab.updateSleepTimerState(remaining, false)
                if (remaining in 1L..5000L) {
                    activePlayer?.volume = (remaining / 5000f).coerceIn(0.1f, 1f)
                }
            }
            if (isActive) {
                deckPlayers.forEach { it?.pause() }
                activePlayer?.volume = 1f
                audioLab.updateSleepTimerState(0L, false)
                updatePlaybackAndUiState { it.copy(isPlaying = false) }
            }
        }
    }

    /**
     * Detiene por completo la reproducción y libera el foco de audio cuando el usuario cierra la app desde Recientes.
     */
    fun stopPlaybackForTaskRemoval() {
        persistQueueStateAsync(activePlayer?.currentPosition)
        stopProgressUpdates()
        playJob?.cancel()
        sleepTimerJob?.cancel()
        crossfadeJob?.cancel()
        isCrossfading = false
        retiringDeckIdx = -1
        abandonAppAudioFocus()
        deckPlayers.forEachIndexed { idx, _ ->
            stopStandbyDeck(idx)
        }
        activeDeckIdx = 0
        updatePlaybackAndUiState {
            it.copy(
                isPlaying = false,
                isLoading = false,
                currentPositionMs = 0L
            )
        }
    }

    fun release() {
        stopPlaybackForTaskRemoval()
        audioLab.releaseEffects()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        deckPlayers.forEachIndexed { i, p ->
            p?.release()
            deckPlayers[i] = null
        }
    }
}
