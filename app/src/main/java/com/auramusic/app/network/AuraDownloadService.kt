package com.auramusic.app.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.auramusic.app.MainActivity

/**
 * Foreground Service that prevents Android from killing or throttling background music downloads
 * when the app is minimized or the screen is turned off.
 */
class AuraDownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "aura_downloads_service"
        const val CHANNEL_NAME = "Descargas en Segundo Plano"
        const val NOTIFICATION_ID = 9081

        const val ACTION_START = "com.auramusic.app.action.START_DOWNLOAD_SERVICE"
        const val ACTION_STOP = "com.auramusic.app.action.STOP_DOWNLOAD_SERVICE"

        @Volatile
        private var isRunning = false

        fun start(context: Context) {
            if (isRunning) return
            try {
                val intent = Intent(context, AuraDownloadService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, AuraDownloadService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        isRunning = true
        startInForeground()
        return START_STICKY
    }

    private fun startInForeground() {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Aura Music")
            .setContentText("Descargando música en segundo plano...")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AuraMusic:DownloadServiceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(45 * 60 * 1000L) // 45 min safety timeout
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "AuraMusic:DownloadServiceWifiLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopForegroundService() {
        isRunning = false
        releaseLocks()
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopForegroundService()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente de descarga en segundo plano"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

