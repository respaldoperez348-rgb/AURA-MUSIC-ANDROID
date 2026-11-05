package com.auramusic.app.ui.components

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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.locale.AuraStrings
import com.auramusic.app.ui.theme.CyberBackgroundDark
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.CyberSurfaceActive
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorderSubtle
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.ui.theme.TextSecondary
import com.auramusic.app.viewmodel.MainTab

@Composable
fun GlassBottomDock(
    currentTab: MainTab,
    dockStyle: String = "Midnight Island",
    appLanguage: String = "Español",
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(CyberSurfaceCard)
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isHome = currentTab == MainTab.STUDIO
            val isSearch = currentTab == MainTab.RADAR
            val isVault = currentTab == MainTab.VAULT

            ModernDockTabItem(
                icon = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                label = AuraStrings.get("tab_studio", appLanguage),
                isSelected = isHome,
                onClick = { onSelectTab(MainTab.STUDIO) },
                modifier = Modifier.weight(1f)
            )

            ModernDockTabItem(
                icon = Icons.Filled.Search,
                label = AuraStrings.get("tab_search", appLanguage),
                isSelected = isSearch,
                onClick = { onSelectTab(MainTab.RADAR) },
                modifier = Modifier.weight(1f)
            )

            ModernDockTabItem(
                icon = if (isVault) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                label = AuraStrings.get("tab_library", appLanguage),
                isSelected = isVault,
                onClick = { onSelectTab(MainTab.VAULT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModernDockTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CyberCyan.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) CyberCyan else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isSelected) CyberCyan else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.2).sp
            )
        }
    }
}
