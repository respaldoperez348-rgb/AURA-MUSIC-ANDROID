package com.auramusic.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.network.sync.LibrarySyncRepository
import com.auramusic.app.network.sync.SyncManager
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberCyanGlow
import com.auramusic.app.ui.theme.CyberGreen
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.GlassBorderSubtle
import com.auramusic.app.ui.theme.NeonPurple
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSyncSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val syncTopic = remember { SyncManager.getSyncTopic(context) }
    var isSyncing by remember { mutableStateOf(false) }
    var lastSyncStatus by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberBackgroundDark,
        tonalElevation = 10.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CyberCyan.copy(alpha = 0.25f),
                                        NeonPurple.copy(alpha = 0.25f)
                                    )
                                )
                            )
                            .border(1.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "AURA DESKTOP SYNC",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "PUSH-TO-PHONE & CLOUD FIRESTORE",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Indicador de Estado FCM
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurfaceCard)
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CyberGreen)
                            .border(1.5.dp, CyberGreen.copy(alpha = 0.5f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "FCM Topic Activo & Vinculado",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Listo para recibir órdenes de favoritos y descargas desde PC",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tarjeta del Código Único con borde CyberCyan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CyberSurfaceCard,
                                CyberCyanGlow.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(1.5.dp, CyberCyan.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CÓDIGO DE VINCULACIÓN",
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Aura Sync Code", syncTopic)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Código copiado: $syncTopic", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Código",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = syncTopic,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Aura Sync Code", syncTopic)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                            }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ingresa este código en tu reproductor Aura Music Desktop para enlazar tu PC con este teléfono.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón de Sincronización Manual a Firestore
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSyncing) Brush.linearGradient(listOf(CyberSurfaceActive, CyberSurfaceActive))
                        else Brush.linearGradient(listOf(CyberCyan, NeonPurple))
                    )
                    .clickable(enabled = !isSyncing) {
                        isSyncing = true
                        scope.launch {
                            try {
                                val success = LibrarySyncRepository.pushFullLibraryToCloud(context)
                                isSyncing = false
                                if (success) {
                                    lastSyncStatus = "Sincronizado a las ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
                                    Toast.makeText(
                                        context,
                                        "¡Toda tu biblioteca se sincronizó con Cloud Firestore!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "No se pudo sincronizar con Firestore. Revisa tu conexión y reglas.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Throwable) {
                                isSyncing = false
                                Toast.makeText(
                                    context,
                                    "Error al sincronizar: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .padding(vertical = 15.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SUBIENDO BIBLIOTECA A FIRESTORE...",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = CyberBackgroundDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SINCRONIZAR BIBLIOTECA AHORA",
                            color = CyberBackgroundDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            if (lastSyncStatus != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CyberGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lastSyncStatus!!,
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini Tarjetas de Información
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceCard)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "PUSH-TO-PHONE",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Descargas y favoritos remotos con pantalla apagada (Doze Mode).",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceCard)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "FIRESTORE CLOUD",
                            color = NeonPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Catálogo completo disponible en tu reproductor de escritorio.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

