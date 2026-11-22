package com.auramusic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.auramusic.app.data.models.Album
import com.auramusic.app.data.models.Artist
import com.auramusic.app.data.models.PlayerStyleMode
import com.auramusic.app.data.models.Song
import com.auramusic.app.data.repository.MusicRepository
import com.auramusic.app.i18n.ProvideAuraStrings
import com.auramusic.app.ui.components.AudioLabBottomSheet
import com.auramusic.app.ui.components.AuraBottomNavigation
import com.auramusic.app.ui.components.AuraMiniPlayer
import com.auramusic.app.ui.components.AuraSystemAmbientBackground
import com.auramusic.app.ui.components.AuraTab
import com.auramusic.app.ui.components.SongOptionsBottomSheet
import com.auramusic.app.ui.components.YouTubeLoginDialog
import com.auramusic.app.ui.components.rememberCoverVisualData
import com.auramusic.app.ui.screens.AccountScreen
import com.auramusic.app.ui.screens.AlbumScreen
import com.auramusic.app.ui.screens.ArtistScreen
import com.auramusic.app.ui.screens.CategoryDetailScreen
import com.auramusic.app.ui.screens.ExpandedPlayerScreen
import com.auramusic.app.ui.screens.ExploreScreen
import com.auramusic.app.ui.screens.HomeScreen
import com.auramusic.app.ui.screens.ImportMusicScreen
import com.auramusic.app.ui.screens.LibraryScreen
import com.auramusic.app.ui.screens.LoginScreen
import com.auramusic.app.ui.screens.LyricsScreen
import com.auramusic.app.ui.screens.QueueScreen
import com.auramusic.app.ui.screens.RegisterScreen
import com.auramusic.app.ui.screens.SearchScreen
import com.auramusic.app.ui.screens.SettingsScreen
import com.auramusic.app.ui.screens.StatsScreen
import com.auramusic.app.ui.screens.WelcomeScreen
import com.auramusic.app.ui.theme.AuraMusicTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen {
    object Main : Screen()
    object ExpandedPlayer : Screen()
    object Lyrics : Screen()
    object Queue : Screen()
    data class ArtistDetail(val artist: Artist, val isLoading: Boolean = false) : Screen()
    data class AlbumDetail(val album: Album, val isLoading: Boolean = false) : Screen()
    data class CategoryDetail(val categoryTitle: String) : Screen()
    object Settings : Screen()
    object Account : Screen()
    object ImportMusic : Screen()
    object Stats : Screen()
    object Welcome : Screen()
    object Login : Screen()
    object Register : Screen()
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val modes = display?.supportedModes
                val highest = modes?.maxByOrNull { it.refreshRate }
                if (highest != null && highest.refreshRate > 60f) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = highest.modeId
                    }
                }
            }
        }
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            AuraMusicApp()
        }
    }

    override fun onResume() {
        super.onResume()
        AuraApplication.playerManager.setUiVisible(true)
    }

    override fun onStop() {
        super.onStop()
        AuraApplication.playerManager.setUiVisible(false)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun AuraMusicApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerManager = AuraApplication.playerManager
    val repository = AuraApplication.repository
    val userManager = AuraApplication.userManager

    val uiSongState by playerManager.uiSongState.collectAsState()
    val currentUser by userManager.currentUser.collectAsState()
    val hasEnteredApp by userManager.hasEnteredApp.collectAsState()
    val favorites by userManager.favorites.collectAsState()
    val followedArtists by userManager.followedArtists.collectAsState()
    val savedAlbums by userManager.savedAlbums.collectAsState()
    val downloadedSongs by userManager.downloadedSongs.collectAsState()
    val userPlaylists by userManager.userPlaylists.collectAsState()
    val mostListenedSongs by userManager.mostListenedSongs.collectAsState()
    val currentAppIcon by userManager.currentAppIcon.collectAsState()
    val userSettings by userManager.userSettings.collectAsState()
    val toastMessage by userManager.toastMessage.collectAsState()

    var currentScreen by remember {
        mutableStateOf<Screen>(
            if (userManager.currentUser.value == null && !userManager.hasEnteredApp.value) Screen.Welcome else Screen.Main
        )
    }

    // Pila de navegación real para que al volver desde un Álbum regrese al Artista (y no se salga a Main)
    val screenStack = remember { mutableStateListOf<Screen>() }

    val navigateTo: (Screen) -> Unit = { destination ->
        val canPushCurrent = currentScreen !is Screen.Welcome ||
            destination is Screen.Login ||
            destination is Screen.Register ||
            destination is Screen.ImportMusic
        if (canPushCurrent && currentScreen != destination) {
            screenStack.add(currentScreen)
        }
        currentScreen = destination
    }

    val navigateBack: () -> Unit = {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeAt(screenStack.lastIndex)
        } else {
            currentScreen = if (currentUser == null && !hasEnteredApp) Screen.Welcome else Screen.Main
        }
    }

    // Interceptar botón/gesto físico Atrás de Android cuando no estamos en la pantalla raíz
    BackHandler(enabled = currentScreen !is Screen.Main && currentScreen !is Screen.Welcome) {
        navigateBack()
    }

    LaunchedEffect(currentUser, hasEnteredApp) {
        if ((currentUser != null || hasEnteredApp) && currentScreen is Screen.Welcome) {
            screenStack.clear()
            currentScreen = Screen.Main
        }
    }

    var currentTab by remember { mutableStateOf(AuraTab.HOME) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showGoogleLoginDialog by remember { mutableStateOf(false) }
    var showAudioLabSheet by remember { mutableStateOf(false) }

    var quickPicks by remember { mutableStateOf(MusicRepository.getCuratedQuickPicks()) }
    var trendingSongs by remember { mutableStateOf(MusicRepository.getCuratedQuickPicks()) }
    var exploreSongs by remember { mutableStateOf(MusicRepository.getCuratedQuickPicks()) }
    val basePopularArtists = remember { MusicRepository.getCuratedPopularArtists() }
    val popularArtists = remember(basePopularArtists, quickPicks, trendingSongs) {
        MusicRepository.buildExpandedArtistCatalog(basePopularArtists, quickPicks + trendingSongs)
    }
    var featuredAlbums by remember { mutableStateOf(MusicRepository.getCuratedFeaturedAlbums()) }

    val displayQuickPicks = remember(quickPicks, userSettings.artistFeedMode) {
        MusicRepository.applyArtistFeedMode(quickPicks, userSettings.artistFeedMode)
    }
    val displayExploreSongs = remember(exploreSongs, userSettings.artistFeedMode) {
        MusicRepository.applyArtistFeedMode(exploreSongs, userSettings.artistFeedMode)
    }

    var searchResults by remember { mutableStateOf(quickPicks) }
    var searchArtists by remember { mutableStateOf(popularArtists) }
    var searchAlbums by remember { mutableStateOf(featuredAlbums) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var suggestionsJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(popularArtists) {
        if (!isSearching && searchArtists.size <= basePopularArtists.size) {
            searchArtists = popularArtists
        }
    }

    // Cargar feed en vivo al iniciar y pre-resolver las primeras canciones para Play instantáneo
    LaunchedEffect(currentUser) {
        launch {
            val liveHome = repository.fetchHomeFeed()
            if (liveHome.isNotEmpty()) {
                quickPicks = liveHome
                if (exploreSongs == MusicRepository.getCuratedQuickPicks()) {
                    exploreSongs = liveHome
                }
                if (searchResults == MusicRepository.getCuratedQuickPicks()) {
                    searchResults = liveHome
                }
                if (userSettings.fastPlaybackStart) {
                    liveHome.take(2).forEach { song ->
                        launch { repository.prefetchStreamUrl(song.id) }
                    }
                }
            }
        }
        launch {
            val liveTrending = repository.searchSongs("Top Hits Globales 2025")
            if (liveTrending.isNotEmpty()) {
                trendingSongs = liveTrending
            }
        }
        launch {
            val liveAlbums = repository.fetchLiveFeaturedAlbums()
            if (liveAlbums.isNotEmpty()) {
                featuredAlbums = liveAlbums
                if (!isSearching) {
                    searchAlbums = liveAlbums
                }
            }
        }
    }

    // Reproducir canción respetando orden exacto (shuffleOrder = false) o aleatorio explícito (shuffleOrder = true)
    val playSongAndRecord: (Song, List<Song>, Boolean?, Boolean) -> Unit = { song, queueList, shuffleOrder, isUserCollection ->
        val cleanQueue = if (queueList.isNotEmpty()) queueList else listOf(song)
        playerManager.playSong(
            song = song,
            queue = cleanQueue,
            shuffleOrder = shuffleOrder,
            isUserCollection = isUserCollection
        )
        userManager.recordSongPlay(song)

        coroutineScope.launch {
            // Diferir actualización de Explore para no competir por red/CPU con el inicio inmediato del audio
            delay(2500L)
            val related = repository.fetchRelatedTracks(song.id, song.title, song.artist)
            if (related.isNotEmpty()) {
                exploreSongs = (listOf(song) + related).distinctBy { it.id }
            }
        }
    }

    // Abrir Artista empujando la pantalla anterior en la pila
    val openArtistScreen: (Artist) -> Unit = { artist ->
        navigateTo(Screen.ArtistDetail(artist, isLoading = artist.topSongs.isEmpty()))
        coroutineScope.launch {
            val fullArtist = repository.getArtistDetails(artist.id, artist) ?: artist
            if (currentScreen is Screen.ArtistDetail) {
                currentScreen = Screen.ArtistDetail(fullArtist, isLoading = false)
            }
        }
    }

    // Abrir Álbum empujando la pantalla anterior (ej. Artista) en la pila para que Atrás regrese al Artista
    val openAlbumScreen: (Album) -> Unit = { album ->
        navigateTo(Screen.AlbumDetail(album, isLoading = album.songs.isEmpty()))
        coroutineScope.launch {
            val fullAlbum = repository.getAlbumDetails(album.id, album) ?: album
            if (currentScreen is Screen.AlbumDetail) {
                currentScreen = Screen.AlbumDetail(fullAlbum, isLoading = false)
            }
        }
    }

    val isCurrentSongFavorite = remember(uiSongState.currentSong, favorites) {
        uiSongState.currentSong?.let { cur ->
            userManager.isSongFavorite(cur)
        } ?: false
    }

    AuraMusicTheme(
        appTheme = userSettings.theme,
        accentColor = userSettings.accentColor
    ) {
        ProvideAuraStrings(languageOption = userSettings.language) {
        val activeSong = uiSongState.currentSong
        val sysVisual = rememberCoverVisualData(
            songId = activeSong?.id ?: "none",
            thumbnailUrl = activeSong?.thumbnailUrl.orEmpty(),
            fallbackColor = MaterialTheme.colorScheme.primary,
            computeBlurBitmap = false
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AuraSystemAmbientBackground(
                haloColor = sysVisual.haloColor,
                secondaryColor = sysVisual.secondaryColor,
                isPlaying = uiSongState.isPlaying,
                enabled = userSettings.dynamicSystemBackdrop && activeSong != null
            )
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    val pagerState = rememberPagerState(
                        initialPage = currentTab.ordinal,
                        pageCount = { AuraTab.entries.size }
                    )
                    LaunchedEffect(pagerState.settledPage) {
                        val targetTab = AuraTab.entries[pagerState.settledPage]
                        if (currentTab != targetTab) {
                            currentTab = targetTab
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1,
                            key = { AuraTab.entries[it].name },
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (AuraTab.entries[page]) {
                                AuraTab.HOME -> {
                                    HomeScreen(
                                        userName = currentUser?.name,
                                        userAvatarUrl = currentUser?.avatarUrl.orEmpty(),
                                        currentAppIcon = currentAppIcon,
                                        currentPlayingSong = uiSongState.currentSong,
                                        isPlaying = uiSongState.isPlaying,
                                        quickPicks = displayQuickPicks,
                                        topTracks = mostListenedSongs,
                                        popularArtists = popularArtists,
                                        featuredAlbums = featuredAlbums,
                                        trendingSongs = trendingSongs,
                                        onSongClick = { song ->
                                            playSongAndRecord(song, displayQuickPicks, false, false)
                                        },
                                        onTogglePlayPause = { playerManager.togglePlayPause() },
                                        onArtistClick = { artist -> openArtistScreen(artist) },
                                        onAlbumClick = { album -> openAlbumScreen(album) },
                                        onSongOptionsClick = { selectedSongForOptions = it },
                                        onProfileClick = { navigateTo(Screen.Settings) }
                                    )
                                }
                                AuraTab.EXPLORE -> {
                                    ExploreScreen(
                                        realSongs = displayExploreSongs,
                                        currentPlayingSong = uiSongState.currentSong,
                                        isPlaying = uiSongState.isPlaying,
                                        onGenreFilterSelected = { genre ->
                                            coroutineScope.launch {
                                                if (genre == "Para ti") {
                                                    val cur = uiSongState.currentSong
                                                    if (cur != null) {
                                                        val rel = repository.fetchRelatedTracks(cur.id, cur.title, cur.artist)
                                                        if (rel.isNotEmpty()) exploreSongs = rel
                                                    } else {
                                                        exploreSongs = quickPicks
                                                    }
                                                } else {
                                                    val genreTracks = repository.searchSongs("$genre música éxitos")
                                                    if (genreTracks.isNotEmpty()) {
                                                        exploreSongs = genreTracks
                                                    }
                                                }
                                            }
                                        },
                                        onSongSelected = { song ->
                                            playSongAndRecord(song, displayExploreSongs, false, false)
                                        },
                                        onSongOptionsClick = { selectedSongForOptions = it },
                                        onOpenSettings = { navigateTo(Screen.Settings) }
                                    )
                                }
                                AuraTab.LIBRARY -> {
                                    LibraryScreen(
                                        favoritesCount = favorites.size,
                                        followingCount = followedArtists.size,
                                        albumsCount = savedAlbums.size,
                                        downloadsCount = downloadedSongs.size,
                                        playlistsCount = userPlaylists.size,
                                        offlineMode = userSettings.offlineMode,
                                        userPlaylists = userPlaylists,
                                        recentAlbums = savedAlbums,
                                        recentFavorites = favorites,
                                        onAlbumClick = { openAlbumScreen(it) },
                                        onPlaylistClick = { playlist ->
                                            navigateTo(
                                                Screen.AlbumDetail(
                                                    Album(
                                                        id = playlist.id,
                                                        title = playlist.title,
                                                        artist = "Mi Biblioteca",
                                                        year = null,
                                                        thumbnailUrl = playlist.songs.firstOrNull()?.thumbnailUrl.orEmpty(),
                                                        songCount = playlist.songs.size,
                                                        songs = playlist.songs
                                                    ),
                                                    isLoading = false
                                                )
                                            )
                                        },
                                        onSongClick = { song -> playSongAndRecord(song, favorites, false, true) },
                                        onSongOptionsClick = { selectedSongForOptions = it },
                                        onCategoryClick = { category ->
                                            navigateTo(Screen.CategoryDetail(category))
                                        },
                                        onCreatePlaylist = { title -> userManager.createPlaylist(title) },
                                        onImportClick = { navigateTo(Screen.ImportMusic) },
                                        onStatsClick = { navigateTo(Screen.Stats) },
                                        onToggleOfflineMode = {
                                            val nextOffline = !userSettings.offlineMode
                                            userManager.updateSettings(userSettings.copy(offlineMode = nextOffline))
                                            userManager.showToast(
                                                if (nextOffline) "Modo Solo Offline activado" else "Modo Online activado"
                                            )
                                        }
                                    )
                                }
                                AuraTab.SEARCH -> {
                                    SearchScreen(
                                        artists = searchArtists,
                                        albums = searchAlbums,
                                        songs = searchResults,
                                        suggestions = searchSuggestions,
                                        isSearching = isSearching,
                                        isSongFavorite = { song -> userManager.isSongFavorite(song) },
                                        onToggleFavorite = { song -> userManager.toggleFavorite(song) },
                                        onAlreadyInFavoritesNotice = {
                                            userManager.showToast("Esta música ya se encuentra disponible en favoritos")
                                        },
                                        onSearch = { query ->
                                            searchJob?.cancel()
                                            suggestionsJob?.cancel()
                                            if (query.isNotBlank()) {
                                                suggestionsJob = coroutineScope.launch {
                                                    searchSuggestions = repository.getSearchSuggestions(query)
                                                }
                                                searchJob = coroutineScope.launch {
                                                    delay(260)
                                                    isSearching = true
                                                    val songsDeferred = launch {
                                                        val songs = repository.searchSongs(query)
                                                        if (songs.isNotEmpty()) searchResults = songs
                                                    }
                                                    val artistsDeferred = launch {
                                                        val artists = repository.searchArtists(query)
                                                        if (artists.isNotEmpty()) searchArtists = artists
                                                    }
                                                    val albumsDeferred = launch {
                                                        val albums = repository.searchAlbums(query)
                                                        if (albums.isNotEmpty()) searchAlbums = albums
                                                    }
                                                    songsDeferred.join()
                                                    artistsDeferred.join()
                                                    albumsDeferred.join()
                                                    isSearching = false
                                                }
                                            } else {
                                                isSearching = false
                                                searchSuggestions = emptyList()
                                                searchResults = quickPicks
                                                searchArtists = popularArtists
                                                searchAlbums = featuredAlbums
                                            }
                                        },
                                        onSongClick = { song ->
                                            playSongAndRecord(song, searchResults, false, false)
                                        },
                                        onArtistClick = { artist -> openArtistScreen(artist) },
                                        onAlbumClick = { album -> openAlbumScreen(album) },
                                        onSongOptionsClick = { selectedSongForOptions = it }
                                    )
                                }
                            }
                        }

                        // Capa flotante inferior: MiniPlayer deslizable + Barra segmentada sincronizada
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ActiveMiniPlayerSlot(
                                playerManager = playerManager,
                                isFavorite = isCurrentSongFavorite,
                                onFavoriteClick = { song -> userManager.toggleFavorite(song) },
                                onExpandClick = { navigateTo(Screen.ExpandedPlayer) }
                            )

                            AuraBottomNavigation(
                                selectedTab = currentTab,
                                onTabSelected = { target ->
                                    currentTab = target
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(target.ordinal)
                                    }
                                }
                            )
                        }
                    }
                }
                is Screen.CategoryDetail -> {
                    CategoryDetailScreen(
                        title = screen.categoryTitle,
                        songs = when (screen.categoryTitle) {
                            "Favorito" -> favorites
                            "Descargados" -> downloadedSongs
                            else -> emptyList()
                        },
                        artists = if (screen.categoryTitle == "Seguidos") followedArtists else emptyList(),
                        albums = when (screen.categoryTitle) {
                            "Álbumes" -> savedAlbums
                            "Playlist" -> userPlaylists.map {
                                Album(
                                    id = it.id,
                                    title = it.title,
                                    artist = "Mi Biblioteca",
                                    year = null,
                                    thumbnailUrl = it.songs.firstOrNull()?.thumbnailUrl.orEmpty(),
                                    songCount = it.songs.size,
                                    songs = it.songs
                                )
                            }
                            else -> emptyList()
                        },
                        onBackClick = { navigateBack() },
                        onCreatePlaylist = { title -> userManager.createPlaylist(title) },
                        onDeletePlaylist = { plId -> userManager.deletePlaylist(plId) },
                        onSongClick = { song, list ->
                            // En Favoritos y Descargados, tocar una canción o "Reproducir" reproduce en orden exacto
                            playSongAndRecord(song, list, false, true)
                        },
                        onShuffleClick = { list ->
                            list.randomOrNull()?.let { randomFirst ->
                                playSongAndRecord(randomFirst, list, true, true)
                            }
                        },
                        onArtistClick = { artist -> openArtistScreen(artist) },
                        onAlbumClick = { album ->
                            if (album.id.startsWith("pl_")) {
                                navigateTo(Screen.AlbumDetail(album, isLoading = false))
                            } else {
                                openAlbumScreen(album)
                            }
                        },
                        onSongOptionsClick = { selectedSongForOptions = it }
                    )
                }
                is Screen.ExpandedPlayer -> {
                    val livePlaybackState by playerManager.playbackState.collectAsState()
                    ExpandedPlayerScreen(
                        playbackState = livePlaybackState,
                        backdropMode = userSettings.playerBackdropMode,
                        styleMode = userSettings.playerStyleMode,
                        isFavorite = isCurrentSongFavorite,
                        onToggleFavorite = {
                            livePlaybackState.currentSong?.let { userManager.toggleFavorite(it) }
                        },
                        onCycleStyle = {
                            val all = PlayerStyleMode.entries
                            val next = all[(all.indexOf(userSettings.playerStyleMode) + 1) % all.size]
                            userManager.updateSettings(userSettings.copy(playerStyleMode = next))
                            userManager.showToast("Estilo: ${next.title}")
                        },
                        onPlayPause = { playerManager.togglePlayPause() },
                        onNext = { playerManager.playNext() },
                        onPrevious = { playerManager.playPrevious() },
                        onShuffle = { playerManager.toggleShuffle() },
                        onRepeat = { playerManager.toggleRepeat() },
                        onSeek = { fraction ->
                            playerManager.seekTo((fraction * livePlaybackState.durationMs).toLong())
                        },
                        onBackClick = { navigateBack() },
                        onLyricsClick = { navigateTo(Screen.Lyrics) },
                        onQueueClick = { navigateTo(Screen.Queue) },
                        onAudioLabClick = { showAudioLabSheet = true },
                        onOptionsClick = { selectedSongForOptions = it }
                    )
                }
                is Screen.Lyrics -> {
                    val livePlaybackState by playerManager.playbackState.collectAsState()
                    livePlaybackState.currentSong?.let { song ->
                        LyricsScreen(
                            song = song,
                            playbackState = livePlaybackState,
                            backdropMode = userSettings.playerBackdropMode,
                            isFavorite = isCurrentSongFavorite,
                            onToggleFavorite = { userManager.toggleFavorite(song) },
                            onPlayPause = { playerManager.togglePlayPause() },
                            onNext = { playerManager.playNext() },
                            onPrevious = { playerManager.playPrevious() },
                            onShuffle = { playerManager.toggleShuffle() },
                            onRepeat = { playerManager.toggleRepeat() },
                            onSeek = { fraction ->
                                playerManager.seekTo((fraction * livePlaybackState.durationMs).toLong())
                            },
                            onAdjustLyricsOffset = { delta ->
                                playerManager.adjustLyricsOffset(delta)
                            },
                            onHideLyrics = { navigateBack() }
                        )
                    }
                }
                is Screen.Queue -> {
                    QueueScreen(
                        currentSong = uiSongState.currentSong,
                        queue = uiSongState.queue,
                        onSongClick = { song ->
                            playSongAndRecord(song, uiSongState.queue, null, false)
                        },
                        onMoveSong = { songId, dir -> playerManager.moveQueueItem(songId, dir) },
                        onRemoveSong = { songId -> playerManager.removeFromQueue(songId) },
                        onClearUpcoming = { playerManager.clearUpcomingQueue() },
                        onBackClick = { navigateBack() }
                    )
                }
                is Screen.ArtistDetail -> {
                    val isFollowing = followedArtists.any {
                        it.id == screen.artist.id || it.name.equals(screen.artist.name, ignoreCase = true)
                    }
                    ArtistScreen(
                        artist = screen.artist,
                        isFollowing = isFollowing,
                        isLoading = screen.isLoading,
                        currentPlayingSongId = uiSongState.currentSong?.id,
                        currentAppIcon = currentAppIcon,
                        onBackClick = { navigateBack() },
                        onFollowToggle = {
                            userManager.toggleFollowArtist(screen.artist)
                        },
                        onPlayAllClick = {
                            screen.artist.topSongs.firstOrNull()?.let {
                                playSongAndRecord(it, screen.artist.topSongs, false, false)
                            }
                        },
                        onShufflePlayClick = {
                            screen.artist.topSongs.randomOrNull()?.let {
                                playSongAndRecord(it, screen.artist.topSongs, true, false)
                            }
                        },
                        onAlbumClick = { album -> openAlbumScreen(album) },
                        onPlayAlbumClick = { album ->
                            coroutineScope.launch {
                                val fullAlbum = repository.getAlbumDetails(album.id, album) ?: album
                                fullAlbum.songs.firstOrNull()?.let {
                                    playSongAndRecord(it, fullAlbum.songs, false, true)
                                }
                            }
                        },
                        onSongClick = { song ->
                            playSongAndRecord(song, screen.artist.topSongs, false, false)
                        },
                        onSongOptionsClick = { selectedSongForOptions = it },
                        onRelatedArtistClick = { relArtist -> openArtistScreen(relArtist) }
                    )
                }
                is Screen.AlbumDetail -> {
                    val activePlaylist = userPlaylists.find { it.id == screen.album.id }
                    val effectiveAlbum = if (activePlaylist != null) {
                        screen.album.copy(
                            title = activePlaylist.title,
                            songCount = activePlaylist.songs.size,
                            songs = activePlaylist.songs,
                            thumbnailUrl = activePlaylist.songs.firstOrNull()?.thumbnailUrl.orEmpty()
                        )
                    } else {
                        screen.album
                    }
                    val isSaved = savedAlbums.any { it.id == effectiveAlbum.id }
                    AlbumScreen(
                        album = effectiveAlbum,
                        isSaved = isSaved,
                        isLoading = screen.isLoading,
                        onBackClick = { navigateBack() },
                        onSaveToggle = {
                            userManager.toggleSaveAlbum(effectiveAlbum)
                        },
                        onDownloadAlbum = { songs ->
                            songs.forEach { userManager.addDownload(it) }
                        },
                        onPlaySong = { song, songs ->
                            playSongAndRecord(song, songs, false, true)
                        },
                        onShuffleAlbum = { songs ->
                            songs.randomOrNull()?.let { randomSong ->
                                playSongAndRecord(randomSong, songs, true, true)
                            }
                        },
                        onSongOptionsClick = { selectedSongForOptions = it }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(
                        currentUser = currentUser,
                        currentSettings = userSettings,
                        selectedAppIcon = currentAppIcon,
                        onAppIconSelected = { userManager.setAppIcon(it) },
                        onSettingsChanged = { updated ->
                            val shuffleChanged = updated.defaultShuffle != userSettings.defaultShuffle
                            userManager.updateSettings(updated)
                            if (shuffleChanged) {
                                playerManager.syncShuffleFromSettings(updated.defaultShuffle)
                            }
                        },
                        onBackClick = { navigateBack() },
                        onAccountClick = { navigateTo(Screen.Account) },
                        onOpenAudioLab = { showAudioLabSheet = true },
                        onOpenImportMusic = { navigateTo(Screen.ImportMusic) }
                    )
                }
                is Screen.ImportMusic -> {
                    ImportMusicScreen(
                        onBackClick = { navigateBack() },
                        onPlayLocalSongs = { song, list -> playSongAndRecord(song, list, false, true) }
                    )
                }
                is Screen.Stats -> {
                    StatsScreen(
                        onBackClick = { navigateBack() },
                        onSongClick = { song, list -> playSongAndRecord(song, list, false, true) }
                    )
                }
                is Screen.Account -> {
                    AccountScreen(
                        currentUser = currentUser,
                        onBackClick = { navigateBack() },
                        onLinkGoogle = { showGoogleLoginDialog = true },
                        onChangeAccount = { navigateTo(Screen.Login) },
                        onLogout = {
                            userManager.logout()
                            screenStack.clear()
                            currentScreen = Screen.Welcome
                        }
                    )
                }
                is Screen.Welcome -> {
                    WelcomeScreen(
                        onEnterClick = {
                            userManager.enterAsGuest()
                            screenStack.clear()
                            currentScreen = Screen.Main
                        },
                        onImportClick = { navigateTo(Screen.ImportMusic) },
                        onLoginClick = { navigateTo(Screen.Login) },
                        onRegisterClick = { navigateTo(Screen.Register) },
                        onGoogleClick = { showGoogleLoginDialog = true }
                    )
                }
                is Screen.Login -> {
                    LoginScreen(
                        onLoginSuccess = { name, email ->
                            userManager.login(name, email, isGoogle = false)
                            screenStack.clear()
                            currentScreen = Screen.Main
                        },
                        onRegisterClick = { navigateTo(Screen.Register) },
                        onGoogleClick = { showGoogleLoginDialog = true }
                    )
                }
                is Screen.Register -> {
                    RegisterScreen(
                        onRegisterSuccess = { name, email ->
                            userManager.login(name, email, isGoogle = false)
                            screenStack.clear()
                            currentScreen = Screen.Main
                        },
                        onLoginClick = { navigateTo(Screen.Login) },
                        onGoogleClick = { showGoogleLoginDialog = true },
                        onBackClick = { navigateBack() }
                    )
                }
            }

            // Aviso flotante superior (Toast / Banner informativo) con statusBarsPadding para nunca tapar el reloj
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 24.dp, end = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = toastMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }

            // Diálogo real de inicio de sesión con Google / YouTube Music
            if (showGoogleLoginDialog) {
                YouTubeLoginDialog(
                    onLoginCaptured = { cookie ->
                        showGoogleLoginDialog = false
                        userManager.loginWithGoogleCookie(cookie) {
                            screenStack.clear()
                            currentScreen = Screen.Main
                        }
                    },
                    onDismiss = { showGoogleLoginDialog = false }
                )
            }

            // Bottom sheet de opciones de canción
            selectedSongForOptions?.let { song ->
                val isSongFav = favorites.any {
                    it.id == song.id ||
                        (it.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                            it.artist.trim().equals(song.artist.trim(), ignoreCase = true))
                }
                val isSongDl = downloadedSongs.any {
                    it.id == song.id ||
                        (it.title.trim().equals(song.title.trim(), ignoreCase = true) &&
                            it.artist.trim().equals(song.artist.trim(), ignoreCase = true))
                }
                val currentOpenPlaylistId = (currentScreen as? Screen.AlbumDetail)?.album?.id?.takeIf { it.startsWith("pl_") }
                SongOptionsBottomSheet(
                    song = song,
                    isFavorite = isSongFav,
                    isDownloaded = isSongDl,
                    playlists = userPlaylists,
                    currentPlaylistId = currentOpenPlaylistId,
                    onDismiss = { selectedSongForOptions = null },
                    onPlayNext = { target -> playerManager.playNextInQueue(target) },
                    onAddToQueue = { target -> playerManager.addToQueue(target) },
                    onAddToFavorites = {
                        userManager.toggleFavorite(it)
                    },
                    onAddToPlaylist = { playlistTitle, targetSong ->
                        userManager.addToPlaylist(playlistTitle, targetSong)
                    },
                    onRemoveFromCurrentPlaylist = { plId, targetSong ->
                        userManager.removeSongFromPlaylist(plId, targetSong.id)
                    },
                    onDownload = {
                        userManager.addDownload(it)
                    },
                    onRemoveDownload = {
                        userManager.removeDownload(it)
                    },
                    onShare = {
                        // Compartir ÚNICAMENTE el enlace oficial de la canción
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${it.id}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    }
                )
            }

            if (showAudioLabSheet) {
                AudioLabBottomSheet(
                    audioLab = playerManager.audioLab,
                    onSetSpeedAndPitch = { speed, pitch ->
                        playerManager.setSpeedAndPitch(speed, pitch)
                    },
                    onStartSleepTimer = { mins, stopAfterSong ->
                        playerManager.startSleepTimer(mins, stopAfterSong)
                    },
                    onDismiss = { showAudioLabSheet = false }
                )
            }
        }
        }
        }
    }
}

@Composable
private fun ActiveMiniPlayerSlot(
    playerManager: com.auramusic.app.playback.PlayerManager,
    isFavorite: Boolean,
    onFavoriteClick: (Song) -> Unit,
    onExpandClick: () -> Unit,
) {
    val playbackState by playerManager.playbackState.collectAsState()
    val song = playbackState.currentSong ?: return
    val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs).coerceIn(0f, 1f)
    } else 0f

    AuraMiniPlayer(
        song = song,
        isPlaying = playbackState.isPlaying,
        isFavorite = isFavorite,
        progress = progress,
        onPlayPauseClick = { playerManager.togglePlayPause() },
        onFavoriteClick = { onFavoriteClick(song) },
        onNextClick = { playerManager.playNext() },
        onPreviousClick = { playerManager.playPrevious() },
        onClick = onExpandClick,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}
