package com.auramusic.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.AuraApplication
import com.auramusic.app.data.models.Song
import com.auramusic.app.data.repository.MusicImporter
import com.auramusic.app.i18n.LocalAuraStrings
import com.auramusic.app.ui.components.AuraCover
import com.auramusic.app.utils.formatDuration
import kotlinx.coroutines.launch

@Composable
fun ImportMusicScreen(
    onBackClick: () -> Unit,
    onPlayLocalSongs: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAuraStrings.current
    val repository = AuraApplication.repository
    val userManager = AuraApplication.userManager

    var linkInput by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var progressProcessed by remember { mutableIntStateOf(0) }
    var progressTotal by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var localSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    val scanLocal: () -> Unit = {
        scope.launch {
            isWorking = true
            statusMessage = "Escaneando música en tu dispositivo..."
            val found = MusicImporter.scanLocalMusic(context)
            localSongs = found
            isWorking = false
            statusMessage = if (found.isNotEmpty()) {
                "Se encontraron ${found.size} canciones locales en el teléfono."
            } else {
                "No se encontraron archivos de audio compatibles."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scanLocal() else statusMessage = "Permiso denegado para leer audio local."
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                progressProcessed = 0
                progressTotal = 0
                statusMessage = "Leyendo archivo CSV..."
                runCatching {
                    val res = MusicImporter.importFromCsv(context, uri, repository) { p, t ->
                        progressProcessed = p
                        progressTotal = t
                    }
                    userManager.importSongsToPlaylist(res.playlistTitle, res.songs)
                    statusMessage = "✓ ${res.songs.size} canciones importadas desde CSV."
                }.onFailure {
                    statusMessage = it.message ?: "Error leyendo el archivo CSV."
                }
                isWorking = false
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                val count = MusicImporter.exportLibraryToCsv(
                    context = context,
                    uri = uri,
                    favorites = userManager.favorites.value,
                    playlists = userManager.userPlaylists.value
                )
                isWorking = false
                statusMessage = "✓ Respaldo exportado ($count canciones)."
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.importMusic,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Tarjeta 1: Importar desde Enlace Público (Spotify, Apple Music, Deezer, YouTube)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Importar por enlace (Sin contraseña)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pega un enlace público de Spotify, Apple Music, Deezer o YouTube Music para crear la playlist en AURA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        placeholder = { Text("https://open.spotify.com/playlist/...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        enabled = !isWorking && linkInput.isNotBlank(),
                        onClick = {
                            scope.launch {
                                isWorking = true
                                progressProcessed = 0
                                progressTotal = 0
                                statusMessage = "Analizando enlace..."
                                runCatching {
                                    val res = MusicImporter.importFromLink(linkInput, repository) { p, t ->
                                        progressProcessed = p
                                        progressTotal = t
                                    }
                                    userManager.importSongsToPlaylist(res.playlistTitle, res.songs)
                                    statusMessage = "✓ '${res.playlistTitle}' (${res.songs.size} canciones desde ${res.sourceLabel})"
                                    linkInput = ""
                                }.onFailure {
                                    statusMessage = it.message ?: "No se pudo importar este enlace."
                                }
                                isWorking = false
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Importar Playlist", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Progreso / Estado
        if (isWorking || statusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        statusMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isWorking) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (progressTotal > 0) {
                                LinearProgressIndicator(
                                    progress = { progressProcessed.toFloat() / progressTotal.coerceAtLeast(1) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "$progressProcessed / $progressTotal pistas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }

        // Tarjeta 2: Escanear música local del teléfono
        item {
            ImportActionCard(
                icon = Icons.Default.FolderOpen,
                title = "Escanear música del teléfono",
                subtitle = "Encuentra MP3, FLAC, M4A y WAV guardados en tu dispositivo",
                onClick = {
                    if (MusicImporter.hasAudioPermission(context)) {
                        scanLocal()
                    } else {
                        permissionLauncher.launch(MusicImporter.requiredAudioPermission())
                    }
                }
            )
        }

        // Tarjeta 3: CSV (TuneMyMusic / Respaldo)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { csvImportLauncher.launch(arrayOf("text/*", "text/csv", "application/csv")) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importar CSV", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { csvExportLauncher.launch("aura-music-backup.csv") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar CSV", fontSize = 13.sp)
                }
            }
        }

        if (localSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Música local (${localSongs.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Button(
                        onClick = { userManager.importSongsToPlaylist("Música Local", localSongs) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Guardar todo", fontSize = 12.sp)
                    }
                }
            }

            items(localSongs, key = { it.id }) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPlayLocalSongs(song, localSongs) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraCover(
                        thumbnailUrl = song.thumbnailUrl,
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
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
                }
            }
        }
    }
}

@Composable
private fun ImportActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
