package com.example.flexinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flexinsight.data.preferences.ApiKeyManager
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.data.sync.SyncManager
import com.example.flexinsight.ui.common.LocalSnackbarHostState
import com.example.flexinsight.ui.components.FlexBottomNavigation
import com.example.flexinsight.ui.navigation.FlexAppNavigation
import com.example.flexinsight.ui.navigation.Screen
import com.example.flexinsight.ui.theme.FlexInsightTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    @Inject
    lateinit var apiKeyManager: ApiKeyManager

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreference by userPreferencesManager.themeFlow.collectAsState(initial = "System")

            val darkTheme = when (themePreference) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme() // "System" or any other value
            }

            FlexInsightTheme(darkTheme = darkTheme) {
                MainScreen(
                    apiKeyManager = apiKeyManager,
                    syncManager = syncManager
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    apiKeyManager: ApiKeyManager,
    syncManager: SyncManager
) {
    val scope = rememberCoroutineScope()
    var showApiKeyPrompt by remember { mutableStateOf(false) }

    // Check for API key on first launch
    LaunchedEffect(Unit) {
        try {
            val hasApiKey = apiKeyManager.hasApiKey()
            if (!hasApiKey) {
                showApiKeyPrompt = true
            }
        } catch (e: Exception) {
            // If API key check fails, show prompt anyway
            showApiKeyPrompt = true
        }
    }


    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    val showBottomNav = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.History.route,
        Screen.Planner.route,
        Screen.Recovery.route,
        Screen.Settings.route
    )

    // API Key Prompt Dialog
    if (showApiKeyPrompt) {
        ApiKeyPromptDialog(
            onSave = { apiKey ->
                scope.launch {
                    apiKeyManager.saveApiKey(apiKey)
                    showApiKeyPrompt = false
                }
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomNav) {
                    FlexBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            // Always navigate to the selected tab, clearing any screens on top
                            navController.navigate(route) {
                                // Pop up to the start destination, but don't pop the start destination itself
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = false
                                    inclusive = false
                                }
                                // Avoid multiple copies of the same destination
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            FlexAppNavigation(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun ApiKeyPromptDialog(onSave: (String) -> Unit) {
    var apiKeyText by remember { mutableStateOf("") }
    // Basic validation logic matching ApiKeyManager
    val isValid = apiKeyText.isNotBlank() && apiKeyText.length >= 10
    var error by remember { mutableStateOf<String?>(null) } // Keep potential for future use

    AlertDialog(
        onDismissRequest = { /* Don't allow dismissing without API key */ },
        title = {
            Text(
                text = stringResource(R.string.api_key_required_title),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.api_key_required_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    label = { Text(stringResource(R.string.api_key_label), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    placeholder = { Text(stringResource(R.string.api_key_placeholder), color = MaterialTheme.colorScheme.outline) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    singleLine = true,
                    isError = !isValid && apiKeyText.isNotEmpty(),
                    supportingText = {
                        if (!isValid && apiKeyText.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.api_key_error_length),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKeyText) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Text(stringResource(R.string.save), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    )
}