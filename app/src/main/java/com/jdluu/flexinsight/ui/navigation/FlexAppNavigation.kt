package com.jdluu.flexinsight.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.jdluu.flexinsight.ui.common.LocalSnackbarHostState
import com.jdluu.flexinsight.ui.screens.*
import com.jdluu.flexinsight.ui.screens.history.*
import com.jdluu.flexinsight.ui.viewmodel.*
import kotlinx.coroutines.launch

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jdluu.flexinsight.ui.screens.OnboardingScreen

import androidx.compose.runtime.LaunchedEffect
import com.jdluu.flexinsight.ui.common.LoadingState

@Composable
fun FlexAppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    
    // Check for API key and redirect to Onboarding if missing
    val settingsViewModel = hiltViewModel<SettingsViewModel>()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    
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
                    navController.navigate(Screen.HistoryAnalysis.route)
                },
                onNavigateToPRList = {
                    navController.navigate(Screen.PRList.route)
                }
            )
        }
        composable(Screen.HistoryAnalysis.route) {
            val viewModel = hiltViewModel<HistoryViewModel>()
            HistoryAnalysisScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AITrainer.route) {
            val viewModel = hiltViewModel<AITrainerViewModel>()
            AITrainerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
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
                onNavigateBack = {
                    navController.popBackStack()
                }
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
