package com.auramusic.app.playback

import com.auramusic.app.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow

object AuraPlaybackBridge {
    val currentSong = MutableStateFlow<Song?>(null)
    val isPlaying = MutableStateFlow<Boolean>(false)
    val queue = MutableStateFlow<List<Song>>(emptyList())
    var currentIndex: Int = 0
    var isShuffle: Boolean = false
    var isRepeat: Boolean = false
    var autoRadioEnabled: Boolean = true
    var skipSilenceEnabled: Boolean = false
    var currentPositionMs: Long = 0L
    var durationMs: Long = 0L
    val activeLyrics = MutableStateFlow<List<com.auramusic.app.data.model.LyricLine>>(emptyList())

    var onPlayPauseRequested: (() -> Unit)? = null
    var onNextRequested: (() -> Unit)? = null
    var onPreviousRequested: (() -> Unit)? = null
    var onSeekRequested: ((Long) -> Unit)? = null
    var onPlayIndexRequested: ((Int) -> Unit)? = null
    var onSkipSilenceChanged: ((Boolean) -> Unit)? = null
}
