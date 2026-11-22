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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.models.Album
import com.auramusic.app.data.models.Artist
import com.auramusic.app.data.models.Song
import com.auramusic.app.ui.components.AuraCover
import com.auramusic.app.utils.formatDuration

private val FavoriteRed = Color(0xFFFF3B30)

enum class SearchTab {
    ALL,
    SONGS,
    ARTISTS,
    ALBUMS
}

private data class BrowseCategoryTile(
    val title: String,
    val query: String,
    val startColor: Color,
    val endColor: Color,
    val coverUrl: String,
)

@Composable
fun SearchScreen(
    artists: List<Artist>,
    albums: List<Album> = emptyList(),
    songs: List<Song>,
    suggestions: List<String> = emptyList(),
    isSearching: Boolean = false,
    isSongFavorite: (Song) -> Boolean = { it.isFavorite },
    onToggleFavorite: (Song) -> Unit = {},
    onAlreadyInFavoritesNotice: (Song) -> Unit = {},
    onSearch: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onAlbumClick: (Album) -> Unit = {},
    onSongOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(SearchTab.ALL) }
    var showSuggestions by remember { mutableStateOf(false) }
    var showAllDiscoveryArtists by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val topArtist = artists.firstOrNull()

    val trendingQueries = remember {
        listOf(
            "The Weeknd",
            "Bad Bunny",
            "Karol G",
            "Quevedo",
            "Feid",
            "Daft Punk",
            "Rosalía",
            "Billie Eilish",
            "Bruno Mars",
            "Rauw Alejandro"
        )
    }

    val browseCategories = remember {
        listOf(
            BrowseCategoryTile(
                title = "Éxitos Globales",
                query = "Top Hits Globales 2025",
                startColor = Color(0xFFE13300),
                endColor = Color(0xFF7A1C00),
                coverUrl = "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "Urbano & Reggaetón",
                query = "Reggaeton Urbano Hits",
                startColor = Color(0xFF7358FF),
                endColor = Color(0xFF33208A),
                coverUrl = "https://i.ytimg.com/vi/saGYMhApaH8/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "Pop Latino & Global",
                query = "Pop Latino Global Hits",
                startColor = Color(0xFFE8115B),
                endColor = Color(0xFF78082E),
                coverUrl = "https://i.ytimg.com/vi/5g2hT4GmAGU/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "Rock & Alternativo",
                query = "Rock Clásico y Alternativo",
                startColor = Color(0xFF1E3264),
                endColor = Color(0xFF0E1730),
                coverUrl = "https://i.ytimg.com/vi/HyHNuVaZJ-k/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "Electrónica & Club",
                query = "Electronic Synthwave Club",
                startColor = Color(0xFF148A08),
                endColor = Color(0xFF094503),
                coverUrl = "https://i.ytimg.com/vi/4D7u5KF7SP8/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "R&B & Soul Nocturno",
                query = "R&B Soul Chill Hits",
                startColor = Color(0xFFBC5900),
                endColor = Color(0xFF5E2C00),
                coverUrl = "https://i.ytimg.com/vi/34Na4j8AVgA/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "Hip-Hop & Trap",
                query = "Hip Hop Trap Hits",
                startColor = Color(0xFF8D67AB),
                endColor = Color(0xFF473159),
                coverUrl = "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "Indie & Chill",
                query = "Indie Chill Vibes",
                startColor = Color(0xFF0D73EC),
                endColor = Color(0xFF053875),
                coverUrl = "https://i.ytimg.com/vi/a5uQMwRMHcs/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "Entrenamiento",
                query = "Workout Gym Energy Music",
                startColor = Color(0xFFD84000),
                endColor = Color(0xFF6B1E00),
                coverUrl = "https://i.ytimg.com/vi/JWRlTezTF2k/hqdefault.jpg"
            ),
            BrowseCategoryTile(
                title = "Clásicos 90s y 2000s",
                query = "Clasicos 90s 2000s Hits",
                startColor = Color(0xFF503750),
                endColor = Color(0xFF281A28),
                coverUrl = "https://i.ytimg.com/vi/TUVcZfQe-Kw/hqdefault.jpg"
            )
        )
    }

    val triggerQuery: (String) -> Unit = { newQuery ->
        query = newQuery
        showSuggestions = false
        onSearch(newQuery)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 195.dp)
    ) {
        // Cabecera editorial + Barra de búsqueda estilo Spotify/Studio
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Buscar",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Explora géneros, artistas, álbumes y letras",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (query.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                query = ""
                                showSuggestions = false
                                selectedTab = SearchTab.ALL
                                onSearch("")
                            }
                    ) {
                        Text(
                            text = "Explorar",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    showSuggestions = it.isNotBlank()
                    onSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text(
                        text = "¿Qué quieres escuchar hoy?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = if (query.isNotBlank()) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                query = ""
                                showSuggestions = false
                                onSearch("")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )

            // Autocompletado en tiempo real
            if (showSuggestions && suggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                    tonalElevation = 6.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        suggestions.take(5).forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { triggerQuery(suggestion) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.NorthWest,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // MODO 1: DESCUBRIMIENTO VISUAL ESTILO SPOTIFY (Cuando el buscador está vacío)
        // =========================================================================
        if (query.isBlank()) {
            // 1. Chips de búsquedas en tendencia
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Tendencias ahora",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trendingQueries) { trend ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { triggerQuery(trend) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = trend,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Artistas para explorar (Carrusel o Cuadrícula expandible de todos los artistas)
            if (artists.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Artistas destacados (${artists.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (showAllDiscoveryArtists) "Compactar" else "Ver más artistas",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showAllDiscoveryArtists = !showAllDiscoveryArtists }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (showAllDiscoveryArtists) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            artists.chunked(4).forEach { rowArtists ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowArtists.forEach { artist ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onArtistClick(artist) }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            AuraCover(
                                                thumbnailUrl = artist.thumbnailUrl,
                                                id = artist.id,
                                                title = artist.name,
                                                isArtist = true,
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f), CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(artists, key = { "disc_art_${it.id}" }) { artist ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(82.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onArtistClick(artist) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    AuraCover(
                                        thumbnailUrl = artist.thumbnailUrl,
                                        id = artist.id,
                                        title = artist.name,
                                        isArtist = true,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
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
            }

            // 3. EXPLORAR TODO: Cuadrícula Bento de 2 columnas estilo Spotify con portadas inclinadas
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Explorar todo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    browseCategories.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(98.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(cat.startColor, cat.endColor)
                                            )
                                        )
                                        .clickable { triggerQuery(cat.query) }
                                ) {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            lineHeight = 19.sp
                                        ),
                                        color = Color.White,
                                        maxLines = 2,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 14.dp, top = 12.dp, end = 58.dp)
                                    )

                                    // Portada inclinada en la esquina inferior derecha (firma visual estilo Spotify)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 10.dp, y = 8.dp)
                                            .size(66.dp)
                                            .graphicsLayer { rotationZ = 22f }
                                            .shadow(8.dp, RoundedCornerShape(10.dp))
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        AuraCover(
                                            thumbnailUrl = cat.coverUrl,
                                            id = "cat_${cat.title}",
                                            title = cat.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // MODO 2: RESULTADOS DE BÚSQUEDA ACTIVOS
            // =========================================================================
            item {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTab == SearchTab.ALL,
                            onClick = { selectedTab = SearchTab.ALL },
                            label = { Text("Todo") },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTab == SearchTab.SONGS,
                            onClick = { selectedTab = SearchTab.SONGS },
                            label = { Text("Canciones (${songs.size})") },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTab == SearchTab.ARTISTS,
                            onClick = { selectedTab = SearchTab.ARTISTS },
                            label = { Text("Artistas (${artists.size})") },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedTab == SearchTab.ALBUMS,
                            onClick = { selectedTab = SearchTab.ALBUMS },
                            label = { Text("Álbumes (${albums.size})") },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryColor,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }
            }

            // Tarjeta Hero "Mejor resultado" + Carrusel de artistas en pestaña Todo
            if (topArtist != null && selectedTab == SearchTab.ALL) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Mejor resultado",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onArtistClick(topArtist) },
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraCover(
                                thumbnailUrl = topArtist.thumbnailUrl,
                                id = topArtist.id,
                                title = topArtist.name,
                                isArtist = true,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, primaryColor.copy(alpha = 0.6f), CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = primaryColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "ARTISTA",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp
                                        ),
                                        color = primaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = topArtist.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = topArtist.monthlyListeners?.let { "$it oyentes mensuales" } ?: "Ver perfil y discografía",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    val firstTrack = songs.firstOrNull()
                                    if (firstTrack != null) onSongClick(firstTrack) else onArtistClick(topArtist)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(primaryColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Reproducir",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // Tira horizontal con MÁS ARTISTAS encontrados para que no aparezca uno solo
                if (artists.size > 1) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Artistas relacionados",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Ver todos (${artists.size})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = primaryColor,
                                modifier = Modifier.clickable { selectedTab = SearchTab.ARTISTS }
                            )
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(artists.drop(1).take(12), key = { "rel_s_art_${it.id}" }) { artist ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(78.dp)
                                        .clickable { onArtistClick(artist) }
                                ) {
                                    AuraCover(
                                        thumbnailUrl = artist.thumbnailUrl,
                                        id = artist.id,
                                        title = artist.name,
                                        isArtist = true,
                                        modifier = Modifier
                                            .size(66.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pestaña ARTISTAS completa
            if (selectedTab == SearchTab.ARTISTS) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Artistas (${artists.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(artists, key = { "s_art_${it.id}" }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArtistClick(artist) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuraCover(
                            thumbnailUrl = artist.thumbnailUrl,
                            id = artist.id,
                            title = artist.name,
                            isArtist = true,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = artist.monthlyListeners?.let { "$it oyentes mensuales" } ?: "Artista",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Pestaña ÁLBUMES completa
            if (selectedTab == SearchTab.ALBUMS) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Álbumes (${albums.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(albums, key = { "s_alb_${it.id}" }) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumClick(album) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuraCover(
                            thumbnailUrl = album.thumbnailUrl,
                            id = album.id,
                            title = album.title,
                            artist = album.artist,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} • ${album.year ?: "Álbum"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Lista de canciones (en modo Descubrimiento y en Todo / Canciones)
        if (query.isBlank() || selectedTab == SearchTab.ALL || selectedTab == SearchTab.SONGS) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (query.isBlank()) "Canciones recomendadas" else "Canciones",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            items(songs, key = { "s_song_${it.id}" }) { song ->
                val isFav = isSongFavorite(song)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSuggestions = false
                            onSongClick(song)
                        }
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
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
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
                        if (isFav) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "♥ En tus favoritos",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = FavoriteRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isFav) {
                                onAlreadyInFavoritesNotice(song)
                            } else {
                                onToggleFavorite(song)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFav) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
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
