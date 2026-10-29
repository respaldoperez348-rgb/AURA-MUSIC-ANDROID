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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
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
import com.auramusic.app.ui.theme.GlassBorder
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
                    elevation = 20.dp,
                    shape = RoundedCornerShape(30.dp),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF181B24),
                            Color(0xFF11131A)
                        )
                    )
                )
                .border(1.dp, GlassBorder, RoundedCornerShape(30.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isStudio = currentTab == MainTab.STUDIO
                DockTabItem(
                    icon = if (isStudio) Icons.Filled.Home else Icons.Outlined.Home,
                    label = com.auramusic.app.data.locale.AuraStrings.get("tab_studio", appLanguage),
                    isSelected = isStudio,
                    onClick = { onSelectTab(MainTab.STUDIO) }
                )

                val isVault = currentTab == MainTab.VAULT
                DockTabItem(
                    icon = if (isVault) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                    label = com.auramusic.app.data.locale.AuraStrings.get("tab_library", appLanguage),
                    isSelected = isVault,
                    onClick = { onSelectTab(MainTab.VAULT) }
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Separate Circular Search Button
        val isSearchSelected = currentTab == MainTab.RADAR
        val searchBg by animateColorAsState(
            targetValue = if (isSearchSelected) CyberCyan else Color(0xFF151821),
            label = "search_bg"
        )
        val searchIconColor by animateColorAsState(
            targetValue = if (isSearchSelected) CyberBackgroundDark else TextPrimary,
            label = "search_icon"
        )

        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .clip(CircleShape)
                .background(searchBg)
                .border(
                    1.dp,
                    if (isSearchSelected) CyberCyan else GlassBorder,
                    CircleShape
                )
                .clickable { onSelectTab(MainTab.RADAR) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearchSelected) Icons.Filled.Search else Icons.Outlined.Search,
                contentDescription = "Buscar",
                tint = searchIconColor,
                modifier = Modifier.size(22.dp)
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
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF242938))
    } else {
        Modifier
            .clip(RoundedCornerShape(24.dp))
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
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = (-0.1).sp
        )
    }
}
