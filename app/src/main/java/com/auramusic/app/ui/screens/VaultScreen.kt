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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.database.DownloadedTrack
import com.auramusic.app.data.database.ListeningStats
import com.auramusic.app.data.database.Playlist
import com.auramusic.app.data.locale.AuraStrings
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.components.CreatePlaylistDialog
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.GlassBorderSubtle
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
    localTracks: List<Song>,
    isScanningLocal: Boolean,
    currentSong: Song,
    isPlaying: Boolean,
    appLanguage: String = "Español",
    listeningStats: ListeningStats = ListeningStats(),
    onSelectSong: (Song, List<Song>) -> Unit,
    onPlayAllFavorites: () -> Unit,
    onPlayAllDownloads: () -> Unit,
    onPlayAllLocalTracks: () -> Unit,
    onScanLocalTracks: () -> Unit,
    onCreatePlaylist: (title: String) -> Unit,
    onImportPlaylist: (urlOrQuery: String) -> Unit = {},
    onDeletePlaylist: (playlistId: String) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit = {},
    onOpenSongOptions: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(VaultTab.FAVORITES) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Editorial Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = AuraStrings.get("library_title", appLanguage),
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp
            )
            val infoText = when (selectedTab) {
                VaultTab.FAVORITES -> "${favorites.size} ${AuraStrings.get("count_favorites", appLanguage)}"
                VaultTab.DOWNLOADS -> "${downloads.size} ${AuraStrings.get("count_downloads", appLanguage)}"
                VaultTab.PLAYLISTS -> "${playlists.size} ${AuraStrings.get("count_playlists", appLanguage)}"
                VaultTab.LOCAL -> "${localTracks.size} ${AuraStrings.get("count_local", appLanguage)}"
            }
            Text(
                text = infoText,
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Clean Editorial Listening Summary Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${listeningStats.totalPlays}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = AuraStrings.get("stat_plays", appLanguage),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(GlassBorderSubtle)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "${listeningStats.totalMinutes} min",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = (-0.2).sp
                )
                Text(
                    text = AuraStrings.get("stat_time", appLanguage),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(GlassBorderSubtle)
            )

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .weight(1.3f)
                    .padding(start = 10.dp)
            ) {
                Text(
                    text = listeningStats.topArtist,
                    color = CyberCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = AuraStrings.get("stat_top_artist", appLanguage),
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented 4-Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(13.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            VaultTabButton(
                label = AuraStrings.get("tab_favs", appLanguage),
                isSelected = selectedTab == VaultTab.FAVORITES,
                onClick = { selectedTab = VaultTab.FAVORITES },
                modifier = Modifier.weight(1f)
            )
            VaultTabButton(
                label = AuraStrings.get("tab_offline", appLanguage),
                isSelected = selectedTab == VaultTab.DOWNLOADS,
                onClick = { selectedTab = VaultTab.DOWNLOADS },
                modifier = Modifier.weight(1f)
            )
            VaultTabButton(
                label = AuraStrings.get("tab_lists", appLanguage),
                isSelected = selectedTab == VaultTab.PLAYLISTS,
                onClick = { selectedTab = VaultTab.PLAYLISTS },
                modifier = Modifier.weight(1f)
            )
            VaultTabButton(
                label = AuraStrings.get("tab_local", appLanguage),
                isSelected = selectedTab == VaultTab.LOCAL,
                onClick = { selectedTab = VaultTab.LOCAL },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            VaultTab.FAVORITES -> {
                if (favorites.isEmpty()) {
                    EmptyVaultState(
                        icon = Icons.Default.Favorite,
                        message = AuraStrings.get("empty_favorites", appLanguage)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrimaryActionPill(
                            icon = Icons.Default.PlayArrow,
                            text = AuraStrings.get("play_all", appLanguage),
                            onClick = onPlayAllFavorites
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    EmptyVaultState(
                        icon = Icons.Default.Download,
                        message = AuraStrings.get("empty_downloads", appLanguage)
                    )
                } else {
                    val totalMb = downloads.sumOf { it.fileSize } / (1024 * 1024)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrimaryActionPill(
                            icon = Icons.Default.PlayArrow,
                            text = AuraStrings.get("play_all", appLanguage),
                            onClick = onPlayAllDownloads
                        )

                        Text(
                            text = "${downloads.size} ${AuraStrings.get("tracks_unit", appLanguage)} • ${totalMb} MB",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val dlSongs = remember(downloads) { downloads.map { it.song } }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        text = AuraStrings.get("your_playlists", appLanguage),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryActionPill(
                            icon = Icons.Default.Download,
                            text = AuraStrings.get("import_yt", appLanguage),
                            onClick = { showImportDialog = true }
                        )
                        PrimaryActionPill(
                            icon = Icons.Default.Add,
                            text = AuraStrings.get("new_playlist", appLanguage),
                            onClick = { showCreateDialog = true }
                        )
                    }
                }

                if (playlists.isEmpty()) {
                    EmptyVaultState(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        message = AuraStrings.get("empty_playlists", appLanguage)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 130.dp)
                    ) {
                        items(playlists) { pl ->
                            PlaylistCard(
                                playlist = pl,
                                tracksUnit = AuraStrings.get("tracks_unit", appLanguage),
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
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurfaceCard)
                                    .border(1.dp, GlassBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = AuraStrings.get("local_storage_title", appLanguage),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = AuraStrings.get("local_storage_desc", appLanguage),
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            PrimaryActionPill(
                                icon = Icons.Default.MusicNote,
                                text = if (isScanningLocal) {
                                    AuraStrings.get("scanning_storage", appLanguage)
                                } else {
                                    AuraStrings.get("scan_music_btn", appLanguage)
                                },
                                onClick = onScanLocalTracks
                            )
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
                        PrimaryActionPill(
                            icon = Icons.Default.PlayArrow,
                            text = AuraStrings.get("play_all", appLanguage),
                            onClick = onPlayAllLocalTracks
                        )

                        SecondaryActionPill(
                            icon = Icons.Default.GraphicEq,
                            text = AuraStrings.get("rescan_btn", appLanguage),
                            onClick = onScanLocalTracks
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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

    if (showImportDialog) {
        CreatePlaylistDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { urlOrName ->
                onImportPlaylist(urlOrName)
                showImportDialog = false
            }
        )
    }
}

@Composable
private fun PrimaryActionPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberCyan)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberBackgroundDark,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = CyberBackgroundDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = (-0.1).sp
        )
    }
}

@Composable
private fun SecondaryActionPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EmptyVaultState(
    icon: ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceCard)
                    .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun VaultTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) CyberSurfaceActive else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = (-0.1).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) CyberSurfaceActive else CyberSurfaceCard.copy(alpha = 0.65f))
            .border(
                1.dp,
                if (isCurrent) CyberCyan.copy(alpha = 0.45f) else GlassBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberBackgroundDark),
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
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
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
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    color = if (isCurrent) CyberCyan else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = (-0.1).sp,
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onOptionClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    tracksUnit: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceActive),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = playlist.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.trackCount} $tracksUnit",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
