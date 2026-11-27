package com.jdluu.flexinsight.ui.screens

import android.content.Intent
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.BuildConfig
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.LocalSnackbarHostState
import com.jdluu.flexinsight.ui.components.ErrorBanner
import com.jdluu.flexinsight.ui.components.NetworkStatusIndicator
import com.jdluu.flexinsight.ui.screens.settings.parts.*
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showWeeklyGoalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showHealthPermissionsExplain by remember { mutableStateOf(false) }
    var showHealthPermissionsInfo by remember { mutableStateOf(false) }

    val healthPermissionsDeniedMessage = stringResource(R.string.settings_health_permissions_denied)
    val syncSuccessMessage = stringResource(R.string.settings_sync_success)
    val syncStateFeedback: String? = when (val state = uiState.syncState) {
        is LoadingState.Success -> syncSuccessMessage
        is LoadingState.Error -> stringResource(R.string.settings_sync_failed, state.error.message)
        else -> null
    }
    val exportSubject = stringResource(R.string.settings_export_subject)
    val exportReportTitle = stringResource(R.string.settings_item_export_coach_report)
    val exportFailedMessage = stringResource(R.string.settings_export_failed)
    val contactSupportTitle = stringResource(R.string.settings_item_contact_support)
    val supportEmail = stringResource(R.string.settings_support_email)
    val supportSubject = stringResource(R.string.settings_support_subject)
    val supportNoAppMessage = stringResource(R.string.settings_support_no_app)
    val browserNoAppMessage = stringResource(R.string.settings_browser_no_app)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val readGranted = viewModel.getHealthConnectReadPermissions().all { it in granted }
        viewModel.setHealthConnectEnabled(readGranted)
        if (!readGranted) {
            scope.launch {
                snackbarHostState.showSnackbar(healthPermissionsDeniedMessage)
            }
        }
    }

    LaunchedEffect(uiState.syncState) {
        syncStateFeedback?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.nav_settings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
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
                HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
            }
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
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(top = 16.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    NetworkStatusIndicator(
                        networkState = uiState.networkState,
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
                        value = pluralStringResource(
                            id = R.plurals.settings_item_weekly_goal_value,
                            count = uiState.weeklyGoal,
                            uiState.weeklyGoal
                        ),
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
                        title = exportReportTitle,
                        icon = Icons.Default.Download,
                        value = null,
                        onClick = {
                            scope.launch {
                                val report = viewModel.exportCoachReport()
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, exportSubject)
                                    putExtra(Intent.EXTRA_TEXT, report)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, exportReportTitle))
                                } catch (_: Exception) {
                                    snackbarHostState.showSnackbar(exportFailedMessage)
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
                        title = contactSupportTitle,
                        icon = Icons.Default.Email,
                        value = null,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                                putExtra(Intent.EXTRA_SUBJECT, supportSubject)
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, contactSupportTitle))
                            } catch (_: Exception) {
                                scope.launch { snackbarHostState.showSnackbar(supportNoAppMessage) }
                            }
                        }
                    )
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_documentation),
                        icon = Icons.Default.Description,
                        value = null,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/jdluu/FlexInsight".toUri())
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                scope.launch { snackbarHostState.showSnackbar(browserNoAppMessage) }
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
                        value = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
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

    if (showClearCacheDialog) {
        ClearCacheDialog(
            onDismiss = { showClearCacheDialog = false },
            onConfirm = {
                viewModel.clearCache()
                showClearCacheDialog = false
            }
        )
    }

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
}
