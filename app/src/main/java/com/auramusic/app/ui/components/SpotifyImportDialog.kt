package com.auramusic.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.auramusic.app.data.model.Song
import com.auramusic.app.network.SpotifyImportResult
import com.auramusic.app.network.SpotifyImporter
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberGreen
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SpotifyImportDialog(
    onDismiss: () -> Unit,
    onImportConfirmed: (String, List<Song>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<SpotifyImportResult?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CyberBackgroundDark)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = CyberGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MIGRAR SPOTIFY",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "IMPORTACIÓN DIRECTA SIN LOGIN",
                                color = CyberGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (importResult == null) {
                    // Input View
                    Text(
                        text = "Pega el enlace de cualquier playlist o álbum público de Spotify:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://open.spotify.com/playlist/...", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (urlInput.isNotBlank() && !isLoading) CyberGreen else CyberSurfaceActive)
                            .clickable(enabled = urlInput.isNotBlank() && !isLoading) {
                                isLoading = true
                                scope.launch {
                                    val res = SpotifyImporter.importFromSpotifyUrl(urlInput)
                                    isLoading = false
                                    if (res != null && res.tracks.isNotEmpty()) {
                                        importResult = res
                                    } else {
                                        Toast.makeText(context, "No se pudieron extraer canciones del enlace", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = CyberBackgroundDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "ANALIZAR PLAYLIST",
                                color = if (urlInput.isNotBlank()) CyberBackgroundDark else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                } else {
                    // Preview & Confirm View
                    val res = importResult!!
                    Text(
                        text = res.playlistTitle.uppercase(),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${res.tracks.size} canciones encontradas • Creador: ${res.author}",
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberSurfaceCard)
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            itemsIndexed(res.tracks) { idx, track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = track.title, color = TextPrimary, fontSize = 12.sp, maxLines = 1)
                                        Text(text = track.artist, color = TextSecondary, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyberGreen)
                            .clickable {
                                onImportConfirmed(res.playlistTitle, res.tracks)
                                Toast.makeText(context, "✓ Playlist importada a tu Bóveda", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DownloadDone, contentDescription = null, tint = CyberBackgroundDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GUARDAR EN AURA MUSIC",
                                color = CyberBackgroundDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

