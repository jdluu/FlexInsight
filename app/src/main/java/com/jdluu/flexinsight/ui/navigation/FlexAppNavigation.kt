package com.jdluu.flexinsight.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.screens.AITrainerScreen
import com.jdluu.flexinsight.ui.screens.DashboardScreen
import com.jdluu.flexinsight.ui.screens.HistoryAnalysisScreen
import com.jdluu.flexinsight.ui.screens.HistoryScreen
import com.jdluu.flexinsight.ui.screens.OnboardingScreen
import com.jdluu.flexinsight.ui.screens.PRListScreen
import com.jdluu.flexinsight.ui.screens.PlannerScreen
import com.jdluu.flexinsight.ui.screens.PlateRackScreen
import com.jdluu.flexinsight.ui.screens.RecoveryScreen
import com.jdluu.flexinsight.ui.screens.SettingsScreen
import com.jdluu.flexinsight.ui.screens.WorkoutDetailScreen
import com.jdluu.flexinsight.ui.viewmodel.AITrainerViewModel
import com.jdluu.flexinsight.ui.viewmodel.DashboardViewModel
import com.jdluu.flexinsight.ui.viewmodel.HistoryViewModel
import com.jdluu.flexinsight.ui.viewmodel.PRListViewModel
import com.jdluu.flexinsight.ui.viewmodel.PlannerViewModel
import com.jdluu.flexinsight.ui.viewmodel.PlateRackViewModel
import com.jdluu.flexinsight.ui.viewmodel.RecoveryViewModel
import com.jdluu.flexinsight.ui.viewmodel.SettingsViewModel
import com.jdluu.flexinsight.ui.viewmodel.WorkoutDetailViewModel

@Composable
fun FlexAppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Check for API key and redirect to Onboarding if missing
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(settingsUiState.apiKey, settingsUiState.loadingState) {
        if (settingsUiState.loadingState is LoadingState.Success && settingsUiState.apiKey.isNullOrBlank()) {
            if (navController.currentDestination?.route != Screen.Onboarding.route) {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                }
            }
        }
    }

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
                },
                onNavigateToPlateRack = {
                    navController.navigate(Screen.PlateRack.createRoute())
                }
            )
        }
        composable(Screen.Recovery.route) {
            val viewModel = hiltViewModel<RecoveryViewModel>()
            RecoveryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlanner = { navController.navigate(Screen.Planner.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                    navController.navigate(Screen.HistoryAnalysis.route)
                },
                onNavigateToPRList = {
                    navController.navigate(Screen.PRList.route)
                }
            )
        }
        composable(Screen.HistoryAnalysis.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                runCatching { navController.getBackStackEntry(Screen.History.route) }
                    .getOrDefault(backStackEntry)
            }
            val viewModel = hiltViewModel<HistoryViewModel>(parentEntry)
            HistoryAnalysisScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AITrainer.route) {
            val viewModel = hiltViewModel<AITrainerViewModel>()
            AITrainerScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Planner.route) {
            val viewModel = hiltViewModel<PlannerViewModel>()
            PlannerScreen(
                viewModel = viewModel,
                onNavigateToPlateRack = {
                    navController.navigate(Screen.PlateRack.createRoute())
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlateRack = { weight, unit ->
                    navController.navigate(Screen.PlateRack.createRoute(weight, unit.name))
                }
            )
        }
        composable(
            route = Screen.PlateRack.route,
            arguments = listOf(
                navArgument("weight") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("unit") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val viewModel = hiltViewModel<PlateRackViewModel>()
            PlateRackScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
    }
}
