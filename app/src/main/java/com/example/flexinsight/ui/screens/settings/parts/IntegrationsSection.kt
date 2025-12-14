package com.example.flexinsight.ui.screens.settings.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.SurfaceCardAlt
import com.example.flexinsight.ui.theme.TextSecondary

/**
 * Section dealing with API keys and external integrations.
 */
@Composable
fun ApiKeySection(
    apiKey: String?,
    onApiKeyClick: () -> Unit
) {
    IntegrationItem(
        name = "Hevy API",
        description = if (apiKey != null) "Connected" else "Not configured",
        icon = Icons.Default.FitnessCenter,
        iconColor = Color(0xFF3B82F6),
        isConnected = apiKey != null,
        onClick = onApiKeyClick
    )
}

/**
 * Reusable integration item for settings lists.
 */
@Composable
fun IntegrationItem(
    name: String,
    description: String?,
    icon: ImageVector,
    iconColor: Color?,
    isConnected: Boolean,
    isToggle: Boolean = false,
    toggleState: Boolean = false,
    onToggleChange: (Boolean) -> Unit = {},
    badge: String? = null,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(if (!isToggle) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (iconColor != null) {
                                Modifier.background(iconColor.copy(alpha = 0.1f))
                            } else {
                                Modifier.background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF9333EA))
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor ?: Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (badge != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF3B82F6).copy(alpha = 0.2f),
                                                Color(0xFF9333EA).copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = badge.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                    if (description != null) {
                        Text(
                            text = if (isConnected) "● $description" else description,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isConnected) Primary else TextSecondary
                        )
                    }
                }
            }
            
            if (isToggle) {
                Switch(
                    checked = toggleState,
                    onCheckedChange = onToggleChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }
}
