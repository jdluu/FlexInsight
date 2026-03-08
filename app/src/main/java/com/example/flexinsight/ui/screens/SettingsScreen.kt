package com.example.flexinsight.ui.screens


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
import com.example.flexinsight.FlexInsightApplication
import com.example.flexinsight.ui.components.ErrorBanner
import com.example.flexinsight.ui.components.NetworkStatusIndicator
import com.example.flexinsight.ui.screens.settings.parts.*
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.viewmodel.SettingsViewModel
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.R
import com.example.flexinsight.ui.common.LoadingState
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = com.example.flexinsight.ui.common.LocalSnackbarHostState.current

    var healthConnectEnabled by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showWeeklyGoalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

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
                        description = stringResource(id = R.string.settings_item_health_connect_desc),
                        icon = Icons.Default.Favorite,
                        iconColor = MaterialTheme.colorScheme.error,
                        isConnected = false,
                        isToggle = true,
                        toggleState = healthConnectEnabled,
                        onToggleChange = { healthConnectEnabled = it }
                    )
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
                }
                item {
                    SectionTitle(stringResource(id = R.string.settings_section_data_privacy))
                    PreferenceItem(
                        title = stringResource(id = R.string.settings_item_export_data),
                        icon = Icons.Default.Download,
                        value = null,
                        onClick = {
                            val csvData = "Date,Workout,Sets,Reps,Volume\n${java.time.LocalDate.now()},Full Body (Export Demo),12,120,5400"
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.settings_export_subject))
                                putExtra(android.content.Intent.EXTRA_TEXT, csvData)
                            }
                            try {
                                context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.settings_item_export_data)))
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.settings_export_failed)) }
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
                item {
                    SectionTitle(stringResource(id = R.string.settings_section_developer))
                    ToggleItem(
                        title = stringResource(id = R.string.settings_item_force_ai),
                        icon = Icons.Default.Build,
                        checked = uiState.forceAiEnable,
                        onToggleChange = { viewModel.updateForceAiEnable(it) }
                    )
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
