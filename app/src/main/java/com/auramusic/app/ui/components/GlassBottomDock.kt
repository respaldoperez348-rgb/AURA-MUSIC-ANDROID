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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
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
import com.auramusic.app.ui.theme.CyberSurfaceCard
import com.auramusic.app.ui.theme.GlassBorderSubtle
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
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Navigation Capsule (Inicio + Biblioteca)
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF13151B))
                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isStudio = currentTab == MainTab.STUDIO
            DockPillTabItem(
                icon = if (isStudio) Icons.Filled.Home else Icons.Outlined.Home,
                label = AuraStrings.get("tab_studio", appLanguage),
                isSelected = isStudio,
                onClick = { onSelectTab(MainTab.STUDIO) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            val isVault = currentTab == MainTab.VAULT
            DockPillTabItem(
                icon = if (isVault) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                label = AuraStrings.get("tab_library", appLanguage),
                isSelected = isVault,
                onClick = { onSelectTab(MainTab.VAULT) }
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Satellite Floating Circular Search Button (🔍)
        val isSearch = currentTab == MainTab.RADAR
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(CircleShape)
                .background(if (isSearch) Color.White.copy(alpha = 0.16f) else Color(0xFF13151B))
                .border(
                    width = 1.dp,
                    color = if (isSearch) CyberCyan.copy(alpha = 0.6f) else GlassBorderSubtle,
                    shape = CircleShape
                )
                .clickable { onSelectTab(MainTab.RADAR) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = AuraStrings.get("tab_search", appLanguage),
                tint = if (isSearch) CyberCyan else TextPrimary,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun DockPillTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 22.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) TextPrimary else TextMuted,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (isSelected) TextPrimary else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.1).sp
            )
        }
    }
}
