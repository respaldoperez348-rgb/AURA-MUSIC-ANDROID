package com.auramusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class AuraTab {
    HOME,
    EXPLORE,
    LIBRARY,
    SEARCH
}

/**
 * Barra flotante segmentada fiel al diseño del usuario:
 * - Una caja a la izquierda que agrupa [Home], [Explore] y [Corazón] (con indicador ovalado en el activo).
 * - El buscador [Lupa] solo e independiente a la derecha.
 */
@Composable
fun AuraBottomNavigation(
    selectedTab: AuraTab,
    onTabSelected: (AuraTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Caja contenedora de los primeros 3 items (Home, Explore, Library)
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(26.dp),
                    spotColor = Color(0x20000000)
                )
                .clip(RoundedCornerShape(26.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .height(54.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SegmentedNavItem(
                    icon = if (selectedTab == AuraTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Inicio",
                    isSelected = selectedTab == AuraTab.HOME,
                    onClick = { onTabSelected(AuraTab.HOME) }
                )

                SegmentedNavItem(
                    icon = Icons.Outlined.Home, // Rombo / Explore se dibuja con el vector personalizado abajo
                    customIcon = { tint ->
                        ExploreDiamondIcon(tint = tint)
                    },
                    contentDescription = "Explore",
                    isSelected = selectedTab == AuraTab.EXPLORE,
                    onClick = { onTabSelected(AuraTab.EXPLORE) }
                )

                SegmentedNavItem(
                    icon = if (selectedTab == AuraTab.LIBRARY) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Biblioteca",
                    isSelected = selectedTab == AuraTab.LIBRARY,
                    onClick = { onTabSelected(AuraTab.LIBRARY) }
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Botón circular independiente del buscador (Search) a la derecha
        Surface(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = Color(0x20000000)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onTabSelected(AuraTab.SEARCH) },
            color = if (selectedTab == AuraTab.SEARCH) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            },
            tonalElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = if (selectedTab == AuraTab.SEARCH) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SegmentedNavItem(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    customIcon: (@Composable (Color) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .height(42.dp)
            .width(52.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 22.dp)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val tint = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }

        if (customIcon != null) {
            customIcon(tint)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Icono de rombo geométrico fiel al icono de Explore de la barra
 */
@Composable
private fun ExploreDiamondIcon(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2f, 1f)
            lineTo(w - 1f, h / 2f)
            lineTo(w / 2f, h - 1f)
            lineTo(1f, h / 2f)
            close()
        }

        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f.dp.toPx())
        )

        // Cuadrado pequeño interior en el centro
        val innerSize = 4.5f.dp.toPx()
        drawRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset((w - innerSize) / 2f, (h - innerSize) / 2f),
            size = androidx.compose.ui.geometry.Size(innerSize, innerSize)
        )
    }
}
