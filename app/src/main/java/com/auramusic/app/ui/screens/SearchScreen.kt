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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberCyanGlow
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.NeonPurple
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    searchResults: List<Song>,
    isSearching: Boolean,
    currentSong: Song,
    isPlaying: Boolean,
    onSearch: (String) -> Unit,
    onSelectSong: (Song) -> Unit,
    onOpenSongOptions: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Top Tracks") }
    val focusManager = LocalFocusManager.current

    val categories = listOf("Top Tracks", "Synthwave", "Cyberpunk", "Chillwave", "Rock", "Lo-Fi")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Frosted Glass Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CyberSurfaceCard)
                .border(1.5.dp, GlassBorder, RoundedCornerShape(20.dp))
        ) {
            TextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    if (it.length >= 3) {
                        onSearch(it)
                    }
                },
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums...",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CyberCyan
                    )
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            onSearch("")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextMuted
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    onSearch(searchText)
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyberCyan
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Category Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) CyberCyanGlow.copy(alpha = 0.25f) else CyberSurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) CyberCyan else GlassBorder.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedCategory = category
                            onSearch(category)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) CyberCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section Title with results count or searching loader
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPLORE RESULTS",
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )

            if (isSearching) {
                CircularProgressIndicator(
                    color = CyberCyan,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "${searchResults.size} Songs",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Results List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp) // Room for mini player
        ) {
            items(searchResults) { song ->
                val isCurrent = song.id == currentSong.id
                SearchResultRow(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { onSelectSong(song) },
                    onOptionClick = { onOpenSongOptions?.invoke(song) }
                )
            }
        }
    }
}

@Composable
fun SearchResultRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onOptionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isCurrent) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            colors = listOf(CyberCyanGlow.copy(alpha = 0.2f), CyberSurfaceCard)
                        )
                    )
                } else {
                    Modifier.background(CyberSurfaceCard)
                }
            )
            .border(
                1.dp,
                if (isCurrent) CyberCyan.copy(alpha = 0.7f) else GlassBorder.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail (Circular for Artist, Rounded for Album/Song)
        val thumbShape = if (song.isArtist) CircleShape else if (song.isAlbum) RoundedCornerShape(8.dp) else RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(thumbShape)
                .background(CyberBackgroundDark)
                .border(1.dp, if (song.isArtist) CyberCyan else if (song.isAlbum) NeonPurple else GlassBorder, thumbShape),
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
                    imageVector = if (song.isArtist) Icons.Default.Person else if (song.isAlbum) Icons.Default.Album else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (song.isAlbum) NeonPurple else CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isCurrent && isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBackgroundDark.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) CyberCyan else TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitleText = when {
                song.isArtist -> "ARTISTA • VER PERFIL"
                song.isAlbum -> "ÁLBUM • VER DISCO"
                else -> song.artist
            }
            val subtitleColor = when {
                song.isArtist -> CyberCyan
                song.isAlbum -> NeonPurple
                else -> TextSecondary
            }
            Text(
                text = subtitleText,
                color = subtitleColor,
                fontSize = 12.sp,
                fontWeight = if (song.isArtist || song.isAlbum) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Quick Action Icon (Profile for Artist, Album for Album, Play for Song)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isCurrent) CyberCyan else CyberSurfaceActive)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            val actionIcon = when {
                song.isArtist -> Icons.Default.Person
                song.isAlbum -> Icons.Default.Album
                isCurrent && isPlaying -> Icons.Default.Equalizer
                else -> Icons.Default.PlayArrow
            }
            Icon(
                imageVector = actionIcon,
                contentDescription = if (song.isArtist) "Ver Artista" else if (song.isAlbum) "Ver Álbum" else "Play",
                tint = if (isCurrent) CyberBackgroundDark else if (song.isAlbum) NeonPurple else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        if (onOptionClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onOptionClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
