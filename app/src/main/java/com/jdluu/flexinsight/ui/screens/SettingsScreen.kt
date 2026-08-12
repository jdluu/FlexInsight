package com.jdluu.flexinsight.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.jdluu.flexinsight.FlexInsightApplication
import com.jdluu.flexinsight.ui.components.ErrorBanner
import com.jdluu.flexinsight.ui.components.NetworkStatusIndicator
import com.jdluu.flexinsight.ui.screens.settings.parts.*
import com.jdluu.flexinsight.ui.theme.*
import com.jdluu.flexinsight.ui.viewmodel.SettingsViewModel
import androidx.compose.ui.res.stringResource
import com.jdluu.flexinsight.BuildConfig
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.common.LoadingState
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = com.jdluu.flexinsight.ui.common.LocalSnackbarHostState.current

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showWeeklyGoalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showHealthPermissionsExplain by remember { mutableStateOf(false) }
    var showHealthPermissionsInfo by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val allGranted = viewModel.getHealthConnectPermissions().all { it in granted }
        viewModel.setHealthConnectEnabled(allGranted)
        if (!allGranted) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_health_permissions_denied))
            }
        }
    }

    // Sync Status Feedback
    LaunchedEffect(uiState.syncState) {
        when (val state = uiState.syncState) {
            is LoadingState.Success -> {
                snackbarHostState.showSnackbar(context.getString(R.string.settings_sync_success))
            }
            is LoadingState.Error -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_sync_failed, state.error.message ?: "")
                )
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_item_profile)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                item {
                    // Network status indicator
                    val networkState = uiState.networkState
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
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
                item {
                    SectionTitle(stringResource(id = R.string.settings_section_integrations))
                    ApiKeySection(
                        apiKey = uiState.apiKey,
                        onApiKeyClick = { showApiKeyDialog = true }
                    )
                    IntegrationItem(
                        name = stringResource(id = R.string.settings_item_health_connect),
                        description = if (uiState.healthConnectEnabled) {
                            stringResource(R.string.settings_health_connected)
                        } else if (uiState.healthConnectAvailable) {
                            stringResource(id = R.string.settings_item_health_connect_desc)
                        } else {
                            stringResource(R.string.settings_health_unavailable)
                        },
                        icon = Icons.Default.Favorite,
                        iconColor = MaterialTheme.colorScheme.error,
                        isConnected = uiState.healthConnectEnabled,
                        isToggle = true,
                        toggleState = uiState.healthConnectEnabled,
                        onToggleChange = { enabled ->
                            if (enabled && uiState.healthConnectAvailable) {
                                showHealthPermissionsExplain = true
                            } else {
                                viewModel.setHealthConnectEnabled(false)
                            }
                        }
                    )
                    if (uiState.healthConnectAvailable) {
                        TextButton(
                            onClick = { showHealthPermissionsInfo = true },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(stringResource(R.string.settings_health_view_permissions))
                        }
                    }
                    if (uiState.healthConnectEnabled) {
                        ToggleItem(
                            title = stringResource(R.string.settings_health_write),
                            icon = Icons.Default.FitnessCenter,
                            checked = uiState.healthConnectWriteEnabled,
                            onToggleChange = { viewModel.setHealthConnectWriteEnabled(it) },
                            subtitle = stringResource(R.string.settings_health_write_desc)
                        )
                    }
                }
                item {
                    SectionTitle(stringResource(id = R.string.settings_section_preferences))
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_theme),
                        icon = Icons.Default.DarkMode,
                        value = uiState.theme,
                        onClick = { showThemeDialog = true }
                    )
                    ToggleItem(
                        title = stringResource(id = R.string.settings_item_units),
                        icon = Icons.Default.Straighten,
                        checked = uiState.units == "Metric",
                        onToggleChange = { isMetric ->
                            viewModel.updateUnits(if (isMetric) "Metric" else "Imperial")
                        }
                    )
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_weekly_goal),
                        icon = Icons.Default.EmojiEvents,
                        value = stringResource(id = R.string.settings_item_weekly_goal_value, uiState.weeklyGoal),
                        isHighlighted = true,
                        onClick = { showWeeklyGoalDialog = true }
                    )
                    ToggleItem(
                        title = stringResource(id = R.string.settings_item_notifications),
                        icon = Icons.Default.Notifications,
                        checked = uiState.notificationsEnabled,
                        onToggleChange = { isEnabled ->
                            viewModel.updateNotificationsEnabled(isEnabled)
                        }
                    )
                    ToggleItem(
                        title = stringResource(id = R.string.settings_item_view_only),
                        icon = Icons.Default.Visibility,
                        checked = uiState.viewOnlyMode,
                        onToggleChange = { viewOnly ->
                            viewModel.updateViewOnlyMode(viewOnly)
                        },
                        subtitle = stringResource(id = R.string.settings_item_view_only_desc)
                    )
                }
                item {
                    SectionTitle(stringResource(id = R.string.settings_section_data_privacy))
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_export_coach_report),
                        icon = Icons.Default.Download,
                        value = null,
                        onClick = {
                            scope.launch {
                                val report = viewModel.exportCoachReport()
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.settings_export_subject))
                                    putExtra(android.content.Intent.EXTRA_TEXT, report)
                                }
                                try {
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            intent,
                                            context.getString(R.string.settings_item_export_coach_report)
                                        )
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(context.getString(R.string.settings_export_failed))
                                }
                            }
                        }
                    )
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_clear_cache),
                        icon = Icons.Default.Delete,
                        value = null,
                        isDestructive = true,
                        onClick = { showClearCacheDialog = true }
                    )
                }
                item {
                    SectionTitle(stringResource(id = R.string.settings_section_help_feedback))
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_contact_support),
                        icon = Icons.Default.Email,
                        value = null,
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:")
                                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.settings_support_email)))
                                putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.settings_support_subject))
                            }
                            try {
                                context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.settings_item_contact_support)))
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_support_no_app)) }
                            }
                        }
                    )
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_documentation),
                        icon = Icons.Default.Description,
                        value = null,
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/jdluu/FlexInsight"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_browser_no_app)) }
                            }
                        }
                    )
                }
                if (BuildConfig.DEBUG) {
                    item {
                        SectionTitle(stringResource(id = R.string.settings_section_developer))
                        ToggleItem(
                            title = stringResource(id = R.string.settings_item_force_ai),
                            subtitle = stringResource(id = R.string.settings_item_force_ai_subtitle),
                            icon = Icons.Default.Build,
                            checked = uiState.forceAiEnable,
                            onToggleChange = { viewModel.updateForceAiEnable(it) }
                        )
                    }
                }
                item {
                    SectionTitle(stringResource(id = R.string.settings_section_about))
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_version),
                        icon = Icons.Default.Info,
                        value = "v1.0.4 (Build 203)",
                        isValueOnly = true,
                        onClick = {}
                    )
                }
            }
        }
    }

    if (showHealthPermissionsExplain) {
        HealthConnectPermissionsDialog(
            onDismiss = { showHealthPermissionsExplain = false },
            onConfirm = {
                showHealthPermissionsExplain = false
                permissionLauncher.launch(viewModel.getHealthConnectPermissions())
            }
        )
    }

    if (showHealthPermissionsInfo) {
        HealthConnectPermissionsDialog(
            onDismiss = { showHealthPermissionsInfo = false },
            onConfirm = null
        )
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentApiKey = uiState.apiKey,
            onDismiss = {
                showApiKeyDialog = false
                viewModel.clearApiKeyError()
            },
            onSave = { newApiKey ->
                viewModel.validateAndSaveApiKey(newApiKey) {
                    showApiKeyDialog = false
                }
            },
            error = uiState.apiKeyError
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
