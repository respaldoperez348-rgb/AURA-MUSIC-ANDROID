package com.auramusic.app.network.sync

import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AuraMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AuraMessagingService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        if (data.isEmpty()) {
            Log.d(TAG, "Mensaje FCM recibido sin payload de datos (data).")
            return
        }

        val action = data["action"] ?: return
        val isFullSync = action.equals("REQUEST_FULL_SYNC", ignoreCase = true)

        val trackId = data["trackId"] ?: data["id"]
        if (!isFullSync && trackId.isNullOrEmpty()) {
            Log.w(TAG, "Acción $action omitida: trackId ausente.")
            return
        }

        val title = data["title"] ?: "Pista Remota"
        val artist = data["artist"] ?: "Aura Music"
        val thumbnailUrl = data["thumbnailUrl"] ?: data["coverUrl"] ?: ""
        val videoUrl = data["videoUrl"] ?: data["mediaUri"] ?: ""
        val album = data["album"] ?: "Sincronizado desde PC"

        Log.i(TAG, "FCM Data Message recibido: action=$action, trackId=$trackId, title=$title")

        // Empaquetar parámetros para el Worker
        val inputData = Data.Builder()
            .putString(RemoteActionWorker.KEY_ACTION, action)
            .putString(RemoteActionWorker.KEY_TRACK_ID, trackId ?: "")
            .putString(RemoteActionWorker.KEY_TITLE, title)
            .putString(RemoteActionWorker.KEY_ARTIST, artist)
            .putString(RemoteActionWorker.KEY_THUMBNAIL_URL, thumbnailUrl)
            .putString(RemoteActionWorker.KEY_VIDEO_URL, videoUrl)
            .putString(RemoteActionWorker.KEY_ALBUM, album)
            .build()

        // Expedited OneTimeWorkRequest garantizado para despertar el proceso en Doze Mode
        val workRequest = OneTimeWorkRequestBuilder<RemoteActionWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("aura_remote_sync")
            .build()

        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Nuevo FCM Token generado: $token")
        SyncManager.ensureSubscribed(applicationContext)
    }
}

