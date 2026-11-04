package com.auramusic.app.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.auramusic.app.MainActivity
import com.auramusic.app.data.database.AuraDatabase
import com.auramusic.app.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    context: Context,
    private val database: AuraDatabase,
    externalScope: CoroutineScope? = null
) {
    companion object {
        const val DOWNLOAD_CHANNEL_ID = "aura_downloads"
        const val DOWNLOAD_CHANNEL_NAME = "Aura Descargas"
    }

    private val context: Context = context.applicationContext
    // Independent background scope that survives Activity destruction and app minimization
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val powerManager by lazy {
        this@AuraDownloadManager.context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    }
    private val wifiManager by lazy {
        this@AuraDownloadManager.context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
    }
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    @Synchronized
    private fun acquireLocks() {
        try {
            AuraDownloadService.start(context)

            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,
                    "AuraMusic:DownloadWakeLock"
                )?.apply { setReferenceCounted(false) }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(30 * 60 * 1000L) // 30 min safety timeout
            }

            if (wifiLock == null) {
                wifiLock = wifiManager?.createWifiLock(
                    android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "AuraMusic:DownloadWifiLock"
                )?.apply { setReferenceCounted(false) }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun releaseLocksIfDone() {
        try {
            if (activeDownloads.isEmpty()) {
                AuraDownloadService.stop(context)
                if (wakeLock?.isHeld == true) wakeLock?.release()
                if (wifiLock?.isHeld == true) wifiLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Map: songId -> progress percentage (0..100)
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val activeDownloads = ConcurrentHashMap<String, Boolean>()

    @Volatile
    var downloadOnlyOnWifi: Boolean = false

    var onDownloadCompleted: ((Song) -> Unit)? = null
    var onDownloadFailed: ((Song, String) -> Unit)? = null

    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    // App-private fallback folder
    private val appPrivateDownloadsDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "aura_downloads")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    // Target storage directory (prefers Public Music/AuraMusic for external visibility)
    private fun getStorageDirectory(): File {
        return try {
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "AuraMusic")
            if (!publicDir.exists()) publicDir.mkdirs()
            if (publicDir.exists() && publicDir.canWrite()) {
                publicDir
            } else {
                appPrivateDownloadsDir
            }
        } catch (e: Exception) {
            appPrivateDownloadsDir
        }
    }

    private fun getTargetFileForSong(song: Song, extension: String = "mp3"): File {
        val targetDir = getStorageDirectory()
        val cleanArtist = song.artist.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val cleanTitle = song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val cleanId = song.id.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(15).trim()
        val fileName = if (cleanArtist.isNotBlank() && cleanTitle.isNotBlank()) {
            "$cleanArtist - $cleanTitle [$cleanId].$extension"
        } else {
            "${cleanId.ifBlank { "track_${System.currentTimeMillis()}" }}.$extension"
        }
        return File(targetDir, fileName)
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
                    .bigText("${song.title}\n${song.artist} • Guardada en tu Bóveda y almacenamiento")
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

    private fun notifyDownloadFailed(song: Song, reason: String) {
        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Error al descargar")
            .setContentText("${song.title} • $reason")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(song.id.hashCode(), notification)
        } catch (e: SecurityException) {}
    }

    fun startDownload(song: Song): Boolean {
        if (activeDownloads.containsKey(song.id)) return false
        if (downloadOnlyOnWifi && !isWifiConnected()) {
            return false
        }
        scope.launch {
            downloadSongSuspended(song)
        }
        return true
    }

    suspend fun downloadSongSuspended(song: Song): Boolean = withContext(Dispatchers.IO) {
        if (activeDownloads.containsKey(song.id)) return@withContext false
        if (downloadOnlyOnWifi && !isWifiConnected()) {
            return@withContext false
        }
        activeDownloads[song.id] = true
        _downloadProgress.update { it + (song.id to 0) }
        acquireLocks()

        var targetFile: File? = null
        try {
            // 1. Resolve direct audio stream using browser UA
            val directStreamUrl = if (song.id.startsWith("local_")) {
                song.mediaUri
            } else if (song.mediaUri.startsWith("http") && (song.mediaUri.contains("googlevideo.com") || song.mediaUri.endsWith(".mp3") || song.mediaUri.endsWith(".m4a") || song.mediaUri.endsWith(".opus"))) {
                song.mediaUri
            } else {
                val candidate = if (song.id.isNotBlank() && !song.id.startsWith("http")) song.id else song.mediaUri
                AuraStreamResolver.resolveAudioStream(candidate, "${song.title} ${song.artist}") ?: ""
            }

            if (!directStreamUrl.startsWith("http")) {
                cleanFailedDownload(song.id)
                notifyDownloadFailed(song, "No se pudo obtener el enlace de audio")
                try {
                    withContext(Dispatchers.Main.immediate) {
                        onDownloadFailed?.invoke(song, "No se pudo obtener el audio")
                    }
                } catch (ignored: Exception) {}
                return@withContext false
            }

            // 2. Prepare target file on disk
            val ext = if (directStreamUrl.contains(".mp4") || directStreamUrl.contains("audio/mp4") || directStreamUrl.contains("itag=140")) "m4a" else "mp3"
            targetFile = getTargetFileForSong(song, ext)

            // 3. Request audio stream with Desktop Browser UA to avoid 403 Forbidden from Google CDN
            val request = Request.Builder()
                .url(directStreamUrl)
                .header("User-Agent", AuraDownloaderImpl.USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                cleanFailedDownload(song.id)
                notifyDownloadFailed(song, "Error del servidor: HTTP ${response.code}")
                try {
                    withContext(Dispatchers.Main.immediate) {
                        onDownloadFailed?.invoke(song, "Error del servidor HTTP ${response.code}")
                    }
                } catch (ignored: Exception) {}
                return@withContext false
            }

            val body = response.body ?: run {
                cleanFailedDownload(song.id)
                notifyDownloadFailed(song, "Respuesta vacía del servidor")
                try {
                    withContext(Dispatchers.Main.immediate) {
                        onDownloadFailed?.invoke(song, "Respuesta vacía")
                    }
                } catch (ignored: Exception) {}
                return@withContext false
            }

            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var downloadedBytes = 0L
            var lastReported = 0

            notifyDownloadProgress(song, 0)

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                if (totalBytes > 0) {
                    val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                    _downloadProgress.update { it + (song.id to progress) }

                    if (progress - lastReported >= 5) {
                        lastReported = progress
                        notifyDownloadProgress(song, progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // 4. Critical check: Validate file was actually downloaded and not corrupt/empty
            if (!targetFile.exists() || targetFile.length() < 50_000L) {
                targetFile.delete()
                cleanFailedDownload(song.id)
                notifyDownloadFailed(song, "Archivo incompleto o dañado")
                try {
                    withContext(Dispatchers.Main.immediate) {
                        onDownloadFailed?.invoke(song, "Descarga incompleta o archivo dañado")
                    }
                } catch (ignored: Exception) {}
                return@withContext false
            }

            // 5. Download album cover art locally
            var localCoverPath = song.coverUrl
            if (song.coverUrl.isNotBlank() && (song.coverUrl.startsWith("http://") || song.coverUrl.startsWith("https://"))) {
                try {
                    val coverFile = File(targetFile.parentFile, targetFile.nameWithoutExtension + ".jpg")
                    val coverRequest = Request.Builder()
                        .url(song.coverUrl)
                        .header("User-Agent", AuraDownloaderImpl.USER_AGENT)
                        .build()
                    val coverResponse = client.newCall(coverRequest).execute()
                    if (coverResponse.isSuccessful) {
                        coverResponse.body?.byteStream()?.use { input ->
                            FileOutputStream(coverFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (coverFile.exists() && coverFile.length() > 0) {
                            localCoverPath = coverFile.absolutePath
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 6. Trigger MediaScanner so Android Gallery, Files and LocalAudioScanner see the new file immediately
            try {
                MediaScannerConnection.scanFile(
                    this@AuraDownloadManager.context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("audio/*")
                ) { _, _ -> }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 7. Register download in SQLite Database
            val downloadedSong = song.copy(
                coverUrl = localCoverPath,
                mediaUri = targetFile.absolutePath
            )
            database.saveDownload(
                song = downloadedSong,
                localPath = targetFile.absolutePath,
                fileSize = targetFile.length()
            )

            // 8. Update favorite entry if applicable
            database.updateFavoriteMediaAndCover(song.id, localCoverPath, targetFile.absolutePath)

            // 9. Mark 100% and notify
            _downloadProgress.update { it + (song.id to 100) }
            activeDownloads.remove(song.id)

            notifyDownloadCompleted(song)

            try {
                withContext(Dispatchers.Main.immediate) {
                    onDownloadCompleted?.invoke(downloadedSong)
                }
            } catch (ignored: Exception) {}

            return@withContext true

        } catch (e: Exception) {
            e.printStackTrace()
            targetFile?.let { if (it.exists()) it.delete() }
            cleanFailedDownload(song.id)
            try {
                notifyDownloadFailed(song, "Conexión interrumpida")
            } catch (ignored: Exception) {}
            try {
                withContext(Dispatchers.Main.immediate) {
                    onDownloadFailed?.invoke(song, "Conexión interrumpida: ${e.localizedMessage ?: ""}")
                }
            } catch (ignored: Exception) {}
            return@withContext false
        } finally {
            releaseLocksIfDone()
        }
    }

    private fun cleanFailedDownload(songId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(songId.hashCode())
        } catch (e: Exception) {}
        activeDownloads.remove(songId)
        _downloadProgress.update { it - songId }
    }

    fun isDownloading(songId: String): Boolean = activeDownloads.containsKey(songId)

    fun getProgress(songId: String): Int = _downloadProgress.value[songId] ?: -1

    suspend fun deleteDownload(song: Song) = withContext(Dispatchers.IO) {
        val downloadedTracks = database.getDownloadedTracks()
        val match = downloadedTracks.find {
            it.song.id == song.id ||
            (it.song.title.trim().equals(song.title.trim(), ignoreCase = true) &&
             it.song.artist.trim().equals(song.artist.trim(), ignoreCase = true))
        }
        if (match != null) {
            val f = File(match.localFilePath)
            if (f.exists()) f.delete()
            if (match.song.coverUrl.startsWith("/") || match.song.coverUrl.startsWith("file:")) {
                val c = File(match.song.coverUrl.removePrefix("file://"))
                if (c.exists()) c.delete()
            }
            database.removeDownload(match.song.id)
            _downloadProgress.update { it - match.song.id }
        }
        _downloadProgress.update { it - song.id }
    }

    suspend fun deleteDownload(songId: String) = withContext(Dispatchers.IO) {
        val downloadedTracks = database.getDownloadedTracks()
        val match = downloadedTracks.find { it.song.id == songId }
        if (match != null) {
            val f = File(match.localFilePath)
            if (f.exists()) f.delete()
            if (match.song.coverUrl.startsWith("/") || match.song.coverUrl.startsWith("file:")) {
                val c = File(match.song.coverUrl.removePrefix("file://"))
                if (c.exists()) c.delete()
            }
        }
        database.removeDownload(songId)
        _downloadProgress.update { it - songId }
    }
}
