package com.auramusic.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.models.Song
import com.auramusic.app.data.repository.UserPlaylist
import com.auramusic.app.utils.formatDuration

private val FavoriteRed = Color(0xFFFF3B30)
private val DangerRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song?,
    isFavorite: Boolean = false,
    isDownloaded: Boolean = false,
    playlists: List<UserPlaylist> = emptyList(),
    currentPlaylistId: String? = null,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddToFavorites: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit,
    onRemoveFromCurrentPlaylist: ((String, Song) -> Unit)? = null,
    onDownload: (Song) -> Unit,
    onRemoveDownload: (Song) -> Unit = {},
    onShare: (Song) -> Unit,
) {
    if (song == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPlaylistSelector by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraCover(
                    thumbnailUrl = song.thumbnailUrl,
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${formatDuration(song.durationSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            if (!showPlaylistSelector) {
                OptionRow(
                    icon = Icons.Default.SkipNext,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = "Reproducir a continuación",
                    onClick = {
                        onPlayNext(song)
                        onDismiss()
                    }
                )

                OptionRow(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = "Añadir a la cola",
                    onClick = {
                        onAddToQueue(song)
                        onDismiss()
                    }
                )

                OptionRow(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    iconTint = if (isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurface,
                    label = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    onClick = {
                        onAddToFavorites(song)
                        onDismiss()
                    }
                )

                OptionRow(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    label = "Añadir a playlist",
                    onClick = { showPlaylistSelector = true }
                )

                if (currentPlaylistId != null && onRemoveFromCurrentPlaylist != null) {
                    OptionRow(
                        icon = Icons.Default.RemoveCircleOutline,
                        iconTint = DangerRed,
                        labelColor = DangerRed,
                        label = "Quitar de esta playlist",
                        onClick = {
                            onRemoveFromCurrentPlaylist(currentPlaylistId, song)
                            onDismiss()
                        }
                    )
                }

                if (isDownloaded) {
                    OptionRow(
                        icon = Icons.Default.Delete,
                        iconTint = DangerRed,
                        labelColor = DangerRed,
                        label = "Eliminar descarga",
                        onClick = {
                            onRemoveDownload(song)
                            onDismiss()
                        }
                    )
                } else {
                    OptionRow(
                        icon = Icons.Default.Download,
                        label = "Descargar",
                        onClick = {
                            onDownload(song)
                            onDismiss()
                        }
                    )
                }

                OptionRow(
                    icon = Icons.Default.Share,
                    label = "Compartir enlace oficial",
                    onClick = {
                        onShare(song)
                        onDismiss()
                    }
                )
            } else {
                Text(
                    text = "Selecciona o crea una playlist",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onAddToPlaylist(pl.title, song)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = pl.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${pl.songs.size} canciones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Nueva playlist (ej. Favoritas)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val title = newPlaylistName.trim().ifBlank { "Mi Playlist" }
                            onAddToPlaylist(title, song)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }

                TextButton(
                    onClick = { showPlaylistSelector = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Volver")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            color = labelColor
        )
    }
}
