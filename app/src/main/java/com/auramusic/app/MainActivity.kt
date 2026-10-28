package com.auramusic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.auramusic.app.ui.components.EqualizerSheet
import com.auramusic.app.ui.components.GlassBottomDock
import com.auramusic.app.ui.components.MiniPlayer
import com.auramusic.app.ui.components.PlaylistDetailSheet
import com.auramusic.app.ui.components.QueueSheet
import com.auramusic.app.ui.components.SettingsSheet
import com.auramusic.app.ui.components.SleepTimerDialog
import com.auramusic.app.ui.components.StudioDeck
import com.auramusic.app.ui.components.TrackOptionsBottomSheet
import com.auramusic.app.ui.screens.HomeScreen
import com.auramusic.app.ui.screens.SearchScreen
import com.auramusic.app.ui.screens.VaultScreen
import com.auramusic.app.ui.theme.AuraMusicTheme
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.NeonPurple
import com.auramusic.app.viewmodel.MainTab
import com.auramusic.app.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by playerViewModel.uiState.collectAsState()
            val context = LocalContext.current

            val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    playerViewModel.scanLocalTracks()
                }
            }

            val requestScanLocalAudio = {
                val status = ContextCompat.checkSelfPermission(context, audioPermission)
                if (status == PackageManager.PERMISSION_GRANTED) {
                    playerViewModel.scanLocalTracks()
                } else {
                    permissionLauncher.launch(audioPermission)
                }
            }

            AuraMusicTheme(themeName = uiState.currentTheme) {
                BackHandler(enabled = uiState.isPlayerExpanded) {
                    playerViewModel.setPlayerExpanded(false)
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
                        // Ambient Cyberpunk Neon Glow Blobs
                        Box(
                            modifier = Modifier
                                .size(340.dp)
                                .offset(x = (-90).dp, y = (-60).dp)
                                .blur(100.dp)
                                .background(CyberCyan.copy(alpha = 0.20f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(300.dp)
                                .offset(x = 190.dp, y = 220.dp)
                                .blur(100.dp)
                                .background(NeonPurple.copy(alpha = 0.16f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .offset(x = (-50).dp, y = 560.dp)
                                .blur(110.dp)
                                .background(CyberPink.copy(alpha = 0.14f), CircleShape)
                        )

                        // Main Content View (Studio / Radar / Vault)
                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = uiState.currentTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "tab_transition",
                                modifier = Modifier.weight(1f)
                            ) { tab ->
                                when (tab) {
                                    MainTab.STUDIO -> {
                                        HomeScreen(
                                            featuredTracks = uiState.searchResults,
                                            currentSong = uiState.currentSong,
                                            isPlaying = uiState.isPlaying,
                                            onSelectSong = { song -> playerViewModel.playSong(song) },
                                            onSelectVibe = { vibe ->
                                                playerViewModel.search(vibe)
                                                playerViewModel.selectTab(MainTab.RADAR)
                                            },
                                            onOpenSettings = { playerViewModel.setShowSettings(true) }
                                        )
                                    }
                                    MainTab.RADAR -> {
                                        SearchScreen(
                                            searchResults = uiState.searchResults,
                                            isSearching = uiState.isSearching,
                                            currentSong = uiState.currentSong,
                                            isPlaying = uiState.isPlaying,
                                            onSearch = { q -> playerViewModel.search(q) },
                                            onSelectSong = { song -> playerViewModel.playSong(song) },
                                            onOpenSongOptions = { song -> playerViewModel.openSongOptions(song) }
                                        )
                                    }
                                    MainTab.VAULT -> {
                                        VaultScreen(
                                            favorites = uiState.favorites,
                                            downloads = uiState.downloads,
                                            playlists = uiState.playlists,
                                            localTracks = uiState.localTracks,
                                            isScanningLocal = uiState.isScanningLocal,
                                            currentSong = uiState.currentSong,
                                            isPlaying = uiState.isPlaying,
                                            onSelectSong = { song -> playerViewModel.playSong(song) },
                                            onPlayAllFavorites = { playerViewModel.playAllFavorites() },
                                            onPlayAllDownloads = { playerViewModel.playAllDownloads() },
                                            onPlayAllLocalTracks = { playerViewModel.playAllLocalTracks() },
                                            onScanLocalTracks = requestScanLocalAudio,
                                            onCreatePlaylist = { title -> playerViewModel.createPlaylist(title) },
                                            onDeletePlaylist = { id -> playerViewModel.deletePlaylist(id) },
                                            onOpenPlaylist = { pl -> playerViewModel.openPlaylist(pl) },
                                            onOpenSongOptions = { song -> playerViewModel.openSongOptions(song) }
                                        )
                                    }
                                }
                            }
                        }

                        // Floating Mini-Player & Glass Bottom Dock
                        if (!uiState.isPlayerExpanded) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                            ) {
                                MiniPlayer(
                                    song = uiState.currentSong,
                                    isPlaying = uiState.isPlaying,
                                    onPlayPause = { playerViewModel.playPause() },
                                    onNext = { playerViewModel.next() },
                                    onExpand = { playerViewModel.setPlayerExpanded(true) }
                                )

                                GlassBottomDock(
                                    currentTab = uiState.currentTab,
                                    onSelectTab = { playerViewModel.selectTab(it) }
                                )
                            }
                        }

                        // Full-screen Studio Deck Overlay (Vinyl + Synced Lyrics)
                        AnimatedVisibility(
                            visible = uiState.isPlayerExpanded,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CyberBackgroundDark.copy(alpha = 0.98f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                StudioDeck(
                                    song = uiState.currentSong,
                                    isPlaying = uiState.isPlaying,
                                    currentPositionMs = uiState.currentPositionMs,
                                    durationMs = uiState.durationMs,
                                    isShuffle = uiState.isShuffle,
                                    isRepeat = uiState.isRepeat,
                                    lyrics = uiState.lyrics,
                                    onPlayPause = { playerViewModel.playPause() },
                                    onNext = { playerViewModel.next() },
                                    onPrevious = { playerViewModel.previous() },
                                    onSeekTo = { pos -> playerViewModel.seekTo(pos) },
                                    onToggleFavorite = { playerViewModel.toggleFavorite() },
                                    onToggleShuffle = { playerViewModel.toggleShuffle() },
                                    onToggleRepeat = { playerViewModel.toggleRepeat() },
                                    onOpenEqualizer = { playerViewModel.setShowEqualizer(true) },
                                    onOpenSleepTimer = { playerViewModel.setShowSleepTimer(true) },
                                    onOpenQueue = { playerViewModel.setShowQueue(true) },
                                    sleepTimerRemaining = if (uiState.sleepTimer.remainingSeconds != null) {
                                        val mins = uiState.sleepTimer.remainingSeconds!! / 60
                                        val secs = uiState.sleepTimer.remainingSeconds!! % 60
                                        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
                                    } else if (uiState.sleepTimer.isEndOfTrack) "END" else null,
                                    onCollapse = { playerViewModel.setPlayerExpanded(false) }
                                )
                            }
                        }

                        // Equalizer DSP BottomSheet
                        if (uiState.showEqualizer) {
                            EqualizerSheet(
                                onDismiss = { playerViewModel.setShowEqualizer(false) }
                            )
                        }

                        // Sleep Timer Dialog
                        if (uiState.showSleepTimer) {
                            SleepTimerDialog(
                                sleepTimerState = uiState.sleepTimer,
                                onStartTimer = { mins -> playerViewModel.startSleepTimer(mins) },
                                onSetEndOfTrack = { playerViewModel.setSleepTimerEndOfTrack() },
                                onCancelTimer = { playerViewModel.cancelSleepTimer() },
                                onDismiss = { playerViewModel.setShowSleepTimer(false) }
                            )
                        }

                        // Up Next Queue BottomSheet
                        if (uiState.showQueue) {
                            QueueSheet(
                                queue = uiState.queue,
                                currentIndex = uiState.currentIndex,
                                currentSong = uiState.currentSong,
                                onPlayItem = { idx -> playerViewModel.playQueueItem(idx) },
                                onRemoveItem = { idx -> playerViewModel.removeFromQueue(idx) },
                                onClearQueue = { playerViewModel.clearQueue() },
                                onDismiss = { playerViewModel.setShowQueue(false) }
                            )
                        }

                        // Playlist Detail BottomSheet
                        uiState.selectedPlaylist?.let { pl ->
                            PlaylistDetailSheet(
                                playlist = pl,
                                songs = uiState.playlistSongs,
                                currentSong = uiState.currentSong,
                                isPlaying = uiState.isPlaying,
                                onPlayAll = { playerViewModel.playPlaylist(uiState.playlistSongs, 0, false) },
                                onShufflePlay = { playerViewModel.playPlaylist(uiState.playlistSongs, 0, true) },
                                onPlaySong = { s -> playerViewModel.playSong(s) },
                                onRemoveSong = { sId -> playerViewModel.removeSongFromPlaylist(pl.id, sId) },
                                onOpenSongOptions = { s -> playerViewModel.openSongOptions(s) },
                                onDismiss = { playerViewModel.openPlaylist(null) }
                            )
                        }

                        // Settings Studio BottomSheet
                        if (uiState.showSettings) {
                            SettingsSheet(
                                audioQuality = uiState.audioQuality,
                                loudnessNormalizer = uiState.loudnessNormalizer,
                                currentTheme = uiState.currentTheme,
                                onQualitySelected = { q -> playerViewModel.setAudioQuality(q) },
                                onToggleLoudness = { playerViewModel.toggleLoudnessNormalizer() },
                                onSelectTheme = { themeName -> playerViewModel.setThemePalette(themeName) },
                                onDismiss = { playerViewModel.setShowSettings(false) }
                            )
                        }

                        // Track Options BottomSheet (Download, Add to Playlist, Favorite)
                        uiState.activeOptionSong?.let { song ->
                            val isDownloaded = playerViewModel.isSongDownloaded(song.id)
                            val progress = uiState.downloadProgressMap[song.id] ?: -1

                            TrackOptionsBottomSheet(
                                song = song,
                                playlists = uiState.playlists,
                                isDownloaded = isDownloaded,
                                downloadProgress = progress,
                                onDismiss = { playerViewModel.openSongOptions(null) },
                                onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                                onDownload = { playerViewModel.startDownload(it) },
                                onAddToQueueNext = { playerViewModel.addToQueueNext(it) },
                                onAddToPlaylist = { plId, s -> playerViewModel.addSongToPlaylist(plId, s) },
                                onCreatePlaylist = { name -> playerViewModel.createPlaylist(name) }
                            )
                        }
                    }
                }
            }
        }
    }
}
