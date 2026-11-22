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
    DARK("Obsidiana Estudio", "Negro obsidiana (#090B0F) con cristal esmerilado"),
    AMOLED("Negro Puro AMOLED", "Negro absoluto (#000000) de alto contraste"),
    MIDNIGHT_VIOLET("Noche Violeta", "Atmósfera nocturna con matices púrpura"),
    CYBER_CARBON("Carbono Marino", "Acabado técnico en pizarra y cian oscuro"),
    WARM_ESPRESSO("Café Nocturno", "Tonos cálidos oscuros para sesiones largas"),
    AUTO("Automático", "Sigue la configuración del sistema")
}

enum class AccentColor(val hex: Long, val displayName: String) {
    VIOLET(0xFF7C3AED, "AURA Violeta"),
    GOLD(0xFFF5A623, "Ámbar AURA"),
    CYAN(0xFF06B6D4, "Cian Estudio"),
    BLUE(0xFF3B82F6, "Azul Eléctrico"),
    TEAL(0xFF10B981, "Verde Esmeralda"),
    ORANGE(0xFFF97316, "Naranja Solar"),
    CRIMSON(0xFFE11D48, "Rojo Carmesí"),
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

enum class PlayerBackdropMode(val title: String, val subtitle: String) {
    AURA_HALO("Halo AURA", "Luz cromática focalizada detrás de la carátula"),
    SOFT_BLUR("Desenfoque Total", "Fondo suave cubierto por la carátula desenfocada"),
    SOLID("Sólido / Mate", "Fondo limpio sin desenfoque para ahorrar batería")
}

enum class PlayerStyleMode(val title: String, val subtitle: String) {
    STUDIO_HALO("Studio Halo", "Halo AURA + onda acústica interactiva"),
    FULL_BLEED("Inmersivo Full-Bleed", "Portada de borde a borde con transición difuminada"),
    EDITORIAL_CURVO("Editorial Curvo", "Corte curvo con letra gigante en vivo y botones flotantes"),
    VINILO_ESTUDIO("Tocadiscos Vinilo", "Vinilo giratorio con brazo de aguja, controles laterales y siguiente pista"),
    RELOJ_AMBIENTE("Letras y Reloj", "Portada dual: muestra la letra en vivo y al deslizar revela el reloj gigante")
}

enum class ArtistFeedMode(val title: String, val subtitle: String) {
    MORE_ARTISTS("Más variedad de artistas", "Prioriza descubrir más artistas distintos sin repetir tantas canciones del mismo"),
    FULL_ARTIST("Toda la música por artista", "Muestra el catálogo amplio de cada artista sin limitar repeticiones")
}

enum class AppLanguageOption(val tag: String, val displayName: String, val nativeName: String) {
    SYSTEM("system", "Automático", "Sistema"),
    ES("es", "Español", "Español"),
    EN("en", "Inglés", "English"),
    PT("pt", "Portugués", "Português"),
    FR("fr", "Francés", "Français")
}

data class ListeningStats(
    val totalPlays: Int = 0,
    val totalMinutes: Int = 0,
    val topSongs: List<Pair<Song, Int>> = emptyList(),
    val topArtists: List<Pair<String, Int>> = emptyList(),
)

data class UserSettings(
    val theme: AppTheme = AppTheme.DARK,
    val accentColor: AccentColor = AccentColor.GOLD,
    val audioQuality: AudioQualityOption = AudioQualityOption.HIGH,
    val defaultShuffle: Boolean = false,
    val continuousPlayback: Boolean = true,
    val lyricsMode: LyricsDisplayMode = LyricsDisplayMode.CENTERED,
    val playerBackdropMode: PlayerBackdropMode = PlayerBackdropMode.SOFT_BLUR,
    val playerStyleMode: PlayerStyleMode = PlayerStyleMode.STUDIO_HALO,
    val dynamicSystemBackdrop: Boolean = true,
    val language: AppLanguageOption = AppLanguageOption.SYSTEM,
    val offlineMode: Boolean = false,
    val crossfadeSeconds: Int = 0,
    val artistFeedMode: ArtistFeedMode = ArtistFeedMode.MORE_ARTISTS,
    val fastPlaybackStart: Boolean = true,
    val playbackNotifications: Boolean = true,
    val newMusicNotifications: Boolean = false,
    val appUpdateNotifications: Boolean = true,
)
