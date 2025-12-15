package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.flexinsight.core.network.NetworkState
import com.example.flexinsight.ui.components.ErrorBanner
import com.example.flexinsight.ui.components.NetworkStatusIndicator
import com.example.flexinsight.ui.common.LoadingState
import com.example.flexinsight.ui.screens.dashboard.parts.*
import com.example.flexinsight.ui.theme.BackgroundDark
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.flowOf
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onNavigateToRecovery: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAITrainer: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState.loadingState == LoadingState.Loading
    val useMetric = uiState.units == "Metric"
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            // This LaunchedEffect might need adjustment if isRefreshing is fully derived.
            // For now, keeping it as is based on the instruction's partial change.
        }
    }

    if (uiState.isLoading && !isRefreshing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Network status indicator
    val networkState = uiState.networkState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                top = if (isRefreshing) 60.dp else 0.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (networkState is NetworkState.Unavailable) {
                item {
                    NetworkStatusIndicator(
                        networkState = networkState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            // Error banner
            uiState.error?.let { error ->
                item {
                    ErrorBanner(
                        error = error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onDismiss = null
                    )
                }
            }

            item {
                DashboardHeader(
                    profileInfo = uiState.profileInfo,
                    onNotificationsClick = { onNavigateToSettings() },
                    onRefreshClick = { viewModel.sync() }
                )
            }
            item {
                uiState.latestWorkout?.let { workout ->
                    FeaturedWorkoutCard(
                        workout = workout,
                        workoutStats = uiState.latestWorkoutStats,
                        useMetric = useMetric,
                        onClick = { onNavigateToWorkoutDetail(workout.id) }
                    )
                } ?: run {
                    // Show placeholder if no workout
                    FeaturedWorkoutCardPlaceholder()
                }
            }
            item {
                WeeklyProgressSection(
                    progress = uiState.weeklyProgress,
                    muscleGroupProgress = uiState.muscleGroupProgress,
                    useMetric = useMetric,
                    onSeeAllClick = { onNavigateToHistory() }
                )
            }
            item {
                DailyInsightCard(
                    dailyInsight = uiState.dailyInsight,
                    isGeneratingInsight = uiState.isGeneratingInsight,
                    onChatClick = { onNavigateToAITrainer() }
                )
            }
        }

        // Pull-to-refresh indicator
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
