package com.example.flexinsight.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.RedAccent
import com.example.flexinsight.ui.theme.SurfaceCardAlt
import com.example.flexinsight.ui.theme.TextSecondary

/**
 * Standard preference item for settings list
 */
@Composable
fun PreferenceItem(
    title: String,
    icon: ImageVector,
    value: String?,
    isHighlighted: Boolean = false,
    isDestructive: Boolean = false,
    isValueOnly: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
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
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = if (isDestructive) RedAccent.copy(alpha = 0.1f)
                    else Color.White.copy(alpha = 0.05f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isDestructive) RedAccent else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) RedAccent else Color.White
                )
            }
            
            if (isValueOnly) {
                Text(
                    text = value ?: "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            } else if (value != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isHighlighted) {
                        Text(
                            text = value,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = value,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = if (isDestructive) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }
}

/**
 * Toggleable preference item.
 */
@Composable
fun ToggleItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onToggleChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }
    }
}

/**
 * Section Title
 */
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
