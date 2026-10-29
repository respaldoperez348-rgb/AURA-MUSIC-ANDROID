package com.auramusic.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.model.LyricLine
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberCyanGlow
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.NeonPurple
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import java.util.Locale

enum class DeckDisplayMode {
    VINYL, LYRICS
}

@Composable
fun StudioDeck(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    isRepeat: Boolean,
    lyrics: List<LyricLine> = emptyList(),
    lyricsOffsetMs: Long = -200L,
    showTranslation: Boolean = false,
    onToggleTranslation: () -> Unit = {},
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    sleepTimerRemaining: String? = null,
    onCollapse: (() -> Unit)? = null,
    onOpenFullscreenLyrics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf(DeckDisplayMode.VINYL) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CyberSurfaceCard,
                        CyberBackgroundDark.copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.2.dp, GlassBorder, RoundedCornerShape(28.dp))
            .padding(18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Deck Header Pill & Collapse Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onCollapse != null) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = CyberCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }

                // Mode Switcher Pill (VINYL vs LYRICS)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberSurfaceActive)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModePillButton(
                        icon = Icons.Default.Album,
                        label = "VINYL",
                        isSelected = displayMode == DeckDisplayMode.VINYL,
                        onClick = { displayMode = DeckDisplayMode.VINYL }
                    )
                    ModePillButton(
                        icon = Icons.Default.Lyrics,
                        label = "LYRICS",
                        isSelected = displayMode == DeckDisplayMode.LYRICS,
                        onClick = { displayMode = DeckDisplayMode.LYRICS }
                    )
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) CyberCyan else TextMuted)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Display (Vinyl or Synced Lyrics)
            AnimatedContent(
                targetState = displayMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "deck_display_mode"
            ) { mode ->
                when (mode) {
                    DeckDisplayMode.VINYL -> {
                        var vinylSwipeX by remember { mutableFloatStateOf(0f) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { vinylSwipeX = 0f },
                                        onDragEnd = {
                                            if (vinylSwipeX < -95f) onNext()
                                            else if (vinylSwipeX > 95f) onPrevious()
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            vinylSwipeX += dragAmount
                                        }
                                    )
                                }
                        ) {
                            VinylRecord(
                                isPlaying = isPlaying,
                                coverUrl = song.coverUrl,
                                size = 210.dp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Audio Waveform Spectrum
                            AudioVisualizer(
                                isPlaying = isPlaying,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                    DeckDisplayMode.LYRICS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(264.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenFullscreenLyrics() }
                        ) {
                            LyricsView(
                                lyrics = lyrics,
                                currentPositionMs = currentPositionMs,
                                lyricsOffsetMs = lyricsOffsetMs,
                                showTranslation = showTranslation,
                                onSeekTo = onSeekTo
                            )

                            // Translate & Fullscreen lyrics action icons
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (showTranslation) CyberPink.copy(alpha = 0.25f) else CyberSurfaceCard)
                                        .border(1.dp, if (showTranslation) CyberPink else GlassBorder, CircleShape)
                                        .clickable { onToggleTranslation() }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = "Traducir letra",
                                        tint = if (showTranslation) CyberPink else CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(CyberSurfaceCard)
                                        .border(1.dp, GlassBorder, CircleShape)
                                        .clickable { onOpenFullscreenLyrics() }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInFull,
                                        contentDescription = "Pantalla completa de letras",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track Title & Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title.ifBlank { "No Track Selected" },
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = song.artist.ifBlank { "Aura Audio Engine" },
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) CyberPink else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scrubber Progress Slider
            val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

            Slider(
                value = progress,
                onValueChange = { frac ->
                    onSeekTo((frac * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTimestamp(currentPositionMs),
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formatTimestamp(durationMs),
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) CyberCyan else TextSecondary
                    )
                }

                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Glowing Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(elevation = 14.dp, shape = CircleShape, spotColor = CyberCyanGlow)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CyberCyan, NeonPurple)
                            )
                        )
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = CyberBackgroundDark,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) CyberCyan else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Studio Quick Tools (EQ, SLEEP TIMER, QUEUE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Equalizer Quick Button
                StudioToolPill(
                    icon = Icons.Default.GraphicEq,
                    label = "EQ DSP",
                    activeHighlight = false,
                    onClick = onOpenEqualizer
                )

                // Sleep Timer Quick Button
                StudioToolPill(
                    icon = Icons.Default.Bedtime,
                    label = sleepTimerRemaining ?: "SLEEP",
                    activeHighlight = sleepTimerRemaining != null,
                    onClick = onOpenSleepTimer
                )

                // Up Next Queue Button
                StudioToolPill(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = "QUEUE",
                    activeHighlight = false,
                    onClick = onOpenQueue
                )
            }
        }
    }
}

@Composable
private fun StudioToolPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    activeHighlight: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (activeHighlight) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceActive)
            .border(1.dp, if (activeHighlight) CyberCyan else GlassBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (activeHighlight) CyberCyan else TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = if (activeHighlight) CyberCyan else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ModePillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyberCyan else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) CyberBackgroundDark else TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isSelected) CyberBackgroundDark else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatTimestamp(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
