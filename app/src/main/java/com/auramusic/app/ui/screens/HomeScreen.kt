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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auramusic.app.data.database.Playlist
import com.auramusic.app.data.locale.AuraStrings
import com.auramusic.app.data.model.Album
import com.auramusic.app.data.model.Song
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.auramusic.app.ui.components.AuraIcon
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    featuredTracks: List<Song>,
    currentSong: Song,
    isPlaying: Boolean,
    popularArtists: List<Song> = emptyList(),
    trendingAlbums: List<Album> = emptyList(),
    userTopArtists: List<Song> = emptyList(),
    popularStations: List<String> = emptyList(),
    playlists: List<Playlist> = emptyList(),
    appLanguage: String = "Español",
    onSelectSong: (Song) -> Unit,
    onSelectVibe: (String) -> Unit,
    onSelectAlbum: (Album) -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenPlaylist: (Playlist) -> Unit = {},
    onOpenAuraLink: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stations = if (popularStations.isNotEmpty()) {
        popularStations
    } else {
        com.auramusic.app.network.YouTubeMusicEngine.getCuratedStations()
    }

    val displayTracks = if (featuredTracks.isNotEmpty()) {
        featuredTracks
    } else {
        com.auramusic.app.network.YouTubeMusicEngine.getCuratedFallbackTracks()
    }

    val currentDateStr = rememberCurrentDateFormatted(appLanguage)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // 1. Editorial Header (Limpio y Senior)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraIcon(
                        size = 40.dp,
                        isAnimated = isPlaying
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AURA MUSIC",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentDateStr,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyberSurfaceCard)
                            .clickable { onOpenAuraLink() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Aura Link",
                            tint = CyberCyan,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyberSurfaceCard)
                            .clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Ajustes",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // 2. Genre & Mood Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stations) { vibe ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(CyberSurfaceCard)
                            .clickable { onSelectVibe(vibe) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = vibe,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.1).sp
                        )
                    }
                }
            }
        }

        // 3. Quick Picks (Editorial Unboxed Album Artwork Carousel)
        item {
            Column {
                EditorialSectionHeader(
                    title = AuraStrings.get("quick_picks", appLanguage),
                    subtitle = AuraStrings.get("for_you", appLanguage)
                )
                Spacer(modifier = Modifier.height(14.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(displayTracks.take(6)) { song ->
                        QuickPickCard(
                            song = song,
                            isCurrent = song.id == currentSong.id,
                            isPlaying = isPlaying && song.id == currentSong.id,
                            onClick = { onSelectSong(song) }
                        )
                    }
                }
            }
        }

        // 4. User's Most Listened Artists (if any)
        if (userTopArtists.isNotEmpty()) {
            item {
                Column {
                    EditorialSectionHeader(
                        title = AuraStrings.get("favorite_artists", appLanguage),
                        subtitle = AuraStrings.get("frequent", appLanguage)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(userTopArtists) { artistSong ->
                            ArtistAvatarCard(
                                song = artistSong,
                                onClick = { onSelectSong(artistSong) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Popular Artists
        if (popularArtists.isNotEmpty()) {
            item {
                Column {
                    EditorialSectionHeader(
                        title = AuraStrings.get("popular_artists", appLanguage),
                        subtitle = AuraStrings.get("trending", appLanguage)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(popularArtists) { artistSong ->
                            ArtistAvatarCard(
                                song = artistSong,
                                onClick = { onSelectSong(artistSong) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Trending & Featured Albums Carousel
        if (trendingAlbums.isNotEmpty()) {
            item {
                Column {
                    EditorialSectionHeader(
                        title = AuraStrings.get("trending_albums", appLanguage),
                        subtitle = AuraStrings.get("collections", appLanguage)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(trendingAlbums) { album ->
                            TrendingAlbumCard(
                                album = album,
                                onClick = { onSelectAlbum(album) }
                            )
                        }
                    }
                }
            }
        }

        // 7. Top Trending Charts Section
        item {
            EditorialSectionHeader(
                title = AuraStrings.get("global_charts", appLanguage),
                subtitle = AuraStrings.get("top_tracks", appLanguage)
            )
        }

        itemsIndexed(displayTracks) { index, song ->
            val isCurrent = song.id == currentSong.id
            ChartTrackRow(
                rank = index + 1,
                song = song,
                isCurrent = isCurrent,
                isPlaying = isCurrent && isPlaying,
                onClick = { onSelectSong(song) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
private fun EditorialSectionHeader(
    title: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ArtistAvatarCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(CyberSurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.1).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TrendingAlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = album.title,
            color = TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = album.year.ifBlank { "Álbum" },
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickPickCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(14.dp))
                .background(CyberSurfaceCard),
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
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(34.dp)
                )
            }

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = song.title,
            color = if (isCurrent) CyberCyan else TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = song.artist,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChartTrackRow(
    rank: Int,
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) CyberSurfaceCard else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%02d", rank),
            color = if (isCurrent) CyberCyan else TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(28.dp)
        )

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberSurfaceCard),
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
            Text(
                text = song.title,
                color = if (isCurrent) CyberCyan else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.1).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = if (isCurrent && isPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = if (isCurrent) CyberCyan else TextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun rememberCurrentDateFormatted(appLanguage: String): String {
    return androidx.compose.runtime.remember(appLanguage) {
        val locale = when (appLanguage.lowercase()) {
            "english" -> Locale.ENGLISH
            "português" -> Locale("pt")
            "français" -> Locale.FRENCH
            "deutsch" -> Locale.GERMAN
            else -> Locale("es", "ES")
        }
        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", locale)
        sdf.format(Date())
    }
}

