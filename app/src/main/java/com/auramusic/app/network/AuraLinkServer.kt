package com.auramusic.app.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.auramusic.app.data.model.Song
import com.auramusic.app.playback.AuraPlaybackBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val lastSeenMs: Long = System.currentTimeMillis()
)

data class AuraLinkState(
    val isRunning: Boolean = false,
    val localIp: String = "127.0.0.1",
    val port: Int = 8765,
    val connectedClientsCount: Int = 0,
    val discoveredDevices: List<DiscoveredDevice> = emptyList()
)

object AuraLinkServer {
    private const val TAG = "AuraLinkServer"
    const val DEFAULT_PORT = 8765
    const val DISCOVERY_PORT = 8766

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var discoveryJob: Job? = null
    private var broadcastJob: Job? = null

    private var wifiLock: WifiManager.WifiLock? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var appContext: Context? = null

    private val _state = MutableStateFlow(AuraLinkState())
    val state: StateFlow<AuraLinkState> = _state.asStateFlow()

    private val devicesMap = ConcurrentHashMap<String, DiscoveredDevice>()

    // Callbacks to interact directly with PlayerViewModel/AuraPlaybackBridge
    var onPlayPauseRequested: (() -> Unit)? = null
    var onNextRequested: (() -> Unit)? = null
    var onPreviousRequested: (() -> Unit)? = null
    var onSeekRequested: ((Long) -> Unit)? = null
    var onHandoverReceived: ((Song, List<Song>, Long) -> Unit)? = null
    var onDownloadRequested: ((Song) -> Unit)? = null

    fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val ipInt = wifiManager.connectionInfo.ipAddress
                if (ipInt != 0) {
                    return String.format(
                        java.util.Locale.US,
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                }
            }
            // Fallback: network interfaces
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP: ${e.message}")
        }
        return "127.0.0.1"
    }

    fun start(context: Context, port: Int = DEFAULT_PORT) {
        if (_state.value.isRunning) return

        val appContext = context.applicationContext
        this.appContext = appContext
        val localIp = getLocalIpAddress(appContext)
        _state.update { it.copy(isRunning = true, localIp = localIp, port = port) }

        // Acquire High-Performance Wi-Fi Lock & Multicast Lock to prevent disconnection over distance/sleep
        try {
            val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wifiManager.createWifiLock(mode, "AuraMusic:AuraLinkWifiLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }

                multicastLock = wifiManager.createMulticastLock("AuraMusic:AuraLinkMulticastLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire Wi-Fi locks: ${e.message}")
        }

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    setPerformancePreferences(1, 2, 0)
                    bind(InetSocketAddress("0.0.0.0", port), 64)
                }
                Log.d(TAG, "Aura Link server running on $localIp:$port")

                while (isActive) {
                    val clientSocket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: Exception) {
                        break
                    }
                    scope.launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            } finally {
                stop()
            }
        }

        // Start UDP peer discovery
        startDiscovery(appContext, localIp, port)
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        discoveryJob?.cancel()
        broadcastJob?.cancel()

        try {
            wifiLock?.let { if (it.isHeld) it.release() }
            multicastLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {}
        wifiLock = null
        multicastLock = null

        _state.update { it.copy(isRunning = false, connectedClientsCount = 0) }
    }

    private fun startDiscovery(context: Context, localIp: String, port: Int) {
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val deviceId = "${Build.SERIAL}_$localIp"

        // Broadcast presence every 4 seconds
        broadcastJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket().apply { broadcast = true }
                val broadcastPayload = JSONObject().apply {
                    put("app", "AuraMusic")
                    put("id", deviceId)
                    put("name", deviceName)
                    put("ip", localIp)
                    put("port", port)
                }.toString().toByteArray(Charsets.UTF_8)

                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(broadcastPayload, broadcastPayload.size, broadcastAddr, DISCOVERY_PORT)

                while (isActive && _state.value.isRunning) {
                    try {
                        socket.send(packet)
                    } catch (e: Exception) {}
                    delay(4000)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Broadcast error: ${e.message}")
            } finally {
                socket?.close()
            }
        }

        // Listen for peers on DISCOVERY_PORT
        discoveryJob = scope.launch {
            var listenSocket: DatagramSocket? = null
            try {
                listenSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                }
                val buffer = ByteArray(2048)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive && _state.value.isRunning) {
                    try {
                        listenSocket.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val json = JSONObject(text)
                        if (json.optString("app") == "AuraMusic") {
                            val id = json.optString("id")
                            val ip = json.optString("ip")
                            val peerPort = json.optInt("port", DEFAULT_PORT)
                            val name = json.optString("name", "Aura Peer")

                            if (id != deviceId && ip != localIp) {
                                devicesMap[id] = DiscoveredDevice(id, name, ip, peerPort, System.currentTimeMillis())
                                cleanAndPublishDevices()
                            }
                        }
                    } catch (e: Exception) {
                        delay(500)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Discovery listen error: ${e.message}")
            } finally {
                listenSocket?.close()
            }
        }
    }

    private fun cleanAndPublishDevices() {
        val now = System.currentTimeMillis()
        val valid = devicesMap.values.filter { (now - it.lastSeenMs) < 12000 }
        _state.update { it.copy(discoveredDevices = valid) }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.tcpNoDelay = true
            socket.keepAlive = true
            socket.soTimeout = 30000 // 30s timeout to survive Wi-Fi signal jitter & distance
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0]
            val path = parts[1]

            // Read headers
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read body if POST
            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                reader.read(chars, 0, contentLength)
                String(chars)
            } else ""

            when {
                path == "/" || path == "/index.html" -> serveWebController(out)
                path.startsWith("/api/status") -> serveStatusJson(out)
                path.startsWith("/api/account") -> serveAccountJson(out)
                path.startsWith("/api/library") -> serveLibraryJson(out)
                path.startsWith("/api/control") -> handleControlApi(out, path, body)
                path.startsWith("/api/handover") -> handleHandoverApi(out, body)
                path.startsWith("/api/download") -> handleDownloadApi(out, body)
                else -> sendResponse(out, 404, "text/plain", "Not Found")
            }
        } catch (e: Exception) {
            // Client disconnect or timeout
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun serveStatusJson(out: OutputStream) {
        val current = AuraPlaybackBridge.currentSong.value
        val isPlaying = AuraPlaybackBridge.isPlaying.value
        val queue = AuraPlaybackBridge.queue.value
        val curPos = AuraPlaybackBridge.currentPositionMs
        val dur = AuraPlaybackBridge.durationMs.coerceAtLeast(current?.durationMs ?: 0L)
        val lyrics = AuraPlaybackBridge.activeLyrics.value

        val json = JSONObject().apply {
            put("status", "ok")
            put("isPlaying", isPlaying)
            put("currentPositionMs", curPos)
            put("durationMs", dur)
            put("isShuffle", AuraPlaybackBridge.isShuffle)
            put("isRepeat", AuraPlaybackBridge.isRepeat)
            put("currentIndex", AuraPlaybackBridge.currentIndex)
            put("currentSong", if (current != null) JSONObject().apply {
                put("id", current.id)
                put("title", current.title)
                put("artist", current.artist)
                put("album", current.album)
                put("coverUrl", current.coverUrl)
                put("durationMs", dur)
            } else JSONObject.NULL)
            put("queueCount", queue.size)

            val qArr = org.json.JSONArray()
            queue.take(60).forEachIndexed { idx, song ->
                qArr.put(JSONObject().apply {
                    put("index", idx)
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                    put("coverUrl", song.coverUrl)
                    put("durationMs", song.durationMs)
                    put("isCurrent", song.id == current?.id)
                })
            }
            put("queue", qArr)

            val lArr = org.json.JSONArray()
            lyrics.forEach { line ->
                lArr.put(JSONObject().apply {
                    put("timeMs", line.timestampMs)
                    put("text", line.text)
                    put("translation", line.translatedText)
                })
            }
            put("lyrics", lArr)

            val ytState = YouTubeAuthManager.getState()
            put("account", JSONObject().apply {
                put("isLoggedIn", ytState.isLoggedIn)
                put("accountName", ytState.accountName)
                put("accountHandle", ytState.accountHandle)
                put("avatarUrl", ytState.avatarUrl)
                put("hasCookie", ytState.isLoggedIn)
            })
        }
        sendResponse(out, 200, "application/json", json.toString())
    }

    private fun serveLibraryJson(out: OutputStream) {
        val ctx = appContext
        if (ctx == null) {
            sendResponse(out, 200, "application/json", """{"status":"ok","favorites":[],"followedArtists":[],"savedAlbums":[],"playlists":[]}""")
            return
        }
        try {
            val db = com.auramusic.app.data.database.AuraDatabase.getInstance(ctx)
            val jsonStr = kotlinx.coroutines.runBlocking { db.exportBackupJson() }
            sendResponse(out, 200, "application/json", jsonStr)
        } catch (e: Exception) {
            sendResponse(out, 500, "application/json", """{"status":"error","message":"${e.message}"}""")
        }
    }

    private fun serveAccountJson(out: OutputStream) {
        val ytState = YouTubeAuthManager.getState()
        val json = JSONObject().apply {
            put("status", "ok")
            put("isLoggedIn", ytState.isLoggedIn)
            put("accountName", ytState.accountName)
            put("accountHandle", ytState.accountHandle)
            put("avatarUrl", ytState.avatarUrl)
            put("cookie", YouTubeAuthManager.rawCookie)
        }
        sendResponse(out, 200, "application/json", json.toString())
    }

    private fun handleControlApi(out: OutputStream, path: String, body: String) {
        var action = ""
        var index = -1
        var seekPos = -1L

        if (body.isNotBlank() && body.startsWith("{")) {
            try {
                val json = JSONObject(body)
                action = json.optString("action")
                index = json.optInt("index", -1)
                seekPos = json.optLong("positionMs", -1L)
            } catch (e: Exception) {}
        } else {
            action = path.substringAfter("action=").substringBefore("&")
            if (path.contains("index=")) {
                index = path.substringAfter("index=").substringBefore("&").toIntOrNull() ?: -1
            }
            if (path.contains("positionMs=")) {
                seekPos = path.substringAfter("positionMs=").substringBefore("&").toLongOrNull() ?: -1L
            }
        }

        when (action) {
            "play_pause", "toggle" -> {
                onPlayPauseRequested?.invoke() ?: AuraPlaybackBridge.onPlayPauseRequested?.invoke()
            }
            "next" -> {
                onNextRequested?.invoke() ?: AuraPlaybackBridge.onNextRequested?.invoke()
            }
            "previous", "prev" -> {
                onPreviousRequested?.invoke() ?: AuraPlaybackBridge.onPreviousRequested?.invoke()
            }
            "seek" -> {
                if (seekPos >= 0) {
                    onSeekRequested?.invoke(seekPos) ?: AuraPlaybackBridge.onSeekRequested?.invoke(seekPos)
                }
            }
            "play_index" -> {
                if (index >= 0) {
                    AuraPlaybackBridge.onPlayIndexRequested?.invoke(index)
                }
            }
            "toggle_shuffle" -> {
                AuraPlaybackBridge.isShuffle = !AuraPlaybackBridge.isShuffle
            }
            "toggle_repeat" -> {
                AuraPlaybackBridge.isRepeat = !AuraPlaybackBridge.isRepeat
            }
        }

        serveStatusJson(out)
    }

    private fun handleHandoverApi(out: OutputStream, body: String) {
        try {
            val json = JSONObject(body)
            val songObj = json.getJSONObject("song")
            val song = Song(
                id = songObj.getString("id"),
                title = songObj.getString("title"),
                artist = songObj.getString("artist"),
                album = songObj.optString("album", ""),
                coverUrl = songObj.optString("coverUrl", ""),
                durationMs = songObj.optLong("durationMs", 0L)
            )
            val posMs = json.optLong("positionMs", 0L)

            val queueArray = json.optJSONArray("queue") ?: JSONArray()
            val queue = mutableListOf<Song>()
            for (i in 0 until queueArray.length()) {
                val item = queueArray.getJSONObject(i)
                queue.add(
                    Song(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        artist = item.getString("artist"),
                        album = item.optString("album", ""),
                        coverUrl = item.optString("coverUrl", ""),
                        durationMs = item.optLong("durationMs", 0L)
                    )
                )
            }

            onHandoverReceived?.invoke(song, queue, posMs)
            sendResponse(out, 200, "application/json", """{"status":"handover_accepted"}""")
        } catch (e: Exception) {
            sendResponse(out, 400, "application/json", """{"error":"${e.message}"}""")
        }
    }

    private fun handleDownloadApi(out: OutputStream, body: String) {
        try {
            val json = JSONObject(body)
            val song = Song(
                id = json.getString("id"),
                title = json.getString("title"),
                artist = json.getString("artist"),
                album = json.optString("album", "Aura Download"),
                coverUrl = json.optString("coverUrl", "")
            )
            onDownloadRequested?.invoke(song)
            sendResponse(out, 200, "application/json", """{"status":"enqueued","id":"${song.id}"}""")
        } catch (e: Exception) {
            sendResponse(out, 400, "application/json", """{"error":"${e.message}"}""")
        }
    }

    private fun serveWebController(out: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Aura Music // Studio Web Deck</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
                <style>
                    :root {
                        --bg: #08090C;
                        --panel: rgba(18, 20, 26, 0.75);
                        --panel-border: rgba(255, 255, 255, 0.08);
                        --cyan: #00F0FF;
                        --cyan-glow: rgba(0, 240, 255, 0.35);
                        --purple: #8B5CF6;
                        --text: #F8FAFC;
                        --text-sec: #94A3B8;
                        --text-muted: #64748B;
                        --active-card: rgba(0, 240, 255, 0.12);
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', -apple-system, sans-serif; }
                    body {
                        background: var(--bg);
                        color: var(--text);
                        min-height: 100vh;
                        overflow-x: hidden;
                        display: flex;
                        flex-direction: column;
                        position: relative;
                    }
                    .ambient-bg {
                        position: fixed;
                        top: -20%;
                        left: -20%;
                        width: 140%;
                        height: 140%;
                        background-size: cover;
                        background-position: center;
                        filter: blur(90px) brightness(0.22) saturate(1.8);
                        z-index: 0;
                        transition: background-image 0.8s ease;
                        pointer-events: none;
                    }
                    .noise-overlay {
                        position: fixed;
                        inset: 0;
                        background: radial-gradient(circle at 50% 50%, transparent 0%, rgba(8, 9, 12, 0.8) 100%);
                        z-index: 1;
                        pointer-events: none;
                    }
                    .app-shell {
                        position: relative;
                        z-index: 2;
                        width: 100%;
                        max-width: 1380px;
                        margin: 0 auto;
                        padding: 24px 32px 40px;
                        display: flex;
                        flex-direction: column;
                        min-height: 100vh;
                    }
                    /* Top Header */
                    header {
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                        padding-bottom: 24px;
                        border-bottom: 1px solid var(--panel-border);
                        margin-bottom: 30px;
                    }
                    .brand {
                        display: flex;
                        align-items: center;
                        gap: 14px;
                    }
                    .brand-logo {
                        width: 38px;
                        height: 38px;
                        border-radius: 10px;
                        background: linear-gradient(135deg, var(--cyan), var(--purple));
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 0 20px var(--cyan-glow);
                    }
                    .brand-logo svg { width: 22px; height: 22px; fill: #000; }
                    .brand-name {
                        font-weight: 800;
                        font-size: 20px;
                        letter-spacing: -0.5px;
                    }
                    .brand-name span { color: var(--cyan); }
                    .connection-pill {
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        background: rgba(0, 240, 255, 0.08);
                        border: 1px solid rgba(0, 240, 255, 0.2);
                        padding: 6px 14px;
                        border-radius: 999px;
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 12px;
                        color: var(--cyan);
                    }
                    .pulse-dot {
                        width: 8px;
                        height: 8px;
                        border-radius: 50%;
                        background: var(--cyan);
                        box-shadow: 0 0 10px var(--cyan);
                        animation: pulse 1.6s infinite;
                    }
                    @keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(0.85); } }
                    /* Main Grid */
                    .main-grid {
                        display: grid;
                        grid-template-columns: 1.15fr 0.85fr;
                        gap: 32px;
                        flex: 1;
                        align-items: start;
                    }
                    @media (max-width: 960px) {
                        .main-grid { grid-template-columns: 1fr; }
                    }
                    /* Left Column: Player Console */
                    .player-console {
                        background: var(--panel);
                        backdrop-filter: blur(24px);
                        -webkit-backdrop-filter: blur(24px);
                        border: 1px solid var(--panel-border);
                        border-radius: 28px;
                        padding: 36px 32px;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        text-align: center;
                        box-shadow: 0 24px 60px rgba(0,0,0,0.5);
                    }
                    .cover-art-wrapper {
                        position: relative;
                        width: 320px;
                        height: 320px;
                        margin-bottom: 30px;
                    }
                    .cover-art-glow {
                        position: absolute;
                        inset: 12px;
                        background-size: cover;
                        background-position: center;
                        filter: blur(35px);
                        opacity: 0.7;
                        border-radius: 24px;
                        z-index: 0;
                        transition: all 0.5s ease;
                    }
                    .cover-art {
                        position: relative;
                        z-index: 1;
                        width: 100%;
                        height: 100%;
                        border-radius: 22px;
                        object-fit: cover;
                        border: 1px solid rgba(255, 255, 255, 0.15);
                        box-shadow: 0 16px 40px rgba(0,0,0,0.6);
                    }
                    .track-meta {
                        width: 100%;
                        margin-bottom: 24px;
                    }
                    .badge-specs {
                        display: inline-block;
                        padding: 4px 10px;
                        border-radius: 6px;
                        background: rgba(255, 255, 255, 0.05);
                        border: 1px solid rgba(255, 255, 255, 0.08);
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 11px;
                        color: var(--cyan);
                        letter-spacing: 1px;
                        margin-bottom: 12px;
                    }
                    .track-title {
                        font-size: 26px;
                        font-weight: 800;
                        letter-spacing: -0.5px;
                        margin-bottom: 6px;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        max-width: 100%;
                    }
                    .track-artist {
                        font-size: 16px;
                        color: var(--text-sec);
                        font-weight: 500;
                    }
                    .track-album {
                        font-size: 13px;
                        color: var(--text-muted);
                        margin-top: 4px;
                    }
                    /* Timeline Scrubber */
                    .timeline-box {
                        width: 100%;
                        margin-bottom: 28px;
                    }
                    .scrub-bar {
                        width: 100%;
                        height: 6px;
                        background: rgba(255, 255, 255, 0.1);
                        border-radius: 999px;
                        position: relative;
                        cursor: pointer;
                        overflow: hidden;
                        transition: height 0.15s ease;
                    }
                    .scrub-bar:hover { height: 10px; }
                    .scrub-fill {
                        position: absolute;
                        top: 0;
                        left: 0;
                        bottom: 0;
                        width: 0%;
                        background: linear-gradient(90deg, var(--purple), var(--cyan));
                        border-radius: 999px;
                        box-shadow: 0 0 12px var(--cyan-glow);
                    }
                    .time-labels {
                        display: flex;
                        justify-content: space-between;
                        margin-top: 8px;
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 12px;
                        color: var(--text-muted);
                    }
                    /* Primary Controls */
                    .playback-controls {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 22px;
                        margin-bottom: 24px;
                    }
                    .btn-ctrl {
                        background: rgba(255, 255, 255, 0.05);
                        border: 1px solid rgba(255, 255, 255, 0.1);
                        color: var(--text);
                        width: 52px;
                        height: 52px;
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        cursor: pointer;
                        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
                    }
                    .btn-ctrl:hover {
                        background: rgba(0, 240, 255, 0.15);
                        border-color: var(--cyan);
                        transform: translateY(-2px);
                    }
                    .btn-ctrl:active { transform: translateY(1px); }
                    .btn-ctrl svg { width: 22px; height: 22px; fill: currentColor; }
                    .btn-ctrl.btn-main {
                        width: 76px;
                        height: 76px;
                        background: var(--cyan);
                        color: #000;
                        border: none;
                        box-shadow: 0 0 30px var(--cyan-glow);
                    }
                    .btn-ctrl.btn-main:hover {
                        background: #fff;
                        box-shadow: 0 0 40px rgba(255, 255, 255, 0.6);
                        transform: scale(1.05);
                    }
                    .btn-ctrl.btn-main svg { width: 34px; height: 34px; }
                    .btn-ctrl.active-toggle {
                        color: var(--cyan);
                        border-color: var(--cyan);
                        background: rgba(0, 240, 255, 0.12);
                    }
                    /* Shortcut Hints */
                    .keyboard-hints {
                        display: flex;
                        gap: 16px;
                        font-size: 11px;
                        color: var(--text-muted);
                        font-family: 'JetBrains Mono', monospace;
                    }
                    .keyboard-hints kbd {
                        background: rgba(255, 255, 255, 0.06);
                        border: 1px solid rgba(255, 255, 255, 0.12);
                        border-radius: 4px;
                        padding: 2px 6px;
                        color: var(--text-sec);
                    }
                    /* Right Column: Queue & Lyrics Hub */
                    .deck-hub {
                        background: var(--panel);
                        backdrop-filter: blur(24px);
                        -webkit-backdrop-filter: blur(24px);
                        border: 1px solid var(--panel-border);
                        border-radius: 28px;
                        overflow: hidden;
                        display: flex;
                        flex-direction: column;
                        height: 640px;
                        box-shadow: 0 24px 60px rgba(0,0,0,0.5);
                    }
                    .hub-tabs {
                        display: flex;
                        padding: 16px 20px;
                        background: rgba(0, 0, 0, 0.25);
                        border-bottom: 1px solid var(--panel-border);
                        gap: 12px;
                    }
                    .hub-tab {
                        background: transparent;
                        border: none;
                        color: var(--text-muted);
                        font-weight: 700;
                        font-size: 14px;
                        padding: 8px 16px;
                        border-radius: 10px;
                        cursor: pointer;
                        transition: all 0.2s;
                    }
                    .hub-tab.active {
                        background: rgba(255, 255, 255, 0.08);
                        color: var(--text);
                    }
                    .hub-tab.active span {
                        color: var(--cyan);
                    }
                    .hub-body {
                        flex: 1;
                        overflow-y: auto;
                        padding: 12px;
                    }
                    /* Queue List */
                    .queue-list {
                        display: flex;
                        flex-direction: column;
                        gap: 6px;
                    }
                    .queue-item {
                        display: flex;
                        align-items: center;
                        gap: 14px;
                        padding: 10px 14px;
                        border-radius: 12px;
                        background: transparent;
                        cursor: pointer;
                        transition: all 0.2s;
                        border: 1px solid transparent;
                    }
                    .queue-item:hover {
                        background: rgba(255, 255, 255, 0.04);
                    }
                    .queue-item.playing {
                        background: var(--active-card);
                        border-color: rgba(0, 240, 255, 0.25);
                    }
                    .queue-idx {
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 12px;
                        color: var(--text-muted);
                        width: 22px;
                    }
                    .queue-item.playing .queue-idx {
                        color: var(--cyan);
                        font-weight: 700;
                    }
                    .queue-cover {
                        width: 44px;
                        height: 44px;
                        border-radius: 8px;
                        object-fit: cover;
                        background: #000;
                    }
                    .queue-info {
                        flex: 1;
                        min-width: 0;
                    }
                    .queue-title {
                        font-weight: 600;
                        font-size: 14px;
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                    }
                    .queue-item.playing .queue-title {
                        color: var(--cyan);
                    }
                    .queue-artist {
                        font-size: 12px;
                        color: var(--text-sec);
                        white-space: nowrap;
                        overflow: hidden;
                        text-overflow: ellipsis;
                    }
                    .queue-dur {
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 12px;
                        color: var(--text-muted);
                    }
                    /* Lyrics Container */
                    .lyrics-box {
                        display: flex;
                        flex-direction: column;
                        gap: 20px;
                        padding: 24px 20px;
                        text-align: center;
                    }
                    .lyric-line {
                        font-size: 19px;
                        font-weight: 600;
                        color: var(--text-muted);
                        line-height: 1.5;
                        transition: all 0.25s;
                    }
                    .lyric-line.active {
                        color: #FFF;
                        font-size: 23px;
                        font-weight: 800;
                        text-shadow: 0 0 20px var(--cyan);
                        transform: scale(1.03);
                    }
                    .lyric-trans {
                        display: block;
                        font-size: 14px;
                        font-weight: 500;
                        color: var(--text-sec);
                        margin-top: 4px;
                    }
                </style>
            </head>
            <body>
                <div class="ambient-bg" id="ambientBg"></div>
                <div class="noise-overlay"></div>

                <div class="app-shell">
                    <header>
                        <div class="brand">
                            <div class="brand-logo">
                                <svg viewBox="0 0 24 24"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>
                            </div>
                            <div class="brand-name">AURA <span>MUSIC</span></div>
                        </div>

                        <div class="connection-pill">
                            <div class="pulse-dot"></div>
                            <span>P2P LAN CONNECT // DIRECT AUDIO</span>
                        </div>
                    </header>

                    <div class="main-grid">
                        <!-- Left Console -->
                        <div class="player-console">
                            <div class="cover-art-wrapper">
                                <div class="cover-art-glow" id="coverGlow"></div>
                                <img class="cover-art" id="coverArt" src="https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600" alt="Cover">
                            </div>

                            <div class="track-meta">
                                <div class="badge-specs">HI-RES 320 KBPS // LOSSLESS PIPELINE</div>
                                <h1 class="track-title" id="trackTitle">Conectando...</h1>
                                <div class="track-artist" id="trackArtist">Aura Music Client</div>
                                <div class="track-album" id="trackAlbum"></div>
                            </div>

                            <div class="timeline-box">
                                <div class="scrub-bar" id="scrubBar">
                                    <div class="scrub-fill" id="scrubFill"></div>
                                </div>
                                <div class="time-labels">
                                    <span id="curTime">00:00</span>
                                    <span id="totalTime">00:00</span>
                                </div>
                            </div>

                            <div class="playback-controls">
                                <button class="btn-ctrl" id="btnShuffle" onclick="sendAction('toggle_shuffle')" title="Modo Aleatorio">
                                    <svg viewBox="0 0 24 24"><path d="M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z"/></svg>
                                </button>
                                <button class="btn-ctrl" onclick="sendAction('previous')" title="Anterior">
                                    <svg viewBox="0 0 24 24"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
                                </button>
                                <button class="btn-ctrl btn-main" onclick="sendAction('play_pause')" title="Play / Pausa">
                                    <svg id="playIcon" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                                </button>
                                <button class="btn-ctrl" onclick="sendAction('next')" title="Siguiente">
                                    <svg viewBox="0 0 24 24"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
                                </button>
                                <button class="btn-ctrl" id="btnRepeat" onclick="sendAction('toggle_repeat')" title="Repetir">
                                    <svg viewBox="0 0 24 24"><path d="M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z"/></svg>
                                </button>
                            </div>

                            <div class="keyboard-hints">
                                <span><kbd>Espacio</kbd> Play/Pausa</span>
                                <span><kbd>←</kbd> <kbd>→</kbd> Cambiar</span>
                                <span><kbd>F</kbd> Pantalla Completa</span>
                            </div>
                        </div>

                        <!-- Right Panel: Queue / Lyrics -->
                        <div class="deck-hub">
                            <div class="hub-tabs">
                                <button class="hub-tab active" id="tabQueue" onclick="switchTab('queue')">A continuación <span id="queueCountBadge"></span></button>
                                <button class="hub-tab" id="tabLyrics" onclick="switchTab('lyrics')">Letras en vivo</button>
                            </div>

                            <div class="hub-body">
                                <div id="panelQueue" class="queue-list">
                                    <!-- Populated via JS -->
                                </div>

                                <div id="panelLyrics" class="lyrics-box" style="display: none;">
                                    <p class="lyric-line">Letras sincronizadas preparadas...</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <script>
                    let currentPosMs = 0;
                    let durationMs = 0;
                    let isPlaying = false;
                    let lastPollTs = Date.now();
                    let lyricsData = [];

                    function formatMs(ms) {
                        const totalSec = Math.max(0, Math.floor(ms / 1000));
                        const m = Math.floor(totalSec / 60);
                        const s = totalSec % 60;
                        return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
                    }

                    function switchTab(tab) {
                        const tabQ = document.getElementById('tabQueue');
                        const tabL = document.getElementById('tabLyrics');
                        const pQ = document.getElementById('panelQueue');
                        const pL = document.getElementById('panelLyrics');

                        if (tab === 'queue') {
                            tabQ.classList.add('active');
                            tabL.classList.remove('active');
                            pQ.style.display = 'flex';
                            pL.style.display = 'none';
                        } else {
                            tabL.classList.add('active');
                            tabQ.classList.remove('active');
                            pL.style.display = 'flex';
                            pQ.style.display = 'none';
                        }
                    }

                    let consecutiveErrors = 0;
                    async function fetchStatus() {
                        try {
                            const controller = new AbortController();
                            const timeoutId = setTimeout(() => controller.abort(), 8000);
                            const res = await fetch('/api/status', { signal: controller.signal });
                            clearTimeout(timeoutId);
                            const data = await res.json();
                            consecutiveErrors = 0;
                            const statusPill = document.querySelector('.connection-pill span');
                            if (statusPill) statusPill.textContent = 'P2P LAN CONNECT // DIRECT AUDIO';

                            isPlaying = data.isPlaying || false;
                            currentPosMs = data.currentPositionMs || 0;
                            durationMs = data.durationMs || 0;
                            lastPollTs = Date.now();

                            if (data.currentSong) {
                                document.getElementById('trackTitle').textContent = data.currentSong.title || 'Desconocido';
                                document.getElementById('trackArtist').textContent = data.currentSong.artist || 'Aura Music';
                                document.getElementById('trackAlbum').textContent = data.currentSong.album || '';

                                const cover = data.currentSong.coverUrl || 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600';
                                document.getElementById('coverArt').src = cover;
                                document.getElementById('coverGlow').style.backgroundImage = 'url(' + cover + ')';
                                document.getElementById('ambientBg').style.backgroundImage = 'url(' + cover + ')';
                            }

                            // Play / Pause Icon
                            const playIcon = document.getElementById('playIcon');
                            if (isPlaying) {
                                playIcon.innerHTML = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
                            } else {
                                playIcon.innerHTML = '<path d="M8 5v14l11-7z"/>';
                            }

                            // Shuffle & Repeat buttons
                            const bShuf = document.getElementById('btnShuffle');
                            if (data.isShuffle) bShuf.classList.add('active-toggle'); else bShuf.classList.remove('active-toggle');

                            const bRep = document.getElementById('btnRepeat');
                            if (data.isRepeat) bRep.classList.add('active-toggle'); else bRep.classList.remove('active-toggle');

                            // Queue render
                            if (Array.isArray(data.queue)) {
                                document.getElementById('queueCountBadge').textContent = '(' + data.queue.length + ')';
                                renderQueue(data.queue, data.currentSong ? data.currentSong.id : '');
                            }

                            // Lyrics
                            if (Array.isArray(data.lyrics)) {
                                lyricsData = data.lyrics;
                                renderLyrics();
                            }

                            updateTimelineUI();
                        } catch (e) {
                            consecutiveErrors++;
                            if (consecutiveErrors >= 3) {
                                const statusPill = document.querySelector('.connection-pill span');
                                if (statusPill) statusPill.textContent = 'SEÑAL WI-FI DÉBIL // RECONECTANDO...';
                            }
                        }
                    }

                    function renderQueue(queue, activeId) {
                        const container = document.getElementById('panelQueue');
                        let html = '';
                        queue.forEach((item, idx) => {
                            const isCur = item.id === activeId || item.isCurrent;
                            const cover = item.coverUrl || 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=120';
                            html += '<div class="queue-item ' + (isCur ? 'playing' : '') + '" onclick="playIndex(' + idx + ')">' +
                                '<div class="queue-idx">' + (isCur ? '▶' : (idx + 1)) + '</div>' +
                                '<img class="queue-cover" src="' + cover + '" alt="Cover">' +
                                '<div class="queue-info">' +
                                    '<div class="queue-title">' + escapeHtml(item.title) + '</div>' +
                                    '<div class="queue-artist">' + escapeHtml(item.artist) + '</div>' +
                                '</div>' +
                                '<div class="queue-dur">' + (item.durationMs > 0 ? formatMs(item.durationMs) : '') + '</div>' +
                            '</div>';
                        });
                        container.innerHTML = html;
                    }

                    function renderLyrics() {
                        const box = document.getElementById('panelLyrics');
                        if (!lyricsData.length) {
                            box.innerHTML = '<p class="lyric-line">Letras sincronizadas no disponibles para esta pista.</p>';
                            return;
                        }
                        let html = '';
                        lyricsData.forEach((l) => {
                            const isActive = (currentPosMs >= l.timeMs);
                            html += '<div class="lyric-line ' + (isActive ? 'active' : '') + '">' +
                                escapeHtml(l.text) +
                                (l.translation ? '<span class="lyric-trans">' + escapeHtml(l.translation) + '</span>' : '') +
                            '</div>';
                        });
                        box.innerHTML = html;
                    }

                    function updateTimelineUI() {
                        let cur = currentPosMs;
                        if (isPlaying) {
                            cur += (Date.now() - lastPollTs);
                        }
                        if (durationMs > 0 && cur > durationMs) cur = durationMs;

                        document.getElementById('curTime').textContent = formatMs(cur);
                        document.getElementById('totalTime').textContent = formatMs(durationMs);

                        const pct = durationMs > 0 ? (cur / durationMs) * 100 : 0;
                        document.getElementById('scrubFill').style.width = Math.min(100, Math.max(0, pct)) + '%';
                    }

                    function escapeHtml(str) {
                        return (str || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
                    }

                    async function sendAction(action, payload = {}) {
                        try {
                            await fetch('/api/control', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify(Object.assign({action: action}, payload))
                            });
                            fetchStatus();
                        } catch (e) {}
                    }

                    function playIndex(idx) {
                        sendAction('play_index', { index: idx });
                    }

                    // Scrubber Click Handler
                    document.getElementById('scrubBar').addEventListener('click', function(e) {
                        if (durationMs <= 0) return;
                        const rect = this.getBoundingClientRect();
                        const clickX = e.clientX - rect.left;
                        const pct = Math.max(0, Math.min(1, clickX / rect.width));
                        const targetMs = Math.floor(pct * durationMs);
                        currentPosMs = targetMs;
                        updateTimelineUI();
                        sendAction('seek', { positionMs: targetMs });
                    });

                    // Keyboard shortcuts
                    window.addEventListener('keydown', function(e) {
                        if (e.target.tagName === 'INPUT') return;
                        if (e.code === 'Space') {
                            e.preventDefault();
                            sendAction('play_pause');
                        } else if (e.code === 'ArrowRight') {
                            sendAction('next');
                        } else if (e.code === 'ArrowLeft') {
                            sendAction('previous');
                        } else if (e.key === 'f' || e.key === 'F') {
                            if (!document.fullscreenElement) {
                                document.documentElement.requestFullscreen().catch(()=>{});
                            } else {
                                document.exitFullscreen().catch(()=>{});
                            }
                        }
                    });

                    // Animation Loop for smooth scrub bar
                    setInterval(updateTimelineUI, 250);
                    setInterval(fetchStatus, 1200);
                    fetchStatus();
                </script>
            </body>
            </html>
        """.trimIndent()

        sendResponse(out, 200, "text/html; charset=UTF-8", html)
    }

    private fun sendResponse(out: OutputStream, code: Int, contentType: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code OK\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }
}

