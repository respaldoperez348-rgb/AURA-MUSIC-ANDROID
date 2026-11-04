package com.auramusic.app.network.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.auramusic.app.data.database.AuraDatabase
import com.auramusic.app.data.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object LibrarySyncRepository {
    private const val TAG = "LibrarySyncRepository"

    /**
     * Sube toda la biblioteca local de AuraDatabase a Cloud Firestore
     * en lotes (batch) de hasta 450 operaciones para respetar el límite de 500 de Firestore.
     */
    suspend fun pushFullLibraryToCloud(context: Context): Boolean = withContext(Dispatchers.IO) {
        val syncCode = SyncManager.getSyncTopic(context)
        val db = AuraDatabase.getInstance(context)
        val firestore = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Firestore no inicializado: ${e.message}")
            return@withContext false
        }

        try {
            val favorites = db.getFavorites()
            val downloads = db.getDownloadedTracks()
            val playlists = db.getPlaylists()
            val artists = db.getFollowedArtists()
            val albums = db.getSavedAlbums()

            // 1. Unificar canciones de favoritos, descargas y playlists para el catálogo principal
            val allSongsMap = mutableMapOf<String, Song>()
            favorites.forEach { allSongsMap[it.id] = it }
            downloads.forEach { dt ->
                allSongsMap.putIfAbsent(dt.song.id, dt.song)
            }
            playlists.forEach { pl ->
                val songsInPl = db.getSongsInPlaylist(pl.id)
                songsInPl.forEach { song ->
                    allSongsMap.putIfAbsent(song.id, song)
                }
            }

            val sessionRef = firestore.collection("sync_sessions").document(syncCode)

            // 2. Metadatos de la sesión de sincronización
            val metaData = hashMapOf(
                "syncCode" to syncCode,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "androidVersion" to Build.VERSION.RELEASE,
                "lastSyncedAt" to System.currentTimeMillis(),
                "totalSongs" to allSongsMap.size,
                "totalFavorites" to favorites.size,
                "totalPlaylists" to playlists.size,
                "totalArtists" to artists.size,
                "totalAlbums" to albums.size
            )
            sessionRef.set(metaData, SetOptions.merge()).await()

            // 3. Subir Canciones (en bloques <= 450)
            val libraryCol = sessionRef.collection("library")
            val songsList = allSongsMap.values.toList()
            songsList.chunked(450).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { song ->
                    val docRef = libraryCol.document(song.id)
                    val map = songToMap(song)
                    batch.set(docRef, map, SetOptions.merge())
                }
                batch.commit().await()
            }

            // 4. Subir Listas de Reproducción (Playlists) con sus canciones
            if (playlists.isNotEmpty()) {
                val playlistsCol = sessionRef.collection("playlists")
                val batch = firestore.batch()
                playlists.forEach { pl ->
                    val plRef = playlistsCol.document(pl.id)
                    val songsInPl = db.getSongsInPlaylist(pl.id)
                    val songsMapList = songsInPl.map { songToMap(it) }
                    val cover = if (pl.coverUrl.isNotBlank()) pl.coverUrl else (songsInPl.firstOrNull()?.coverUrl ?: "")
                    val plMap = hashMapOf(
                        "id" to pl.id,
                        "title" to pl.title,
                        "description" to pl.description,
                        "coverUrl" to cover,
                        "trackCount" to songsInPl.size,
                        "tracks" to songsMapList,
                        "createdAt" to pl.createdAt
                    )
                    batch.set(plRef, plMap, SetOptions.merge())
                }
                batch.commit().await()
            }

            // 5. Subir Artistas Seguidos
            if (artists.isNotEmpty()) {
                val artistsCol = sessionRef.collection("artists")
                val batch = firestore.batch()
                artists.forEach { art ->
                    val artRef = artistsCol.document(art.id)
                    val artMap = hashMapOf(
                        "id" to art.id,
                        "name" to art.artist,
                        "coverUrl" to art.coverUrl,
                        "browseId" to art.browseId
                    )
                    batch.set(artRef, artMap, SetOptions.merge())
                }
                batch.commit().await()
            }

            // 6. Subir Álbumes Guardados
            if (albums.isNotEmpty()) {
                val albumsCol = sessionRef.collection("albums")
                val batch = firestore.batch()
                albums.forEach { alb ->
                    val albRef = albumsCol.document(alb.browseId)
                    val albMap = hashMapOf(
                        "browseId" to alb.browseId,
                        "title" to alb.title,
                        "year" to alb.year,
                        "type" to alb.type,
                        "coverUrl" to alb.coverUrl
                    )
                    batch.set(albRef, albMap, SetOptions.merge())
                }
                batch.commit().await()
            }

            Log.i(TAG, "Sincronización completa subida a Firestore para $syncCode (${allSongsMap.size} canciones)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error subiendo biblioteca a Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Sube o actualiza una canción individual en Cloud Firestore
     */
    suspend fun syncSingleSongToCloud(context: Context, song: Song) = withContext(Dispatchers.IO) {
        val syncCode = SyncManager.getSyncTopic(context)
        try {
            val firestore = FirebaseFirestore.getInstance()
            val songRef = firestore.collection("sync_sessions")
                .document(syncCode)
                .collection("library")
                .document(song.id)

            songRef.set(songToMap(song), SetOptions.merge()).await()
            Log.d(TAG, "Canción individual sincronizada en Firestore: ${song.title} (${song.id})")
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo sincronizar canción individual en Firestore: ${e.message}")
        }
    }

    /**
     * Elimina una canción de Firestore cuando se desmarca de favoritos o biblioteca
     */
    suspend fun removeSongFromCloud(context: Context, songId: String) = withContext(Dispatchers.IO) {
        val syncCode = SyncManager.getSyncTopic(context)
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("sync_sessions")
                .document(syncCode)
                .collection("library")
                .document(songId)
                .delete()
                .await()
            Log.d(TAG, "Canción eliminada de Firestore: $songId")
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando canción de Firestore: ${e.message}")
        }
    }

    /**
     * Sube o actualiza una lista de reproducción individual en Cloud Firestore con todas sus canciones
     */
    suspend fun syncSinglePlaylistToCloud(context: Context, playlistId: String) = withContext(Dispatchers.IO) {
        val syncCode = SyncManager.getSyncTopic(context)
        val db = AuraDatabase.getInstance(context)
        try {
            val firestore = FirebaseFirestore.getInstance()
            val playlists = db.getPlaylists()
            val pl = playlists.find { it.id == playlistId } ?: return@withContext
            val songsInPl = db.getSongsInPlaylist(pl.id)
            val songsMapList = songsInPl.map { songToMap(it) }
            val cover = if (pl.coverUrl.isNotBlank()) pl.coverUrl else (songsInPl.firstOrNull()?.coverUrl ?: "")

            val plMap = hashMapOf(
                "id" to pl.id,
                "title" to pl.title,
                "description" to pl.description,
                "coverUrl" to cover,
                "trackCount" to songsInPl.size,
                "tracks" to songsMapList,
                "createdAt" to pl.createdAt
            )
            firestore.collection("sync_sessions")
                .document(syncCode)
                .collection("playlists")
                .document(pl.id)
                .set(plMap, SetOptions.merge())
                .await()
            Log.d(TAG, "Playlist sincronizada en Firestore: ${pl.title} (${songsInPl.size} canciones)")
        } catch (e: Exception) {
            Log.w(TAG, "Error sincronizando playlist a Firestore: ${e.message}")
        }
    }

    private fun songToMap(song: Song): Map<String, Any?> {
        return hashMapOf(
            "id" to song.id,
            "title" to song.title,
            "artist" to song.artist,
            "album" to song.album,
            "durationMs" to song.durationMs,
            "coverUrl" to song.coverUrl,
            "mediaUri" to song.mediaUri,
            "isFavorite" to song.isFavorite,
            "browseId" to song.browseId,
            "updatedAt" to System.currentTimeMillis()
        )
    }
}

