package com.auramusic.app.data.models

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val thumbnailUrl: String,
    val durationSeconds: Long = 0,
    val streamUrl: String? = null,
    val localFilePath: String? = null,
    val isFavorite: Boolean = false,
)

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val monthlyListeners: String? = null,
    val bio: String? = null,
    val isFollowing: Boolean = false,
    val topSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val singles: List<Album> = emptyList(),
    val relatedArtists: List<Artist> = emptyList(),
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: String? = null,
    val thumbnailUrl: String,
    val songCount: Int = 0,
    val songs: List<Song> = emptyList(),
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
)

enum class AppTheme(val title: String, val subtitle: String) {
    LIGHT("Claro", "Fondo luminoso, limpio y elegante"),
    DARK("Oscuro Estudio", "Azul noche profundo (#080B11) ideal de noche"),
    AMOLED("Negro Puro AMOLED", "Negro absoluto (#000000) de alto contraste"),
    MIDNIGHT_VIOLET("Noche Violeta", "Atmósfera nocturna con matices púrpura"),
    CYBER_CARBON("Carbono Marino", "Acabado técnico en pizarra y cian oscuro"),
    WARM_ESPRESSO("Café Nocturno", "Tonos cálidos oscuros para sesiones largas"),
    AUTO("Automático", "Sigue la configuración del sistema")
}

enum class AccentColor(val hex: Long, val displayName: String) {
    VIOLET(0xFF7C3AED, "AURA Violeta"),
    GOLD(0xFFD9B44A, "Oro Imperial"),
    CYAN(0xFF06B6D4, "Cian Estudio"),
    BLUE(0xFF3B82F6, "Azul Eléctrico"),
    TEAL(0xFF10B981, "Verde Esmeralda"),
    ORANGE(0xFFF97316, "Naranja Solar"),
    CRIMSON(0xFFF43F5E, "Rojo Carmesí"),
    SAKURA(0xFFEC4899, "Rosa Sakura"),
    LAVENDER(0xFFA855F7, "Lavanda"),
    LIME(0xFF84CC16, "Lima Neón"),
    COPPER(0xFFD97706, "Ámbar Cobre"),
    GRAPHITE(0xFF475569, "Plata / Grafito")
}

enum class AudioQualityOption {
    AUTO,
    STANDARD,
    HIGH
}

enum class LyricsDisplayMode {
    CENTERED,
    WITH_CONTROLS,
    HIDDEN
}

data class UserSettings(
    val theme: AppTheme = AppTheme.LIGHT,
    val accentColor: AccentColor = AccentColor.VIOLET,
    val audioQuality: AudioQualityOption = AudioQualityOption.HIGH,
    val continuousPlayback: Boolean = true,
    val lyricsMode: LyricsDisplayMode = LyricsDisplayMode.CENTERED,
    val playbackNotifications: Boolean = true,
    val newMusicNotifications: Boolean = false,
    val appUpdateNotifications: Boolean = true,
)
