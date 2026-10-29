package com.auramusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auramusic.app.data.database.DownloadedTrack
import com.auramusic.app.data.database.Playlist
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.components.CreatePlaylistDialog
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberCyanGlow
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary

enum class VaultTab {
    FAVORITES, DOWNLOADS, PLAYLISTS, LOCAL
}

@Composable
fun VaultScreen(
    favorites: List<Song>,
    downloads: List<DownloadedTrack>,
    playlists: List<Playlist>,
    localTracks: List<Song> = emptyList(),
    isScanningLocal: Boolean = false,
    currentSong: Song,
    isPlaying: Boolean,
    onSelectSong: (Song, List<Song>) -> Unit,
    onPlayAllFavorites: () -> Unit,
    onPlayAllDownloads: () -> Unit,
    onPlayAllLocalTracks: () -> Unit = {},
    onScanLocalTracks: () -> Unit = {},
    onCreatePlaylist: (title: String) -> Unit,
    onDeletePlaylist: (playlistId: String) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit = {},
    onOpenSongOptions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(VaultTab.FAVORITES) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AURA",
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            val infoText = when (selectedTab) {
                VaultTab.FAVORITES -> "${favorites.size} Favorites"
                VaultTab.DOWNLOADS -> "${downloads.size} Offline"
                VaultTab.PLAYLISTS -> "${playlists.size} Playlists"
                VaultTab.LOCAL -> "Local Storage"
            }
            Text(
                text = infoText,
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vault 4-Tab Navigation Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VaultTabButton(
                icon = Icons.Default.Favorite,
                label = "FAVS",
                isSelected = selectedTab == VaultTab.FAVORITES,
                accentColor = CyberPink,
                onClick = { selectedTab = VaultTab.FAVORITES }
            )
            VaultTabButton(
                icon = Icons.Default.Download,
                label = "OFFLINE",
                isSelected = selectedTab == VaultTab.DOWNLOADS,
                accentColor = CyberCyan,
                onClick = { selectedTab = VaultTab.DOWNLOADS }
            )
            VaultTabButton(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                label = "LISTS",
                isSelected = selectedTab == VaultTab.PLAYLISTS,
                accentColor = CyberCyan,
                onClick = { selectedTab = VaultTab.PLAYLISTS }
            )
            VaultTabButton(
                icon = Icons.Default.Folder,
                label = "LOCAL",
                isSelected = selectedTab == VaultTab.LOCAL,
                accentColor = CyberCyan,
                onClick = { selectedTab = VaultTab.LOCAL }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            VaultTab.FAVORITES -> {
                if (favorites.isEmpty()) {
                    EmptyVaultState("No favorites saved yet. Tap the heart on any song!")
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPlayAllFavorites,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = CyberBackgroundDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Play All", color = CyberBackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {
                        items(favorites) { song ->
                            val isDownloaded = downloads.any {
                                it.song.id == song.id ||
                                (it.song.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                                 it.song.artist.trim().equals(song.artist.trim(), ignoreCase = true))
                            }
                            VaultSongRow(
                                song = song,
                                isCurrent = song.id == currentSong.id,
                                isPlaying = isPlaying && song.id == currentSong.id,
                                isDownloaded = isDownloaded,
                                onClick = { onSelectSong(song, favorites) },
                                onOptionClick = { onOpenSongOptions(song) }
                            )
                        }
                    }
                }
            }

            VaultTab.DOWNLOADS -> {
                if (downloads.isEmpty()) {
                    EmptyVaultState("No offline tracks downloaded yet. Tap the download button on any song to listen without internet!")
                } else {
                    val totalMb = downloads.sumOf { it.fileSize } / (1024 * 1024)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPlayAllDownloads,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = CyberBackgroundDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Play All Offline", color = CyberBackgroundDark, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "${downloads.size} tracks • ${totalMb} MB",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val dlSongs = remember(downloads) { downloads.map { it.song } }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {
                        items(downloads) { dt ->
                            VaultSongRow(
                                song = dt.song,
                                isCurrent = dt.song.id == currentSong.id,
                                isPlaying = isPlaying && dt.song.id == currentSong.id,
                                isDownloaded = true,
                                onClick = { onSelectSong(dt.song, dlSongs) },
                                onOptionClick = { onOpenSongOptions(dt.song) }
                            )
                        }
                    }
                }
            }

            VaultTab.PLAYLISTS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR PLAYLISTS",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CyberBackgroundDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "New Playlist", color = CyberBackgroundDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (playlists.isEmpty()) {
                    EmptyVaultState("No custom playlists yet. Tap '+ New Playlist' to create your first collection!")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 130.dp)
                    ) {
                        items(playlists) { pl ->
                            PlaylistCard(
                                playlist = pl,
                                onClick = { onOpenPlaylist(pl) },
                                onDelete = { onDeletePlaylist(pl.id) }
                            )
                        }
                    }
                }
            }

            VaultTab.LOCAL -> {
                if (localTracks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "LOCAL AUDIO STORAGE",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Scan your device for downloaded MP3, FLAC and WAV files.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onScanLocalTracks,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (isScanningLocal) "Scanning Storage..." else "Scan Music Files",
                                    color = CyberBackgroundDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onPlayAllLocalTracks,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = CyberBackgroundDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Play All Local", color = CyberBackgroundDark, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onScanLocalTracks,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceActive),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(text = "Rescan", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {
                        items(localTracks) { song ->
                            VaultSongRow(
                                song = song,
                                isCurrent = song.id == currentSong.id,
                                isPlaying = isPlaying && song.id == currentSong.id,
                                isDownloaded = true,
                                onClick = { onSelectSong(song, localTracks) },
                                onOptionClick = { onOpenSongOptions(song) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title ->
                onCreatePlaylist(title)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun EmptyVaultState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextMuted,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
private fun VaultTabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) CyberBackgroundDark else TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isSelected) CyberBackgroundDark else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun VaultSongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean = false,
    onClick: () -> Unit,
    onOptionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) CyberCyanGlow.copy(alpha = 0.18f) else CyberSurfaceCard)
            .border(
                1.dp,
                if (isCurrent) CyberCyan.copy(alpha = 0.7f) else GlassBorder.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CyberSurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNotBlank()) {
                coil.compose.SubcomposeAsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(CyberSurfaceCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = CyberCyan.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(CyberSurfaceCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            } else {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    color = if (isCurrent) CyberCyan else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isDownloaded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Offline Ready",
                        tint = CyberCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onOptionClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceActive),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = playlist.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = "${playlist.trackCount} tracks", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}
