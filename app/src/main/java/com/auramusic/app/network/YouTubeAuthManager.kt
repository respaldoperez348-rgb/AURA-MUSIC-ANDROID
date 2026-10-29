package com.auramusic.app.network

import android.content.Context
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class YouTubeAccountState(
    val isLoggedIn: Boolean = false,
    val accountName: String = "",
    val accountHandle: String = "",
    val avatarUrl: String = ""
)

object YouTubeAuthManager {
    private const val ORIGIN = "https://music.youtube.com"
    private const val ACCOUNT_MENU_URL = "https://music.youtube.com/youtubei/v1/account/account_menu?prettyPrint=false"

    @Volatile
    var rawCookie: String = ""
        private set

    @Volatile
    var accountName: String = ""
        private set

    @Volatile
    var accountHandle: String = ""
        private set

    @Volatile
    var avatarUrl: String = ""
        private set

    val isLoggedIn: Boolean
        get() = rawCookie.contains("SAPISID") || rawCookie.contains("__Secure-3PAPISID")

    fun loadFromPrefs(context: Context): YouTubeAccountState {
        val prefs = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        rawCookie = prefs.getString("yt_cookie", "") ?: ""
        accountName = prefs.getString("yt_account_name", "") ?: ""
        accountHandle = prefs.getString("yt_account_handle", "") ?: ""
        avatarUrl = prefs.getString("yt_avatar_url", "") ?: ""
        return getState()
    }

    fun getState(): YouTubeAccountState {
        return YouTubeAccountState(
            isLoggedIn = isLoggedIn,
            accountName = accountName.ifBlank { if (isLoggedIn) "YouTube Music" else "" },
            accountHandle = accountHandle,
            avatarUrl = avatarUrl
        )
    }

    fun saveSession(
        context: Context,
        cookie: String,
        name: String,
        handle: String,
        avatar: String
    ): YouTubeAccountState {
        rawCookie = cookie
        accountName = name.ifBlank { "Cuenta Conectada" }
        accountHandle = handle
        avatarUrl = avatar
        val prefs = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("yt_cookie", rawCookie)
            .putString("yt_account_name", accountName)
            .putString("yt_account_handle", accountHandle)
            .putString("yt_avatar_url", avatarUrl)
            .apply()
        return getState()
    }

    fun logout(context: Context): YouTubeAccountState {
        rawCookie = ""
        accountName = ""
        accountHandle = ""
        avatarUrl = ""
        val prefs = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("yt_cookie")
            .remove("yt_account_name")
            .remove("yt_account_handle")
            .remove("yt_avatar_url")
            .apply()
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        return getState()
    }

    private fun extractSapisid(cookie: String): String? {
        val parts = cookie.split(";")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("SAPISID=")) {
                return trimmed.substringAfter("SAPISID=").trim()
            }
            if (trimmed.startsWith("__Secure-3PAPISID=")) {
                return trimmed.substringAfter("__Secure-3PAPISID=").trim()
            }
        }
        return null
    }

    private fun computeSapisidHash(sapisid: String): String {
        val timestamp = System.currentTimeMillis() / 1000L
        val input = "$timestamp $sapisid $ORIGIN"
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${timestamp}_$hex"
    }

    fun applyAuthHeaders(builder: Request.Builder) {
        val cookie = rawCookie
        if (cookie.isBlank()) return
        val sapisid = extractSapisid(cookie) ?: return
        builder.header("Cookie", cookie)
        builder.header("Authorization", computeSapisidHash(sapisid))
        builder.header("X-Origin", ORIGIN)
        builder.header("Origin", ORIGIN)
        builder.header("X-Goog-AuthUser", "0")
    }

    suspend fun fetchAccountDetailsFromCookie(cookie: String): Triple<String, String, String> = withContext(Dispatchers.IO) {
        val sapisid = extractSapisid(cookie) ?: return@withContext Triple("YouTube Music", "Sesión activa", "")
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .build()
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"es","gl":"ES"}}}"""
            val req = Request.Builder()
                .url(ACCOUNT_MENU_URL)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0")
                .header("Content-Type", "application/json")
                .header("Cookie", cookie)
                .header("Authorization", computeSapisidHash(sapisid))
                .header("X-Origin", ORIGIN)
                .header("Origin", ORIGIN)
                .header("X-Goog-AuthUser", "0")
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return@withContext Triple("YouTube Music", "Cuenta Google vinculada", "")
                val root = JSONObject(body)
                val header = root.optJSONArray("actions")
                    ?.optJSONObject(0)
                    ?.optJSONObject("openPopupAction")
                    ?.optJSONObject("popup")
                    ?.optJSONObject("multiPageMenuRenderer")
                    ?.optJSONObject("header")
                    ?.optJSONObject("activeAccountHeaderRenderer")

                val name = header?.optJSONObject("accountName")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: "Cuenta YouTube Music"
                val handle = header?.optJSONObject("channelHandle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: header?.optJSONObject("email")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: "Google Audio Sync"
                val thumbs = header?.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
                val photo = if (thumbs != null && thumbs.length() > 0) {
                    thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                } else ""
                Triple(name, handle, photo)
            }
        } catch (e: Exception) {
            Triple("YouTube Music", "Cuenta Google vinculada", "")
        }
    }
}

