package com.example.hevyinsight

import android.os.Bundle
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
import com.example.hevyinsight.data.preferences.ApiKeyManager
import com.example.hevyinsight.ui.components.HevyBottomNavigation
import com.example.hevyinsight.ui.navigation.Screen
import com.example.hevyinsight.ui.screens.*
import com.example.hevyinsight.ui.theme.HevyInsightTheme
import com.example.hevyinsight.ui.viewmodel.DashboardViewModel
import com.example.hevyinsight.ui.viewmodel.HistoryViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HevyInsightTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun getApplication(): HevyInsightApplication? {
    val context = LocalContext.current
    return context.applicationContext as? HevyInsightApplication
}

@Composable
fun MainScreen() {
    val application = getApplication()
    
    if (application == null) {
        // Show error screen if application can't be accessed
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.example.hevyinsight.ui.theme.BackgroundDark)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Application error: Failed to initialize",
                color = com.example.hevyinsight.ui.theme.RedAccent
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
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "To use HevyInsight, you need to provide your Hevy API key. You can get it from https://hevy.com/settings?developer",
                        color = com.example.hevyinsight.ui.theme.TextSecondary,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it },
                        label = { Text("API Key", color = com.example.hevyinsight.ui.theme.TextSecondary) },
                        placeholder = { Text("Enter your API key", color = com.example.hevyinsight.ui.theme.TextTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = androidx.compose.ui.graphics.Color.White,
                            unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                            focusedBorderColor = com.example.hevyinsight.ui.theme.Primary,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = com.example.hevyinsight.ui.theme.TextSecondary,
                            unfocusedLabelColor = com.example.hevyinsight.ui.theme.TextSecondary
                        ),
                        singleLine = true
                    )
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = com.example.hevyinsight.ui.theme.RedAccent,
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
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.hevyinsight.ui.theme.Primary)
                ) {
                    Text("Save", color = com.example.hevyinsight.ui.theme.BackgroundDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = com.example.hevyinsight.ui.theme.SurfaceCardAlt,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                HevyBottomNavigation(
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
                    }
                )
            }
            composable(Screen.AITrainer.route) {
                AITrainerScreen()
            }
                composable(Screen.Planner.route) {
                    val viewModel: com.example.hevyinsight.ui.viewmodel.PlannerViewModel = viewModel {
                        com.example.hevyinsight.ui.viewmodel.PlannerViewModel(application.repository)
                    }
                    PlannerScreen(viewModel = viewModel)
                }
            composable(Screen.Recovery.route) {
                RecoveryScreen()
            }
            composable(Screen.Settings.route) {
                val viewModel: com.example.hevyinsight.ui.viewmodel.SettingsViewModel = viewModel {
                    com.example.hevyinsight.ui.viewmodel.SettingsViewModel(application.repository, application.userPreferencesManager)
                }
                SettingsScreen()
            }
            composable(Screen.WorkoutDetail.route) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getString("workoutId")
                WorkoutDetailScreen(
                    workoutId = workoutId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}