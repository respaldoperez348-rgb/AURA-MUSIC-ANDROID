package com.auramusic.app.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val coverUrl: String = "",
    val mediaUri: String = "",
    val isFavorite: Boolean = false,
    val isArtist: Boolean = false
)
