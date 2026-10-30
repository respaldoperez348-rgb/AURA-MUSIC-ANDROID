package com.auramusic.app.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.auramusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalAudioScanner {

    suspend fun scanDeviceAudio(context: Context, filterShortAudios: Boolean = true): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        // Filter out short notification beeps, UI sounds and voice notes (>= 30s when active)
        val minDuration = if (filterShortAudios) 30000L else 10000L
        val selection = "${MediaStore.Audio.Media.DURATION} >= $minDuration"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown Track"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn) ?: ""
                    val duration = it.getLong(durationColumn)
                    val albumId = it.getLong(albumIdColumn)
                    val dataPath = it.getString(dataColumn) ?: ""

                    if (filterShortAudios) {
                        val lower = dataPath.lowercase()
                        if (lower.contains("whatsapp") ||
                            lower.contains("telegram") ||
                            lower.contains("voice notes") ||
                            lower.contains("voicenotes") ||
                            lower.contains("call_rec") ||
                            lower.contains("/recordings/") ||
                            lower.endsWith(".opus")
                        ) {
                            continue
                        }
                    }

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    songList.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            coverUrl = albumArtUri,
                            mediaUri = contentUri.toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Include offline downloaded tracks into device library
        try {
            val db = com.auramusic.app.data.database.AuraDatabase.getInstance(context)
            val dls = db.getDownloadedTracks()
            for (dl in dls) {
                val f = java.io.File(dl.localFilePath)
                if (f.exists() && f.length() > 0) {
                    val alreadyIn = songList.any { 
                        it.title.trim().equals(dl.song.title.trim(), ignoreCase = true) && 
                        it.artist.trim().equals(dl.song.artist.trim(), ignoreCase = true)
                    }
                    if (!alreadyIn) {
                        songList.add(
                            dl.song.copy(
                                id = "local_dl_${dl.song.id}",
                                mediaUri = f.absolutePath
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songList
    }

    suspend fun searchLocalAudio(context: Context, query: String): List<Song> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val songList = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "(${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?) AND ${MediaStore.Audio.Media.DURATION} >= 15000"
        val selectionArgs = arrayOf("%$trimmed%", "%$trimmed%")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown Track"
                    val artist = it.getString(artistColumn) ?: "Unknown Artist"
                    val album = it.getString(albumColumn) ?: ""
                    val duration = it.getLong(durationColumn)
                    val albumId = it.getLong(albumIdColumn)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    songList.add(
                        Song(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            coverUrl = albumArtUri,
                            mediaUri = contentUri.toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Include offline downloaded tracks matching search query
        try {
            val db = com.auramusic.app.data.database.AuraDatabase.getInstance(context)
            val dls = db.getDownloadedTracks()
            for (dl in dls) {
                val f = java.io.File(dl.localFilePath)
                if (f.exists() && f.length() > 0) {
                    val matchesQuery = dl.song.title.contains(trimmed, ignoreCase = true) || 
                                       dl.song.artist.contains(trimmed, ignoreCase = true)
                    if (!matchesQuery) continue

                    val alreadyIn = songList.any { 
                        it.title.trim().equals(dl.song.title.trim(), ignoreCase = true) && 
                        it.artist.trim().equals(dl.song.artist.trim(), ignoreCase = true)
                    }
                    if (!alreadyIn) {
                        songList.add(
                            dl.song.copy(
                                id = "local_dl_${dl.song.id}",
                                mediaUri = f.absolutePath
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songList
    }
}
