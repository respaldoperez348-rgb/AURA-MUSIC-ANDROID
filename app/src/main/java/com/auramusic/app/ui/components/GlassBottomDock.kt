package com.auramusic.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.locale.AuraStrings
import com.auramusic.app.ui.theme.CyberBackground
import com.auramusic.app.ui.theme.CyberCyan
import com.auramusic.app.ui.theme.GlassBorderSubtle
import com.auramusic.app.ui.theme.TextMuted
import com.auramusic.app.ui.theme.TextPrimary
import com.auramusic.app.viewmodel.MainTab

@Composable
fun GlassBottomDock(
    currentTab: MainTab,
    appLanguage: String = "Español",
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberBackground.copy(alpha = 0.96f))
    ) {
        HorizontalDivider(color = GlassBorderSubtle, thickness = 0.5.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isStudio = currentTab == MainTab.STUDIO
            DockTabItem(
                icon = if (isStudio) Icons.Filled.Home else Icons.Outlined.Home,
                label = AuraStrings.get("tab_studio", appLanguage),
                isSelected = isStudio,
                onClick = { onSelectTab(MainTab.STUDIO) },
                modifier = Modifier.weight(1f)
            )

            val isSearch = currentTab == MainTab.RADAR
            DockTabItem(
                icon = if (isSearch) Icons.Filled.Search else Icons.Outlined.Search,
                label = AuraStrings.get("tab_search", appLanguage),
                isSelected = isSearch,
                onClick = { onSelectTab(MainTab.RADAR) },
                modifier = Modifier.weight(1f)
            )

            val isVault = currentTab == MainTab.VAULT
            DockTabItem(
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
private fun DockTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) TextPrimary else TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = (-0.1).sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) CyberCyan else androidx.compose.ui.graphics.Color.Transparent)
        )
    }
}
