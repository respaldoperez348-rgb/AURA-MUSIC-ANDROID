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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.auramusic.app.ui.components.AlbumDetailSheet
import com.auramusic.app.ui.components.ArtistDetailSheet
import com.auramusic.app.ui.components.EqualizerSheet
import com.auramusic.app.ui.components.FullscreenLyricsDialog
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
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        setContent {
            val uiState by playerViewModel.uiState.collectAsState()
            val context = LocalContext.current

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                if (perms[Manifest.permission.READ_MEDIA_AUDIO] == true || perms[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                    playerViewModel.scanLocalTracks()
                }
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                val neededPermissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                if (neededPermissions.isNotEmpty()) {
                    permissionLauncher.launch(neededPermissions.toTypedArray())
                }
            }

            val requestScanLocalAudio = {
                val needed = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.READ_MEDIA_AUDIO)
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                if (needed.isEmpty()) {
                    playerViewModel.scanLocalTracks()
                } else {
                    permissionLauncher.launch(needed.toTypedArray())
                }
            }

            AuraMusicTheme(themeName = uiState.currentTheme) {
                BackHandler(enabled = uiState.isPlayerExpanded) {
                    playerViewModel.setPlayerExpanded(false)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = CyberBackgroundDark
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Subtle Top Ambient Studio Light
                        Box(
                            modifier = Modifier
                                .size(380.dp)
                                .offset(x = (-100).dp, y = (-120).dp)
                                .blur(130.dp)
                                .background(CyberCyan.copy(alpha = 0.08f), CircleShape)
                        )

                        // Main Content View (Studio / Radar / Vault)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
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
                                            popularArtists = uiState.popularArtists,
                                            trendingAlbums = uiState.trendingAlbums,
                                            userTopArtists = uiState.userTopArtists,
                                            popularStations = uiState.popularStations,
                                            appLanguage = uiState.appLanguage,
                                            onSelectSong = { song -> playerViewModel.playSong(song, uiState.searchResults, buildSmartRadio = true) },
                                            onSelectVibe = { vibe ->
                                                playerViewModel.search(vibe)
                                                playerViewModel.selectTab(MainTab.RADAR)
                                            },
                                            onSelectAlbum = { album -> playerViewModel.openAlbumDetail(album) },
                                            onOpenSettings = { playerViewModel.setShowSettings(true) }
                                        )
                                    }
                                    MainTab.RADAR -> {
                                        SearchScreen(
                                            searchResults = uiState.searchResults,
                                            isSearching = uiState.isSearching,
                                            currentSong = uiState.currentSong,
                                            isPlaying = uiState.isPlaying,
                                            searchHistory = uiState.searchHistory,
                                            searchSuggestions = uiState.searchSuggestions,
                                            appLanguage = uiState.appLanguage,
                                            onSearch = { q -> playerViewModel.search(q) },
                                            onFetchSuggestions = { q -> playerViewModel.fetchSearchSuggestions(q) },
                                            onDeleteHistoryItem = { q -> playerViewModel.deleteSearchHistoryItem(q) },
                                            onClearHistory = { playerViewModel.clearSearchHistory() },
                                            onSelectSong = { song -> playerViewModel.playSong(song, uiState.searchResults, buildSmartRadio = true) },
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
                                            appLanguage = uiState.appLanguage,
                                            listeningStats = uiState.listeningStats,
                                            onSelectSong = { song, queue ->
                                                playerViewModel.playSongWithQueue(song, queue, resetShuffle = true)
                                            },
                                            onPlayAllFavorites = { playerViewModel.playAllFavorites() },
                                            onPlayAllDownloads = { playerViewModel.playAllDownloads() },
                                            onPlayAllLocalTracks = { playerViewModel.playAllLocalTracks() },
                                            onScanLocalTracks = requestScanLocalAudio,
                                            onCreatePlaylist = { title -> playerViewModel.createPlaylist(title) },
                                            onImportPlaylist = { queryOrUrl -> playerViewModel.importExternalPlaylist(queryOrUrl) },
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
                                    .navigationBarsPadding()
                                    .padding(bottom = 6.dp)
                            ) {
                                MiniPlayer(
                                    song = uiState.currentSong,
                                    isPlaying = uiState.isPlaying,
                                    currentPositionMs = uiState.currentPositionMs,
                                    durationMs = uiState.durationMs,
                                    isFavorite = uiState.favorites.any { it.id == uiState.currentSong.id },
                                    onToggleFavorite = { playerViewModel.toggleFavorite(uiState.currentSong) },
                                    onPlayPause = { playerViewModel.playPause() },
                                    onNext = { playerViewModel.next() },
                                    onPrevious = { playerViewModel.previous() },
                                    onExpand = { playerViewModel.setPlayerExpanded(true) }
                                )

                                GlassBottomDock(
                                    currentTab = uiState.currentTab,
                                    appLanguage = uiState.appLanguage,
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
                                    lyricsOffsetMs = uiState.lyricsOffsetMs,
                                    appLanguage = uiState.appLanguage,
                                    showTranslation = uiState.showLyricsTranslation,
                                    onToggleTranslation = { playerViewModel.toggleLyricsTranslation() },
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
                                    onOpenFullscreenLyrics = { playerViewModel.setShowFullscreenLyrics(true) },
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
                                onMoveItem = { fromIdx, toIdx -> playerViewModel.moveQueueItem(fromIdx, toIdx) },
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
                                onPlaySong = { s ->
                                    val idx = uiState.playlistSongs.indexOfFirst { it.id == s.id }.coerceAtLeast(0)
                                    playerViewModel.playPlaylist(uiState.playlistSongs, idx, false)
                                },
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
                                autoRadioEnabled = uiState.autoRadioEnabled,
                                skipSilenceEnabled = uiState.skipSilenceEnabled,
                                currentTheme = uiState.currentTheme,
                                appLanguage = uiState.appLanguage,
                                lyricsOffsetMs = uiState.lyricsOffsetMs,
                                onQualitySelected = { q -> playerViewModel.setAudioQuality(q) },
                                onToggleLoudness = { playerViewModel.toggleLoudnessNormalizer() },
                                onToggleAutoRadio = { playerViewModel.toggleAutoRadio() },
                                onToggleSkipSilence = { playerViewModel.toggleSkipSilence() },
                                onSelectTheme = { themeName -> playerViewModel.setThemePalette(themeName) },
                                onSelectLanguage = { lang -> playerViewModel.setAppLanguage(lang) },
                                onSelectLyricsOffset = { offset -> playerViewModel.setLyricsOffsetMs(offset) },
                                onExportBackup = { playerViewModel.exportBackup() },
                                onImportBackup = { playerViewModel.importBackup() },
                                onClearCache = { playerViewModel.clearAppCache() },
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
                                onDeleteDownload = { playerViewModel.deleteDownload(it) },
                                onAddToQueueNext = { playerViewModel.addToQueueNext(it) },
                                onAddToPlaylist = { plId, s -> playerViewModel.addSongToPlaylist(plId, s) },
                                onCreatePlaylist = { name -> playerViewModel.createPlaylist(name) }
                            )
                        }

                        // Fullscreen Synced Karaoke Lyrics Dialog
                        if (uiState.showFullscreenLyrics) {
                            FullscreenLyricsDialog(
                                song = uiState.currentSong,
                                lyrics = uiState.lyrics,
                                isPlaying = uiState.isPlaying,
                                currentPositionMs = uiState.currentPositionMs,
                                durationMs = uiState.durationMs,
                                lyricsOffsetMs = uiState.lyricsOffsetMs,
                                showTranslation = uiState.showLyricsTranslation,
                                onToggleTranslation = { playerViewModel.toggleLyricsTranslation() },
                                onSeekTo = { pos -> playerViewModel.seekTo(pos) },
                                onPlayPause = { playerViewModel.playPause() },
                                onNext = { playerViewModel.next() },
                                onPrevious = { playerViewModel.previous() },
                                onDismiss = { playerViewModel.setShowFullscreenLyrics(false) }
                            )
                        }

                        // Artist Profile Detail BottomSheet
                        uiState.selectedArtist?.let { artist ->
                            ArtistDetailSheet(
                                artist = artist,
                                profile = uiState.artistProfile,
                                artistSongs = uiState.artistTopSongs,
                                isLoading = uiState.isLoadingArtist,
                                currentPlayingId = uiState.currentSong.id,
                                onPlaySong = { s, list -> playerViewModel.playSong(s, list) },
                                onPlayAll = { list -> playerViewModel.playPlaylist(list) },
                                onSelectAlbum = { album -> playerViewModel.openAlbumDetail(album) },
                                onPlayAlbum = { album -> playerViewModel.playAlbum(album) },
                                onDismiss = { playerViewModel.dismissArtistDetail() }
                            )
                        }

                        // Album Detail BottomSheet
                        if (uiState.showAlbumDetail) {
                            uiState.selectedAlbum?.let { album ->
                                AlbumDetailSheet(
                                    album = album,
                                    tracks = uiState.albumTracks,
                                    isLoading = uiState.isLoadingAlbum,
                                    currentPlayingId = uiState.currentSong.id,
                                    onPlaySong = { s, list -> playerViewModel.playSong(s, list) },
                                    onPlayAll = { list -> playerViewModel.playPlaylist(list) },
                                    onDismiss = { playerViewModel.dismissAlbumDetail() }
                                )
                            }
                        }

                        // Cyber HUD Toast / In-app Notification Banner
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.toastMessage != null,
                            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                        ) {
                            uiState.toastMessage?.let { msg ->
                                Box(
                                    modifier = Modifier
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                        .background(com.auramusic.app.ui.theme.CyberBackgroundDark.copy(alpha = 0.95f))
                                        .border(1.5.dp, com.auramusic.app.ui.theme.CyberCyan, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                                        .padding(horizontal = 18.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = msg,
                                        color = com.auramusic.app.ui.theme.CyberCyan,
                                        fontSize = 13.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
