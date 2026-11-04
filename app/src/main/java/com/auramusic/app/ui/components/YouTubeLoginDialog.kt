package com.auramusic.app.ui.components

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.auramusic.app.data.locale.AuraStrings
import com.auramusic.app.ui.theme.CyberBackground
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorderSubtle
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLoginDialog(
    appLanguage: String = "Español",
    onLoginCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasCaptured by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Web, 1: Manual Cookie
    var manualCookieText by remember { mutableStateOf("") }

    // Proven YouTube Music sign-in URL that redirects through youtube.com sign-in handler
    val loginUrl = "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
                .statusBarsPadding()
        ) {
            // Clean Hardware Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = AuraStrings.get("yt_connect_title", appLanguage),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "accounts.google.com · music.youtube.com",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceCard)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Tab Selector: Web vs Cookie Manual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurfaceCard)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 0) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceCard)
                        .border(
                            width = 1.dp,
                            color = if (selectedTab == 0) CyberCyan else GlassBorderSubtle,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = if (selectedTab == 0) CyberCyan else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Navegador Web",
                            color = if (selectedTab == 0) CyberCyan else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 1) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceCard)
                        .border(
                            width = 1.dp,
                            color = if (selectedTab == 1) CyberCyan else GlassBorderSubtle,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (selectedTab == 1) CyberCyan else TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pegar Cookie",
                            color = if (selectedTab == 1) CyberCyan else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                // Progress Bar for Web
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = CyberCyan,
                        trackColor = CyberSurfaceCard
                    )
                } else {
                    HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp)
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false

                                // Strip WebView identifiers (; wv and Version/4.0) so it matches standard Chrome Mobile
                                val defaultUa = userAgentString ?: ""
                                val cleanMobileUa = defaultUa
                                    .replace("; wv", "")
                                    .replace("Version/4.0 ", "")
                                    .replace("; Android System WebView", "")
                                userAgentString = if (cleanMobileUa.isNotBlank()) cleanMobileUa else "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                            }

                            // Strip X-Requested-With header so Google accounts does not detect embedded WebView package name
                            try {
                                if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                                    WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet<String>())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                    checkCookies(url)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    CookieManager.getInstance().flush()
                                    checkCookies(url)
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    checkCookies(url)
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    checkCookies(request?.url?.toString())
                                    return false
                                }

                                private fun checkCookies(url: String?) {
                                    if (hasCaptured) return
                                    val ytCookies = CookieManager.getInstance().getCookie("https://music.youtube.com") ?: ""
                                    val googleCookies = CookieManager.getInstance().getCookie("https://www.youtube.com") ?: ""
                                    val merged = if (ytCookies.contains("SAPISID")) ytCookies else googleCookies
                                    if ((merged.contains("SAPISID=") || merged.contains("__Secure-3PAPISID=")) &&
                                        (url?.contains("music.youtube.com") == true || url?.contains("youtube.com") == true)
                                    ) {
                                        hasCaptured = true
                                        onLoginCaptured(merged)
                                    }
                                }
                            }

                            loadUrl(loginUrl)
                        }
                    }
                )
            } else {
                // Manual Cookie Fallback View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Vincular mediante Cookie",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Si Google bloquea el acceso en tu dispositivo por políticas de seguridad, puedes iniciar sesión en music.youtube.com desde Chrome o cualquier navegador en tu móvil o PC, copiar el contenido de las cookies y pegarlo aquí:",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = manualCookieText,
                        onValueChange = { manualCookieText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = {
                            Text(
                                text = "Pega aquí tus cookies (debe contener SAPISID o __Secure-3PAPISID)...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val item = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                if (!item.isNullOrBlank()) {
                                    manualCookieText = item
                                    Toast.makeText(context, "Pegado desde el portapapeles", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberSurfaceCard,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CyberCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pegar", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val trimmed = manualCookieText.trim()
                                if (trimmed.contains("SAPISID") || trimmed.contains("__Secure-3PAPISID") || trimmed.contains("LOGIN_INFO")) {
                                    onLoginCaptured(trimmed)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "La cookie no parece válida (debe incluir SAPISID o __Secure-3PAPISID)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = CyberBackground
                            )
                        ) {
                            Text("Vincular Cuenta", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}


