package com.auramusic.app.network.sync

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID

object SyncManager {
    private const val TAG = "SyncManager"
    private const val PREFS_NAME = "aura_sync_prefs"
    private const val KEY_SYNC_CODE = "sync_code"

    /**
     * Devuelve el código de emparejamiento único del dispositivo (ej: aura_sync_a1b2c3d4)
     */
    fun getSyncTopic(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var code = prefs.getString(KEY_SYNC_CODE, null)

        if (code == null) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                null
            }

            val shortId = if (!androidId.isNullOrEmpty()) {
                androidId.take(8).lowercase()
            } else {
                UUID.randomUUID().toString().replace("-", "").take(8).lowercase()
            }
            code = "aura_sync_$shortId"
            prefs.edit().putString(KEY_SYNC_CODE, code).apply()
        }

        return code
    }

    /**
     * Obtiene el código corto de sincronización (sin prefijo aura_sync_)
     */
    fun getShortSyncCode(context: Context): String {
        return getSyncTopic(context).removePrefix("aura_sync_")
    }

    /**
     * Suscribe la app a su topic FCM automáticamente
     */
    fun ensureSubscribed(context: Context, onSubscribed: ((String) -> Unit)? = null) {
        val topic = getSyncTopic(context)
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Suscrito exitosamente al topic FCM: $topic")
                        onSubscribed?.invoke(topic)
                    } else {
                        Log.e(TAG, "Error al suscribir al topic: $topic", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseMessaging no disponible aún: ${e.message}")
        }
    }
}

