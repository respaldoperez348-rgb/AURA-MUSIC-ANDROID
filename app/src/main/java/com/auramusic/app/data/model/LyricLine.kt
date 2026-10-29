package com.auramusic.app.data.model

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val translatedText: String = ""
)
