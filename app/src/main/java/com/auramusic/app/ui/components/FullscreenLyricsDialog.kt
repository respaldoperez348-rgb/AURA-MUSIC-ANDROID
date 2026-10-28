package com.auramusic.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.auramusic.app.data.model.LyricLine
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenLyricsDialog(
    song: Song,
    lyrics: List<LyricLine>,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val listState = rememberLazyListState()
        val activeIndex = lyrics.indexOfLast { it.timestampMs <= currentPositionMs }.coerceAtLeast(0)

        // Controls auto-hide state and touch toggle
        var controlsVisible by remember { mutableStateOf(true) }
        var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }

        // Automatically hide controls after 3.5 seconds of inactivity
        LaunchedEffect(controlsVisible, lastInteraction) {
            if (controlsVisible) {
                delay(3500L)
                controlsVisible = false
            }
        }

        // Keep active lyric smoothly centered
        LaunchedEffect(activeIndex) {
            if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
                listState.animateScrollToItem(
                    index = (activeIndex - 2).coerceAtLeast(0)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    controlsVisible = !controlsVisible
                    if (controlsVisible) {
                        lastInteraction = System.currentTimeMillis()
                    }
                }
        ) {
            // 1. Ambient Frosted Glass Background (Cover Art blurred)
            if (song.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(85.dp)
                )
            }

            // Dark cyber overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CyberBackgroundDark.copy(alpha = 0.88f),
                                CyberBackgroundDark.copy(alpha = 0.94f),
                                CyberBackgroundDark.copy(alpha = 0.98f)
                            )
                        )
                    )
            )

            // 2. Middle: Big Synced Karaoke Typography (Full Screen Viewport)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (lyrics.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Letra no disponible",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Disfruta de la pista instrumental o vuelve a intentar",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 110.dp, bottom = 140.dp, start = 14.dp, end = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(lyrics) { index, line ->
                            val isActive = index == activeIndex
                            val isUpcoming = index > activeIndex

                            val textColor by animateColorAsState(
                                targetValue = when {
                                    isActive -> CyberCyan
                                    isUpcoming -> TextMuted.copy(alpha = 0.40f)
                                    else -> TextSecondary.copy(alpha = 0.50f)
                                },
                                animationSpec = tween(durationMillis = 250),
                                label = "lyric_color"
                            )

                            val textScale by animateFloatAsState(
                                targetValue = if (isActive) 1.12f else 1.0f,
                                animationSpec = tween(durationMillis = 250),
                                label = "lyric_scale"
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSeekTo(line.timestampMs)
                                        lastInteraction = System.currentTimeMillis()
                                    }
                                    .padding(vertical = 18.dp)
                                    .scale(textScale),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = line.text,
                                    color = textColor,
                                    fontSize = if (isActive) 27.sp else 21.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = if (isActive) 36.sp else 28.sp,
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Top Header with Smooth Slide & Fade
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyberSurfaceCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cerrar",
                            tint = CyberCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = song.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
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

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "LYRICS",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 4. Floating Mini-Player Bottom Bar (Automatically hides for pure full-screen view)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut(tween(300)) + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(CyberSurfaceCard.copy(alpha = 0.95f))
                        .border(1.2.dp, GlassBorder, RoundedCornerShape(26.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            lastInteraction = System.currentTimeMillis()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Seek Slider
                    val safeDuration = durationMs.coerceAtLeast(1L)
                    val progressFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

                    Slider(
                        value = progressFraction,
                        onValueChange = { frac ->
                            lastInteraction = System.currentTimeMillis()
                            onSeekTo((frac * safeDuration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = CyberBackgroundDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Playback Controls (Prev, Play/Pause, Next)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                onPrevious()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        IconButton(
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                onPlayPause()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = CyberBackgroundDark,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        IconButton(
                            onClick = {
                                lastInteraction = System.currentTimeMillis()
                                onNext()
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Siguiente",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSec = millis / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}
