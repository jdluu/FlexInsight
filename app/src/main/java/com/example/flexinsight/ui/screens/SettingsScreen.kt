package com.example.flexinsight.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexinsight.FlexInsightApplication
import com.example.flexinsight.data.preferences.ApiKeyManager
import com.example.flexinsight.ui.components.ErrorBanner
import com.example.flexinsight.ui.components.NetworkStatusIndicator
import com.example.flexinsight.ui.screens.settings.parts.*
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.viewmodel.SettingsViewModel
import com.example.flexinsight.ui.common.LoadingState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as? FlexInsightApplication

    if (application == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Application error: Failed to initialize",
                color = MaterialTheme.colorScheme.error
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
    val snackbarHostState = com.example.flexinsight.ui.common.LocalSnackbarHostState.current

    var healthConnectEnabled by remember { mutableStateOf(false) }
    var geminiEnabled by remember { mutableStateOf(true) }
    var apiKey by remember { mutableStateOf<String?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyError by remember { mutableStateOf<String?>(null) }
    var showWeeklyGoalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Load API key
    LaunchedEffect(Unit) {
        apiKey = apiKeyManager.getApiKey()
    }

    // Sync Status Feedback
    LaunchedEffect(uiState.syncState) {
        when (val state = uiState.syncState) {
            is LoadingState.Success -> {
                snackbarHostState.showSnackbar("Sync completed successfully")
            }
            is LoadingState.Error -> {
                snackbarHostState.showSnackbar("Sync failed: ${state.error.message}")
            }
            else -> {}
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            SettingsHeader()
        }
        item {
            // Network status indicator
            val networkState by application.networkMonitor.networkState.collectAsState(
                initial = com.example.flexinsight.core.network.NetworkState.Unknown
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
                onSyncClick = { viewModel.syncData() },
                onEditProfileClick = { showEditProfileDialog = true }
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
                description = "Sync workouts with Google Health",
                icon = Icons.Default.Favorite,
                iconColor = MaterialTheme.colorScheme.error,
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
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar("Notifications features coming soon!") }
                }
            )
        }
        item {
            SectionTitle("Data & Privacy")
            PreferenceItem(
                title = "Export Data",
                icon = Icons.Default.Download,
                value = null,
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar("Export Data feature coming soon!") }
                }
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
            SectionTitle("Help & Feedback")
            PreferenceItem(
                title = "Contact Support",
                icon = Icons.Default.Email,
                value = null,
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:")
                        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@example.com"))
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "FlexInsight Feedback")
                    }
                    try {
                        context.startActivity(android.content.Intent.createChooser(intent, "Send Email"))
                    } catch (e: Exception) {
                         scope.launch { snackbarHostState.showSnackbar("No email app found") }
                    }
                }
            )
             PreferenceItem(
                title = "Documentation",
                icon = Icons.Default.Description,
                value = null,
                onClick = {
                    scope.launch { snackbarHostState.showSnackbar("Documentation coming soon") }
                }
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

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        val currentName = uiState.profileInfo?.displayName ?: ""
        EditProfileDialog(
            currentName = currentName,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName ->
                viewModel.updateDisplayName(newName)
                showEditProfileDialog = false
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Profile",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
