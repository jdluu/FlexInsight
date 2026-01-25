package com.example.flexinsight.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.flexinsight.ui.common.LocalSnackbarHostState
import com.example.flexinsight.ui.screens.*
import com.example.flexinsight.ui.viewmodel.*
import kotlinx.coroutines.launch

@Composable
fun FlexAppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
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
            // Note: SettingsViewModel appears unused in original call but might be needed, kept it just in case or removed if strict cleanup.
            // Original code: val viewModel = hiltViewModel<SettingsViewModel>(); SettingsScreen()
            // It seems SettingsScreen doesn't take viewModel argument in original file view?
            // Let's check original MainActivity line 276: SettingsScreen() - no args.
            // But line 275 instantiates it. I will keep instantiation to ensure ViewModel lifecycle if it does init logic.
            hiltViewModel<SettingsViewModel>()
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
        composable(Screen.WorkoutDetail.route) {
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
