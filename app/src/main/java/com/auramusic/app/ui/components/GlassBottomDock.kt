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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberCyanGlow
import com.auramusic.app.ui.theme.GlassBorder
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import com.auramusic.app.viewmodel.MainTab

@Composable
fun GlassBottomDock(
    currentTab: MainTab,
    appLanguage: String = "Español",
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Navigation Capsule (Inicio + Biblioteca)
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = CyberCyanGlow.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E212B),
                            Color(0xFF14161E)
                        )
                    )
                )
                .border(1.2.dp, GlassBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DockTabItem(
                    icon = Icons.Default.Home,
                    label = com.auramusic.app.data.locale.AuraStrings.get("tab_studio", appLanguage),
                    isSelected = currentTab == MainTab.STUDIO,
                    onClick = { onSelectTab(MainTab.STUDIO) }
                )

                DockTabItem(
                    icon = Icons.Default.Folder,
                    label = com.auramusic.app.data.locale.AuraStrings.get("tab_library", appLanguage),
                    isSelected = currentTab == MainTab.VAULT,
                    onClick = { onSelectTab(MainTab.VAULT) }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Separate Circular Search Button
        val isSearchSelected = currentTab == MainTab.RADAR
        val searchBg by animateColorAsState(
            targetValue = if (isSearchSelected) CyberCyan else Color(0xFF1A1C25),
            label = "search_bg"
        )
        val searchIconColor by animateColorAsState(
            targetValue = if (isSearchSelected) CyberBackgroundDark else TextPrimary,
            label = "search_icon"
        )

        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = if (isSearchSelected) CyberCyanGlow else Color.Transparent
                )
                .clip(CircleShape)
                .background(searchBg)
                .border(
                    1.2.dp,
                    if (isSearchSelected) CyberCyan else GlassBorder,
                    CircleShape
                )
                .clickable { onSelectTab(MainTab.RADAR) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar",
                tint = searchIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DockTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgModifier = if (isSelected) {
        Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF2C303E))
    } else {
        Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Transparent)
    }

    Row(
        modifier = bgModifier
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) CyberCyan else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
