package com.auramusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
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
import coil.compose.AsyncImage
import com.auramusic.app.data.database.DownloadedTrack
import com.auramusic.app.data.database.ListeningStats
import com.auramusic.app.data.database.Playlist
import com.auramusic.app.data.locale.AuraStrings
import com.auramusic.app.data.model.Album
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.components.CreatePlaylistDialog
import com.auramusic.app.ui.theme.CyberBackground
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberSurface
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorderSubtle
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary

enum class VaultTab {
    FAVORITES, ARTISTS, ALBUMS, PLAYLISTS, DOWNLOADS, LOCAL
}

@Composable
fun VaultScreen(
    favorites: List<Song>,
    downloads: List<DownloadedTrack>,
    playlists: List<Playlist>,
    localTracks: List<Song>,
    followedArtists: List<Song> = emptyList(),
    savedAlbums: List<Album> = emptyList(),
    isScanningLocal: Boolean,
    currentSong: Song,
    isPlaying: Boolean,
    appLanguage: String = "Español",
    listeningStats: ListeningStats = ListeningStats(),
    onSelectSong: (Song, List<Song>) -> Unit,
    onSelectArtist: (Song) -> Unit = {},
    onSelectAlbum: (Album) -> Unit = {},
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
    var isGridView by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 1. Senior Editorial Header + Grid/List Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = AuraStrings.get("library_title", appLanguage),
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.7).sp
                )
                val infoText = when (selectedTab) {
                    VaultTab.FAVORITES -> "${favorites.size} ${AuraStrings.get("count_favorites", appLanguage)}"
                    VaultTab.ARTISTS -> "${followedArtists.size} ${AuraStrings.get("tab_artists", appLanguage).lowercase()}"
                    VaultTab.ALBUMS -> "${savedAlbums.size} ${AuraStrings.get("tab_albums", appLanguage).lowercase()}"
                    VaultTab.PLAYLISTS -> "${playlists.size} ${AuraStrings.get("count_playlists", appLanguage)}"
                    VaultTab.DOWNLOADS -> "${downloads.size} ${AuraStrings.get("count_downloads", appLanguage)}"
                    VaultTab.LOCAL -> "${localTracks.size} ${AuraStrings.get("count_local", appLanguage)}"
                }
                Text(
                    text = infoText,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            // Tactile View Mode Toggle (List vs Grid)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberSurfaceCard)
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isGridView) CyberSurfaceActive else Color.Transparent)
                        .clickable { isGridView = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = AuraStrings.get("view_list", appLanguage),
                        tint = if (!isGridView) TextPrimary else TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isGridView) CyberSurfaceActive else Color.Transparent)
                        .clickable { isGridView = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = AuraStrings.get("view_grid", appLanguage),
                        tint = if (isGridView) TextPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 2. Minimalist Listening Summary Strip
        if (listeningStats.totalPlays > 0) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AuraStrings.get("stat_plays", appLanguage),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${listeningStats.totalPlays}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(24.dp)
                        .background(GlassBorderSubtle)
                )
                Column {
                    Text(
                        text = AuraStrings.get("stat_time", appLanguage),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${listeningStats.totalMinutes} min",
                        color = CyberCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(24.dp)
                        .background(GlassBorderSubtle)
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(128.dp)
                ) {
                    Text(
                        text = AuraStrings.get("stat_top_artist", appLanguage),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = listeningStats.topArtist,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Collection Category Filter Pills (Favorites, Followed Artists, Saved Albums, Playlists, Downloads, Local)
        val filterTabs = listOf(
            VaultTab.FAVORITES to AuraStrings.get("tab_favs", appLanguage),
            VaultTab.ARTISTS to AuraStrings.get("tab_artists", appLanguage),
            VaultTab.ALBUMS to AuraStrings.get("tab_albums", appLanguage),
            VaultTab.PLAYLISTS to AuraStrings.get("tab_lists", appLanguage),
            VaultTab.DOWNLOADS to AuraStrings.get("tab_offline", appLanguage),
            VaultTab.LOCAL to AuraStrings.get("tab_local", appLanguage)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterTabs) { (tab, label) ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) TextPrimary else CyberSurfaceCard)
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) CyberBackgroundDark else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Content Area
        when (selectedTab) {
            VaultTab.FAVORITES -> {
                TrackCollectionSection(
                    songs = favorites,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isGridView = isGridView,
                    isDownloadedSection = false,
                    appLanguage = appLanguage,
                    emptyIcon = Icons.Default.Favorite,
                    emptyMessage = AuraStrings.get("empty_favorites", appLanguage),
                    onSelectSong = { song -> onSelectSong(song, favorites) },
                    onPlayAll = onPlayAllFavorites,
                    onOpenSongOptions = onOpenSongOptions
                )
            }

            VaultTab.ARTISTS -> {
                if (followedArtists.isEmpty()) {
                    EmptyVaultState(
                        icon = Icons.Default.Person,
                        message = AuraStrings.get("empty_followed_artists", appLanguage)
                    )
                } else if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(bottom = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(followedArtists, key = { it.id }) { artist ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectArtist(artist) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape)
                                        .background(CyberSurfaceCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (artist.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = artist.coverUrl,
                                            contentDescription = artist.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = artist.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = AuraStrings.get("following_artist", appLanguage),
                                    color = CyberCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 160.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(followedArtists, key = { it.id }) { artist ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectArtist(artist) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(CyberSurfaceCard),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (artist.coverUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = artist.coverUrl,
                                                contentDescription = artist.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.title,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (artist.artist.isNotBlank()) artist.artist else AuraStrings.get("following_artist", appLanguage),
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = AuraStrings.get("following_artist", appLanguage),
                                        color = CyberCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                HorizontalDivider(
                                    color = GlassBorderSubtle,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 66.dp)
                                )
                            }
                        }
                    }
                }
            }

            VaultTab.ALBUMS -> {
                if (savedAlbums.isEmpty()) {
                    EmptyVaultState(
                        icon = Icons.Default.Album,
                        message = AuraStrings.get("empty_saved_albums", appLanguage)
                    )
                } else if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedAlbums, key = { it.browseId.ifBlank { it.title } }) { album ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectAlbum(album) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CyberSurfaceCard)
                                ) {
                                    AsyncImage(
                                        model = album.coverUrl,
                                        contentDescription = album.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = album.title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = album.year.ifBlank { AuraStrings.get("album_label", appLanguage) },
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 160.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedAlbums, key = { it.browseId.ifBlank { it.title } }) { album ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectAlbum(album) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CyberSurfaceCard)
                                    ) {
                                        AsyncImage(
                                            model = album.coverUrl,
                                            contentDescription = album.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.title,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = album.year.ifBlank { AuraStrings.get("album_label", appLanguage) },
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                HorizontalDivider(
                                    color = GlassBorderSubtle,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 66.dp)
                                )
                            }
                        }
                    }
                }
            }

            VaultTab.PLAYLISTS -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AuraStrings.get("your_playlists", appLanguage),
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberSurfaceCard)
                                    .clickable { showImportDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Importar",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AuraStrings.get("import_yt", appLanguage),
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCyan)
                                    .clickable { showCreateDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Crear",
                                    tint = CyberBackgroundDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = AuraStrings.get("new_playlist", appLanguage),
                                    color = CyberBackgroundDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (playlists.isEmpty()) {
                        EmptyVaultState(
                            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                            message = AuraStrings.get("empty_playlists", appLanguage)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 160.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenPlaylist(playlist) }
                                            .padding(vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(CyberSurfaceCard),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                                contentDescription = null,
                                                tint = CyberCyan,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.title,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${playlist.trackCount} ${AuraStrings.get("tracks_unit", appLanguage)}",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeletePlaylist(playlist.id) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = GlassBorderSubtle,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 62.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            VaultTab.DOWNLOADS -> {
                val dlSongs = remember(downloads) { downloads.map { it.song } }
                TrackCollectionSection(
                    songs = dlSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    isGridView = isGridView,
                    isDownloadedSection = true,
                    appLanguage = appLanguage,
                    emptyIcon = Icons.Default.Download,
                    emptyMessage = AuraStrings.get("empty_downloads", appLanguage),
                    onSelectSong = { song -> onSelectSong(song, dlSongs) },
                    onPlayAll = onPlayAllDownloads,
                    onOpenSongOptions = onOpenSongOptions
                )
            }

            VaultTab.LOCAL -> {
                if (localTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurfaceCard),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = AuraStrings.get("local_storage_title", appLanguage),
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = AuraStrings.get("local_storage_desc", appLanguage),
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TextPrimary)
                                    .clickable(enabled = !isScanningLocal) { onScanLocalTracks() }
                                    .padding(horizontal = 20.dp, vertical = 11.dp)
                            ) {
                                Text(
                                    text = if (isScanningLocal) {
                                        AuraStrings.get("scanning_storage", appLanguage)
                                    } else {
                                        AuraStrings.get("scan_music_btn", appLanguage)
                                    },
                                    color = CyberBackgroundDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    TrackCollectionSection(
                        songs = localTracks,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        isGridView = isGridView,
                        isDownloadedSection = false,
                        appLanguage = appLanguage,
                        emptyIcon = Icons.Default.MusicNote,
                        emptyMessage = "",
                        onSelectSong = { song -> onSelectSong(song, localTracks) },
                        onPlayAll = onPlayAllLocalTracks,
                        onOpenSongOptions = onOpenSongOptions,
                        extraActionLabel = if (isScanningLocal) "..." else AuraStrings.get("rescan_btn", appLanguage),
                        onExtraAction = onScanLocalTracks
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { title ->
                onCreatePlaylist(title)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showImportDialog) {
        ImportPlaylistDialog(
            onConfirm = { urlOrQuery ->
                onImportPlaylist(urlOrQuery)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
private fun ImportPlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CyberSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "Importar lista de YouTube",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pega el enlace de una playlist de YouTube Music o el nombre del mix",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            androidx.compose.material3.OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("https://music.youtube.com/playlist?list=...", color = TextMuted, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text("Cancelar", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TextPrimary)
                        .clickable {
                            if (input.isNotBlank()) onConfirm(input.trim())
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text("Importar", color = CyberBackgroundDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TrackCollectionSection(
    songs: List<Song>,
    currentSong: Song,
    isPlaying: Boolean,
    isGridView: Boolean,
    isDownloadedSection: Boolean,
    appLanguage: String,
    emptyIcon: ImageVector,
    emptyMessage: String,
    onSelectSong: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onOpenSongOptions: (Song) -> Unit,
    extraActionLabel: String? = null,
    onExtraAction: (() -> Unit)? = null
) {
    if (songs.isEmpty()) {
        EmptyVaultState(icon = emptyIcon, message = emptyMessage)
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TextPrimary)
                    .clickable { onPlayAll() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = CyberBackgroundDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = AuraStrings.get("play_all", appLanguage),
                    color = CyberBackgroundDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (extraActionLabel != null && onExtraAction != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceCard)
                        .clickable { onExtraAction() }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = extraActionLabel,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(songs, key = { it.id }) { song ->
                    val isCurrent = song.id == currentSong.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSong(song) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberSurfaceCard)
                        ) {
                            AsyncImage(
                                model = song.coverUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text = song.title,
                            color = if (isCurrent) CyberCyan else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 160.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(songs, key = { it.id }) { song ->
                    val isCurrent = song.id == currentSong.id
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) CyberSurfaceCard else CyberBackground)
                                .clickable { onSelectSong(song) }
                                .padding(vertical = 9.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(CyberSurfaceCard),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = song.coverUrl,
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = if (isCurrent) CyberCyan else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isDownloadedSection) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = CyberCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = song.artist,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isCurrent && isPlaying) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            IconButton(
                                onClick = { onOpenSongOptions(song) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(
                            color = GlassBorderSubtle,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 58.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVaultState(
    icon: ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 110.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(CyberSurfaceCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(24.dp)
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
