package com.auramusic.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.auramusic.app.playback.AmbientNoiseAnalyzer
import com.auramusic.app.playback.NoiseType
import com.auramusic.app.ui.theme.CyberAmber
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberGreen
import com.auramusic.app.ui.theme.CyberPink
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.NeonPurple
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun StreetDspCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by AmbientNoiseAnalyzer.status.collectAsState()

    // Periodically verify headphones state on composition
    LaunchedEffect(Unit) {
        AmbientNoiseAnalyzer.checkHeadphonesConnected(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            AmbientNoiseAnalyzer.setEnabled(true, context)
        } else {
            Toast.makeText(
                context,
                "Se requiere permiso de micrófono para Street DSP",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Pulse animation for sampling state indicator
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (status.isEnabled && status.isHeadphonesConnected) CyberCyan.copy(alpha = 0.5f) else GlassBorder,
        animationSpec = tween(400),
        label = "BorderColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header Row: Icon, Title, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (status.isEnabled) Brush.linearGradient(listOf(CyberCyan.copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.25f)))
                                else Brush.linearGradient(listOf(CyberSurfaceActive, CyberSurfaceActive))
                            )
                            .border(
                                1.dp,
                                if (status.isEnabled) CyberCyan else GlassBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = if (status.isEnabled) CyberCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AURA SENSE",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyberCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "STREET DSP",
                                    color = CyberCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "AMBIENT NOISE UNMASKING EQ",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Switch
                Switch(
                    checked = status.isEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                AmbientNoiseAnalyzer.setEnabled(true, context)
                            }
                        } else {
                            AmbientNoiseAnalyzer.setEnabled(false, context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBackgroundDark,
                        checkedTrackColor = CyberCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberSurfaceActive
                    )
                )
            }

            // Headphone Warning Banner (if no headphones plugged)
            if (!status.isHeadphonesConnected) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberAmber.copy(alpha = 0.12f))
                        .border(1.dp, CyberAmber.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = CyberAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CONECTA AURICULARES REQUERIDOS",
                                color = CyberAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Para evitar acople y feedback con el altavoz, Street DSP funciona exclusivamente con audífonos.",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            // Active Analysis Panel (when enabled and headphones connected)
            AnimatedVisibility(
                visible = status.isEnabled && status.isHeadphonesConnected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Radar Status Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceActive.copy(alpha = 0.6f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Pulse dot
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (status.isSampling) CyberPink.copy(alpha = pulseAlpha)
                                            else when (status.noiseType) {
                                                NoiseType.QUIET -> CyberGreen
                                                NoiseType.TRAFFIC -> CyberCyan
                                                NoiseType.CROWD -> NeonPurple
                                                NoiseType.HIGH_NOISE -> CyberPink
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (status.isSampling) "ESCANEANDO RUIDO EXTERIOR..." else status.statusLabel.uppercase(Locale.ROOT),
                                        color = if (status.isSampling) CyberPink else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = status.detailLabel,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Decibels pill
                            if (status.detectedDb > 0f) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberBackgroundDark)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${status.detectedDb.toInt()} dB",
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5-Band Live Compensation Meters
                    Text(
                        text = "COMPENSACIÓN DINÁMICA (UNMASKING EQ)",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")
                        val compensations = status.compensationsMb

                        for (i in 0 until 5) {
                            val compMb = compensations.getOrElse(i) { 0f }
                            val compDb = compMb / 100f
                            val progress = (compMb / 450f).coerceIn(0f, 1f)
                            val animProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(300),
                                label = "BandProgress$i"
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // dB Value
                                Text(
                                    text = if (compDb > 0.05f) String.format(Locale.US, "+%.1f", compDb) else "0.0",
                                    color = if (compDb > 0.05f) CyberCyan else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                // Vertical Bar
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CyberSurfaceActive)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(animProgress.coerceAtLeast(0.06f))
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(CyberPink, CyberCyan)
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                // Band Label
                                Text(
                                    text = bandLabels[i],
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer / Privacy & Duty-Cycle Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Duty-Cycle de 400ms c/6s para 0% impacto en batería. Procesamiento FFT en RAM sin almacenamiento.",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

