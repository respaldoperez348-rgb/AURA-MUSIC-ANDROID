package com.auramusic.app.data.model

data class ArtistProfile(
    val browseId: String,
    val name: String,
    val bio: String = "",
    val subscribers: String = "",
    val heroCoverUrl: String = "",
    val topTracks: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val singles: List<Album> = emptyList()
)
