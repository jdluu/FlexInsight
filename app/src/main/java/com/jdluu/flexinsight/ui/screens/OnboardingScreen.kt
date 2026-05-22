package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.screens.settings.parts.HealthConnectPermissionsDialog
import com.jdluu.flexinsight.ui.theme.*
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
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val isValid = apiKey.length >= 10
    var showHealthPermissionsExplain by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        val allGranted = viewModel.getHealthConnectPermissions().all { it in granted }
        viewModel.setHealthConnectEnabled(allGranted)
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

    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = BackgroundDark) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            BackgroundDark
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OnboardingProgress(step = step, total = 4)

                Spacer(modifier = Modifier.height(24.dp))

                when (step) {
                    0 -> OnboardingStepApiKey(
                        apiKey = apiKey,
                        onApiKeyChange = { apiKey = it },
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
                            scope.launch {
                                aiStatus = when (viewModel.uiState.value.forceAiEnable) {
                                    true -> "Debug AI stubs enabled (not real Gemini Nano)"
                                    else -> "Checking on-device AI…"
                                }
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
private fun OnboardingProgress(step: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (index <= step) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun OnboardingStepApiKey(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
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
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = onSave,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth()
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
            CircularProgressIndicator()
        } else {
            Button(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_sync_now))
            }
        }
        OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
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
            fontSize = 12.sp,
            color = TextTertiary
        )
        Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_enable_health))
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
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
            Text(status, fontSize = 14.sp, color = TextSecondary)
        }
        Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_check_ai))
        }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_finish))
        }
    }
}

@Composable
private fun OnboardingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Primary)
                Spacer(Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Text(subtitle, fontSize = 14.sp, color = TextTertiary)
            content()
        }
    }
}
