package com.example.hevyinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hevyinsight.ui.theme.*

@Composable
fun SettingsScreen() {
    var healthConnectEnabled by remember { mutableStateOf(false) }
    var geminiEnabled by remember { mutableStateOf(true) }
    var privacyModeEnabled by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDarkAlt),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            SettingsHeader()
        }
        item {
            ProfileSection()
        }
        item {
            SectionTitle("Integrations")
            IntegrationItem(
                name = "Hevy",
                description = "Connected",
                icon = Icons.Default.FitnessCenter,
                iconColor = Color(0xFF3B82F6),
                isConnected = true,
                onClick = {}
            )
            IntegrationItem(
                name = "Health Connect",
                description = null,
                icon = Icons.Default.Favorite,
                iconColor = RedAccent,
                isConnected = false,
                isToggle = true,
                toggleState = healthConnectEnabled,
                onToggleChange = { healthConnectEnabled = it }
            )
            IntegrationItem(
                name = "Gemini AI",
                description = "On-device analysis",
                icon = Icons.Default.AutoAwesome,
                iconColor = null, // Gradient
                isConnected = false,
                isToggle = true,
                toggleState = geminiEnabled,
                onToggleChange = { geminiEnabled = it },
                badge = "Beta"
            )
        }
        item {
            SectionTitle("Preferences")
            PreferenceItem(
                title = "Theme",
                icon = Icons.Default.DarkMode,
                value = "Dark",
                onClick = {}
            )
            PreferenceItem(
                title = "Units",
                icon = Icons.Default.Straighten,
                value = "Metric",
                onClick = {}
            )
            PreferenceItem(
                title = "Weekly Goal",
                icon = Icons.Default.EmojiEvents,
                value = "4 Days",
                isHighlighted = true,
                onClick = {}
            )
            PreferenceItem(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                value = null,
                onClick = {}
            )
        }
        item {
            SectionTitle("Data & Privacy")
            PreferenceItem(
                title = "Export Data",
                icon = Icons.Default.Download,
                value = null,
                onClick = {}
            )
            IntegrationItem(
                name = "Privacy Mode",
                description = null,
                icon = Icons.Default.VisibilityOff,
                iconColor = TextSecondary,
                isConnected = false,
                isToggle = true,
                toggleState = privacyModeEnabled,
                onToggleChange = { privacyModeEnabled = it }
            )
            PreferenceItem(
                title = "Clear Cache",
                icon = Icons.Default.Delete,
                value = null,
                isDestructive = true,
                onClick = {}
            )
        }
        item {
            SectionTitle("About")
            PreferenceItem(
                title = "Version",
                icon = Icons.Default.Info,
                value = "v1.0.4 (Build 203)",
                isValueOnly = true,
                onClick = {}
            )
            PreferenceItem(
                title = "Report a Bug",
                icon = Icons.Default.BugReport,
                value = null,
                onClick = {}
            )
        }
    }
}

@Composable
fun SettingsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Text(
            text = "Profile",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
fun ProfileSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(112.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = Color.Gray.copy(alpha = 0.3f),
                border = BorderStroke(4.dp, SurfaceCardAlt)
            ) {}
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp),
                shape = CircleShape,
                color = Primary,
                border = BorderStroke(4.dp, BackgroundDarkAlt)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit profile",
                        tint = BackgroundDarkAlt,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Text(
            text = "Alex Strider",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Pro Member",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        
        Button(
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                tint = BackgroundDarkAlt
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sync Hevy Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundDarkAlt
            )
        }
    }
}

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

@Composable
fun IntegrationItem(
    name: String,
    description: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF3B82F6).copy(alpha = 0.2f),
                                        Color(0xFF9333EA).copy(alpha = 0.2f)
                                    )
                                ),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
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

@Composable
fun PreferenceItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
                    imageVector = if (isDestructive) Icons.Default.OpenInNew else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }
}
