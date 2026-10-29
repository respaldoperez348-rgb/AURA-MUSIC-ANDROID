package com.auramusic.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.model.LyricLine
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextSecondary

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    lyricsOffsetMs: Long = -200L,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Compensate for audio hardware latency so UI does not advance prematurely
    val effectivePosition = (currentPositionMs + lyricsOffsetMs).coerceAtLeast(0L)

    // Find active index strictly when current audio reached timestamp
    val activeIndex = lyrics.indexOfLast { it.timestampMs <= effectivePosition }

    // Smooth auto-scroll to keep active line centered
    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && activeIndex in lyrics.indices) {
            listState.animateScrollToItem(
                index = (activeIndex - 2).coerceAtLeast(0)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (lyrics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No synced lyrics available",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 120.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = activeIndex >= 0 && index == activeIndex
                    val isUpcoming = activeIndex < 0 || index > activeIndex

                    val textColor by animateColorAsState(
                        targetValue = when {
                            isActive -> CyberCyan
                            isUpcoming -> TextMuted.copy(alpha = 0.5f)
                            else -> TextSecondary.copy(alpha = 0.6f)
                        },
                        label = "lyric_color"
                    )

                    val textScale by animateFloatAsState(
                        targetValue = if (isActive) 1.08f else 1.0f,
                        label = "lyric_scale"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeekTo(line.timestampMs) }
                            .padding(vertical = 14.dp)
                            .scale(textScale),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = line.text,
                            color = textColor,
                            fontSize = if (isActive) 22.sp else 18.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 30.sp
                        )
                    }
                }
            }
        }
    }
}
