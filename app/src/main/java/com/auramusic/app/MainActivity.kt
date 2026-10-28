package com.auramusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.components.MiniPlayer
import com.auramusic.app.ui.components.StudioDeck
import com.auramusic.app.ui.screens.SearchScreen
import com.auramusic.app.ui.theme.AuraMusicTheme
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberCyanGlow
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.NeonPurple
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import com.auramusic.app.viewmodel.AppScreen
import com.auramusic.app.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraMusicTheme {
                val uiState by playerViewModel.uiState.collectAsState()

                BackHandler(enabled = uiState.currentScreen != AppScreen.DECK) {
                    playerViewModel.setScreen(AppScreen.DECK)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = CyberBackgroundDark
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Ambient Neon Blur Blobs
                        Box(
                            modifier = Modifier
                                .size(320.dp)
                                .offset(x = (-80).dp, y = (-50).dp)
                                .blur(95.dp)
                                .background(CyberCyan.copy(alpha = 0.20f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .offset(x = 180.dp, y = 200.dp)
                                .blur(95.dp)
                                .background(NeonPurple.copy(alpha = 0.16f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .offset(x = (-40).dp, y = 520.dp)
                                .blur(105.dp)
                                .background(CyberPink.copy(alpha = 0.12f), CircleShape)
                        )

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Navigation Header
                            AuraTopNavigationBar(
                                currentScreen = uiState.currentScreen,
                                onSelectScreen = { playerViewModel.setScreen(it) }
                            )

                            // Main Animated Content
                            Box(modifier = Modifier.weight(1f)) {
                                AnimatedContent(
                                    targetState = uiState.currentScreen,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "screen_transition"
                                ) { screen ->
                                    when (screen) {
                                        AppScreen.DECK -> {
                                            DeckView(
                                                uiState = uiState,
                                                onPlayPause = { playerViewModel.playPause() },
                                                onNext = { playerViewModel.next() },
                                                onPrevious = { playerViewModel.previous() },
                                                onSeekTo = { pos -> playerViewModel.seekTo(pos) },
                                                onToggleFavorite = { playerViewModel.toggleFavorite() },
                                                onToggleShuffle = { playerViewModel.toggleShuffle() },
                                                onToggleRepeat = { playerViewModel.toggleRepeat() },
                                                onSelectSong = { song -> playerViewModel.playSong(song) }
                                            )
                                        }
                                        AppScreen.SEARCH -> {
                                            SearchScreen(
                                                searchResults = uiState.searchResults,
                                                isSearching = uiState.isSearching,
                                                currentSong = uiState.currentSong,
                                                isPlaying = uiState.isPlaying,
                                                onSearch = { q -> playerViewModel.search(q) },
                                                onSelectSong = { song -> playerViewModel.playSong(song) }
                                            )
                                        }
                                        AppScreen.LYRICS -> {
                                            // Future lyrics screen
                                        }
                                    }
                                }
                            }
                        }

                        // Floating Mini-Player when in Search view
                        if (uiState.currentScreen == AppScreen.SEARCH) {
                            MiniPlayer(
                                song = uiState.currentSong,
                                isPlaying = uiState.isPlaying,
                                onPlayPause = { playerViewModel.playPause() },
                                onNext = { playerViewModel.next() },
                                onExpand = { playerViewModel.setScreen(AppScreen.DECK) },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraTopNavigationBar(
    currentScreen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(CyberCyan)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "AURA MUSIC",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }

        // Screen Selector Switcher
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            NavButton(
                icon = Icons.Default.GraphicEq,
                label = "DECK",
                isSelected = currentScreen == AppScreen.DECK,
                onClick = { onSelectScreen(AppScreen.DECK) }
            )
            NavButton(
                icon = Icons.Default.Search,
                label = "SEARCH",
                isSelected = currentScreen == AppScreen.SEARCH,
                onClick = { onSelectScreen(AppScreen.SEARCH) }
            )
        }
    }
}

@Composable
fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyberCyan else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) CyberBackgroundDark else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.size(4.dp))
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
fun DeckView(
    uiState: com.auramusic.app.viewmodel.PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSelectSong: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StudioDeck(
                song = uiState.currentSong,
                isPlaying = uiState.isPlaying,
                currentPositionMs = uiState.currentPositionMs,
                durationMs = uiState.durationMs,
                isShuffle = uiState.isShuffle,
                isRepeat = uiState.isRepeat,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeekTo = onSeekTo,
                onToggleFavorite = onToggleFavorite,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAYLIST QUEUE",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "${uiState.queue.size} Tracks",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        itemsIndexed(uiState.queue) { index, song ->
            val isCurrent = song.id == uiState.currentSong.id
            TrackQueueRow(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isCurrent && uiState.isPlaying,
                onClick = { onSelectSong(song) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TrackQueueRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) CyberCyanGlow.copy(alpha = 0.15f) else CyberSurfaceCard)
            .border(
                1.dp,
                if (isCurrent) CyberCyan.copy(alpha = 0.6f) else GlassBorder.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrent) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isCurrent) CyberCyan else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

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

        Text(
            text = formatDuration(song.durationMs),
            color = TextMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}
