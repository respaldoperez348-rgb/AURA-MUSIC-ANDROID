package com.auramusic.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.app.data.models.NavBarStyle
import com.auramusic.app.i18n.LocalAuraStrings

enum class AuraTab {
    HOME,
    EXPLORE,
    LIBRARY,
    SEARCH
}

/**
 * Barra flotante segmentada en cristal esmerilado:
 * - Cápsula izquierda flotante con [Inicio | Explore | Biblioteca].
 * - Botón circular independiente [Buscar] flotando a la derecha.
 */
@Composable
fun AuraBottomNavigation(
    selectedTab: AuraTab,
    onTabSelected: (AuraTab) -> Unit,
    style: NavBarStyle = NavBarStyle.FLOATING,
    modifier: Modifier = Modifier,
) {
    if (style == NavBarStyle.NORMAL) {
        NormalBottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = modifier
        )
        return
    }
    val strings = LocalAuraStrings.current
    val glassBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
    val glassBorder = BorderStroke(
        width = 0.85.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color(0x45000000)
                )
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = glassBg,
            border = glassBorder,
            tonalElevation = 0.dp
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
                    contentDescription = strings.homeGreeting,
                    isSelected = selectedTab == AuraTab.HOME,
                    onClick = { onTabSelected(AuraTab.HOME) }
                )

                SegmentedNavItem(
                    icon = Icons.Outlined.Home,
                    customIcon = { tint ->
                        ExploreDiamondIcon(tint = tint)
                    },
                    contentDescription = strings.explore,
                    isSelected = selectedTab == AuraTab.EXPLORE,
                    onClick = { onTabSelected(AuraTab.EXPLORE) }
                )

                SegmentedNavItem(
                    icon = if (selectedTab == AuraTab.LIBRARY) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = strings.library,
                    isSelected = selectedTab == AuraTab.LIBRARY,
                    onClick = { onTabSelected(AuraTab.LIBRARY) }
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Surface(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = Color(0x45000000)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true)
                ) { onTabSelected(AuraTab.SEARCH) },
            shape = CircleShape,
            color = if (selectedTab == AuraTab.SEARCH) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
            } else {
                glassBg
            },
            border = if (selectedTab == AuraTab.SEARCH) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
            } else {
                glassBorder
            },
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = strings.search,
                    tint = if (selectedTab == AuraTab.SEARCH) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.size(23.dp)
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
            .width(54.dp)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Tab
            }
            .clip(RoundedCornerShape(21.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 24.dp)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val tint = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
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

@Composable
private fun ExploreDiamondIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w / 2f, 1f)
            lineTo(w - 1f, h / 2f)
            lineTo(w / 2f, h - 1f)
            lineTo(1f, h / 2f)
            close()
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.1f.dp.toPx())
        )

        val innerSize = 4.4f.dp.toPx()
        drawRect(
            color = tint,
            topLeft = Offset((w - innerSize) / 2f, (h - innerSize) / 2f),
            size = Size(innerSize, innerSize)
        )
    }
}

/**
 * Barra clásica de ancho completo anclada al borde inferior con íconos y etiquetas.
 */
@Composable
private fun NormalBottomNavigation(
    selectedTab: AuraTab,
    onTabSelected: (AuraTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAuraStrings.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.6.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(62.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NormalNavItem(
                    label = strings.homeGreeting,
                    icon = if (selectedTab == AuraTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    isSelected = selectedTab == AuraTab.HOME,
                    onClick = { onTabSelected(AuraTab.HOME) },
                    modifier = Modifier.weight(1f)
                )
                NormalNavItem(
                    label = strings.explore,
                    icon = Icons.Outlined.Home,
                    customIcon = { tint -> ExploreDiamondIcon(tint = tint) },
                    isSelected = selectedTab == AuraTab.EXPLORE,
                    onClick = { onTabSelected(AuraTab.EXPLORE) },
                    modifier = Modifier.weight(1f)
                )
                NormalNavItem(
                    label = strings.library,
                    icon = if (selectedTab == AuraTab.LIBRARY) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    isSelected = selectedTab == AuraTab.LIBRARY,
                    onClick = { onTabSelected(AuraTab.LIBRARY) },
                    modifier = Modifier.weight(1f)
                )
                NormalNavItem(
                    label = strings.search,
                    icon = Icons.Default.Search,
                    isSelected = selectedTab == AuraTab.SEARCH,
                    onClick = { onTabSelected(AuraTab.SEARCH) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NormalNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    customIcon: (@Composable (Color) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    }

    Column(
        modifier = modifier
            .semantics {
                this.contentDescription = label
                this.role = Role.Tab
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true)
            ) { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    } else {
                        Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (customIcon != null) {
                customIcon(tint)
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
