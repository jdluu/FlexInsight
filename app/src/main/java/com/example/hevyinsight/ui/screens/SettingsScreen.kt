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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.example.hevyinsight.ui.common.LoadingState
import com.example.hevyinsight.ui.components.ErrorBanner
import com.example.hevyinsight.ui.components.NetworkStatusIndicator
import com.example.hevyinsight.ui.components.SyncStatusIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hevyinsight.HevyInsightApplication
import com.example.hevyinsight.data.preferences.ApiKeyManager
import com.example.hevyinsight.ui.theme.*
import com.example.hevyinsight.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as? HevyInsightApplication
    
    if (application == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDarkAlt)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Application error: Failed to initialize",
                color = RedAccent
            )
        }
        return
    }
    
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(application.repository, application.userPreferencesManager)
    }
    val uiState by viewModel.uiState.collectAsState()
    
    val apiKeyManager = remember { ApiKeyManager(context) }
    val scope = rememberCoroutineScope()
    
    var healthConnectEnabled by remember { mutableStateOf(false) }
    var geminiEnabled by remember { mutableStateOf(true) }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyError by remember { mutableStateOf<String?>(null) }
    var showWeeklyGoalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    
    // Load API key
    LaunchedEffect(Unit) {
        apiKey = apiKeyManager.getApiKey()
    }
    
    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDarkAlt),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }
    
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
            // Network status indicator
            val networkState by application.networkMonitor.networkState.collectAsState(
                initial = com.example.hevyinsight.core.network.NetworkState.Unknown
            )
            NetworkStatusIndicator(
                networkState = networkState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        
        item {
            ProfileSection(
                profileInfo = uiState.profileInfo,
                syncState = uiState.syncState,
                syncError = uiState.syncError,
                viewOnlyMode = uiState.viewOnlyMode,
                onSyncClick = { viewModel.syncData() }
            )
        }
        
        // Error banner
        uiState.error?.let { error ->
            item {
                ErrorBanner(
                    error = error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onDismiss = { /* TODO: Add dismiss action */ }
                )
            }
        }
        item {
            SectionTitle("Integrations")
            ApiKeySection(
                apiKey = apiKey,
                onApiKeyClick = { showApiKeyDialog = true }
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
                value = uiState.theme,
                onClick = { showThemeDialog = true }
            )
            ToggleItem(
                title = "Use Metric System",
                icon = Icons.Default.Straighten,
                checked = uiState.units == "Metric",
                onToggleChange = { isMetric ->
                    viewModel.updateUnits(if (isMetric) "Metric" else "Imperial")
                }
            )
            PreferenceItem(
                title = "Weekly Goal",
                icon = Icons.Default.EmojiEvents,
                value = "${uiState.weeklyGoal} Days",
                isHighlighted = true,
                onClick = { showWeeklyGoalDialog = true }
            )
            PreferenceItem(
                title = "Notifications",
                icon = Icons.Default.Notifications,
                value = null,
                onClick = {}
            )
            IntegrationItem(
                name = "View Only Mode",
                description = "Hide edit/create buttons",
                icon = Icons.Default.Visibility,
                iconColor = TextSecondary,
                isConnected = false,
                isToggle = true,
                toggleState = uiState.viewOnlyMode,
                onToggleChange = { viewModel.updateViewOnlyMode(it) }
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
            PreferenceItem(
                title = "Clear Cache",
                icon = Icons.Default.Delete,
                value = null,
                isDestructive = true,
                onClick = { showClearCacheDialog = true }
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
    
    // API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentApiKey = apiKey,
            onDismiss = { 
                showApiKeyDialog = false
                apiKeyError = null
            },
            onSave = { newApiKey ->
                if (apiKeyManager.isValidApiKeyFormat(newApiKey)) {
                    scope.launch {
                        apiKeyManager.saveApiKey(newApiKey)
                        apiKey = newApiKey
                        showApiKeyDialog = false
                        apiKeyError = null
                        viewModel.refresh() // Refresh to update profile info
                    }
                } else {
                    apiKeyError = "API key must be at least 10 characters"
                }
            },
            error = apiKeyError
        )
    }
    
    // Weekly Goal Dialog
    if (showWeeklyGoalDialog) {
        WeeklyGoalDialog(
            currentGoal = uiState.weeklyGoal,
            onDismiss = { showWeeklyGoalDialog = false },
            onSave = { goal ->
                viewModel.updateWeeklyGoal(goal)
                showWeeklyGoalDialog = false
            }
        )
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = uiState.theme,
            onDismiss = { showThemeDialog = false },
            onSelect = { theme ->
                viewModel.updateTheme(theme)
                showThemeDialog = false
            }
        )
    }
    
    // Clear Cache Dialog
    if (showClearCacheDialog) {
        ClearCacheDialog(
            onDismiss = { showClearCacheDialog = false },
            onConfirm = {
                viewModel.clearCache()
                showClearCacheDialog = false
            }
        )
    }
    
    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Error is displayed in UI state
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
fun ProfileSection(
    profileInfo: com.example.hevyinsight.data.model.ProfileInfo?,
    syncState: LoadingState,
    syncError: com.example.hevyinsight.ui.common.UiError?,
    viewOnlyMode: Boolean,
    onSyncClick: () -> Unit
) {
    val isSyncing = syncState.isLoading
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
            if (!viewOnlyMode) {
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
        }
        
        Text(
            text = getDisplayName(profileInfo),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = if (profileInfo?.isProMember == true) "Pro Member" else "Free Member",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
        
        // Profile stats
        if (profileInfo != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatNumber(profileInfo.totalWorkouts),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Workouts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
                if (profileInfo.memberSince != null) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatMemberSince(profileInfo.memberSince),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Member since",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        Button(
            onClick = onSyncClick,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 320.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSyncing
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = BackgroundDarkAlt,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = BackgroundDarkAlt
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSyncing) "Syncing..." else "Sync Hevy Data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BackgroundDarkAlt
            )
        }
        
        // Sync status indicator
        val syncStateForIndicator = when (syncState) {
            is LoadingState.Loading -> com.example.hevyinsight.data.sync.SyncState.Syncing
            is LoadingState.Success -> com.example.hevyinsight.data.sync.SyncState.Success(
                timestamp = System.currentTimeMillis()
            )
            is LoadingState.Error -> com.example.hevyinsight.data.sync.SyncState.Error(
                error = syncState.error
            )
            else -> com.example.hevyinsight.data.sync.SyncState.Idle
        }
        SyncStatusIndicator(
            syncState = syncStateForIndicator,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Sync error banner
        syncError?.let { error ->
            ErrorBanner(
                error = error,
                modifier = Modifier.fillMaxWidth(),
                onDismiss = null
            )
        }
    }
}

/**
 * Format member since date
 */
fun formatMemberSince(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

/**
 * Format account age
 */
fun formatAccountAge(days: Int): String {
    return when {
        days < 30 -> "$days days"
        days < 365 -> "${days / 30} months"
        else -> "${days / 365} years"
    }
}

/**
 * Get display name from profile info
 */
fun getDisplayName(profileInfo: com.example.hevyinsight.data.model.ProfileInfo?): String {
    return profileInfo?.displayName ?: "User"
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
                    imageVector = if (isDestructive) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }
    }
}

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

@Composable
fun ApiKeyDialog(
    currentApiKey: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    error: String?
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hevy API Key",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your Hevy API key. You can get it from https://hevy.com/settings?developer",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text("API Key", color = TextSecondary) },
                    placeholder = { Text("Enter your API key", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    singleLine = true
                )
                val errorText = error
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = RedAccent,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKeyText) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun WeeklyGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Weekly Goal",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Set your weekly workout goal (number of days per week)",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { 
                        goalText = it
                        error = null
                    },
                    label = { Text("Days per week", color = TextSecondary) },
                    placeholder = { Text("Enter number", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = TextSecondary,
                        unfocusedLabelColor = TextSecondary
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                val errorText = error
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = RedAccent,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goal = goalText.toIntOrNull()
                    if (goal != null && goal > 0 && goal <= 7) {
                        onSave(goal)
                    } else {
                        error = "Please enter a number between 1 and 7"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ThemeDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val themes = listOf("Dark", "Light", "System")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Theme",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { theme ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (theme == currentTheme) Primary.copy(alpha = 0.2f) else SurfaceCardAlt,
                        border = if (theme == currentTheme) BorderStroke(1.dp, Primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = theme,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (theme == currentTheme) FontWeight.Bold else FontWeight.Normal
                            )
                            if (theme == currentTheme) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ToggleItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCardAlt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggleChange
            )
        }
    }
}

@Composable
fun ClearCacheDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Clear Cache",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This will clear all cached data (exercise templates, routines). Your workout data will not be affected.",
                color = TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
            ) {
                Text("Clear", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceCardAlt,
        shape = RoundedCornerShape(16.dp)
    )
}
