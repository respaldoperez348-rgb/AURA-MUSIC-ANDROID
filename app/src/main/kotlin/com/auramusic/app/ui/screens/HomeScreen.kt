package com.auramusic.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.models.Album
import com.auramusic.app.data.models.Artist
import com.auramusic.app.data.models.Song
import com.auramusic.app.i18n.LocalAuraStrings
import com.auramusic.app.ui.components.AuraCover
import com.auramusic.app.ui.components.AuraMonogramGlyph
import com.auramusic.app.ui.components.WaveformVisualizer
import com.auramusic.app.ui.components.rememberCoverVisualData
import com.auramusic.app.utils.formatDuration

@Composable
fun HomeScreen(
    userName: String?,
    userAvatarUrl: String = "",
    currentAppIcon: String = "GOLD",
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    quickPicks: List<Song>,
    topTracks: List<Song>,
    popularArtists: List<Artist>,
    featuredAlbums: List<Album>,
    trendingSongs: List<Song>,
    onSongClick: (Song) -> Unit,
    onTogglePlayPause: () -> Unit = {},
    onArtistClick: (Artist) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAuraStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val hairlineBorder = BorderStroke(0.85.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
    val glassSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)

    val moods = remember { listOf("Para ti", "Energía", "Chill", "Workout", "Focus", "Noche") }
    var selectedMood by remember { mutableStateOf("Para ti") }
    var showAllPopularArtists by remember { mutableStateOf(false) }

    val moodOffset = moods.indexOf(selectedMood).coerceAtLeast(0)
    val rotatedQuickPicks = remember(quickPicks, moodOffset) {
        if (quickPicks.size <= 1 || moodOffset == 0) {
            quickPicks
        } else {
            val shift = moodOffset % quickPicks.size
            quickPicks.drop(shift) + quickPicks.take(shift)
        }
    }

    val heroSong = currentPlayingSong ?: rotatedQuickPicks.firstOrNull()
    val isHeroPlaying = currentPlayingSong != null && isPlaying
    val heroVisual = rememberCoverVisualData(
        songId = heroSong?.id ?: "home_hero",
        thumbnailUrl = heroSong?.thumbnailUrl.orEmpty(),
        fallbackColor = primaryColor,
        computeBlurBitmap = false
    )
    val historySongs = remember(topTracks, rotatedQuickPicks) {
        (topTracks + rotatedQuickPicks).distinctBy { it.id }.take(9)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .drawBehind {
                if (currentPlayingSong == null && heroSong != null) {
                    val r = size.width * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(heroVisual.haloColor.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.5f, 120f),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(size.width * 0.5f, 120f)
                    )
                }
            }
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 185.dp)
    ) {
        // Cabecera limpia: Monograma "A" AURA + Avatar del usuario / Ajustes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraMonogramGlyph(color = primaryColor, size = 28.dp)

                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    shape = CircleShape,
                    color = glassSurface,
                    border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f))
                ) {
                    if (userAvatarUrl.isNotBlank()) {
                        AuraCover(
                            thumbnailUrl = userAvatarUrl,
                            title = userName.orEmpty(),
                            isArtist = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = strings.settings,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = strings.homeGreeting,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 27.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!userName.isNullOrBlank()) {
                    Text(
                        text = "Hola, $userName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Chips de estado de ánimo con borde fino
        item {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moods) { mood ->
                    val isSel = selectedMood == mood
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                selectedMood = mood
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSel) primaryColor.copy(alpha = 0.16f) else glassSurface,
                        border = BorderStroke(
                            width = if (isSel) 1.dp else 0.8.dp,
                            color = if (isSel) primaryColor.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                        )
                    ) {
                        Text(
                            text = mood,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            ),
                            color = if (isSel) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 1. HERO BENTO CARD + MOSAICO ASIMÉTRICO DE ACCESO RÁPIDO
        if (heroSong != null) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                val cardBg = MaterialTheme.colorScheme.surface

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onSongClick(heroSong) },
                    shape = RoundedCornerShape(24.dp),
                    color = glassSurface,
                    border = hairlineBorder
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AuraCover(
                            thumbnailUrl = heroSong.thumbnailUrl,
                            id = heroSong.id,
                            title = heroSong.title,
                            artist = heroSong.artist,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.64f)
                                .align(Alignment.CenterEnd),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        0f to cardBg,
                                        0.38f to cardBg.copy(alpha = 0.88f),
                                        0.72f to cardBg.copy(alpha = 0.45f),
                                        1f to cardBg.copy(alpha = 0.22f)
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(0.76f)) {
                                Text(
                                    text = heroSong.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = heroSong.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                WaveformVisualizer(
                                    progress = if (isHeroPlaying) 0.65f else 0.42f,
                                    songSeed = heroSong.id,
                                    activeColor = primaryColor,
                                    inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                                    barCount = 26,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                IconButton(
                                    onClick = {
                                        if (currentPlayingSong?.id == heroSong.id) {
                                            onTogglePlayPause()
                                        } else {
                                            onSongClick(heroSong)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(primaryColor, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isHeroPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isHeroPlaying) "Pausar" else "Reproducir",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Mosaico Bento asimétrico debajo de la Hero Card (Portada cuadrada + Tarjetas compactas)
                val spotlightSong = rotatedQuickPicks.getOrNull(1) ?: heroSong
                val rightSong = rotatedQuickPicks.getOrNull(2) ?: heroSong
                val bentoArtist1 = popularArtists.getOrNull(0)
                val bentoArtist2 = popularArtists.getOrNull(1)

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tarjeta izquierda alta con portada completa y título abajo
                    Surface(
                        modifier = Modifier
                            .weight(0.46f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSongClick(spotlightSong) },
                        shape = RoundedCornerShape(20.dp),
                        color = glassSurface,
                        border = hairlineBorder
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AuraCover(
                                thumbnailUrl = spotlightSong.thumbnailUrl,
                                id = spotlightSong.id,
                                title = spotlightSong.title,
                                artist = spotlightSong.artist,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.35f to Color.Transparent,
                                            0.85f to Color.Black.copy(alpha = 0.78f),
                                            1f to Color.Black.copy(alpha = 0.92f)
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = spotlightSong.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = spotlightSong.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Columna derecha apilada: Canción rápida + 2 Artistas circulares
                    Column(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSongClick(rightSong) },
                            shape = RoundedCornerShape(16.dp),
                            color = glassSurface,
                            border = hairlineBorder
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AuraCover(
                                    thumbnailUrl = rightSong.thumbnailUrl,
                                    id = rightSong.id,
                                    title = rightSong.title,
                                    artist = rightSong.artist,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(9.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rightSong.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = rightSong.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (bentoArtist1 != null) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onArtistClick(bentoArtist1) },
                                shape = RoundedCornerShape(16.dp),
                                color = glassSurface,
                                border = hairlineBorder
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AuraCover(
                                        thumbnailUrl = bentoArtist1.thumbnailUrl,
                                        id = bentoArtist1.id,
                                        title = bentoArtist1.name,
                                        isArtist = true,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = bentoArtist1.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (bentoArtist2 != null) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onArtistClick(bentoArtist2) },
                                shape = RoundedCornerShape(16.dp),
                                color = glassSurface,
                                border = hairlineBorder
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AuraCover(
                                        thumbnailUrl = bentoArtist2.thumbnailUrl,
                                        id = bentoArtist2.id,
                                        title = bentoArtist2.name,
                                        isArtist = true,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = bentoArtist2.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. HISTORIAL RECIENTE DESLIZABLE (Paginado horizontal de 3 en 3)
        if (historySongs.isNotEmpty()) {
            item {
                val historyPages = remember(historySongs) { historySongs.chunked(3) }
                val historyPager = rememberPagerState(pageCount = { historyPages.size })

                Spacer(modifier = Modifier.height(18.dp))
                SectionTitle(title = strings.recentHistory, showChevron = true)

                HorizontalPager(
                    state = historyPager,
                    contentPadding = PaddingValues(start = 20.dp, end = 44.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { pageIdx ->
                    val pageTracks = historyPages[pageIdx]
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = glassSurface.copy(alpha = 0.72f),
                        border = hairlineBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pageTracks.forEach { song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSongClick(song) }
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AuraCover(
                                        thumbnailUrl = song.thumbnailUrl,
                                        id = song.id,
                                        title = song.title,
                                        artist = song.artist,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = if (currentPlayingSong?.id == song.id) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { onSongOptionsClick(song) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Opciones",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. TUS MÁS ESCUCHADOS (Gráfico numerado deslizable horizontalmente 1-3 | 4-6 | 7-9)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            SectionTitle(title = strings.mostListened, showChevron = true)

            val chartSongs = remember(topTracks, trendingSongs) {
                if (topTracks.size >= 3) topTracks.take(9)
                else (topTracks + trendingSongs).distinctBy { it.id }.take(9)
            }

            if (chartSongs.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = glassSurface,
                    border = hairlineBorder
                ) {
                    Text(
                        text = strings.emptyTopTracksHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }
            } else {
                val chartPages = remember(chartSongs) { chartSongs.chunked(3) }
                val chartPager = rememberPagerState(pageCount = { chartPages.size })

                HorizontalPager(
                    state = chartPager,
                    contentPadding = PaddingValues(start = 20.dp, end = 48.dp),
                    pageSpacing = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { pageIdx ->
                    val pageTracks = chartPages[pageIdx]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pageTracks.forEachIndexed { rowIdx, song ->
                            val rankNumber = pageIdx * 3 + rowIdx + 1
                            val isCurrent = currentPlayingSong?.id == song.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSongClick(song) }
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$rankNumber",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(26.dp)
                                )
                                AuraCover(
                                    thumbnailUrl = song.thumbnailUrl,
                                    id = song.id,
                                    title = song.title,
                                    artist = song.artist,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        ),
                                        color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { onSongOptionsClick(song) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Opciones",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. LISTA DE ARTISTAS (Tarjetas Bento "Similar a..." deslizables horizontalmente)
        if (popularArtists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SectionTitle(title = strings.artistListTitle, showChevron = true)

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(popularArtists, key = { idx, a -> "bento_art_${idx}_${a.id}" }) { index, focalArtist ->
                        val relatedBatch = remember(popularArtists, index) {
                            val others = popularArtists.filter { it.id != focalArtist.id }
                            if (others.size >= 3) {
                                val start = index % others.size
                                val rotated = others.drop(start) + others.take(start)
                                rotated.take(3)
                            } else others
                        }
                        val miniCovers = remember(featuredAlbums, rotatedQuickPicks, index) {
                            val pool = (featuredAlbums.map { it.thumbnailUrl } + rotatedQuickPicks.map { it.thumbnailUrl })
                                .filter { it.isNotBlank() }
                                .distinct()
                            if (pool.isEmpty()) emptyList()
                            else List(4) { i -> pool[(index + i) % pool.size] }
                        }

                        Surface(
                            modifier = Modifier
                                .width(292.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onArtistClick(focalArtist) },
                            shape = RoundedCornerShape(24.dp),
                            color = glassSurface,
                            border = hairlineBorder
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(primaryColor.copy(alpha = 0.14f), Color.Transparent),
                                                center = Offset(size.width * 0.2f, size.height * 0.25f),
                                                radius = size.width * 0.65f
                                            ),
                                            center = Offset(size.width * 0.2f, size.height * 0.25f),
                                            radius = size.width * 0.65f
                                        )
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AuraCover(
                                        thumbnailUrl = focalArtist.thumbnailUrl,
                                        id = focalArtist.id,
                                        title = focalArtist.name,
                                        isArtist = true,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .border(1.2.dp, primaryColor.copy(alpha = 0.55f), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.similarToArtist(focalArtist.name),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.5.sp,
                                                lineHeight = 19.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (miniCovers.size == 4) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                miniCovers.take(2).forEachIndexed { idx, url ->
                                                    AuraCover(
                                                        thumbnailUrl = url,
                                                        id = "mc_${focalArtist.id}_$idx",
                                                        title = focalArtist.name,
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(RoundedCornerShape(6.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                miniCovers.drop(2).forEachIndexed { idx, url ->
                                                    AuraCover(
                                                        thumbnailUrl = url,
                                                        id = "mc2_${focalArtist.id}_$idx",
                                                        title = focalArtist.name,
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(RoundedCornerShape(6.dp)),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    relatedBatch.forEach { relArtist ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(78.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onArtistClick(relArtist) }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            AuraCover(
                                                thumbnailUrl = relArtist.thumbnailUrl,
                                                id = relArtist.id,
                                                title = relArtist.name,
                                                isArtist = true,
                                                modifier = Modifier
                                                    .size(66.dp)
                                                    .clip(CircleShape)
                                                    .border(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f), CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = relArtist.name,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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

        // 5. ARTISTAS POPULARES (Carrusel circular o cuadrícula expandible con todos los artistas)
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.popularArtists} (${popularArtists.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (showAllPopularArtists) "Compactar" else "Ver más artistas",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAllPopularArtists = !showAllPopularArtists }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (showAllPopularArtists) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    popularArtists.chunked(4).forEach { rowArtists ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowArtists.forEach { artist ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onArtistClick(artist) }
                                ) {
                                    Surface(
                                        modifier = Modifier.size(72.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        AuraCover(
                                            thumbnailUrl = artist.thumbnailUrl,
                                            id = artist.id,
                                            title = artist.name,
                                            isArtist = true,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            repeat(4 - rowArtists.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(popularArtists, key = { "art_${it.id}" }) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(86.dp)
                                .clickable { onArtistClick(artist) }
                        ) {
                            Surface(
                                modifier = Modifier.size(78.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                AuraCover(
                                    thumbnailUrl = artist.thumbnailUrl,
                                    id = artist.id,
                                    title = artist.name,
                                    isArtist = true,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. ÁLBUMES DESTACADOS
        item {
            Spacer(modifier = Modifier.height(18.dp))
            SectionTitle(strings.featuredAlbums)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(featuredAlbums, key = { "alb_${it.id}" }) { album ->
                    Column(
                        modifier = Modifier
                            .width(136.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAlbumClick(album) }
                    ) {
                        Surface(
                            modifier = Modifier.size(136.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            AuraCover(
                                thumbnailUrl = album.thumbnailUrl,
                                id = album.id,
                                title = album.title,
                                artist = album.artist,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${album.artist} • ${album.year ?: "2024"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 5. TENDENCIAS GLOBALES
        item {
            Spacer(modifier = Modifier.height(18.dp))
            SectionTitle(strings.globalTrends)
        }

        itemsIndexed(trendingSongs, key = { idx, s -> "trend_${idx}_${s.id}" }) { index, song ->
            SongListRow(
                rank = index + 1,
                song = song,
                isCurrent = currentPlayingSong?.id == song.id,
                onClick = { onSongClick(song) },
                onOptionsClick = { onSongOptionsClick(song) }
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    showChevron: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SongListRow(
    rank: Int? = null,
    song: Song,
    isCurrent: Boolean = false,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(26.dp)
            )
        }

        AuraCover(
            thumbnailUrl = song.thumbnailUrl,
            id = song.id,
            title = song.title,
            artist = song.artist,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${formatDuration(song.durationSeconds)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onOptionsClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Más opciones",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
