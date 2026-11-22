package com.auramusic.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.models.Song
import com.auramusic.app.i18n.LocalAuraStrings
import com.auramusic.app.ui.components.AuraCover
import com.auramusic.app.ui.components.AuraMonogramGlyph
import com.auramusic.app.utils.formatDuration
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos

private val RingPalette = listOf(
    Color(0xFFF5A623),
    Color(0xFFCBD5E1),
    Color(0xFFE11D48),
    Color(0xFF94A3B8),
    Color(0xFFD97706),
    Color(0xFFE2E8F0)
)

@Composable
fun ExploreScreen(
    realSongs: List<Song>,
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onGenreFilterSelected: (String) -> Unit = {},
    onSongSelected: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val strings = LocalAuraStrings.current
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
    val onBgMuted = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
    ) {
        // Doble arco logarítmico sutil dorado-plata para la espiral caracol
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val outerArc = Path().apply {
                moveTo(w * 0.44f, h * 0.16f)
                cubicTo(
                    w * 0.06f, h * 0.36f,
                    w * 0.06f, h * 0.68f,
                    w * 0.44f, h * 0.88f
                )
            }
            drawPath(
                path = outerArc,
                color = primaryColor.copy(alpha = 0.20f),
                style = Stroke(width = 1.8.dp.toPx())
            )

            val innerArc = Path().apply {
                moveTo(w * 0.48f, h * 0.21f)
                cubicTo(
                    w * 0.14f, h * 0.38f,
                    w * 0.14f, h * 0.66f,
                    w * 0.48f, h * 0.83f
                )
            }
            drawPath(
                path = innerArc,
                color = onBgMuted,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraMonogramGlyph(color = primaryColor, size = 26.dp)
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = strings.settings,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
            ) {
                Text(
                    text = strings.explore,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (currentPlayingSong != null) {
                        "${currentPlayingSong.title} • ${currentPlayingSong.artist}"
                    } else {
                        "Desliza la espiral para descubrir música"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genres) { genre ->
                    val isSel = selectedGenre == genre
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                selectedGenre = genre
                                onGenreFilterSelected(genre)
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSel) {
                            primaryColor.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                        },
                        border = BorderStroke(
                            width = if (isSel) 1.dp else 0.8.dp,
                            color = if (isSel) {
                                primaryColor.copy(alpha = 0.85f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                            }
                        )
                    ) {
                        Text(
                            text = genre,
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

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 48.dp, bottom = 230.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(
                    items = realSongs,
                    key = { index, song -> "snail_${index}_${song.id}" },
                    contentType = { _, _ -> "snail_song_row" }
                ) { index, song ->
                    val isFocal = index == centerIndex
                    val distFromCenter = (index - centerIndex).coerceIn(-4, 4)

                    val normalizedAngle = (distFromCenter / 3.5f) * (Math.PI / 2.0)
                    val targetIndentDp = (20f + (1f - cos(normalizedAngle).toFloat()) * 78f)
                        .coerceIn(20f, 104f).dp

                    val circleSize = when (abs(distFromCenter)) {
                        0 -> 108.dp
                        1 -> 72.dp
                        2 -> 58.dp
                        else -> 48.dp
                    }

                    val itemAlpha = when (abs(distFromCenter)) {
                        0 -> 1f
                        1 -> 0.86f
                        2 -> 0.65f
                        else -> 0.45f
                    }

                    val ringColor = RingPalette[index % RingPalette.size]
                    val isCurrentPlaying = currentPlayingSong?.id == song.id && isPlaying

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = targetIndentDp, end = 20.dp)
                            .graphicsLayer { alpha = itemAlpha }
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
                                        width = if (isFocal) 2.5.dp else 1.3.dp,
                                        color = if (isFocal) primaryColor else ringColor.copy(alpha = 0.55f),
                                        shape = CircleShape
                                    ),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = if (isFocal) 12.dp else 2.dp
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
                                        .background(primaryColor, CircleShape)
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
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                } else {
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp
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
