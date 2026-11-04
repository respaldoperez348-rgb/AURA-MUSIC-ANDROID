package com.auramusic.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.auramusic.app.data.database.Playlist
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    song: Song,
    playlists: List<Playlist>,
    isDownloaded: Boolean,
    downloadProgress: Int, // -1: none, 0..99: downloading, 100: done
    currentPositionSeconds: Int = 0,
    onDismiss: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onDeleteDownload: ((Song) -> Unit)? = null,
    onAddToQueueNext: (Song) -> Unit,
    onAddToPlaylist: (playlistId: String, song: Song) -> Unit,
    onCreatePlaylist: (title: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var includeTimestamp by remember { mutableStateOf(false) }

    val isLocal = song.id.startsWith("local_") || song.id.isBlank()

    // Formatear segundos a MM:SS
    fun formatTime(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }

    // Construcción de la URL limpia
    fun buildFinalUrl(): String {
        if (isLocal) return ""
        val baseUrl = "https://www.youtube.com/watch?v=${song.id}"
        return if (includeTimestamp && currentPositionSeconds > 0) {
            "$baseUrl&t=${currentPositionSeconds}s"
        } else {
            baseUrl
        }
    }

    val finalShareUrl = buildFinalUrl()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberBackgroundDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(GlassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Track Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBackgroundDark),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = CyberCyan)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!showPlaylistPicker) {
                // Action: Add to Playlist
                OptionRow(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    title = "Añadir a playlist...",
                    tint = CyberCyan,
                    onClick = { showPlaylistPicker = true }
                )

                // Action: Download
                val downloadText = when {
                    isDownloaded || downloadProgress == 100 -> "Descargada (Lista sin conexión)"
                    downloadProgress in 0..99 -> "Descargando... $downloadProgress%"
                    else -> "Descargar para escuchar sin conexión"
                }
                OptionRow(
                    icon = if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                    title = downloadText,
                    tint = if (isDownloaded) CyberCyan else TextPrimary,
                    trailingContent = {
                        if (downloadProgress in 0..99) {
                            CircularProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.size(20.dp),
                                color = CyberCyan,
                                strokeWidth = 2.dp
                            )
                        }
                    },
                    onClick = {
                        if (!isDownloaded && downloadProgress < 0) {
                            onDownload(song)
                        }
                    }
                )

                if (isDownloaded && onDeleteDownload != null) {
                    OptionRow(
                        icon = Icons.Default.Delete,
                        title = "Eliminar descarga sin conexión",
                        tint = CyberPink,
                        onClick = {
                            onDeleteDownload(song)
                            onDismiss()
                        }
                    )
                }

                // Action: Favorite
                val favRed = Color(0xFFFF1A40)
                OptionRow(
                    icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    title = if (song.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = if (song.isFavorite) favRed else TextPrimary,
                    onClick = {
                        onToggleFavorite(song)
                        onDismiss()
                    }
                )

                // Action: Play Next in Queue
                OptionRow(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Reproducir siguiente en cola",
                    tint = TextPrimary,
                    onClick = {
                        onAddToQueueNext(song)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fila del Timestamp (Opcional si hay tiempo reproducido)
                if (!isLocal && currentPositionSeconds > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceCard)
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = if (includeTimestamp) CyberCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Iniciar en ${formatTime(currentPositionSeconds)}",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Switch(
                            checked = includeTimestamp,
                            onCheckedChange = { includeTimestamp = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBackgroundDark,
                                checkedTrackColor = CyberCyan,
                                uncheckedTrackColor = CyberBackgroundDark,
                                uncheckedThumbColor = TextMuted
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Módulo de Compartir: Acciones Rápidas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón 1: Copiar Enlace
                    QuickShareButton(
                        modifier = Modifier.weight(1f),
                        title = "Copiar Link",
                        icon = Icons.Default.ContentCopy,
                        tint = CyberCyan,
                        onClick = {
                            if (!isLocal && finalShareUrl.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("URL Canción", finalShareUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Pista local sin enlace público", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Botón 2: Compartir Vía (Apps del sistema)
                    QuickShareButton(
                        modifier = Modifier.weight(1f),
                        title = "Compartir",
                        icon = Icons.Default.Share,
                        tint = CyberPink,
                        onClick = {
                            if (!isLocal && finalShareUrl.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, finalShareUrl)
                                    clipData = ClipData.newRawUri("Song URL", Uri.parse(finalShareUrl))
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir canción"))
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Pista local sin enlace público", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Botón 3: Código QR
                    QuickShareButton(
                        modifier = Modifier.weight(1f),
                        title = "Código QR",
                        icon = Icons.Default.QrCode,
                        tint = TextPrimary,
                        onClick = {
                            if (!isLocal && finalShareUrl.isNotBlank()) {
                                showQrDialog = true
                            } else {
                                Toast.makeText(context, "Pista local sin código QR disponible", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            } else {
                // Playlist Selection Subview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECCIONAR PLAYLIST",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(onClick = { showCreateDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Nueva", color = CyberCyan, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    items(playlists) { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onAddToPlaylist(pl.id, song)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = pl.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(text = "${pl.trackCount} pistas", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            }
        )
    }

    if (showQrDialog) {
        QrCodePopupDialog(
            title = song.title,
            url = finalShareUrl,
            onDismiss = { showQrDialog = false }
        )
    }
}

@Composable
private fun QuickShareButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun QrCodePopupDialog(
    title: String,
    url: String,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(url) { generateQrBitmap(url, 512) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CyberBackgroundDark)
                .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESCANEAR PISTA",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                qrBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Código QR",
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: CircularProgressIndicator(color = CyberCyan)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    tint: Color,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        trailingContent?.invoke()
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberBackgroundDark,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(text = "NEW PLAYLIST", color = CyberCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
        },
        text = {
            Column {
                Text(text = "Enter a title for your custom music collection:", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Cyberpunk 2077 Night Mix", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Create", color = CyberBackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextMuted)
            }
        }
    )
}