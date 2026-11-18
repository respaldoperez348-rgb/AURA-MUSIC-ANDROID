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
import com.auramusic.app.data.models.Song
import com.auramusic.app.data.repository.MusicRepository
import com.auramusic.app.ui.components.AuraBottomNavigation
import com.auramusic.app.ui.components.AuraMiniPlayer
import com.auramusic.app.ui.components.AuraTab
import com.auramusic.app.ui.components.SongOptionsBottomSheet
import com.auramusic.app.ui.components.YouTubeLoginDialog
import com.auramusic.app.ui.screens.AccountScreen
import com.auramusic.app.ui.screens.AlbumScreen
import com.auramusic.app.ui.screens.ArtistScreen
import com.auramusic.app.ui.screens.CategoryDetailScreen
import com.auramusic.app.ui.screens.ExpandedPlayerScreen
import com.auramusic.app.ui.screens.ExploreScreen
import com.auramusic.app.ui.screens.HomeScreen
import com.auramusic.app.ui.screens.LibraryScreen
import com.auramusic.app.ui.screens.LoginScreen
import com.auramusic.app.ui.screens.LyricsScreen
import com.auramusic.app.ui.screens.QueueScreen
import com.auramusic.app.ui.screens.RegisterScreen
import com.auramusic.app.ui.screens.SearchScreen
import com.auramusic.app.ui.screens.SettingsScreen
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
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            AuraMusicApp()
        }
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

    val playbackState by playerManager.playbackState.collectAsState()
    val currentUser by userManager.currentUser.collectAsState()
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
            if (userManager.currentUser.value == null) Screen.Welcome else Screen.Main
        )
    }

    // Pila de navegación real para que al volver desde un Álbum regrese al Artista (y no se salga a Main)
    val screenStack = remember { mutableStateListOf<Screen>() }

    val navigateTo: (Screen) -> Unit = { destination ->
        if (currentScreen !is Screen.Welcome && currentScreen != destination) {
            screenStack.add(currentScreen)
        }
        currentScreen = destination
    }

    val navigateBack: () -> Unit = {
        if (screenStack.isNotEmpty()) {
            currentScreen = screenStack.removeAt(screenStack.lastIndex)
        } else {
            currentScreen = Screen.Main
        }
    }

    // Interceptar botón/gesto físico Atrás de Android cuando no estamos en la pantalla raíz
    BackHandler(enabled = currentScreen !is Screen.Main && currentScreen !is Screen.Welcome) {
        navigateBack()
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null && currentScreen is Screen.Welcome) {
            screenStack.clear()
            currentScreen = Screen.Main
        }
    }

    var currentTab by remember { mutableStateOf(AuraTab.HOME) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showGoogleLoginDialog by remember { mutableStateOf(false) }

    var quickPicks by remember { mutableStateOf(MusicRepository.getCuratedQuickPicks()) }
    var trendingSongs by remember { mutableStateOf(MusicRepository.getCuratedQuickPicks()) }
    var exploreSongs by remember { mutableStateOf(MusicRepository.getCuratedQuickPicks()) }
    val popularArtists = remember { MusicRepository.getCuratedPopularArtists() }
    val featuredAlbums = remember { MusicRepository.getCuratedFeaturedAlbums() }

    var searchResults by remember { mutableStateOf(quickPicks) }
    var searchArtists by remember { mutableStateOf(popularArtists) }
    var searchAlbums by remember { mutableStateOf(featuredAlbums) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var suggestionsJob by remember { mutableStateOf<Job?>(null) }

    // Cargar feed en vivo al iniciar
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
            }
        }
        launch {
            val liveTrending = repository.searchSongs("Top Hits Globales 2025")
            if (liveTrending.isNotEmpty()) {
                trendingSongs = liveTrending
            }
        }
    }

    // Reproducir canción, registrar en "Tus más escuchados" y actualizar Explore con música relacionada del mismo estilo/género
    val playSongAndRecord: (Song, List<Song>) -> Unit = { song, queueList ->
        val cleanQueue = if (queueList.isNotEmpty()) queueList else listOf(song)
        playerManager.playSong(song, cleanQueue)
        userManager.recordSongPlay(song)

        coroutineScope.launch {
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

    val isCurrentSongFavorite = playbackState.currentSong?.let { cur ->
        userManager.isSongFavorite(cur)
    } ?: false

    AuraMusicTheme(
        appTheme = userSettings.theme,
        accentColor = userSettings.accentColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            AuraTab.HOME -> {
                                HomeScreen(
                                    userName = currentUser?.name,
                                    userAvatarUrl = currentUser?.avatarUrl.orEmpty(),
                                    currentAppIcon = currentAppIcon,
                                    currentPlayingSong = playbackState.currentSong,
                                    isPlaying = playbackState.isPlaying,
                                    quickPicks = quickPicks,
                                    topTracks = mostListenedSongs,
                                    popularArtists = popularArtists,
                                    featuredAlbums = featuredAlbums,
                                    trendingSongs = trendingSongs,
                                    onSongClick = { song ->
                                        playSongAndRecord(song, quickPicks)
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
                                    realSongs = exploreSongs,
                                    currentPlayingSong = playbackState.currentSong,
                                    isPlaying = playbackState.isPlaying,
                                    onGenreFilterSelected = { genre ->
                                        coroutineScope.launch {
                                            if (genre == "Para ti") {
                                                val cur = playbackState.currentSong
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
                                        playSongAndRecord(song, exploreSongs)
                                    },
                                    onSongOptionsClick = { selectedSongForOptions = it }
                                )
                            }
                            AuraTab.LIBRARY -> {
                                LibraryScreen(
                                    favoritesCount = favorites.size,
                                    followingCount = followedArtists.size,
                                    albumsCount = savedAlbums.size,
                                    downloadsCount = downloadedSongs.size,
                                    playlistsCount = userPlaylists.size,
                                    recentAlbums = savedAlbums,
                                    recentFavorites = favorites,
                                    onAlbumClick = { openAlbumScreen(it) },
                                    onSongClick = { song -> playSongAndRecord(song, favorites) },
                                    onSongOptionsClick = { selectedSongForOptions = it },
                                    onCategoryClick = { category ->
                                        navigateTo(Screen.CategoryDetail(category))
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
                                        playSongAndRecord(song, searchResults)
                                    },
                                    onArtistClick = { artist -> openArtistScreen(artist) },
                                    onAlbumClick = { album -> openAlbumScreen(album) },
                                    onSongOptionsClick = { selectedSongForOptions = it }
                                )
                            }
                        }

                        // Capa flotante inferior: MiniPlayer + Barra segmentada
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            playbackState.currentSong?.let { song ->
                                val progress = if (playbackState.durationMs > 0) {
                                    playbackState.currentPositionMs.toFloat() / playbackState.durationMs
                                } else 0f

                                AuraMiniPlayer(
                                    song = song,
                                    isPlaying = playbackState.isPlaying,
                                    isFavorite = isCurrentSongFavorite,
                                    progress = progress,
                                    onPlayPauseClick = { playerManager.togglePlayPause() },
                                    onFavoriteClick = { userManager.toggleFavorite(song) },
                                    onClick = { navigateTo(Screen.ExpandedPlayer) },
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            AuraBottomNavigation(
                                selectedTab = currentTab,
                                onTabSelected = { currentTab = it }
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
                        onSongClick = { song, list ->
                            playSongAndRecord(song, list)
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
                    ExpandedPlayerScreen(
                        playbackState = playbackState,
                        isFavorite = isCurrentSongFavorite,
                        onToggleFavorite = {
                            playbackState.currentSong?.let { userManager.toggleFavorite(it) }
                        },
                        onPlayPause = { playerManager.togglePlayPause() },
                        onNext = { playerManager.playNext() },
                        onPrevious = { playerManager.playPrevious() },
                        onShuffle = { playerManager.toggleShuffle() },
                        onRepeat = { playerManager.toggleRepeat() },
                        onSeek = { fraction ->
                            playerManager.seekTo((fraction * playbackState.durationMs).toLong())
                        },
                        onBackClick = { navigateBack() },
                        onLyricsClick = { navigateTo(Screen.Lyrics) },
                        onQueueClick = { navigateTo(Screen.Queue) },
                        onOptionsClick = { selectedSongForOptions = it }
                    )
                }
                is Screen.Lyrics -> {
                    playbackState.currentSong?.let { song ->
                        LyricsScreen(
                            song = song,
                            playbackState = playbackState,
                            isFavorite = isCurrentSongFavorite,
                            onToggleFavorite = { userManager.toggleFavorite(song) },
                            onPlayPause = { playerManager.togglePlayPause() },
                            onNext = { playerManager.playNext() },
                            onPrevious = { playerManager.playPrevious() },
                            onShuffle = { playerManager.toggleShuffle() },
                            onRepeat = { playerManager.toggleRepeat() },
                            onSeek = { fraction ->
                                playerManager.seekTo((fraction * playbackState.durationMs).toLong())
                            },
                            onHideLyrics = { navigateBack() }
                        )
                    }
                }
                is Screen.Queue -> {
                    QueueScreen(
                        currentSong = playbackState.currentSong,
                        queue = playbackState.queue,
                        onSongClick = { song ->
                            playSongAndRecord(song, playbackState.queue)
                        },
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
                        currentPlayingSongId = playbackState.currentSong?.id,
                        currentAppIcon = currentAppIcon,
                        onBackClick = { navigateBack() },
                        onFollowToggle = {
                            userManager.toggleFollowArtist(screen.artist)
                        },
                        onPlayAllClick = {
                            screen.artist.topSongs.firstOrNull()?.let {
                                playSongAndRecord(it, screen.artist.topSongs)
                            }
                        },
                        onAlbumClick = { album -> openAlbumScreen(album) },
                        onPlayAlbumClick = { album ->
                            coroutineScope.launch {
                                val fullAlbum = repository.getAlbumDetails(album.id, album) ?: album
                                fullAlbum.songs.firstOrNull()?.let {
                                    playSongAndRecord(it, fullAlbum.songs)
                                }
                            }
                        },
                        onSongClick = { song ->
                            playSongAndRecord(song, screen.artist.topSongs)
                        },
                        onSongOptionsClick = { selectedSongForOptions = it },
                        onRelatedArtistClick = { relArtist -> openArtistScreen(relArtist) }
                    )
                }
                is Screen.AlbumDetail -> {
                    val isSaved = savedAlbums.any { it.id == screen.album.id }
                    AlbumScreen(
                        album = screen.album,
                        isSaved = isSaved,
                        isLoading = screen.isLoading,
                        onBackClick = { navigateBack() },
                        onSaveToggle = {
                            userManager.toggleSaveAlbum(screen.album)
                        },
                        onDownloadAlbum = { songs ->
                            songs.forEach { userManager.addDownload(it) }
                        },
                        onPlaySong = { song, songs ->
                            playSongAndRecord(song, songs)
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
                        onSettingsChanged = { userManager.updateSettings(it) },
                        onBackClick = { navigateBack() },
                        onAccountClick = { navigateTo(Screen.Account) }
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

            // Aviso flotante superior (Toast / Banner informativo)
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 24.dp, end = 24.dp)
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
                SongOptionsBottomSheet(
                    song = song,
                    isFavorite = isSongFav,
                    isDownloaded = isSongDl,
                    playlists = userPlaylists,
                    onDismiss = { selectedSongForOptions = null },
                    onAddToFavorites = {
                        userManager.toggleFavorite(it)
                    },
                    onAddToPlaylist = { playlistTitle, targetSong ->
                        userManager.addToPlaylist(playlistTitle, targetSong)
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
        }
    }
}
