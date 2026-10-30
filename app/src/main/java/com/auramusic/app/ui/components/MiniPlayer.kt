package com.auramusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auramusic.app.data.model.Song
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.GlassBorderSubtle
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long = 0L,
    durationMs: Long = 0L,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (song.id.isBlank()) return

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    val progress = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Floating Capsule MiniPlayer (as in reference screenshot)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(36.dp),
                spotColor = Color.Black.copy(alpha = 0.65f)
            )
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xFF14161C))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(36.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY)) {
                            if (totalDragX < -90f) {
                                onNext()
                            } else if (totalDragX > 90f) {
                                onPrevious()
                            }
                        } else if (totalDragY < -70f) {
                            onExpand()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                )
            }
            .clickable { onExpand() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Artwork with Circular Progress Arc Ring
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFFA8C7FA),
                    strokeWidth = 2.5.dp,
                    strokeCap = StrokeCap.Round,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceActive),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track & Artist Hierarchy
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = song.artist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Circular Heart Favorite Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) CyberPink else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Prominent Solid Tinted Circular Play/Pause Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA8C7FA))
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color(0xFF0F141C),
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}
