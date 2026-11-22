package com.auramusic.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.auramusic.app.data.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Genera y comparte una tarjeta gráfica editorial de AURA Music con el verso seleccionado.
 */
object LyricCardSharer {

    suspend fun shareLyricCard(
        context: Context,
        song: Song,
        lyricLines: List<String>,
        accentHex: Int,
        coverBitmap: Bitmap?,
    ) {
        val file = withContext(Dispatchers.Default) {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fondo obsidiana con gradiente sutil del color de acento
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf("#080B11".toColorInt(), accentHex, "#080B11".toColorInt()),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Capa oscura de contraste para lectura editorial
            val scrimPaint = Paint().apply { color = 0xAA080B11.toInt() }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

            // Tarjeta interior
            val cardRect = RectF(72f, 110f, width - 72f, height - 110f)
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD111827.toInt() }
            canvas.drawRoundRect(cardRect, 52f, 52f, cardPaint)

            // Cabecera con portada
            val coverSize = 140f
            val coverRect = RectF(132f, 178f, 132f + coverSize, 178f + coverSize)
            if (coverBitmap != null) {
                val scaled = Bitmap.createScaledBitmap(coverBitmap, coverSize.toInt(), coverSize.toInt(), true)
                canvas.drawBitmap(scaled, null, coverRect, Paint(Paint.ANTI_ALIAS_FLAG))
            } else {
                val phPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentHex }
                canvas.drawRoundRect(coverRect, 24f, 24f, phPaint)
            }

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF8FAFC.toInt()
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF94A3B8.toInt()
                textSize = 32f
            }
            canvas.drawText(song.title.take(28), 300f, 238f, titlePaint)
            canvas.drawText(song.artist.take(32), 300f, 286f, artistPaint)

            // Versos de la letra
            val lyricsBody = lyricLines.joinToString("\n")
            val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 62f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textWidth = width - 264
            val staticLayout = StaticLayout.Builder
                .obtain(lyricsBody, 0, lyricsBody.length, lyricPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(14f, 1.0f)
                .build()

            canvas.save()
            val textTop = (height / 2f) - (staticLayout.height / 2f)
            canvas.translate(132f, textTop.coerceAtLeast(380f))
            staticLayout.draw(canvas)
            canvas.restore()

            // Pie de marca AURA Music
            val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentHex
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("AURA MUSIC", 132f, height - 165f, brandPaint)

            val outFile = File(context.cacheDir, "aura_lyric_card_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            outFile
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, null))
    }
}
