package com.auramusic.app.playback

import com.auramusic.app.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow

object AuraPlaybackBridge {
    val currentSong = MutableStateFlow<Song?>(null)
    val queue = MutableStateFlow<List<Song>>(emptyList())
    var currentIndex: Int = 0

    var onNextRequested: (() -> Unit)? = null
    var onPreviousRequested: (() -> Unit)? = null
}
