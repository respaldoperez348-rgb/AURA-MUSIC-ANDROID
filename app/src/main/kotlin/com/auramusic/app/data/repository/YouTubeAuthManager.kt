package com.auramusic.app.data.repository

import android.content.Context
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object YouTubeAuthManager {
    private const val ORIGIN = "https://music.youtube.com"
    private const val ACCOUNT_MENU_URL = "https://music.youtube.com/youtubei/v1/account/account_menu?prettyPrint=false"

    @Volatile
    var rawCookie: String = ""
        private set

    val isLoggedIn: Boolean
        get() = rawCookie.contains("SAPISID") || rawCookie.contains("__Secure-3PAPISID")

    fun setCookie(cookie: String) {
        rawCookie = cookie
    }

    fun clearCookie() {
        rawCookie = ""
        runCatching {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies { cm.flush() }
        }
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
        val sapisid = extractSapisid(cookie) ?: return@withContext Triple("Usuario Google", "Cuenta de Google vinculada", "")
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(6, TimeUnit.SECONDS)
                .readTimeout(6, TimeUnit.SECONDS)
                .build()
            val payload = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20250101.01.00","hl":"es","gl":"ES"}}}"""
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
                val body = resp.body?.string() ?: return@withContext Triple("Usuario Google", "Cuenta Google vinculada", "")
                val root = JSONObject(body)
                var header = root.optJSONArray("actions")
                    ?.optJSONObject(0)
                    ?.optJSONObject("openPopupAction")
                    ?.optJSONObject("popup")
                    ?.optJSONObject("multiPageMenuRenderer")
                    ?.optJSONObject("header")
                    ?.optJSONObject("activeAccountHeaderRenderer")

                if (header == null) {
                    fun findHeader(obj: Any?): JSONObject? {
                        when (obj) {
                            is JSONObject -> {
                                obj.optJSONObject("activeAccountHeaderRenderer")?.let { return it }
                                val keys = obj.keys()
                                while (keys.hasNext()) {
                                    findHeader(obj.opt(keys.next()))?.let { return it }
                                }
                            }
                            is JSONArray -> {
                                for (i in 0 until obj.length()) {
                                    findHeader(obj.opt(i))?.let { return it }
                                }
                            }
                        }
                        return null
                    }
                    header = findHeader(root)
                }

                val name = header?.optJSONObject("accountName")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: header?.optJSONObject("accountName")?.optString("simpleText")
                    ?: "Cuenta Google"
                val handle = header?.optJSONObject("email")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: header?.optJSONObject("email")?.optString("simpleText")
                    ?: header?.optJSONObject("channelHandle")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                    ?: "Cuenta Google vinculada"
                val thumbs = header?.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
                val rawPhoto = if (thumbs != null && thumbs.length() > 0) {
                    thumbs.getJSONObject(thumbs.length() - 1).optString("url")
                } else ""
                val photo = when {
                    rawPhoto.startsWith("//") -> "https:$rawPhoto"
                    rawPhoto.startsWith("http://") -> rawPhoto.replaceFirst("http://", "https://")
                    else -> rawPhoto
                }
                Triple(name, handle, photo)
            }
        } catch (_: Exception) {
            Triple("Usuario Google", "Cuenta Google vinculada", "")
        }
    }
}
