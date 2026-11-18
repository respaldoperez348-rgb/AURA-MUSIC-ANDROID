package com.auramusic.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.models.Song
import com.auramusic.app.ui.components.AuraCover
import com.auramusic.app.utils.formatDuration
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos

private val RingPalette = listOf(
    Color(0xFFC4B5FD),
    Color(0xFFFBCFE8),
    Color(0xFFFDE68A),
    Color(0xFF818CF8),
    Color(0xFFA7F3D0),
    Color(0xFFFED7AA)
)

@Composable
fun ExploreScreen(
    realSongs: List<Song>,
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onGenreFilterSelected: (String) -> Unit = {},
    onSongSelected: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedGenre by remember { mutableStateOf("Para ti") }

    val genres = remember {
        listOf("Para ti", "Pop", "Rock", "Urbano", "Indie", "R&B", "Electrónica")
    }

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                0
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }?.index ?: 0
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.42f, h * 0.14f)
                cubicTo(
                    w * 0.10f, h * 0.35f,
                    w * 0.10f, h * 0.68f,
                    w * 0.42f, h * 0.90f
                )
            }
            drawPath(
                path = path,
                color = primaryColor.copy(alpha = 0.10f),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (currentPlayingSong != null) {
                        "Relacionado con: ${currentPlayingSong.title} • ${currentPlayingSong.artist}"
                    } else {
                        "Desliza para descubrir música según tu estilo"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genres) { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = {
                            selectedGenre = genre
                            onGenreFilterSelected(genre)
                        },
                        label = { Text(genre, fontSize = 12.sp) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 56.dp, bottom = 230.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(
                    items = realSongs,
                    key = { index, song -> "snail_${index}_${song.id}" }
                ) { index, song ->
                    val isFocal = index == centerIndex
                    val distFromCenter = (index - centerIndex).coerceIn(-4, 4)

                    val normalizedAngle = (distFromCenter / 3.5f) * (Math.PI / 2.0)
                    val targetIndentDp = (20f + (1f - cos(normalizedAngle).toFloat()) * 78f)
                        .coerceIn(20f, 104f).dp

                    val animatedIndent by animateDpAsState(
                        targetValue = targetIndentDp,
                        animationSpec = tween(durationMillis = 220),
                        label = "snailIndent"
                    )

                    val targetCircleSize = when (abs(distFromCenter)) {
                        0 -> 104.dp
                        1 -> 72.dp
                        2 -> 60.dp
                        else -> 52.dp
                    }
                    val circleSize by animateDpAsState(
                        targetValue = targetCircleSize,
                        animationSpec = tween(durationMillis = 220),
                        label = "snailSize"
                    )

                    val targetAlpha = when (abs(distFromCenter)) {
                        0 -> 1f
                        1 -> 0.85f
                        2 -> 0.65f
                        else -> 0.45f
                    }
                    val itemAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = tween(durationMillis = 220),
                        label = "snailAlpha"
                    )

                    val ringColor = RingPalette[index % RingPalette.size]
                    val isCurrentPlaying = currentPlayingSong?.id == song.id && isPlaying

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = animatedIndent, end = 20.dp)
                            .alpha(itemAlpha)
                            .clickable {
                                coroutineScope.launch {
                                    runCatching {
                                        listState.animateScrollToItem((index - 1).coerceAtLeast(0))
                                    }
                                }
                                onSongSelected(song)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                modifier = Modifier
                                    .size(circleSize)
                                    .border(
                                        width = if (isFocal) 3.5.dp else 2.dp,
                                        color = if (isFocal) MaterialTheme.colorScheme.primary else ringColor,
                                        shape = CircleShape
                                    ),
                                shape = CircleShape,
                                color = ringColor.copy(alpha = 0.35f),
                                shadowElevation = if (isFocal) 10.dp else 2.dp
                            ) {
                                AuraCover(
                                    thumbnailUrl = song.thumbnailUrl,
                                    id = song.id,
                                    title = song.title,
                                    artist = song.artist,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (isFocal) 3.dp else 2.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (isFocal) {
                                IconButton(
                                    onClick = { onSongSelected(song) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = if (isFocal) {
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    )
                                } else {
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                },
                                color = if (isFocal) {
                                    MaterialTheme.colorScheme.onBackground
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${song.artist} • ${formatDuration(song.durationSeconds)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = if (isFocal) 13.sp else 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isFocal) {
                            IconButton(onClick = { onSongOptionsClick(song) }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones de canción",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
