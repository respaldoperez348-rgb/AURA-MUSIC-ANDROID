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
import androidx.compose.ui.graphics.Brush
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
    val isTransparent = dockStyle.equals("Midnight Island", ignoreCase = true) ||
        dockStyle.equals("Transparente", ignoreCase = true)

    val capsuleBg = if (isTransparent) {
        CyberBackgroundDark.copy(alpha = 0.28f)
    } else {
        CyberSurfaceCard
    }

    val capsuleBorderBrush = if (isTransparent) {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.24f),
                CyberCyan.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.horizontalGradient(listOf(GlassBorderSubtle, GlassBorderSubtle))
    }

    val isHome = currentTab == MainTab.STUDIO
    val isVault = currentTab == MainTab.VAULT
    val isSearch = currentTab == MainTab.RADAR

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cápsula principal izquierda: solo Inicio y Biblioteca
        Row(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .then(
                    if (!isTransparent) {
                        Modifier.shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(30.dp),
                            spotColor = Color.Black.copy(alpha = 0.55f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(RoundedCornerShape(30.dp))
                .background(capsuleBg)
                .border(1.dp, capsuleBorderBrush, RoundedCornerShape(30.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernDockTabItem(
                icon = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                label = AuraStrings.get("tab_studio", appLanguage),
                isSelected = isHome,
                isTransparent = isTransparent,
                onClick = { onSelectTab(MainTab.STUDIO) },
                modifier = Modifier.weight(1f)
            )

            ModernDockTabItem(
                icon = if (isVault) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                label = AuraStrings.get("tab_library", appLanguage),
                isSelected = isVault,
                isTransparent = isTransparent,
                onClick = { onSelectTab(MainTab.VAULT) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Botón del Buscador al costado derecho
        val searchBubbleBg = when {
            isSearch -> CyberCyan
            isTransparent -> CyberBackgroundDark.copy(alpha = 0.28f)
            else -> CyberSurfaceCard
        }

        val searchBorderBrush = when {
            isSearch -> Brush.linearGradient(listOf(CyberCyan, CyberCyan))
            isTransparent -> Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.26f),
                    CyberCyan.copy(alpha = 0.45f)
                )
            )
            else -> Brush.linearGradient(listOf(GlassBorderSubtle, GlassBorderSubtle))
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .then(
                    if (!isTransparent) {
                        Modifier.shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = Color.Black.copy(alpha = 0.55f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(CircleShape)
                .background(searchBubbleBg)
                .border(1.dp, searchBorderBrush, CircleShape)
                .clickable { onSelectTab(MainTab.RADAR) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = AuraStrings.get("tab_search", appLanguage),
                tint = if (isSearch) CyberBackgroundDark else CyberCyan,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun ModernDockTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isTransparent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePillBg = if (isTransparent) {
        CyberCyan.copy(alpha = 0.20f)
    } else {
        CyberCyan.copy(alpha = 0.14f)
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) activePillBg else Color.Transparent)
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
                color = if (isSelected) CyberCyan else if (isTransparent) TextSecondary else TextMuted,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.2).sp
            )
        }
    }
}
