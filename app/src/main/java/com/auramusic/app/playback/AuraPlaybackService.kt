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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
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

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(loadControl)
            .build()
        rawPlayer = exo

        // Listen for playback state changes (e.g. auto advance on song end)
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    handleSkipNext()
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
        val queue = AuraPlaybackBridge.queue.value
        if (queue.isEmpty()) return
        var nextIdx = AuraPlaybackBridge.currentIndex + offset
        if (nextIdx >= queue.size) nextIdx = 0
        if (nextIdx < 0) nextIdx = queue.size - 1

        val nextSong = queue[nextIdx]
        AuraPlaybackBridge.currentIndex = nextIdx
        AuraPlaybackBridge.currentSong.value = nextSong

        serviceScope.launch(Dispatchers.IO) {
            val fallbackQuery = "${nextSong.title} ${nextSong.artist}"
            val streamUri = AuraStreamResolver.resolveAudioStream(nextSong.id, fallbackQuery)
            if (!streamUri.isNullOrBlank() && !streamUri.startsWith("https://www.youtube.com")) {
                withContext(Dispatchers.Main) {
                    val exo = rawPlayer ?: return@withContext
                    val mediaItem = MediaItem.Builder()
                        .setUri(streamUri)
                        .setMediaId(nextSong.id)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(nextSong.title)
                                .setArtist(nextSong.artist)
                                .setAlbumTitle(nextSong.album)
                                .setArtworkUri(if (nextSong.coverUrl.isNotBlank()) Uri.parse(nextSong.coverUrl) else null)
                                .build()
                        )
                        .build()

                    exo.setMediaItem(mediaItem)
                    exo.prepare()
                    exo.play()
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
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
