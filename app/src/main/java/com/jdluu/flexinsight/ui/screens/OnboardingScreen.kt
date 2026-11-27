package com.jdluu.flexinsight.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.screens.settings.parts.HealthConnectPermissionsDialog
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(0) }
    var apiKey by remember { mutableStateOf("") }
    var aiStatus by remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val isValid = apiKey.length >= 10
    var showHealthPermissionsExplain by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val readGranted = viewModel.getHealthConnectReadPermissions().all { it in granted }
        viewModel.setHealthConnectEnabled(readGranted)
        step = 3
        scope.launch { viewModel.setOnboardingStep(3) }
    }

    LaunchedEffect(Unit) {
        step = viewModel.getOnboardingStep().coerceIn(0, 3)
    }

    LaunchedEffect(uiState.apiKey) {
        if (!uiState.apiKey.isNullOrBlank() && step == 0) {
            step = 1
            viewModel.setOnboardingStep(1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                PlateStatusPill(
                    label = "${step + 1} / 4",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    borderColor = FlexTheme.colors.hairline
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CalibratedPlateBar(
                progress = ((step + 1) / 4f).coerceIn(0f, 1f),
                segments = 4,
                activeColor = MaterialTheme.colorScheme.primary,
                barHeight = 8.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                0 -> OnboardingStepApiKey(
                    apiKey = apiKey,
                    onApiKeyChange = {
                        apiKey = it
                        viewModel.clearApiKeyError()
                    },
                    apiKeyError = uiState.apiKeyError,
                    isValid = isValid,
                    onSave = { viewModel.validateAndSaveApiKey(apiKey) }
                )
                1 -> OnboardingStepSync(
                    isSyncing = uiState.isSyncing,
                    onSync = { viewModel.syncData() },
                    onNext = {
                        step = 2
                        scope.launch { viewModel.setOnboardingStep(2) }
                    }
                )
                2 -> OnboardingStepHealth(
                    onSkip = {
                        step = 3
                        scope.launch { viewModel.setOnboardingStep(3) }
                    },
                    onEnable = {
                        if (uiState.healthConnectAvailable) {
                            showHealthPermissionsExplain = true
                        } else {
                            step = 3
                            scope.launch { viewModel.setOnboardingStep(3) }
                        }
                    }
                )
                else -> OnboardingStepAi(
                    status = aiStatus,
                    onCheck = {
                        aiStatus = if (uiState.forceAiEnable) {
                            "Debug AI stubs enabled (not real Gemini Nano)"
                        } else {
                            "On-device Gemini Nano ready on supported devices"
                        }
                    },
                    onFinish = {
                        viewModel.completeOnboarding()
                        onComplete()
                    }
                )
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
}

@Composable
private fun OnboardingStepApiKey(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    apiKeyError: String?,
    isValid: Boolean,
    onSave: () -> Unit
) {
    OnboardingCard(
        icon = Icons.Default.Key,
        title = stringResource(R.string.onboarding_connect_hevy),
        subtitle = stringResource(R.string.onboarding_api_key_desc)
    ) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text(stringResource(R.string.onboarding_api_key_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_api_key_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = apiKeyError != null || (apiKey.isNotEmpty() && !isValid),
            supportingText = {
                when {
                    apiKeyError != null -> Text(apiKeyError)
                    apiKey.isNotEmpty() && !isValid -> Text(stringResource(R.string.api_key_error_length))
                }
            }
        )
        Button(
            onClick = onSave,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(R.string.onboarding_get_started))
        }
    }
}

@Composable
private fun OnboardingStepSync(
    isSyncing: Boolean,
    onSync: () -> Unit,
    onNext: () -> Unit
) {
    OnboardingCard(
        icon = Icons.Default.Sync,
        title = stringResource(R.string.onboarding_sync_title),
        subtitle = stringResource(R.string.onboarding_sync_desc)
    ) {
        if (isSyncing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.sync_status_syncing),
                    style = MonoMetricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Button(
                onClick = onSync,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(R.string.onboarding_sync_now))
            }
        }
        OutlinedButton(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(R.string.onboarding_continue))
        }
    }
}

@Composable
private fun OnboardingStepHealth(onSkip: () -> Unit, onEnable: () -> Unit) {
    OnboardingCard(
        icon = Icons.Default.Favorite,
        title = stringResource(R.string.settings_item_health_connect),
        subtitle = stringResource(R.string.onboarding_health_desc)
    ) {
        Text(
            text = stringResource(R.string.health_permissions_dialog_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onEnable,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(R.string.onboarding_enable_health))
        }
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }
    }
}

@Composable
private fun OnboardingStepAi(status: String?, onCheck: () -> Unit, onFinish: () -> Unit) {
    OnboardingCard(
        icon = Icons.Default.AutoAwesome,
        title = stringResource(R.string.onboarding_ai_title),
        subtitle = stringResource(R.string.onboarding_ai_desc)
    ) {
        if (status != null) {
            PlateStatusPill(
                label = status,
                icon = Icons.Default.AutoAwesome,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        OutlinedButton(
            onClick = onCheck,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(R.string.onboarding_check_ai))
        }
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Text(stringResource(R.string.onboarding_finish))
        }
    }
}

@Composable
private fun OnboardingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    PlateCard(
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
