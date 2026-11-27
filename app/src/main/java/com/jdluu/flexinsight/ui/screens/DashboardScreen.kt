package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.components.ErrorBanner
import com.jdluu.flexinsight.ui.components.FeaturedWorkoutCardSkeleton
import com.jdluu.flexinsight.ui.components.NetworkStatusIndicator
import com.jdluu.flexinsight.ui.components.StatsSkeleton
import com.jdluu.flexinsight.ui.screens.dashboard.parts.*
import com.jdluu.flexinsight.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onNavigateToRecovery: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAITrainer: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useMetric = uiState.units == "Metric"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DashboardHeader(
            userName = uiState.profileInfo?.displayName,
            lastSyncAt = uiState.lastSyncAt,
            isSyncing = uiState.isSyncing,
            onSyncClick = { viewModel.sync() },
            onProfileClick = onNavigateToSettings
        )

        if (uiState.isLoading && uiState.workoutStats == null && uiState.latestWorkout == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeaturedWorkoutCardSkeleton()
                StatsSkeleton()
            }
            return@Column
        }

        val error = uiState.error
        if (error != null && uiState.workoutStats == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ErrorBanner(error = error)
                    Button(onClick = { viewModel.refresh() }) {
                        Text(text = stringResource(id = R.string.action_retry))
                    }
                }
            }
            return@Column
        }

        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = { viewModel.sync() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    NetworkStatusIndicator(
                        networkState = uiState.networkState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                if (error != null) {
                    item {
                        ErrorBanner(
                            error = error,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                if (uiState.deloadAlert?.shouldDeload == true) {
                    item {
                        DeloadBanner(message = uiState.deloadAlert?.message)
                    }
                }

                // First Viewport 1: Composite Readiness Hero -> RecoveryScreen
                item {
                    RecoveryPreviewCard(
                        muscleRecovery = uiState.muscleRecovery,
                        onClick = onNavigateToRecovery
                    )
                }

                // First Viewport 2: Next Planned Session
                item {
                    NextSessionCard(
                        plannedWorkout = uiState.todayPlannedWorkout,
                        onOpenPlanner = onNavigateToPlanner
                    )
                }

                // First Viewport 3: Segmented Weekly Volume & Key Metrics
                item {
                    WeeklyVolumePlateCard(
                        workoutStats = uiState.workoutStats,
                        weeklyProgress = uiState.weeklyProgress,
                        weeklyGoalProgress = uiState.weeklyGoalProgress,
                        currentStreak = uiState.currentStreak,
                        useMetric = useMetric
                    )
                }

                // Below fold: Last Session, Training Load, AI Note, 4-week Muscle Focus
                item {
                    LastSessionAnalysisCard(
                        workout = uiState.latestWorkout,
                        stats = uiState.latestWorkoutStats,
                        useMetric = useMetric,
                        onClick = {
                            uiState.latestWorkout?.id?.let(onNavigateToWorkoutDetail)
                        }
                    )
                }

                if (uiState.trainingLoad != null) {
                    item {
                        TrainingLoadCard(score = uiState.trainingLoad)
                    }
                }

                if (uiState.dailyInsight != null || uiState.isGeneratingInsight) {
                    item {
                        QuickInsightCard(
                            insight = uiState.dailyInsight,
                            isLoading = uiState.isGeneratingInsight
                        )
                    }
                }

                if (uiState.muscleGroupProgress.isNotEmpty()) {
                    item {
                        MuscleGroupProgressSection(
                            muscleGroupProgress = uiState.muscleGroupProgress
                        )
                    }
                }
            }
        }
    }
}
