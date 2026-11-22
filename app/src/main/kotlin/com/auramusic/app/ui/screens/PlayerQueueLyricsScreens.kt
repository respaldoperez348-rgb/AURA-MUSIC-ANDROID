package com.auramusic.app.ui.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.AuraApplication
import com.auramusic.app.data.models.LyricLine
import com.auramusic.app.data.models.PlayerBackdropMode
import com.auramusic.app.data.models.PlayerStyleMode
import com.auramusic.app.data.models.Song
import com.auramusic.app.i18n.LocalAuraStrings
import com.auramusic.app.playback.PlaybackState
import com.auramusic.app.ui.components.AuraCover
import com.auramusic.app.ui.components.AuraPlayerBackdrop
import com.auramusic.app.ui.components.WaveformVisualizer
import com.auramusic.app.ui.components.rememberCoverVisualData
import com.auramusic.app.utils.LyricCardSharer
import com.auramusic.app.utils.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val FavoriteRed = Color(0xFFFF3B30)

@Composable
fun ExpandedPlayerScreen(
    playbackState: PlaybackState,
    backdropMode: PlayerBackdropMode = PlayerBackdropMode.AURA_HALO,
    styleMode: PlayerStyleMode = PlayerStyleMode.STUDIO_HALO,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCycleStyle: () -> Unit = {},
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onAudioLabClick: () -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = playbackState.currentSong ?: return
    val strings = LocalAuraStrings.current
    val visualData = rememberCoverVisualData(
        songId = song.id,
        thumbnailUrl = song.thumbnailUrl,
        fallbackColor = MaterialTheme.colorScheme.primary,
        computeBlurBitmap = backdropMode == PlayerBackdropMode.SOFT_BLUR || styleMode == PlayerStyleMode.FULL_BLEED
    )

    var isVinylMode by remember { mutableStateOf(false) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs).coerceIn(0f, 1f)
    } else 0f

    val currentSeconds = playbackState.currentPositionMs / 1000
    val totalSeconds = if (playbackState.durationMs > 0) playbackState.durationMs / 1000 else song.durationSeconds

    val nextSong = remember(playbackState.queue, song.id) {
        val idx = playbackState.queue.indexOfFirst { it.id == song.id }
        if (idx != -1 && idx + 1 < playbackState.queue.size) playbackState.queue[idx + 1] else null
    }

    if (styleMode == PlayerStyleMode.FULL_BLEED) {
        FullBleedPlayerLayout(
            song = song,
            nextSong = nextSong,
            playbackState = playbackState,
            visualData = visualData,
            progress = progress,
            currentSeconds = currentSeconds,
            totalSeconds = totalSeconds,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onCycleStyle = onCycleStyle,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onBackClick = onBackClick,
            onLyricsClick = onLyricsClick,
            onQueueClick = onQueueClick,
            onAudioLabClick = onAudioLabClick,
            onOptionsClick = onOptionsClick,
            modifier = modifier
        )
        return
    }

    if (styleMode == PlayerStyleMode.EDITORIAL_CURVO) {
        EditorialCurvoPlayerLayout(
            song = song,
            playbackState = playbackState,
            visualData = visualData,
            progress = progress,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onCycleStyle = onCycleStyle,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onBackClick = onBackClick,
            onLyricsClick = onLyricsClick,
            onQueueClick = onQueueClick,
            onAudioLabClick = onAudioLabClick,
            onOptionsClick = onOptionsClick,
            modifier = modifier
        )
        return
    }

    if (styleMode == PlayerStyleMode.VINILO_ESTUDIO) {
        ViniloEstudioPlayerLayout(
            song = song,
            nextSong = nextSong,
            playbackState = playbackState,
            visualData = visualData,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onCycleStyle = onCycleStyle,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onBackClick = onBackClick,
            onLyricsClick = onLyricsClick,
            onQueueClick = onQueueClick,
            onAudioLabClick = onAudioLabClick,
            onOptionsClick = onOptionsClick,
            modifier = modifier
        )
        return
    }

    if (styleMode == PlayerStyleMode.RELOJ_AMBIENTE) {
        RelojAmbientePlayerLayout(
            song = song,
            playbackState = playbackState,
            progress = progress,
            currentSeconds = currentSeconds,
            totalSeconds = totalSeconds,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onCycleStyle = onCycleStyle,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onBackClick = onBackClick,
            onLyricsClick = onLyricsClick,
            onOptionsClick = onOptionsClick,
            modifier = modifier
        )
        return
    }

    val coverScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1.03f else 0.95f,
        animationSpec = tween(durationMillis = 380),
        label = "auraCoverScale"
    )

    val vinylTransition = rememberInfiniteTransition(label = "auraVinylSpin")
    val vinylAngle by vinylTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auraVinylAngle"
    )

    var liveLyrics by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    LaunchedEffect(song.id) {
        liveLyrics = AuraApplication.repository.fetchSyncedLyrics(song)
    }
    val livePosMs = playbackState.currentPositionMs + playbackState.lyricsOffsetMs
    val activeLyricIndex = remember(liveLyrics, livePosMs) {
        if (liveLyrics.isEmpty()) -1
        else liveLyrics.indexOfLast { it.timeMs <= livePosMs + 300L }.coerceAtLeast(0)
    }
    val prevLyricLine = if (activeLyricIndex > 0) liveLyrics.getOrNull(activeLyricIndex - 1)?.text else null
    val activeLyricLine = if (activeLyricIndex >= 0) liveLyrics.getOrNull(activeLyricIndex)?.text else null
    val nextLyricLine = if (activeLyricIndex >= 0) liveLyrics.getOrNull(activeLyricIndex + 1)?.text else null

    val onFg = if (backdropMode == PlayerBackdropMode.SOFT_BLUR) Color.White else MaterialTheme.colorScheme.onBackground
    val onFgMuted = if (backdropMode == PlayerBackdropMode.SOFT_BLUR) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant
    val accentControl = MaterialTheme.colorScheme.primary
    val onAccentControl = if (accentControl.luminance() > 0.36f) Color(0xFF090B0F) else Color.White
    var dismissDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dismissDragY.coerceAtLeast(0f) }
            .pointerInput(song.id) {
                detectVerticalDragGestures(
                    onDragStart = { dismissDragY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dismissDragY = (dismissDragY + dragAmount).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (dismissDragY > 150f) onBackClick()
                        dismissDragY = 0f
                    },
                    onDragCancel = { dismissDragY = 0f }
                )
            }
    ) {
        AuraPlayerBackdrop(
            mode = backdropMode,
            visualData = visualData,
            isPlaying = playbackState.isPlaying,
            haloCenterYFraction = 0.34f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Cabecera Kinetic Player: Colapsar | Álbum + OPUS kbps | Estilo & Ecualizador
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cerrar",
                        tint = onFg,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.album?.takeIf { it.isNotBlank() } ?: "AURA Music",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = onFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = onFg.copy(alpha = 0.08f),
                        border = BorderStroke(
                            width = 0.8.dp,
                            color = onFg.copy(alpha = 0.16f)
                        )
                    ) {
                        Text(
                            text = playbackState.streamQualityLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = onFg.copy(alpha = 0.90f),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onCycleStyle, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Cambiar estilo visual",
                            tint = onFg.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(onClick = onAudioLabClick, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = strings.audioLab,
                            tint = onFg,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Portada flotante con halo inferior y swipe izquierdo/derecho
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(278.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    visualData.haloColor.copy(alpha = 0.36f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.68f),
                                radius = size.width * 0.68f
                            ),
                            radius = size.width * 0.68f,
                            center = Offset(size.width * 0.5f, size.height * 0.68f)
                        )
                    }
                    .pointerInput(song.id) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount
                            },
                            onDragEnd = {
                                if (dragAccumulator < -95f) onNext()
                                else if (dragAccumulator > 95f) onPrevious()
                                dragAccumulator = 0f
                            }
                        )
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = coverScale
                            scaleY = coverScale
                            if (isVinylMode && playbackState.isPlaying) {
                                rotationZ = vinylAngle
                            }
                            translationX = (dragAccumulator * 0.25f).coerceIn(-60f, 60f)
                        }
                        .clickable { isVinylMode = !isVinylMode },
                    shape = if (isVinylMode) CircleShape else RoundedCornerShape(24.dp),
                    shadowElevation = 20.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AuraCover(
                            thumbnailUrl = song.thumbnailUrl,
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isVinylMode) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF090B0F))
                                    .border(2.dp, accentControl.copy(alpha = 0.7f), CircleShape)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .padding(10.dp)
                        .clip(CircleShape)
                        .clickable { isVinylMode = !isVinylMode },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = "Estilo Vinilo",
                        tint = if (isVinylMode) accentControl else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Título de la pista, Artista y Corazón único
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = onFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onFgMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (isFavorite) FavoriteRed else onFg
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Onda acústica interactiva en tono cálido AURA
            WaveformVisualizer(
                progress = progress,
                songSeed = song.id,
                barCount = 44,
                activeColor = accentControl,
                inactiveColor = onFgMuted.copy(alpha = 0.28f),
                onSeek = onSeek,
                modifier = Modifier.height(34.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentSeconds),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = onFgMuted
                )
                Text(
                    text = formatDuration(totalSeconds),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = onFgMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controles principales con Anillo Ámbar Luminoso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (playbackState.isShuffleEnabled) accentControl else onFgMuted
                    )
                }

                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = onFg,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentControl.copy(alpha = 0.16f))
                        .border(2.dp, accentControl.copy(alpha = 0.85f), CircleShape)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(accentControl),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                            tint = onAccentControl,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = onFg,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(onClick = onRepeat) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repetir",
                        tint = if (playbackState.isRepeatEnabled) accentControl else onFgMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta expandida de Live-Lyrics (3 líneas sincronizadas) para equilibrar sin espacios vacíos
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 112.dp)
                    .padding(bottom = 14.dp)
                    .shadow(14.dp, RoundedCornerShape(22.dp), spotColor = Color(0x35000000))
                    .clip(RoundedCornerShape(22.dp))
                    .pointerInput(song.id) {
                        var upDy = 0f
                        detectVerticalDragGestures(
                            onDragStart = { upDy = 0f },
                            onVerticalDrag = { _, dragAmount -> upDy += dragAmount },
                            onDragEnd = {
                                if (upDy < -60f) onLyricsClick()
                                else if (upDy > 140f) onBackClick()
                                upDy = 0f
                            }
                        )
                    }
                    .clickable { onLyricsClick() },
                shape = RoundedCornerShape(22.dp),
                color = if (backdropMode == PlayerBackdropMode.SOFT_BLUR) {
                    Color(0xFF13161D).copy(alpha = 0.72f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                },
                border = BorderStroke(0.85.dp, onFg.copy(alpha = 0.14f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.liveLyricsTag,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = accentControl
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onQueueClick,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = strings.queue,
                                    tint = onFg.copy(alpha = 0.88f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onOptionsClick(song) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = onFg.copy(alpha = 0.88f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
                    ) {
                        if (!activeLyricLine.isNullOrBlank()) {
                            if (!prevLyricLine.isNullOrBlank()) {
                                Text(
                                    text = prevLyricLine,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                    color = onFgMuted.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = activeLyricLine,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    lineHeight = 23.sp
                                ),
                                color = onFg,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!nextLyricLine.isNullOrBlank()) {
                                Text(
                                    text = nextLyricLine,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                    color = onFgMuted.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else if (nextSong != null) {
                            Text(
                                text = strings.upNext,
                                style = MaterialTheme.typography.labelSmall,
                                color = onFgMuted
                            )
                            Text(
                                text = "${nextSong.title} • ${nextSong.artist}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = onFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = strings.tapForLyrics,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = onFgMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FullBleedPlayerLayout(
    song: Song,
    nextSong: Song?,
    playbackState: PlaybackState,
    visualData: com.auramusic.app.ui.components.CoverVisualData,
    progress: Float,
    currentSeconds: Long,
    totalSeconds: Long,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCycleStyle: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onAudioLabClick: () -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragAcc by remember { mutableFloatStateOf(0f) }
    var fbDismissY by remember { mutableFloatStateOf(0f) }
    var liveLyrics by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    LaunchedEffect(song.id) {
        liveLyrics = AuraApplication.repository.fetchSyncedLyrics(song)
    }
    val posMs = playbackState.currentPositionMs + playbackState.lyricsOffsetMs
    val activeLyricLine = remember(liveLyrics, posMs) {
        if (liveLyrics.isEmpty()) null
        else {
            val idx = liveLyrics.indexOfLast { it.timeMs <= posMs + 300L }.coerceAtLeast(0)
            liveLyrics.getOrNull(idx)?.text
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12))
            .graphicsLayer { translationY = fbDismissY.coerceAtLeast(0f) }
            .pointerInput("fb_dismiss_${song.id}") {
                detectVerticalDragGestures(
                    onDragStart = { fbDismissY = 0f },
                    onVerticalDrag = { _, dy ->
                        fbDismissY = (fbDismissY + dy).coerceAtLeast(0f)
                    },
                    onDragEnd = {
                        if (fbDismissY > 140f) onBackClick()
                        fbDismissY = 0f
                    },
                    onDragCancel = { fbDismissY = 0f }
                )
            }
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAcc = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragAcc += amount
                    },
                    onDragEnd = {
                        if (dragAcc < -95f) onNext()
                        else if (dragAcc > 95f) onPrevious()
                        dragAcc = 0f
                    }
                )
            }
    ) {
        // Fondo cromático suave en la mitad inferior
        AuraPlayerBackdrop(
            mode = PlayerBackdropMode.SOFT_BLUR,
            visualData = visualData,
            isPlaying = playbackState.isPlaying
        )

        // Portada Edge-to-Edge superior fundiéndose suavemente hacia abajo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
        ) {
            AuraCover(
                thumbnailUrl = song.thumbnailUrl,
                id = song.id,
                title = song.title,
                artist = song.artist,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.28f),
                            0.55f to Color.Transparent,
                            0.85f to Color.Black.copy(alpha = 0.65f),
                            1f to Color.Black.copy(alpha = 0.92f)
                        )
                    )
            )
        }

        // Botones superiores flotantes (respetando status bar mientras la portada sangra por detrás)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.38f))
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", tint = Color.White)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onCycleStyle,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Estilo", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onAudioLabClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f))
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "AudioLab", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = playbackState.streamQualityLabel.substringBefore("•").trim(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) FavoriteRed else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onOptionsClick(song) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            WaveformVisualizer(
                progress = progress,
                songSeed = song.id,
                barCount = 46,
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.30f),
                onSeek = onSeek,
                modifier = Modifier.height(30.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(currentSeconds), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Text(formatDuration(totalSeconds), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = onPlayPause, modifier = Modifier.size(68.dp)) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Píldora inferior con Siguiente Canción + acceso rápido a Letras / Cola
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onLyricsClick() },
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!activeLyricLine.isNullOrBlank()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LETRA EN VIVO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp), color = visualData.haloColor)
                            Text(
                                text = activeLyricLine,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else if (nextSong != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AuraCover(
                                thumbnailUrl = nextSong.thumbnailUrl,
                                id = nextSong.id,
                                title = nextSong.title,
                                artist = nextSong.artist,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Siguiente canción", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                                Text(
                                    text = "${nextSong.title} • ${nextSong.artist}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text("Toca para ver letras sincronizadas", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    Row {
                        IconButton(onClick = onLyricsClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Subtitles, contentDescription = "Letras", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onQueueClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Cola", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialCurvoPlayerLayout(
    song: Song,
    playbackState: PlaybackState,
    visualData: com.auramusic.app.ui.components.CoverVisualData,
    progress: Float,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCycleStyle: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onAudioLabClick: () -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lyrics by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    LaunchedEffect(song.id) {
        lyrics = AuraApplication.repository.fetchSyncedLyrics(song)
    }
    val posMs = playbackState.currentPositionMs + playbackState.lyricsOffsetMs
    val activeIdx = remember(lyrics, posMs) {
        if (lyrics.isEmpty()) -1 else lyrics.indexOfLast { it.timeMs <= posMs + 300L }.coerceAtLeast(0)
    }
    val prevLineText = if (activeIdx > 0) lyrics.getOrNull(activeIdx - 1)?.text else null
    val currentLineText = lyrics.getOrNull(activeIdx)?.text ?: song.title
    val nextLineText = if (activeIdx >= 0) lyrics.getOrNull(activeIdx + 1)?.text else null
    var curvoDismissY by remember { mutableFloatStateOf(0f) }
    var curvoDragX by remember { mutableFloatStateOf(0f) }
    val curvoAccent = MaterialTheme.colorScheme.primary
    val onCurvoAccent = MaterialTheme.colorScheme.onPrimary

    val curvedShape = remember {
        GenericShape { size, _ ->
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height * 0.66f)
            quadraticTo(size.width * 0.48f, size.height * 0.98f, 0f, size.height * 0.96f)
            close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090B0F))
            .graphicsLayer { translationY = curvoDismissY.coerceAtLeast(0f) }
            .pointerInput(song.id) {
                detectVerticalDragGestures(
                    onDragStart = { curvoDismissY = 0f },
                    onVerticalDrag = { _, dy -> curvoDismissY = (curvoDismissY + dy).coerceAtLeast(0f) },
                    onDragEnd = {
                        if (curvoDismissY > 140f) onBackClick()
                        curvoDismissY = 0f
                    },
                    onDragCancel = { curvoDismissY = 0f }
                )
            }
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragStart = { curvoDragX = 0f },
                    onHorizontalDrag = { _, dx -> curvoDragX += dx },
                    onDragEnd = {
                        if (curvoDragX < -95f) onNext()
                        else if (curvoDragX > 95f) onPrevious()
                        curvoDragX = 0f
                    }
                )
            }
    ) {
        // Fondo cromático cálido de la portada abajo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF090B0F),
                        0.48f to visualData.haloColor.copy(alpha = 0.30f),
                        0.76f to visualData.secondaryColor.copy(alpha = 0.28f),
                        1f to Color(0xFF090B0F)
                    )
                )
        )

        // Portada superior con recorte curvo diagonal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(445.dp)
                .clip(curvedShape)
        ) {
            AuraCover(
                thumbnailUrl = song.thumbnailUrl,
                id = song.id,
                title = song.title,
                artist = song.artist,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.28f),
                            0.65f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.48f)
                        )
                    )
            )
        }

        // Cabecera superior respetando barra de estado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f))
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", tint = Color.White)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onCycleStyle,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Cambiar estilo", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onAudioLabClick,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f))
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "AudioLab", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Bloque inferior equilibrado sin huecos: 3 versos grandes + Onda + Título y Artista
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 86.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLyricsClick() },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!prevLineText.isNullOrBlank()) {
                    Text(
                        text = prevLineText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.48f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = currentLineText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 29.sp,
                        lineHeight = 35.sp
                    ),
                    color = Color.White,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (!nextLineText.isNullOrBlank()) {
                    Text(
                        text = nextLineText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.48f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            WaveformVisualizer(
                progress = progress,
                songSeed = song.id,
                barCount = 32,
                activeColor = curvoAccent,
                inactiveColor = Color.White.copy(alpha = 0.25f),
                onSeek = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            )

            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Botones circulares flotantes en la columna derecha
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FloatingRoundPlayerButton(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (isFavorite) FavoriteRed else Color.White,
                onClick = onToggleFavorite
            )
            FloatingRoundPlayerButton(
                icon = Icons.Default.SkipPrevious,
                tint = Color.White,
                onClick = onPrevious
            )
            FloatingRoundPlayerButton(
                icon = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                tint = onCurvoAccent,
                containerColor = curvoAccent,
                onClick = onPlayPause
            )
            FloatingRoundPlayerButton(
                icon = Icons.Default.SkipNext,
                tint = Color.White,
                onClick = onNext
            )
            FloatingRoundPlayerButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                tint = Color.White,
                onClick = onQueueClick
            )
            FloatingRoundPlayerButton(
                icon = Icons.Default.MoreVert,
                tint = Color.White,
                onClick = { onOptionsClick(song) }
            )
        }
    }
}

@Composable
private fun FloatingRoundPlayerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    containerColor: Color = Color.White.copy(alpha = 0.16f),
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Estilo 4: TOCADISCOS VINILO (VINILO_ESTUDIO)
 * Disco LP giratorio descentrado a la izquierda, brazo metálico con aguja a la derecha,
 * columna vertical de controles tipo cápsula, título en marquesina con botón '^' y tarjeta Next Song inferior.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ViniloEstudioPlayerLayout(
    song: Song,
    nextSong: Song?,
    playbackState: PlaybackState,
    visualData: com.auramusic.app.ui.components.CoverVisualData,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCycleStyle: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onAudioLabClick: () -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dismissY by remember { mutableFloatStateOf(0f) }
    var dragX by remember { mutableFloatStateOf(0f) }

    val spinTransition = rememberInfiniteTransition(label = "vinylEstudioSpin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylSpinAngle"
    )
    val effectiveAngle = if (playbackState.isPlaying) spinAngle else 18f
    val tonearmAngle by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 14f else 2f,
        animationSpec = tween(durationMillis = 520),
        label = "tonearmAngle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0D))
            .graphicsLayer { translationY = dismissY.coerceAtLeast(0f) }
            .pointerInput("vinyl_dismiss_${song.id}") {
                detectVerticalDragGestures(
                    onDragStart = { dismissY = 0f },
                    onVerticalDrag = { _, dy -> dismissY = (dismissY + dy).coerceAtLeast(0f) },
                    onDragEnd = {
                        if (dismissY > 140f) onBackClick()
                        dismissY = 0f
                    },
                    onDragCancel = { dismissY = 0f }
                )
            }
            .pointerInput("vinyl_skip_${song.id}") {
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onHorizontalDrag = { _, dx -> dragX += dx },
                    onDragEnd = {
                        if (dragX < -95f) onNext()
                        else if (dragX > 95f) onPrevious()
                        dragX = 0f
                    }
                )
            }
    ) {
        // Resplandor ambiental cálido en el borde derecho detrás del brazo y botones
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to visualData.haloColor.copy(alpha = 0.24f),
                                0.55f to visualData.secondaryColor.copy(alpha = 0.12f),
                                1f to Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.48f),
                            radius = size.width * 0.85f
                        ),
                        radius = size.width * 0.85f,
                        center = Offset(size.width * 0.88f, size.height * 0.48f)
                    )
                }
        )

        // Disco de Vinilo LP gigante descentrado a la izquierda
        Box(
            modifier = Modifier
                .size(410.dp)
                .offset(x = (-92).dp, y = 44.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f
                drawCircle(color = Color(0xFF0E0E12), radius = maxR, center = center)
                // Surcos concéntricos realistas
                for (i in 1..18) {
                    val fraction = 0.54f + (i / 18f) * 0.44f
                    val alpha = if (i % 3 == 0) 0.16f else 0.07f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = maxR * fraction,
                        center = center,
                        style = Stroke(width = if (i % 4 == 0) 1.6f else 0.9f)
                    )
                }
                // Brillo diagonal satinado sobre los surcos
                drawCircle(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.12f to Color.White.copy(alpha = 0.08f),
                        0.24f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.62f to Color.White.copy(alpha = 0.08f),
                        0.74f to Color.Transparent,
                        1f to Color.Transparent,
                        center = center
                    ),
                    radius = maxR * 0.98f,
                    center = center
                )
            }

            // Carátula central giratoria del LP
            Box(
                modifier = Modifier
                    .size(226.dp)
                    .graphicsLayer { rotationZ = effectiveAngle }
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF1B1B22), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AuraCover(
                    thumbnailUrl = song.thumbnailUrl,
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Brazo de tocadiscos (Tonearm) con base superior derecha, tubo plateado y cabezal con aguja
        Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .width(150.dp)
                .height(355.dp)
        ) {
            val pivotX = size.width * 0.70f
            val pivotY = size.height * 0.17f

            // Base cápsula negra del pivote
            drawRoundRect(
                color = Color(0xFF08080B),
                topLeft = Offset(size.width * 0.42f, size.height * 0.04f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.52f, size.height * 0.34f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(42f, 42f)
            )
            // Contrapeso superior
            drawRoundRect(
                color = Color(0xFF191A20),
                topLeft = Offset(size.width * 0.50f, size.height * 0.02f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.36f, size.height * 0.14f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
            )

            // Ángulo dinámico del brazo hacia el surco del vinilo
            val rad = Math.toRadians((102f + tonearmAngle).toDouble())
            val armLen = size.height * 0.68f
            val endX = (pivotX + armLen * kotlin.math.cos(rad)).toFloat()
            val endY = (pivotY + armLen * kotlin.math.sin(rad)).toFloat()

            // Tubo metálico plateado del brazo
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE5E9F0), Color(0xFF8C929E), Color(0xFFD8DEE9))
                ),
                start = Offset(pivotX, pivotY),
                end = Offset(endX, endY),
                strokeWidth = 14f,
                cap = StrokeCap.Round
            )

            // Aguja plateada en la punta inferior del cabezal
            val needleX = endX - 10f
            val needleY = endY + 34f
            drawLine(
                color = Color(0xFFD8DEE9),
                start = Offset(endX - 4f, endY + 18f),
                end = Offset(needleX, needleY),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // Cabezal negro (Headshell)
            drawRoundRect(
                color = Color(0xFF17181E),
                topLeft = Offset(endX - 24f, endY - 12f),
                size = androidx.compose.ui.geometry.Size(36f, 48f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
        }

        // Barra superior: Tirador central + Cerrar y Cambiar estilo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.55f))
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onCycleStyle,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Cambiar estilo visual",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onAudioLabClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "AudioLab DSP",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Columna derecha de 4 botones tipo cápsula oscura (Favorito, Rebobinar, Pausa/Play, Avanzar)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp, top = 155.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VinylCapsuleButton(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (isFavorite) FavoriteRed else Color.White,
                contentDesc = "Me gusta",
                onClick = onToggleFavorite
            )
            VinylCapsuleButton(
                icon = Icons.Default.FastRewind,
                tint = Color.White,
                contentDesc = "Anterior",
                onClick = onPrevious
            )
            VinylCapsuleButton(
                icon = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                tint = Color.White,
                contentDesc = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                onClick = onPlayPause
            )
            VinylCapsuleButton(
                icon = Icons.Default.FastForward,
                tint = Color.White,
                contentDesc = "Siguiente",
                onClick = onNext
            )
        }

        // Bloque inferior: Botón '^' + Título en marquesina + Artista + Tarjeta Next Song
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onLyricsClick() },
                    shape = CircleShape,
                    color = Color(0xFF222228)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Ver letras",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${song.title}          ${song.title}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 23.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Tarjeta inferior "Next Song / Siguiente canción"
            val displayNext = nextSong ?: song
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        if (nextSong != null) onNext() else onQueueClick()
                    },
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141419),
                border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraCover(
                        thumbnailUrl = displayNext.thumbnailUrl,
                        id = displayNext.id,
                        title = displayNext.title,
                        artist = displayNext.artist,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Next Song",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = Color.White.copy(alpha = 0.55f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${displayNext.title} - ${displayNext.artist}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onQueueClick, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Cola",
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VinylCapsuleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    contentDesc: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(60.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF232329)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Estilo 5: LETRAS Y RELOJ (RELOJ_AMBIENTE)
 * Diseño dual con portada difuminada a negro + controles compactos en el panel izquierdo,
 * y un panel derecho interactivo deslizable:
 * - Página 0 (al abrir): muestra la letra sincronizada de la canción.
 * - Página 1 (al deslizar): muestra la fecha ("vie 20") y el reloj digital gigante apilado ("21 / 12").
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RelojAmbientePlayerLayout(
    song: Song,
    playbackState: PlaybackState,
    progress: Float,
    currentSeconds: Long,
    totalSeconds: Long,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCycleStyle: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onBackClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onOptionsClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var dismissY by remember { mutableFloatStateOf(0f) }
    val rightPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    var lyrics by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    LaunchedEffect(song.id) {
        lyrics = AuraApplication.repository.fetchSyncedLyrics(song)
    }
    val posMs = playbackState.currentPositionMs + playbackState.lyricsOffsetMs
    val activeIdx = remember(lyrics, posMs) {
        if (lyrics.isEmpty()) -1 else lyrics.indexOfLast { it.timeMs <= posMs + 300L }.coerceAtLeast(0)
    }
    val prevLine = if (activeIdx > 0) lyrics.getOrNull(activeIdx - 1)?.text else null
    val currentLine = lyrics.getOrNull(activeIdx)?.text ?: song.title
    val nextLine = if (activeIdx >= 0) lyrics.getOrNull(activeIdx + 1)?.text else song.artist
    val nextLine2 = if (activeIdx >= 0) lyrics.getOrNull(activeIdx + 2)?.text else null

    // Reloj en tiempo real (día + hora apilada HH / mm)
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val dayLabel = remember(currentTimeMillis / 60_000L) {
        val sdf = java.text.SimpleDateFormat("EEE dd", java.util.Locale("es", "ES"))
        sdf.format(java.util.Date(currentTimeMillis)).replace(".", "").lowercase()
    }
    val hourStr = remember(currentTimeMillis / 1000L) {
        java.text.SimpleDateFormat("HH", java.util.Locale.getDefault()).format(java.util.Date(currentTimeMillis))
    }
    val minuteStr = remember(currentTimeMillis / 1000L) {
        java.text.SimpleDateFormat("mm", java.util.Locale.getDefault()).format(java.util.Date(currentTimeMillis))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { translationY = dismissY.coerceAtLeast(0f) }
            .pointerInput("reloj_dismiss_${song.id}") {
                detectVerticalDragGestures(
                    onDragStart = { dismissY = 0f },
                    onVerticalDrag = { _, dy -> dismissY = (dismissY + dy).coerceAtLeast(0f) },
                    onDragEnd = {
                        if (dismissY > 140f) onBackClick()
                        dismissY = 0f
                    },
                    onDragCancel = { dismissY = 0f }
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // PANEL IZQUIERDO: Portada a sangre con degradado hacia negro + miniatura, barra de progreso y controles
            Box(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
            ) {
                AuraCover(
                    thumbnailUrl = song.thumbnailUrl,
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Fundido lateral hacia el negro puro del panel derecho + fundido inferior para legibilidad
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to Color.Black.copy(alpha = 0.10f),
                                0.62f to Color.Black.copy(alpha = 0.25f),
                                1.0f to Color.Black
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.35f),
                                0.45f to Color.Transparent,
                                0.72f to Color.Black.copy(alpha = 0.65f),
                                1.0f to Color.Black.copy(alpha = 0.92f)
                            )
                        )
                )

                // Botones superiores discretos (Cerrar y Cambiar estilo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onCycleStyle,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Cambiar estilo visual",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Controles superpuestos en la esquina inferior izquierda (como en la imagen de referencia)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuraCover(
                            thumbnailUrl = song.thumbnailUrl,
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(9.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.5.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Me gusta",
                                tint = if (isFavorite) FavoriteRed else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Tiempos y barra de progreso lineal blanca
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(currentSeconds),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = formatDuration(totalSeconds),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .pointerInput(song.id) {
                                    detectTapGestures { offset ->
                                        if (size.width > 0) {
                                            onSeek((offset.x / size.width).coerceIn(0f, 1f))
                                        }
                                    }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.28f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                        }
                    }

                    // Botones de transporte (Rebobinar, Pausa/Play, Avanzar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Anterior",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Siguiente",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // PANEL DERECHO INTERACTIVO: Página 0 = Letra de la canción, Página 1 = Reloj gigante ("vie 20" + "21 / 12")
            Column(
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera del panel derecho: Tirador + Fecha ("vie 20") + botón '...'
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = { onOptionsClick(song) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF19191E))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // HorizontalPager: Página 0 = Letra en vivo | Página 1 = Reloj gigante apilado
                HorizontalPager(
                    state = rightPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    if (page == 0) {
                        // PÁGINA 0: Primero se ve la letra de la canción
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onLyricsClick() }
                                .padding(horizontal = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "LETRA EN VIVO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!prevLine.isNullOrBlank()) {
                                Text(
                                    text = prevLine,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.42f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = currentLine,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 21.sp,
                                    lineHeight = 27.sp
                                ),
                                color = Color.White,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!nextLine.isNullOrBlank()) {
                                Text(
                                    text = nextLine,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.48f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!nextLine2.isNullOrBlank()) {
                                Text(
                                    text = nextLine2,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.30f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // PÁGINA 1: Al deslizar aparece la hora gigante apilada (21 / 12)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = hourStr,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 92.sp,
                                    lineHeight = 88.sp,
                                    letterSpacing = (-3).sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = minuteStr,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 92.sp,
                                    lineHeight = 88.sp,
                                    letterSpacing = (-3).sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Selector / Indicador inferior deslizable ("Letra  •  Hora")
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val nextPage = if (rightPagerState.currentPage == 0) 1 else 0
                            scope.launch { rightPagerState.animateScrollToPage(nextPage) }
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Letra",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (rightPagerState.currentPage == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (rightPagerState.currentPage == 0) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.45f)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.35f)
                        )
                        Text(
                            text = "Hora",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (rightPagerState.currentPage == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (rightPagerState.currentPage == 1) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsScreen(
    song: Song,
    playbackState: PlaybackState,
    backdropMode: PlayerBackdropMode = PlayerBackdropMode.AURA_HALO,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSeek: (Float) -> Unit,
    onAdjustLyricsOffset: (Long) -> Unit,
    onHideLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = LocalAuraStrings.current
    val visualData = rememberCoverVisualData(
        songId = song.id,
        thumbnailUrl = song.thumbnailUrl,
        fallbackColor = MaterialTheme.colorScheme.primary,
        computeBlurBitmap = backdropMode == PlayerBackdropMode.SOFT_BLUR
    )

    var lyrics by remember(song.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isLoading by remember(song.id) { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val userScrolling by remember {
        derivedStateOf { listState.isScrollInProgress && !isAutoScrolling }
    }

    val notifyUserInteraction = {
        showControls = true
        lastInteractionMs = System.currentTimeMillis()
    }

    // Ocultar automáticamente la caja de controles tras 3.5 segundos de inactividad real
    LaunchedEffect(showControls, lastInteractionMs) {
        if (showControls) {
            delay(3500L)
            showControls = false
        }
    }

    // Solo mostrar controles cuando EL USUARIO desliza, nunca durante el auto-scroll al cambiar de línea
    LaunchedEffect(userScrolling) {
        if (userScrolling) {
            notifyUserInteraction()
        }
    }

    LaunchedEffect(song.id) {
        isLoading = true
        lyrics = AuraApplication.repository.fetchSyncedLyrics(song)
        isLoading = false
    }

    val currentPosMs = playbackState.currentPositionMs + playbackState.lyricsOffsetMs
    val activeIndex = remember(lyrics, currentPosMs) {
        if (lyrics.isEmpty()) -1
        else lyrics.indexOfLast { it.timeMs <= currentPosMs + 300L }.coerceAtLeast(0)
    }

    val isInterlude by remember(lyrics, activeIndex, currentPosMs) {
        derivedStateOf {
            if (lyrics.isEmpty()) false
            else if (currentPosMs + 500L < lyrics.first().timeMs && lyrics.first().timeMs >= 3500L) true
            else if (activeIndex in 0 until lyrics.lastIndex) {
                val gap = lyrics[activeIndex + 1].timeMs - lyrics[activeIndex].timeMs
                gap >= 5500L && (currentPosMs - lyrics[activeIndex].timeMs) > 3200L
            } else false
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyrics.isNotEmpty() && !userScrolling) {
            try {
                isAutoScrolling = true
                listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
            } catch (_: Exception) {
            } finally {
                isAutoScrolling = false
            }
        }
    }

    val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs).coerceIn(0f, 1f)
    } else 0f
    val currentSeconds = playbackState.currentPositionMs / 1000
    val totalSeconds = if (playbackState.durationMs > 0) playbackState.durationMs / 1000 else song.durationSeconds

    val onFg = if (backdropMode == PlayerBackdropMode.SOFT_BLUR) Color.White else MaterialTheme.colorScheme.onBackground
    val onFgMuted = if (backdropMode == PlayerBackdropMode.SOFT_BLUR) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.fillMaxSize()) {
        AuraPlayerBackdrop(
            mode = if (backdropMode == PlayerBackdropMode.SOLID) PlayerBackdropMode.SOLID else backdropMode,
            visualData = visualData,
            isPlaying = playbackState.isPlaying,
            haloCenterYFraction = 0.45f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                    if (showControls) {
                        lastInteractionMs = System.currentTimeMillis()
                    }
                }
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            var headerDragY by remember { mutableFloatStateOf(0f) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(song.id) {
                        detectVerticalDragGestures(
                            onDragStart = { headerDragY = 0f },
                            onVerticalDrag = { _, dy ->
                                headerDragY = (headerDragY + dy).coerceAtLeast(0f)
                            },
                            onDragEnd = {
                                if (headerDragY > 95f) onHideLyrics()
                                headerDragY = 0f
                            },
                            onDragCancel = { headerDragY = 0f }
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onHideLyrics() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraCover(
                        thumbnailUrl = song.thumbnailUrl,
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = onFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = onFgMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (lyrics.isNotEmpty()) {
                                val startIdx = activeIndex.coerceAtLeast(0)
                                val snippet = lyrics.subList(
                                    startIdx,
                                    (startIdx + 3).coerceAtMost(lyrics.size)
                                ).map { it.text }
                                val accentHex = visualData.haloColor.toArgb()
                                scope.launch {
                                    LyricCardSharer.shareLyricCard(
                                        context = context,
                                        song = song,
                                        lyricLines = snippet,
                                        accentHex = accentHex,
                                        coverBitmap = visualData.blurredBitmap
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = strings.shareLyricCard,
                            tint = onFg.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onHideLyrics) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.hideLyrics,
                            tint = onFg,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = visualData.haloColor)
                    }
                    lyrics.isEmpty() -> {
                        Text(
                            text = strings.noLyrics,
                            style = MaterialTheme.typography.bodyLarge,
                            color = onFgMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            itemsIndexed(lyrics) { idx, line ->
                                val isActive = idx == activeIndex
                                val distance = abs(idx - activeIndex)
                                val blurPx = when {
                                    userScrolling || isActive -> 0f
                                    distance == 1 -> 1.8f
                                    distance == 2 -> 4.0f
                                    else -> 6.5f
                                }
                                val targetAlpha = when {
                                    isActive -> 1f
                                    userScrolling -> 0.72f
                                    distance == 1 -> 0.48f
                                    else -> 0.28f
                                }
                                val animatedAlpha by animateFloatAsState(
                                    targetValue = targetAlpha,
                                    animationSpec = tween(durationMillis = 360),
                                    label = "lyricAlpha"
                                )
                                val animatedScale by animateFloatAsState(
                                    targetValue = if (isActive) 1.05f else 0.96f,
                                    animationSpec = tween(durationMillis = 360),
                                    label = "lyricScale"
                                )

                                val haloInt = visualData.haloColor.toArgb()

                                val accentGlow = MaterialTheme.colorScheme.primary
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .drawBehind {
                                            if (isActive) {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            accentGlow.copy(alpha = 0.20f),
                                                            Color.Transparent
                                                        ),
                                                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                                                        radius = size.width * 0.48f
                                                    ),
                                                    radius = size.width * 0.48f,
                                                    center = Offset(size.width * 0.5f, size.height * 0.5f)
                                                )
                                            }
                                        }
                                        .graphicsLayer {
                                            alpha = animatedAlpha
                                            scaleX = animatedScale
                                            scaleY = animatedScale
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0.5f) {
                                                renderEffect = RenderEffect
                                                    .createBlurEffect(blurPx, blurPx, Shader.TileMode.DECAL)
                                                    .asComposeRenderEffect()
                                            }
                                        }
                                        .clip(RoundedCornerShape(14.dp))
                                        .combinedClickable(
                                            onClick = {
                                                notifyUserInteraction()
                                                if (playbackState.durationMs > 0) {
                                                    onSeek((line.timeMs.toFloat() / playbackState.durationMs).coerceIn(0f, 1f))
                                                }
                                            },
                                            onLongClick = {
                                                val snippet = lyrics.subList(
                                                    idx.coerceAtLeast(0),
                                                    (idx + 3).coerceAtMost(lyrics.size)
                                                ).map { it.text }
                                                scope.launch {
                                                    LyricCardSharer.shareLyricCard(
                                                        context = context,
                                                        song = song,
                                                        lyricLines = snippet,
                                                        accentHex = haloInt,
                                                        coverBitmap = visualData.blurredBitmap
                                                    )
                                                }
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = line.text,
                                        style = if (isActive) {
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 26.sp,
                                                lineHeight = 33.sp
                                            )
                                        } else {
                                            MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 18.sp,
                                                lineHeight = 24.sp
                                            )
                                        },
                                        color = if (isActive) {
                                            if (backdropMode == PlayerBackdropMode.SOLID) MaterialTheme.colorScheme.primary else Color.White
                                        } else {
                                            onFgMuted
                                        },
                                        textAlign = TextAlign.Center
                                    )

                                    if (isActive && isInterlude) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        AuraInterludeWaveform(color = accentGlow)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Caja de controles inferior con ocultación automática (solo se muestra al tocar o deslizar)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Ajuste rápido de sincronía (-0.5s / +0.5s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            notifyUserInteraction()
                            onAdjustLyricsOffset(-500L)
                        }) {
                            Text("-0.5s", fontSize = 12.sp, color = onFgMuted)
                        }
                        Text(
                            text = "${strings.syncOffset}: ${String.format("%.1fs", playbackState.lyricsOffsetMs / 1000f)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = onFgMuted
                        )
                        TextButton(onClick = {
                            notifyUserInteraction()
                            onAdjustLyricsOffset(500L)
                        }) {
                            Text("+0.5s", fontSize = 12.sp, color = onFgMuted)
                        }
                    }

                    val lyricsAccent = MaterialTheme.colorScheme.primary
                    val onLyricsAccent = MaterialTheme.colorScheme.onPrimary

                    WaveformVisualizer(
                        progress = progress,
                        songSeed = song.id,
                        barCount = 40,
                        activeColor = lyricsAccent,
                        inactiveColor = onFgMuted.copy(alpha = 0.28f),
                        onSeek = {
                            notifyUserInteraction()
                            onSeek(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentSeconds),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = onFgMuted
                        )
                        Text(
                            text = formatDuration(totalSeconds),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = onFgMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                notifyUserInteraction()
                                onShuffle()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Aleatorio",
                                tint = if (playbackState.isShuffleEnabled) lyricsAccent else onFgMuted
                            )
                        }
                        IconButton(
                            onClick = {
                                notifyUserInteraction()
                                onPrevious()
                            }
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = onFg)
                        }
                        IconButton(
                            onClick = {
                                notifyUserInteraction()
                                onPlayPause()
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .background(lyricsAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = onLyricsAccent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                notifyUserInteraction()
                                onNext()
                            }
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = onFg)
                        }
                        IconButton(
                            onClick = {
                                notifyUserInteraction()
                                onRepeat()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Repetir",
                                tint = if (playbackState.isRepeatEnabled) lyricsAccent else onFgMuted
                            )
                        }
                    }

                    TextButton(
                        onClick = onHideLyrics,
                        colors = ButtonDefaults.textButtonColors(contentColor = onFgMuted),
                        modifier = Modifier.padding(bottom = 14.dp, top = 2.dp)
                    ) {
                        Text(strings.hideLyrics, fontWeight = FontWeight.SemiBold)
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuraInterludeWaveform(color: Color) {
    val transition = rememberInfiniteTransition(label = "auraInterlude")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auraInterludePhase"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(18.dp)
    ) {
        val weights = listOf(0.45f, 0.85f, 1.0f, 0.75f, 0.50f)
        weights.forEachIndexed { idx, base ->
            val factor = if (idx % 2 == 0) base * (0.6f + 0.4f * phase) else base * (1.0f - 0.35f * phase)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((16.dp * factor).coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun QueueScreen(
    currentSong: Song?,
    queue: List<Song>,
    onSongClick: (Song) -> Unit,
    onMoveSong: (String, Int) -> Unit = { _, _ -> },
    onRemoveSong: (String) -> Unit = {},
    onClearUpcoming: () -> Unit = {},
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAuraStrings.current
    val upcomingSongs = remember(queue, currentSong?.id) {
        queue.filter { it.id != currentSong?.id }
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = strings.queue,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (upcomingSongs.isNotEmpty()) {
                    TextButton(onClick = onClearUpcoming) {
                        Text(
                            text = "Limpiar cola (${upcomingSongs.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = strings.nowPlaying,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            currentSong?.let { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.upNext,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        itemsIndexed(upcomingSongs, key = { idx, s -> "queue_${idx}_${s.id}" }) { idx, song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${formatDuration(song.durationSeconds)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { onMoveSong(song.id, -1) },
                    enabled = idx > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Subir",
                        tint = if (idx > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    )
                }

                IconButton(
                    onClick = { onMoveSong(song.id, 1) },
                    enabled = idx < upcomingSongs.lastIndex,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Bajar",
                        tint = if (idx < upcomingSongs.lastIndex) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                    )
                }

                IconButton(
                    onClick = { onRemoveSong(song.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Quitar de la cola",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
