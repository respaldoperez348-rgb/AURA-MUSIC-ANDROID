package com.auramusic.app.network.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.auramusic.app.R
import com.auramusic.app.data.database.AuraDatabase
import com.auramusic.app.data.model.Song
import com.auramusic.app.network.AuraDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteActionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "RemoteActionWorker"
        const val KEY_ACTION = "action"
        const val KEY_TRACK_ID = "trackId"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_THUMBNAIL_URL = "thumbnailUrl"
        const val KEY_VIDEO_URL = "videoUrl"
        const val KEY_ALBUM = "album"

        private const val SYNC_CHANNEL_ID = "aura_remote_sync_channel"
        private const val NOTIF_ID_SYNC = 7000
        private const val NOTIF_ID_FAVORITE = 7001
        private const val NOTIF_ID_DOWNLOAD = 7002
    }

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Aura Sync")
            .setContentText("Procesando acción en segundo plano...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIF_ID_SYNC,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIF_ID_SYNC, notification)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val action = inputData.getString(KEY_ACTION) ?: return@withContext Result.failure()
        Log.i(TAG, "Ejecutando acción remota WorkManager: $action")

        val database = AuraDatabase.getInstance(applicationContext)

        when (action.uppercase()) {
            "REQUEST_FULL_SYNC" -> {
                // Sincronización completa de toda la biblioteca hacia Cloud Firestore
                setForeground(getForegroundInfo())
                val success = LibrarySyncRepository.pushFullLibraryToCloud(applicationContext)
                if (success) {
                    showDiscreetNotification(
                        id = NOTIF_ID_SYNC,
                        title = "☁️ Aura Sync Completado",
                        message = "Tu biblioteca completa se sincronizó con Aura Desktop."
                    )
                    Result.success()
                } else {
                    Result.retry()
                }
            }

            "FAVORITE" -> {
                val trackId = inputData.getString(KEY_TRACK_ID) ?: return@withContext Result.failure()
                val title = inputData.getString(KEY_TITLE) ?: "Canción"
                val artist = inputData.getString(KEY_ARTIST) ?: "Artista"
                val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL) ?: ""
                val videoUrl = inputData.getString(KEY_VIDEO_URL) ?: ""
                val album = inputData.getString(KEY_ALBUM) ?: "Sincronizado desde PC"

                val song = Song(
                    id = trackId,
                    title = title,
                    artist = artist,
                    album = album,
                    coverUrl = thumbnailUrl,
                    mediaUri = videoUrl,
                    isFavorite = true
                )

                // 1. Guardar en SQLite local
                database.addFavorite(song)

                // 2. Notificar en Cloud Firestore para mantener la sesión actualizada
                LibrarySyncRepository.syncSingleSongToCloud(applicationContext, song)

                showDiscreetNotification(
                    id = NOTIF_ID_FAVORITE,
                    title = "❤️ Añadido a Favoritos",
                    message = "$title — $artist"
                )
                Result.success()
            }

            "UNFAVORITE", "REMOVE_FAVORITE" -> {
                val trackId = inputData.getString(KEY_TRACK_ID) ?: return@withContext Result.failure()
                val title = inputData.getString(KEY_TITLE) ?: "Canción"
                val artist = inputData.getString(KEY_ARTIST) ?: "Artista"

                // 1. Quitar de SQLite local
                database.removeFavorite(trackId)

                // 2. Eliminar de Cloud Firestore
                LibrarySyncRepository.removeSongFromCloud(applicationContext, trackId)

                showDiscreetNotification(
                    id = NOTIF_ID_FAVORITE,
                    title = "💔 Quitado de Favoritos",
                    message = "$title — $artist"
                )
                Result.success()
            }

            "DOWNLOAD" -> {
                val trackId = inputData.getString(KEY_TRACK_ID) ?: return@withContext Result.failure()
                val title = inputData.getString(KEY_TITLE) ?: "Canción"
                val artist = inputData.getString(KEY_ARTIST) ?: "Artista"
                val thumbnailUrl = inputData.getString(KEY_THUMBNAIL_URL) ?: ""
                val videoUrl = inputData.getString(KEY_VIDEO_URL) ?: ""
                val album = inputData.getString(KEY_ALBUM) ?: "Sincronizado desde PC"

                val song = Song(
                    id = trackId,
                    title = title,
                    artist = artist,
                    album = album,
                    coverUrl = thumbnailUrl,
                    mediaUri = videoUrl
                )

                // Mantener Foreground Service activo mientras se descarga completamente el archivo
                setForeground(getForegroundInfo())
                showDiscreetNotification(
                    id = NOTIF_ID_DOWNLOAD,
                    title = "📥 Descarga en progreso",
                    message = "$title — $artist"
                )

                val downloadManager = AuraDownloadManager(applicationContext, database)
                val success = downloadManager.downloadSongSuspended(song)

                if (success) {
                    Log.i(TAG, "Descarga remota finalizada con éxito: $title")
                    showDiscreetNotification(
                        id = NOTIF_ID_DOWNLOAD,
                        title = "✅ Descarga completada",
                        message = "$title — $artist (Guardada en Bóveda)"
                    )
                    Result.success()
                } else {
                    Log.e(TAG, "Fallo al completar la descarga remota: $title")
                    showDiscreetNotification(
                        id = NOTIF_ID_DOWNLOAD,
                        title = "❌ Error en descarga remota",
                        message = "No se pudo descargar $title"
                    )
                    Result.failure()
                }
            }

            else -> {
                Log.w(TAG, "Acción desconocida recibida: $action")
                Result.failure()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SYNC_CHANNEL_ID,
                "Aura Sincronización Remota",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones de acciones automáticas enviadas desde Aura Desktop"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showDiscreetNotification(id: Int, title: String, message: String) {
        createNotificationChannel()
        val notif = NotificationCompat.Builder(applicationContext, SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notif)
    }
}

