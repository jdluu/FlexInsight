package com.example.flexinsight

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.flexinsight.data.preferences.ApiKeyManager
import com.example.flexinsight.data.preferences.UserPreferencesManager
import com.example.flexinsight.ui.components.FlexBottomNavigation
import com.example.flexinsight.ui.navigation.Screen
import com.example.flexinsight.ui.screens.*
import com.example.flexinsight.ui.theme.FlexInsightTheme
import com.example.flexinsight.ui.viewmodel.DashboardViewModel
import com.example.flexinsight.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val application = getApplication()
            val userPreferencesManager = remember {
                application?.let { UserPreferencesManager(it) }
            }
            
            val themePreference by userPreferencesManager?.themeFlow?.collectAsState(initial = "System") ?: remember { mutableStateOf("System") }
            
            val darkTheme = when (themePreference) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme() // "System" or any other value
            }
            
            FlexInsightTheme(darkTheme = darkTheme) {
                MainScreen()
            }
        }
    }
}

@Composable
fun getApplication(): FlexInsightApplication? {
    val context = LocalContext.current
    return context.applicationContext as? FlexInsightApplication
}

@Composable
fun MainScreen() {
    val application = getApplication()
    
    if (application == null) {
        // Show error screen if application can't be accessed
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
    
    val context = LocalContext.current
    val apiKeyManager = remember { ApiKeyManager(context) }
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
        application.syncManager.syncOnResume()
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
                        singleLine = true
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
                        if (apiKeyManager.isValidApiKeyFormat(apiKeyText)) {
                            scope.launch {
                                apiKeyManager.saveApiKey(apiKeyText)
                                showApiKeyPrompt = false
                            }
                        } else {
                            error = "API key must be at least 10 characters"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                FlexBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
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
                val viewModel: DashboardViewModel = viewModel {
                    DashboardViewModel(application.repository)
                }
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
                val viewModel: HistoryViewModel = viewModel {
                    HistoryViewModel(application.repository)
                }
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    },
                    onNavigateToAnalysis = {
                        Toast.makeText(context, "detailed analysis coming soon", Toast.LENGTH_SHORT).show()
                    },
                    onNavigateToPRList = {
                         Toast.makeText(context, "PR list coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            composable(Screen.AITrainer.route) {
                AITrainerScreen()
            }
                composable(Screen.Planner.route) {
                    val viewModel: com.example.flexinsight.ui.viewmodel.PlannerViewModel = viewModel {
                        com.example.flexinsight.ui.viewmodel.PlannerViewModel(application.repository)
                    }
                    PlannerScreen(viewModel = viewModel)
                }
            composable(Screen.Recovery.route) {
                RecoveryScreen()
            }
            composable(Screen.Settings.route) {
                val viewModel: com.example.flexinsight.ui.viewmodel.SettingsViewModel = viewModel {
                    com.example.flexinsight.ui.viewmodel.SettingsViewModel(application.repository, application.userPreferencesManager)
                }
                SettingsScreen()
            }
            composable(Screen.WorkoutDetail.route) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId")
                val viewModel: com.example.flexinsight.ui.viewmodel.WorkoutDetailViewModel = viewModel {
                    com.example.flexinsight.ui.viewmodel.WorkoutDetailViewModel(
                        repository = application.repository,
                        database = application.database,
                        workoutId = workoutId
                    )
                }
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