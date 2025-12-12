package com.example.hevyinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hevyinsight.ui.components.HevyBottomNavigation
import com.example.hevyinsight.ui.navigation.Screen
import com.example.hevyinsight.ui.screens.*
import com.example.hevyinsight.ui.theme.HevyInsightTheme

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
fun MainScreen() {
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
                DashboardScreen(
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    },
                    onNavigateToRecovery = {
                        navController.navigate(Screen.Recovery.route)
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToWorkoutDetail = { workoutId ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workoutId))
                    }
                )
            }
            composable(Screen.AITrainer.route) {
                AITrainerScreen()
            }
            composable(Screen.Planner.route) {
                PlannerScreen()
            }
            composable(Screen.Recovery.route) {
                RecoveryScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(
                    androidx.navigation.NavArgument("workoutId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
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