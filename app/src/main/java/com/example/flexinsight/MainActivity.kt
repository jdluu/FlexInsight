package com.example.flexinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.flexinsight.data.preferences.ApiKeyManager
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.data.sync.SyncManager
import com.example.flexinsight.ui.common.LocalSnackbarHostState
import com.example.flexinsight.ui.components.FlexBottomNavigation
import com.example.flexinsight.ui.navigation.Screen
import com.example.flexinsight.ui.screens.*
import com.example.flexinsight.ui.theme.FlexInsightTheme
import com.example.flexinsight.ui.viewmodel.*
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

    // Sync on app resume
    LaunchedEffect(Unit) {
        syncManager.syncOnResume()
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
        var apiKeyText by remember { mutableStateOf("") }
        // Basic validation logic matching ApiKeyManager
        val isValid = apiKeyText.isNotBlank() && apiKeyText.length >= 10
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { /* Don't allow dismissing without API key */ },
            title = {
                Text(
                    text = "Hevy API Key Required",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "To use FlexInsight, you need to provide your Hevy API key. You can get it from https://hevy.com/settings?developer",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("API Key", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        placeholder = { Text("Enter your API key", color = MaterialTheme.colorScheme.outline) },
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
                                    text = "Must be at least 10 characters",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                    val errorText = error
                    if (errorText != null) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                         scope.launch {
                            apiKeyManager.saveApiKey(apiKeyText)
                            showApiKeyPrompt = false
                        }
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
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
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
            composable(Screen.Dashboard.route) {
                val viewModel = hiltViewModel<DashboardViewModel>()
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    },
                    onNavigateToRecovery = {
                        navController.navigate(Screen.Recovery.route)
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route)
                    },
                    onNavigateToAITrainer = {
                        navController.navigate(Screen.AITrainer.route)
                    },
                    onNavigateToPlanner = {
                        navController.navigate(Screen.Planner.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            composable(Screen.History.route) {
                val viewModel = hiltViewModel<HistoryViewModel>()
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    },
                    onNavigateToAnalysis = {
                        scope.launch { snackbarHostState.showSnackbar("Detail analysis coming soon") }
                    },
                    onNavigateToPRList = {
                         navController.navigate(Screen.PRList.route)
                    }
                )
            }
            composable(Screen.AITrainer.route) {
                val viewModel = hiltViewModel<AITrainerViewModel>()
                AITrainerScreen(viewModel = viewModel)
            }
            composable(Screen.Planner.route) {
                val viewModel = hiltViewModel<PlannerViewModel>()
                PlannerScreen(viewModel = viewModel)
            }
            composable(Screen.Recovery.route) {
                val viewModel = hiltViewModel<RecoveryViewModel>()
                RecoveryScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                val viewModel = hiltViewModel<SettingsViewModel>()
                SettingsScreen()
            }
            composable(Screen.PRList.route) {
                val viewModel = hiltViewModel<PRListViewModel>()
                PRListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    }
                )
            }
            composable(Screen.WorkoutDetail.route) { backStackEntry ->
                // Note: hiltViewModel() automatically handles SavedStateHandle injection for arguments
                val viewModel = hiltViewModel<WorkoutDetailViewModel>()
                WorkoutDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
}