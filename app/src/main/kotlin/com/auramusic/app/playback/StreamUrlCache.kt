package com.auramusic.app.playback

data class CachedStreamUrl(
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val clientName: String = "ANDROID",
    val expiresInSeconds: Int = 3600 * 5,
    val qualityLabel: String = "AUDIO • HQ",
)

/**
 * Cache LRU en memoria para URLs de streaming resueltas.
 * Proporciona una latencia de 0 ms en la reproducción de canciones en cola o reanudadas.
 */
class StreamUrlCache(
    private val maxEntries: Int = 300,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private data class Entry(
        val stream: CachedStreamUrl,
        val expiresAtMillis: Long,
    )

    private val entries = object : LinkedHashMap<String, Entry>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean =
            size > maxEntries
    }

    operator fun get(mediaId: String): CachedStreamUrl? = synchronized(entries) {
        val entry = entries[mediaId] ?: return@synchronized null
        if (entry.expiresAtMillis <= currentTimeMillis()) {
            entries.remove(mediaId)
            null
        } else {
            entry.stream
        }
    }

    fun put(
        mediaId: String,
        url: String,
        requestHeaders: Map<String, String> = emptyMap(),
        clientName: String = "ANDROID",
        expiresInSeconds: Int = 3600 * 5,
        qualityLabel: String = "AUDIO • HQ",
    ) {
        val now = currentTimeMillis()
        val ttlMillis = expiresInSeconds.coerceAtLeast(60).toLong() * 1000L
        val expiresAtMillis = runCatching { Math.addExact(now, ttlMillis) }.getOrDefault(Long.MAX_VALUE)

        synchronized(entries) {
            entries[mediaId] = Entry(
                stream = CachedStreamUrl(url, requestHeaders, clientName, expiresInSeconds, qualityLabel),
                expiresAtMillis = expiresAtMillis
            )
        }
    }

    fun invalidate(mediaId: String) {
        synchronized(entries) {
            entries.remove(mediaId)
        }
    }

    fun clear() {
        synchronized(entries) {
            entries.clear()
        }
    }
}
