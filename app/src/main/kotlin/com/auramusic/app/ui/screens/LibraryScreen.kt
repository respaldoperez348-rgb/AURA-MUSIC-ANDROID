package com.auramusic.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.models.Album
import com.auramusic.app.data.models.Song
import com.auramusic.app.data.repository.UserPlaylist
import com.auramusic.app.i18n.LocalAuraStrings
import com.auramusic.app.ui.components.AuraCover

private val CrimsonHeart = Color(0xFFE11D48)

@Composable
fun LibraryScreen(
    favoritesCount: Int,
    followingCount: Int,
    albumsCount: Int,
    downloadsCount: Int,
    playlistsCount: Int,
    offlineMode: Boolean = false,
    userPlaylists: List<UserPlaylist> = emptyList(),
    recentAlbums: List<Album>,
    recentFavorites: List<Song> = emptyList(),
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (UserPlaylist) -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    onSongOptionsClick: (Song) -> Unit = {},
    onCategoryClick: (String) -> Unit,
    onCreatePlaylist: (String) -> Unit = {},
    onImportClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onToggleOfflineMode: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = LocalAuraStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val hairlineBorder = BorderStroke(0.85.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
    val glassSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Nueva playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Ej. Mix Nocturno") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 185.dp)
    ) {
        // Cabecera limpia: Biblioteca + (+) Nueva playlist + Offline + Estadísticas
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.library,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { showCreatePlaylistDialog = true },
                        shape = CircleShape,
                        color = glassSurface,
                        border = hairlineBorder
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nueva playlist",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onToggleOfflineMode() },
                        shape = CircleShape,
                        color = if (offlineMode) primaryColor.copy(alpha = 0.22f) else glassSurface,
                        border = if (offlineMode) {
                            BorderStroke(1.dp, primaryColor.copy(alpha = 0.7f))
                        } else {
                            hairlineBorder
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = strings.offlineMode,
                                tint = if (offlineMode) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onStatsClick() },
                        shape = CircleShape,
                        color = glassSurface,
                        border = hairlineBorder
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = strings.statsRecap,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Cuadrícula Bento 2x2 en cristal esmerilado: Favoritos, Descargados, Música Local, Playlists
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LibraryBentoCard(
                        title = strings.favorites,
                        subtitle = strings.songsCount(favoritesCount),
                        icon = Icons.Default.Favorite,
                        iconTint = CrimsonHeart,
                        surfaceColor = glassSurface,
                        borderStroke = hairlineBorder,
                        onClick = { onCategoryClick("Favorito") },
                        modifier = Modifier.weight(1f)
                    )
                    LibraryBentoCard(
                        title = strings.downloaded,
                        subtitle = strings.songsCount(downloadsCount),
                        icon = Icons.Default.Download,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        surfaceColor = glassSurface,
                        borderStroke = hairlineBorder,
                        onClick = { onCategoryClick("Descargados") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LibraryBentoCard(
                        title = strings.localMusic,
                        subtitle = strings.localFormatHint,
                        icon = Icons.Default.Folder,
                        iconTint = primaryColor,
                        surfaceColor = glassSurface,
                        borderStroke = hairlineBorder,
                        onClick = onImportClick,
                        modifier = Modifier.weight(1f)
                    )
                    LibraryBentoCard(
                        title = strings.playlists,
                        subtitle = strings.listsCount(playlistsCount),
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        surfaceColor = glassSurface,
                        borderStroke = hairlineBorder,
                        onClick = { onCategoryClick("Playlist") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Banner destacado en cristal esmerilado: Importar música
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onImportClick() },
                shape = RoundedCornerShape(20.dp),
                color = glassSurface,
                border = hairlineBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.16f), Color.Transparent),
                                    center = Offset(size.width * 0.92f, size.height * 0.5f),
                                    radius = size.width * 0.42f
                                ),
                                center = Offset(size.width * 0.92f, size.height * 0.5f),
                                radius = size.width * 0.42f
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.importMusic,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.importSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = strings.importMusic,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Chips rápidos de Seguidos y Álbumes para mantener acceso directo
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCategoryClick("Seguidos") },
                    shape = RoundedCornerShape(14.dp),
                    color = glassSurface,
                    border = hairlineBorder
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = strings.following,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = strings.following,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.artistsCount(followingCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCategoryClick("Álbumes") },
                    shape = RoundedCornerShape(14.dp),
                    color = glassSurface,
                    border = hairlineBorder
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = strings.albums,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = strings.albums,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.albumsCount(albumsCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Sección: Tus Playlists + Tarjeta de Estadísticas
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = strings.yourPlaylists,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(userPlaylists, key = { "pl_card_${it.id}" }) { playlist ->
                    Column(
                        modifier = Modifier
                            .width(106.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlaylistClick(playlist) }
                    ) {
                        Surface(
                            modifier = Modifier.size(106.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = glassSurface,
                            border = hairlineBorder
                        ) {
                            val coverUrl = playlist.songs.firstOrNull()?.thumbnailUrl.orEmpty()
                            if (coverUrl.isNotBlank()) {
                                AuraCover(
                                    thumbnailUrl = coverUrl,
                                    id = playlist.id,
                                    title = playlist.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    primaryColor.copy(alpha = 0.22f),
                                                    MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = playlist.title,
                                        tint = primaryColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.songsCount(playlist.songs.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Tarjeta de Estadísticas integrada al final del carrusel (como en la lámina)
                item {
                    Column(
                        modifier = Modifier
                            .width(106.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onStatsClick() }
                    ) {
                        Surface(
                            modifier = Modifier.size(106.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = glassSurface,
                            border = hairlineBorder
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.20f),
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = strings.statsRecap,
                                    tint = primaryColor,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.statsRecap,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        Text(
                            text = strings.statsSummaryHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Añadidos recientemente (de la biblioteca real del usuario)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strings.recentlyAdded,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        if (recentAlbums.isEmpty() && recentFavorites.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = glassSurface,
                    border = hairlineBorder
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.emptyLibraryTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.emptyLibrarySubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentAlbums, key = { "lib_alb_${it.id}" }) { album ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraCover(
                        thumbnailUrl = album.thumbnailUrl,
                        id = album.id,
                        title = album.title,
                        artist = album.artist,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onAlbumClick(album) }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Ver álbum",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(recentFavorites.take(5), key = { "lib_fav_${it.id}" }) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(song) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraCover(
                        thumbnailUrl = song.thumbnailUrl,
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onSongOptionsClick(song) }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryBentoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    surfaceColor: Color,
    borderStroke: BorderStroke,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = surfaceColor,
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
