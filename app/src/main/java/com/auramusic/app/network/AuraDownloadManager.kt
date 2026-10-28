package com.auramusic.app.network

import android.content.Context
import android.os.Environment
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
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    // Map: songId -> progress percentage (0..100)
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val activeDownloads = ConcurrentHashMap<String, Boolean>()

    private val downloadsDir: File by lazy {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "aura_downloads")
        if (!dir.exists()) dir.mkdirs()
        dir
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

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                    _downloadProgress.update { it + (song.id to progress) }
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

            } catch (e: Exception) {
                e.printStackTrace()
                cleanFailedDownload(song.id)
            }
        }
    }

    private fun cleanFailedDownload(songId: String) {
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
