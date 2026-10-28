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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.auramusic.app.data.model.Song
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
    FAVORITES, LOCAL, PLAYLISTS
}

@Composable
fun VaultScreen(
    favorites: List<Song>,
    currentSong: Song,
    isPlaying: Boolean,
    onSelectSong: (Song) -> Unit,
    onPlayAllFavorites: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(VaultTab.FAVORITES) }

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
                text = "AURA // VAULT",
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "${favorites.size} Saved Tracks",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vault Sub-tabs (Favorites, Local Files, Playlists)
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
                label = "FAVORITES",
                isSelected = selectedTab == VaultTab.FAVORITES,
                accentColor = CyberPink,
                onClick = { selectedTab = VaultTab.FAVORITES }
            )
            VaultTabButton(
                icon = Icons.Default.Folder,
                label = "LOCAL",
                isSelected = selectedTab == VaultTab.LOCAL,
                accentColor = CyberCyan,
                onClick = { selectedTab = VaultTab.LOCAL }
            )
            VaultTabButton(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                label = "PLAYLISTS",
                isSelected = selectedTab == VaultTab.PLAYLISTS,
                accentColor = CyberCyan,
                onClick = { selectedTab = VaultTab.PLAYLISTS }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            VaultTab.FAVORITES -> {
                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No favorites saved yet. Tap the heart on any song!",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
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
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(favorites) { song ->
                            val isCurrent = song.id == currentSong.id
                            VaultTrackRow(
                                song = song,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && isPlaying,
                                onClick = { onSelectSong(song) }
                            )
                        }
                    }
                }
            }
            VaultTab.LOCAL -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "LOCAL AUDIO STORAGE", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Ready to scan device audio files (MP3/FLAC)", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
            VaultTab.PLAYLISTS -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = CyberPink, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "CUSTOM PLAYLISTS", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Organize themed collections & cyber mixes", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
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
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) CyberBackgroundDark else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
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
private fun VaultTrackRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
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
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
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
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
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
            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Favorited",
            tint = CyberPink,
            modifier = Modifier.size(18.dp)
        )
    }
}
