package com.auramusic.app.data.local

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.RequiresApi
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carga la carátula de una canción local a partir de su URI de MediaStore
 * (content://media/external/audio/media/ID), con respaldo directo a ID3
 * embebido mediante MediaMetadataRetriever.
 */
class AudioThumbnailFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext<FetchResult?>(Dispatchers.IO) {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadBitmapQ() ?: loadEmbeddedPicture()
        } else {
            loadEmbeddedPicture()
        }
        if (bitmap != null) {
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        } else {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun loadBitmapQ(): Bitmap? {
        return try {
            context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
        } catch (_: Exception) {
            null
        }
    }

    private fun loadEmbeddedPicture(): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isAudioMediaUri = data.scheme == ContentResolver.SCHEME_CONTENT &&
                data.authority == "media" &&
                ((data.path ?: "").contains("/audio/media/") || (data.path ?: "").contains("/audio/albumart"))
            return if (isAudioMediaUri) {
                AudioThumbnailFetcher(context.applicationContext, data)
            } else {
                null
            }
        }
    }
}
