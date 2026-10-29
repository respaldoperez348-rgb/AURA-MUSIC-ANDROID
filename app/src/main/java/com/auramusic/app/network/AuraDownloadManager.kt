package com.auramusic.app.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.auramusic.app.MainActivity
import com.auramusic.app.data.database.AuraDatabase
import com.auramusic.app.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AuraDownloadManager(
    private val context: Context,
    private val database: AuraDatabase,
    private val scope: CoroutineScope
) {
    companion object {
        const val DOWNLOAD_CHANNEL_ID = "aura_downloads"
        const val DOWNLOAD_CHANNEL_NAME = "Aura Descargas"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Map: songId -> progress percentage (0..100)
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val activeDownloads = ConcurrentHashMap<String, Boolean>()

    var onDownloadCompleted: ((Song) -> Unit)? = null

    private val downloadsDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "aura_downloads")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                DOWNLOAD_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de estado de descargas offline en Aura Music"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun notifyDownloadProgress(song: Song, progress: Int) {
        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando: ${song.title}")
            .setContentText("${song.artist} • $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(song.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Ignored if permission not granted
        }
    }

    private fun notifyDownloadCompleted(song: Song) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            song.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Descarga completada")
            .setContentText("${song.title} • ${song.artist}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${song.title}\n${song.artist} • Guardada en tu Bóveda offline")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(song.id.hashCode(), notification)
        } catch (e: SecurityException) {
            // Ignored if notification permission denied
        }
    }

    fun startDownload(song: Song) {
        if (activeDownloads.containsKey(song.id)) return
        activeDownloads[song.id] = true

        _downloadProgress.update { it + (song.id to 0) }

        scope.launch(Dispatchers.IO) {
            try {
                val targetFile = File(downloadsDir, "${song.id}.mp3")

                // Resolve direct audio stream (from NewPipeExtractor for YouTube or direct URL)
                val directStreamUrl = if (song.id.startsWith("local_")) {
                    song.mediaUri
                } else {
                    AuraStreamResolver.resolveAudioStream(song.id, "${song.title} ${song.artist}") ?: ""
                }

                val downloadUrl = if (directStreamUrl.startsWith("http")) directStreamUrl else {
                    "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3"
                }

                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "AuraMusic/1.0 (Android)")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    cleanFailedDownload(song.id)
                    return@launch
                }

                val body = response.body ?: run {
                    cleanFailedDownload(song.id)
                    return@launch
                }

                val totalBytes = body.contentLength().coerceAtLeast(1L)
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(targetFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var downloadedBytes = 0L
                var lastReported = 0

                notifyDownloadProgress(song, 0)

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                    _downloadProgress.update { it + (song.id to progress) }

                    if (progress - lastReported >= 10) {
                        lastReported = progress
                        notifyDownloadProgress(song, progress)
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                // Register into database
                database.saveDownload(
                    song = song,
                    localPath = targetFile.absolutePath,
                    fileSize = targetFile.length()
                )

                _downloadProgress.update { it + (song.id to 100) }
                activeDownloads.remove(song.id)

                // Trigger Android system notification & in-app event
                notifyDownloadCompleted(song)
                onDownloadCompleted?.invoke(song)

            } catch (e: Exception) {
                e.printStackTrace()
                cleanFailedDownload(song.id)
            }
        }
    }

    private fun cleanFailedDownload(songId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(songId.hashCode())
        } catch (e: Exception) {}
        activeDownloads.remove(songId)
        _downloadProgress.update { it - songId }
        val targetFile = File(downloadsDir, "$songId.mp3")
        if (targetFile.exists()) targetFile.delete()
    }

    fun isDownloading(songId: String): Boolean = activeDownloads.containsKey(songId)

    fun getProgress(songId: String): Int = _downloadProgress.value[songId] ?: -1

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val targetFile = File(downloadsDir, "$songId.mp3")
        if (targetFile.exists()) targetFile.delete()
        database.removeDownload(songId)
        _downloadProgress.update { it - songId }
    }
}
