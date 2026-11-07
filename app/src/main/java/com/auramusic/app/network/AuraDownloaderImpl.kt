package com.auramusic.app.network

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class AuraDownloaderImpl private constructor(private val client: OkHttpClient) : Downloader() {

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
        const val YOUTUBE_PREFS_COOKIE = "PREF=hl=es&gl=US"

        @Volatile
        private var sharedClient: OkHttpClient? = null

        @Volatile
        private var currentInstance: AuraDownloaderImpl? = null

        @Volatile
        private var cachedVisitorResponse: Response? = null

        @Volatile
        private var cachedVisitorTimestampMs: Long = 0L

        private const val VISITOR_CACHE_TTL_MS = 20 * 60 * 1000L

        fun invalidateVisitorCache() {
            cachedVisitorResponse = null
            cachedVisitorTimestampMs = 0L
        }

        fun initCache(cacheDir: java.io.File) {
            synchronized(this) {
                if (sharedClient?.cache == null) {
                    val builder = OkHttpClient.Builder()
                        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(6, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                    try {
                        val httpCache = okhttp3.Cache(java.io.File(cacheDir, "newpipe_http_cache"), 64L * 1024L * 1024L)
                        builder.cache(httpCache)
                    } catch (_: Exception) {}
                    val client = builder.build()
                    sharedClient = client
                    currentInstance = AuraDownloaderImpl(client)
                }
            }
        }

        val instance: AuraDownloaderImpl
            get() {
                currentInstance?.let { return it }
                return synchronized(this) {
                    currentInstance ?: run {
                        val client = sharedClient ?: OkHttpClient.Builder()
                            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .readTimeout(6, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build()
                        AuraDownloaderImpl(client).also { currentInstance = it }
                    }
                }
            }
    }

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        if (url.contains("/visitor_id")) {
            val cached = cachedVisitorResponse
            if (cached != null && (System.currentTimeMillis() - cachedVisitorTimestampMs) < VISITOR_CACHE_TTL_MS) {
                return cached
            }
        }
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val reqBuilder = okhttp3.Request.Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        if (url.contains("youtube.com")) {
            reqBuilder.addHeader("Cookie", YOUTUBE_PREFS_COOKIE)
        }

        // Apply headers from NewPipe, overriding defaults if specified
        headers?.forEach { (name, values) ->
            reqBuilder.removeHeader(name)
            values.forEach { value ->
                reqBuilder.addHeader(name, value)
            }
        }

        when (httpMethod) {
            "GET" -> reqBuilder.get()
            "HEAD" -> reqBuilder.head()
            "POST" -> {
                val contentType = reqBuilder.build().header("Content-Type")
                val body = if (dataToSend != null) {
                    dataToSend.toRequestBody(null)
                } else {
                    ByteArray(0).toRequestBody(null)
                }
                reqBuilder.post(body)
            }
            else -> throw IOException("Unsupported method: $httpMethod")
        }

        val response = client.newCall(reqBuilder.build()).execute()
        val responseBody = response.body?.string() ?: ""
        val responseHeaders = mutableMapOf<String, List<String>>()
        response.headers.names().forEach { name ->
            responseHeaders[name] = response.headers.values(name)
        }

        val resultResponse = Response(
            response.code,
            response.message,
            responseHeaders,
            responseBody,
            response.request.url.toString()
        )
        if (url.contains("/visitor_id") && response.code == 200 && responseBody.isNotBlank()) {
            cachedVisitorResponse = resultResponse
            cachedVisitorTimestampMs = System.currentTimeMillis()
        }
        return resultResponse
    }
}
