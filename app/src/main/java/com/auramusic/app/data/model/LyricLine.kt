package com.auramusic.app.data.model

data class WordTimestamp(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val translatedText: String = "",
    val words: List<WordTimestamp> = emptyList()
)
