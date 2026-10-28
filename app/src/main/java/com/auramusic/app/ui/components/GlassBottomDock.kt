package com.auramusic.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberCyanGlow
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextSecondary
import com.auramusic.app.viewmodel.MainTab

@Composable
fun GlassBottomDock(
    currentTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(26.dp), spotColor = CyberCyanGlow)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(CyberSurfaceCard, CyberBackgroundDark.copy(alpha = 0.95f))
                )
            )
            .border(1.2.dp, GlassBorder, RoundedCornerShape(26.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                icon = Icons.Default.GraphicEq,
                label = "STUDIO",
                isSelected = currentTab == MainTab.STUDIO,
                onClick = { onSelectTab(MainTab.STUDIO) }
            )

            DockItem(
                icon = Icons.Default.Search,
                label = "RADAR",
                isSelected = currentTab == MainTab.RADAR,
                onClick = { onSelectTab(MainTab.RADAR) }
            )

            DockItem(
                icon = Icons.Default.Album,
                label = "VAULT",
                isSelected = currentTab == MainTab.VAULT,
                onClick = { onSelectTab(MainTab.VAULT) }
            )
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(CyberCyan.copy(alpha = 0.25f), CyberCyanGlow.copy(alpha = 0.15f))
        )
    } else {
        Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Transparent))
    }

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) CyberCyan else TextMuted,
        label = "dock_icon_color"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgBrush)
            .border(
                1.dp,
                if (isSelected) CyberCyan.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
        if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
