package com.auramusic.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auramusic.app.AuraApplication
import com.auramusic.app.MainActivity
import com.auramusic.app.R

@OptIn(UnstableApi::class)
class AuraMusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "aura_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .setChannelName(R.string.app_name)
                .build()
                .apply {
                    setSmallIcon(R.mipmap.ic_launcher)
                }
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, AuraApplication.playerManager.forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción AURA Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción en segundo plano y pantalla de bloqueo"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Cuando el usuario cierra la app deslizándola desde Recientes, detener reproducción y cerrar el servicio
        runCatching {
            AuraApplication.playerManager.stopPlaybackForTaskRemoval()
            mediaSession?.player?.pause()
            mediaSession?.player?.stop()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        runCatching {
            AuraApplication.playerManager.stopPlaybackForTaskRemoval()
        }
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
