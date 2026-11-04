package com.auramusic.app.data.local

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
 * (content://media/external/audio/media/ID).
 *
 * La URI antigua "content://media/external/audio/albumart/ID" dejó de funcionar desde
 * Android 10, por eso las canciones locales aparecían sin portada. En Android 10+ la
 * forma correcta es ContentResolver.loadThumbnail().
 */
class AudioThumbnailFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext<FetchResult?>(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bitmap = loadBitmapQ()
            if (bitmap != null) {
                DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            } else {
                null
            }
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

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val isAudioMediaUri = data.scheme == ContentResolver.SCHEME_CONTENT &&
                data.authority == "media" &&
                (data.path ?: "").contains("/audio/media/")
            return if (isAudioMediaUri && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AudioThumbnailFetcher(context.applicationContext, data)
            } else {
                null
            }
        }
    }
}
