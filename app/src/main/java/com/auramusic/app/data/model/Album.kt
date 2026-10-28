package com.auramusic.app.data.model

data class Album(
    val browseId: String,
    val title: String,
    val year: String = "",
    val type: String = "Álbum",
    val coverUrl: String = ""
)
