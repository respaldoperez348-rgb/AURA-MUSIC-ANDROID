package com.auramusic.app.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object AuraStreamResolver {

    private var isInitialized = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(7, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Scope propio para resolver streams: sobrevive a la cancelación de la UI y permite
    // que el prefetch y la reproducción compartan el mismo trabajo en curso.
    private val resolverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Máximo 3 resoluciones de prefetch a la vez, para no competir con la canción que
    // el usuario acaba de tocar (la reproducción principal nunca espera este semáforo).
    private val prefetchGate = Semaphore(3)

    // Resoluciones en curso: videoId -> trabajo. Si el usuario toca una canción que ya se
    // está resolviendo por prefetch, espera ese mismo resultado en vez de empezar de cero.
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<String?>>()

    // Si el método rápido (Innertube) no responde en este tiempo, se lanza NewPipe en
    // paralelo y gana el que termine primero.
    private const val HEDGE_DELAY_MS = 700L

    private val YT_ID_REGEX = Regex("^[a-zA-Z0-9_-]{11}$")

    // Cache: videoId -> Pair<StreamUrl, timestampMillis>
    // YouTube audio streams are valid for ~6 hours. We expire after 4 hours.
    private val streamCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private const val CACHE_EXPIRATION_MS = 4 * 60 * 60 * 1000L

    @Volatile
    var preferredQuality: String = "Hi-Res (320 kbps)"

    fun clearCache() {
        streamCache.clear()
    }

    @Synchronized
    fun init(cacheDir: java.io.File? = null) {
        if (cacheDir != null) {
            AuraDownloaderImpl.initCache(cacheDir)
        }
        if (!isInitialized) {
            NewPipe.init(AuraDownloaderImpl.instance)
            isInitialized = true
        }
    }

    fun prefetchAudioStream(videoIdOrUrl: String, fallbackQuery: String? = null) {
        val key = videoIdOrUrl.substringAfter("v=").substringBefore("&").substringAfter("youtu.be/").substringBefore("?")
        if (streamCache.containsKey(key) || streamCache.containsKey(videoIdOrUrl)) return
        if (inFlight.containsKey(key) || inFlight.containsKey(videoIdOrUrl)) return
        resolverScope.launch {
            try {
                prefetchGate.withPermit {
                    resolveAudioStream(videoIdOrUrl, fallbackQuery)
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun resolveAudioStream(videoIdOrUrl: String, fallbackQuery: String? = null): String? = withContext(Dispatchers.IO) {
        init()

        // Fast-path: Check if direct videoIdOrUrl is already in memory cache
        val earlyCache = streamCache[videoIdOrUrl]
        if (earlyCache != null && (System.currentTimeMillis() - earlyCache.second < CACHE_EXPIRATION_MS)) {
            return@withContext earlyCache.first
        }

        var actualId = when {
            videoIdOrUrl.contains("youtu.be/") -> videoIdOrUrl.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").trim()
            videoIdOrUrl.contains("v=") -> videoIdOrUrl.substringAfter("v=").substringBefore("&").trim()
            else -> videoIdOrUrl.trim()
        }

        // 1. If it's an Album or Playlist browseId, resolve its first track
        if (actualId.startsWith("MPREb_") || actualId.startsWith("VL") || actualId.startsWith("PL")) {
            val resolved = YouTubeMusicEngine.resolveFirstTrackFromBrowse(actualId)
            if (!resolved.isNullOrBlank()) {
                actualId = resolved
            }
        }

        // 2. If it's not a standard 11-char YouTube ID (e.g. Spotify 'spot_...', or search query), resolve via search
        if (!YT_ID_REGEX.matches(actualId)) {
            if (!fallbackQuery.isNullOrBlank()) {
                val searched = YouTubeMusicEngine.searchFirstPlayableVideoId(fallbackQuery)
                if (!searched.isNullOrBlank()) {
                    actualId = searched
                } else {
                    return@withContext null
                }
            } else {
                return@withContext null
            }
        }

        // 3. Check in-memory stream cache
        val cached = streamCache[actualId]
        if (cached != null) {
            val (url, timestamp) = cached
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                streamCache[videoIdOrUrl] = cached
                return@withContext url
            } else {
                streamCache.remove(actualId)
            }
        }

        // 4. Resolución compartida: si ya hay una resolución en curso para este video
        //    (por ejemplo un prefetch), se espera ese mismo resultado en vez de duplicar el trabajo.
        val idForJob = actualId
        val job = CompletableDeferred<String?>()
        val running = inFlight.putIfAbsent(idForJob, job)
        if (running != null) {
            val shared = running.await()
            if (!shared.isNullOrBlank()) streamCache[videoIdOrUrl] = Pair(shared, System.currentTimeMillis())
            return@withContext shared
        }

        // La resolución corre en resolverScope (no en el scope del que llama): si el usuario
        // cambia de canción a mitad, el resultado igual queda en caché para la próxima vez.
        resolverScope.launch {
            var resolved: String? = null
            try {
                resolved = raceResolve(idForJob)

                // Respaldo: buscar otro video reproducible con el título + artista
                if (resolved.isNullOrBlank() && !fallbackQuery.isNullOrBlank() && fallbackQuery != idForJob) {
                    val altId = try { YouTubeMusicEngine.searchFirstPlayableVideoId(fallbackQuery) } catch (_: Exception) { null }
                    if (!altId.isNullOrBlank() && altId != idForJob) {
                        resolved = resolveAudioStream(altId, null)
                    }
                }

                if (!resolved.isNullOrBlank()) {
                    val entry = Pair(resolved, System.currentTimeMillis())
                    streamCache[idForJob] = entry
                    streamCache[videoIdOrUrl] = entry
                }
            } catch (_: Throwable) {
                resolved = null
            } finally {
                job.complete(resolved)
                inFlight.remove(idForJob, job)
            }
        }

        job.await()
    }

    /**
     * Intenta primero el método rápido (Innertube). Si no responde en [HEDGE_DELAY_MS] o falla,
     * lanza NewPipe en paralelo. Gana el primero que entregue una URL válida.
     */
    private suspend fun raceResolve(id: String): String? {
        val result = CompletableDeferred<String?>()
        val failures = AtomicInteger(0)
        val fastDone = CompletableDeferred<Unit>()

        fun report(url: String?) {
            if (!url.isNullOrBlank()) {
                result.complete(url)
            } else if (failures.incrementAndGet() >= 2) {
                result.complete(null)
            }
        }

        resolverScope.launch {
            val url = try { resolveViaInnertube(id) } catch (_: Throwable) { null }
            report(url)
            fastDone.complete(Unit)
        }

        resolverScope.launch {
            // Si el método rápido termina antes (bien o mal) no hace falta esperar todo el plazo
            withTimeoutOrNull(HEDGE_DELAY_MS) { fastDone.await() }
            if (result.isCompleted) return@launch
            val url = try { resolveViaNewPipe(id) } catch (_: Throwable) { null }
            report(url)
        }

        return result.await()
    }

    private fun resolveViaNewPipe(actualId: String): String? {
        try {
            val url = "https://www.youtube.com/watch?v=$actualId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)

            val audioStreams = info.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val chosen = when {
                    preferredQuality.contains("96") || preferredQuality.contains("Ahorro", ignoreCase = true) -> {
                        audioStreams.filter { !it.content.isNullOrBlank() }
                            .minByOrNull { it.averageBitrate.takeIf { b -> b > 0 } ?: Int.MAX_VALUE }
                    }
                    preferredQuality.contains("160") || preferredQuality.contains("Estándar", ignoreCase = true) -> {
                        audioStreams.filter { !it.content.isNullOrBlank() }
                            .minByOrNull { kotlin.math.abs(it.averageBitrate - 128) }
                    }
                    else -> {
                        audioStreams.filter { !it.content.isNullOrBlank() }
                            .maxByOrNull { it.averageBitrate }
                    }
                } ?: audioStreams.firstOrNull { !it.content.isNullOrBlank() }

                if (chosen != null && !chosen.content.isNullOrBlank()) {
                    return chosen.content
                }
            }

            val videoStreams = info.videoStreams
            if (!videoStreams.isNullOrEmpty()) {
                val videoWithAudio = videoStreams.firstOrNull { !it.content.isNullOrBlank() }
                if (videoWithAudio != null) {
                    return videoWithAudio.content
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun resolveViaInnertube(videoId: String): String? {
        try {
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "IOS")
                        put("clientVersion", "20.03.02")
                        put("deviceMake", "Apple")
                        put("deviceModel", "iPhone16,2")
                        put("osName", "iPhone")
                        put("osVersion", "18.2.1.22C161")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
            }

            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", "com.google.ios.youtube/20.03.02 (iPhone16,2; U; CPU iOS 18_2_1 like Mac OS X;)")
                .header("Content-Type", "application/json")
                .header("X-YouTube-Client-Name", "5")
                .header("X-YouTube-Client-Version", "20.03.02")
                .build()

            httpClient.newCall(req).execute().use { res ->
                val body = res.body?.string() ?: return null
                val json = JSONObject(body)
                val status = json.optJSONObject("playabilityStatus")?.optString("status")
                if (status != "OK") return null

                val formats = json.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats")
                if (formats != null && formats.length() > 0) {
                    data class Candidate(val url: String, val bitrate: Int, val isMp4: Boolean)
                    val candidates = mutableListOf<Candidate>()

                    for (i in 0 until formats.length()) {
                        val fmt = formats.getJSONObject(i)
                        val mime = fmt.optString("mimeType", "")
                        if (mime.startsWith("audio/")) {
                            val url = fmt.optString("url")
                            val bitrate = fmt.optInt("bitrate", 0)
                            if (url.isNotBlank()) {
                                candidates.add(
                                    Candidate(
                                        url = url,
                                        bitrate = bitrate,
                                        isMp4 = mime.startsWith("audio/mp4")
                                    )
                                )
                            }
                        }
                    }

                    if (candidates.isNotEmpty()) {
                        val chosen = when {
                            preferredQuality.contains("96") || preferredQuality.contains("Ahorro", ignoreCase = true) -> {
                                candidates.minByOrNull { it.bitrate.takeIf { b -> b > 0 } ?: Int.MAX_VALUE }
                            }
                            preferredQuality.contains("160") || preferredQuality.contains("Estándar", ignoreCase = true) -> {
                                candidates.minByOrNull { kotlin.math.abs(it.bitrate - 128_000) }
                            }
                            else -> {
                                // Preferir audio/mp4 (AAC ~130kbps) o el mayor bitrate disponible para arranque instantáneo
                                candidates.maxByOrNull { it.bitrate + (if (it.isMp4) 15_000 else 0) }
                            }
                        }
                        if (chosen != null) return chosen.url
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }

    fun isCached(videoId: String): Boolean {
        val key = videoId.substringAfterLast("v=").substringBefore("&")
        val cached = streamCache[videoId] ?: streamCache[key] ?: return false
        return (System.currentTimeMillis() - cached.second) < CACHE_EXPIRATION_MS
    }
}
