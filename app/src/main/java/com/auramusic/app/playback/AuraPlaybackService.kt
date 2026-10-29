package com.auramusic.app.playback

import android.app.PendingIntent
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
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auramusic.app.MainActivity
import com.auramusic.app.network.AuraStreamResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuraPlaybackService : MediaSessionService() {

    companion object {
        @Volatile
        private var simpleCache: SimpleCache? = null

        @OptIn(UnstableApi::class)
        @Synchronized
        fun getCache(context: android.content.Context): SimpleCache {
            return simpleCache ?: run {
                val cacheFolder = java.io.File(context.cacheDir, "aura_audio_stream_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(512L * 1024L * 1024L) // 512 MB smart LRU audio cache
                val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
                SimpleCache(cacheFolder, evictor, dbProvider).also { simpleCache = it }
            }
        }
    }

    private var mediaSession: MediaSession? = null
    private var rawPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Configure audio attributes for music playback & focus handling
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                45_000,
                500,
                1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
            .setAllowCrossProtocolRedirects(true)

        val upstreamFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(this))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
        exo.skipSilenceEnabled = AuraPlaybackBridge.skipSilenceEnabled
        rawPlayer = exo

        AuraPlaybackBridge.onSkipSilenceChanged = { enabled ->
            rawPlayer?.skipSilenceEnabled = enabled
        }

        // Listen for playback state changes (auto advance when running standalone without active UI controller)
        exo.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (exo.currentMediaItemIndex > 0) {
                    exo.removeMediaItems(0, exo.currentMediaItemIndex)
                }
                if (AuraPlaybackBridge.onNextRequested == null && mediaItem != null) {
                    val q = AuraPlaybackBridge.queue.value
                    val idx = q.indexOfFirst { it.id == mediaItem.mediaId }
                    if (idx >= 0) {
                        AuraPlaybackBridge.currentIndex = idx
                        AuraPlaybackBridge.currentSong.value = q[idx]
                    }
                    prefetchNextInService()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    val wasEndOfTrack = SleepTimerManager.timerState.value.isEndOfTrack
                    SleepTimerManager.notifyTrackEnded()
                    if (wasEndOfTrack) return

                    if (AuraPlaybackBridge.onNextRequested == null) {
                        if (AuraPlaybackBridge.isRepeat) {
                            exo.seekTo(0L)
                            exo.play()
                        } else {
                            playQueueOffset(1)
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (AuraPlaybackBridge.onNextRequested == null) {
                    playQueueOffset(1)
                }
            }
        })

        // Wrap ExoPlayer in ForwardingPlayer to guarantee Next/Previous notification controls
        val forwardingPlayer = object : ForwardingPlayer(exo) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun seekToNext() {
                handleSkipNext()
            }

            override fun seekToNextMediaItem() {
                handleSkipNext()
            }

            override fun seekToPrevious() {
                handleSkipPrevious()
            }

            override fun seekToPreviousMediaItem() {
                handleSkipPrevious()
            }
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(playerCommands)
                    .setAvailableSessionCommands(sessionCommands.build())
                    .build()
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(callback)
            .build()

        // Attach hardware DSP audio effects (5-band EQ, BassBoost, 3D Virtualizer)
        AuraAudioEffects.attach(exo.audioSessionId, this)
    }

    private fun handleSkipNext() {
        val callback = AuraPlaybackBridge.onNextRequested
        if (callback != null) {
            callback.invoke()
        } else {
            // Standalone background queue advance
            playQueueOffset(1)
        }
    }

    private fun handleSkipPrevious() {
        val callback = AuraPlaybackBridge.onPreviousRequested
        if (callback != null) {
            callback.invoke()
        } else {
            playQueueOffset(-1)
        }
    }

    private fun playQueueOffset(offset: Int) {
        val queue = AuraPlaybackBridge.queue.value.filter { !it.isArtist && !it.isAlbum }
        if (queue.isEmpty()) return
        val curIdx = AuraPlaybackBridge.currentIndex
        val nextIdx = if (AuraPlaybackBridge.isShuffle && queue.size > 1) {
            val choices = queue.indices.filter { it != curIdx }
            choices.randomOrNull() ?: ((curIdx + offset).mod(queue.size))
        } else {
            (curIdx + offset).mod(queue.size)
        }

        val nextSong = queue[nextIdx]
        AuraPlaybackBridge.currentIndex = nextIdx
        AuraPlaybackBridge.currentSong.value = nextSong

        serviceScope.launch(Dispatchers.IO) {
            val db = com.auramusic.app.data.database.AuraDatabase.getInstance(this@AuraPlaybackService)
            val downloads = db.getDownloadedTracks()
            val localTrack = downloads.find {
                it.song.id == nextSong.id ||
                (it.song.title.trim().equals(nextSong.title.trim(), ignoreCase = true) &&
                 it.song.artist.trim().equals(nextSong.artist.trim(), ignoreCase = true))
            }

            val streamUri = if (nextSong.mediaUri.isNotBlank() && (java.io.File(nextSong.mediaUri.removePrefix("file://")).let { it.exists() && it.length() > 0 } || nextSong.mediaUri.startsWith("content://"))) {
                nextSong.mediaUri
            } else if (localTrack != null && java.io.File(localTrack.localFilePath).let { it.exists() && it.length() > 0 }) {
                localTrack.localFilePath
            } else if (nextSong.id.startsWith("local_")) {
                nextSong.mediaUri
            } else {
                val fallbackQuery = "${nextSong.title} ${nextSong.artist}"
                AuraStreamResolver.resolveAudioStream(nextSong.id, fallbackQuery)
            }
            if (!streamUri.isNullOrBlank() && !streamUri.startsWith("https://www.youtube.com")) {
                withContext(Dispatchers.Main) {
                    val exo = rawPlayer ?: return@withContext
                    val mediaUri = if (streamUri.startsWith("/") || streamUri.startsWith("file:")) {
                        Uri.fromFile(java.io.File(streamUri.removePrefix("file://")))
                    } else {
                        Uri.parse(streamUri)
                    }
                    val artUri = if (nextSong.coverUrl.isNotBlank()) {
                        if (nextSong.coverUrl.startsWith("/") || nextSong.coverUrl.startsWith("file:")) {
                            Uri.fromFile(java.io.File(nextSong.coverUrl.removePrefix("file://")))
                        } else {
                            Uri.parse(nextSong.coverUrl)
                        }
                    } else null

                    val mediaItem = MediaItem.Builder()
                        .setUri(mediaUri)
                        .setMediaId(nextSong.id)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(nextSong.title)
                                .setArtist(nextSong.artist)
                                .setAlbumTitle(nextSong.album)
                                .setArtworkUri(artUri)
                                .build()
                        )
                        .build()

                    exo.setMediaItem(mediaItem)
                    exo.prepare()
                    exo.play()
                    prefetchNextInService()
                }
            } else {
                // Skip unplayable item and try the next one in queue
                if (queue.size > 1) {
                    withContext(Dispatchers.Main) {
                        playQueueOffset(offset)
                    }
                }
            }
        }
    }

    private fun prefetchNextInService() {
        val queue = AuraPlaybackBridge.queue.value.filter { !it.isArtist && !it.isAlbum }
        val curSong = AuraPlaybackBridge.currentSong.value ?: return
        val currentIdx = queue.indexOfFirst { it.id == curSong.id }
        if (queue.size <= 1 || currentIdx < 0) return

        val nextIdx = if (AuraPlaybackBridge.isShuffle && queue.size > 1) {
            val choices = queue.indices.filter { it != currentIdx }
            choices.randomOrNull() ?: ((currentIdx + 1) % queue.size)
        } else {
            (currentIdx + 1) % queue.size
        }
        val nextSong = queue[nextIdx]

        serviceScope.launch(Dispatchers.IO) {
            val db = com.auramusic.app.data.database.AuraDatabase.getInstance(this@AuraPlaybackService)
            val downloads = db.getDownloadedTracks()
            val localTrack = downloads.find {
                it.song.id == nextSong.id ||
                (it.song.title.trim().equals(nextSong.title.trim(), ignoreCase = true) &&
                 it.song.artist.trim().equals(nextSong.artist.trim(), ignoreCase = true))
            }

            val streamUri = if (nextSong.mediaUri.isNotBlank() && (java.io.File(nextSong.mediaUri.removePrefix("file://")).let { it.exists() && it.length() > 0 } || nextSong.mediaUri.startsWith("content://"))) {
                nextSong.mediaUri
            } else if (localTrack != null && java.io.File(localTrack.localFilePath).let { it.exists() && it.length() > 0 }) {
                localTrack.localFilePath
            } else if (nextSong.id.startsWith("local_")) {
                nextSong.mediaUri
            } else {
                val fallbackQuery = "${nextSong.title} ${nextSong.artist}"
                AuraStreamResolver.resolveAudioStream(nextSong.id, fallbackQuery)
            }

            if (!streamUri.isNullOrBlank() && !streamUri.startsWith("https://www.youtube.com")) {
                withContext(Dispatchers.Main) {
                    val exo = rawPlayer ?: return@withContext
                    if (exo.currentMediaItem?.mediaId == curSong.id) {
                        val curItemIdx = exo.currentMediaItemIndex
                        if (exo.mediaItemCount > curItemIdx + 1) {
                            exo.removeMediaItems(curItemIdx + 1, exo.mediaItemCount)
                        }
                        val mediaUri = if (streamUri.startsWith("/") || streamUri.startsWith("file:")) {
                            Uri.fromFile(java.io.File(streamUri.removePrefix("file://")))
                        } else {
                            Uri.parse(streamUri)
                        }
                        val artUri = if (nextSong.coverUrl.isNotBlank()) {
                            if (nextSong.coverUrl.startsWith("/") || nextSong.coverUrl.startsWith("file:")) {
                                Uri.fromFile(java.io.File(nextSong.coverUrl.removePrefix("file://")))
                            } else {
                                Uri.parse(nextSong.coverUrl)
                            }
                        } else null

                        val nextMediaItem = MediaItem.Builder()
                            .setUri(mediaUri)
                            .setMediaId(nextSong.id)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(nextSong.title)
                                    .setArtist(nextSong.artist)
                                    .setAlbumTitle(nextSong.album)
                                    .setArtworkUri(artUri)
                                    .build()
                            )
                            .build()

                        exo.addMediaItem(nextMediaItem)
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        AuraPlaybackBridge.onSkipSilenceChanged = null
        serviceScope.cancel()
        AuraAudioEffects.detach()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        rawPlayer = null
        super.onDestroy()
    }
}
