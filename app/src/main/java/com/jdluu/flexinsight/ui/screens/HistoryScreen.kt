package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.components.ErrorBanner
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.WorkoutHistoryListSkeleton
import com.jdluu.flexinsight.ui.screens.history.parts.*
import com.jdluu.flexinsight.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToPRList: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useMetric = uiState.units == "Metric"
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    if (showFilterDialog) {
        HistoryFilterDialog(
            currentFilter = uiState.dateFilter,
            onFilterSelected = { filter ->
                viewModel.setDateFilter(filter)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HistoryHeader(
            activeFilter = uiState.dateFilter,
            onFilterClick = { showFilterDialog = true }
        )

        if (uiState.isLoading) {
            WorkoutHistoryListSkeleton()
            return@Column
        }

        val error = uiState.error
        if (error != null) {
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
                        Text(stringResource(id = R.string.action_retry))
                    }
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TabSelector(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            }

            when (selectedTab) {
                0 -> {
                    if (uiState.isGeneratingTrend) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    item {
                        StatsGrid(
                            workoutCount = uiState.workoutCount,
                            avgVolume = uiState.workoutStats?.averageVolume?.toInt() ?: 0,
                            bestWeek = uiState.workoutStats?.bestWeekDate?.let {
                                formatDateShort(it)
                            } ?: "N/A",
                            useMetric = useMetric
                        )
                    }

                    item {
                        TotalVolumeCard(
                            workoutStats = uiState.workoutStats,
                            volumeTrend = uiState.volumeTrend,
                            weeklyVolumeData = uiState.weeklyVolumeData,
                            consistencyData = uiState.consistencyData,
                            useMetric = useMetric
                        )
                    }

                    if (uiState.workouts.isNotEmpty()) {
                        item {
                            RecentSessionsSection(
                                workouts = uiState.workouts,
                                onWorkoutClick = onNavigateToWorkoutDetail
                            )
                        }
                    }

                    item {
                        RoutineDiffCard(comparison = uiState.routineComparison)
                    }

                    if (uiState.aiTrendAnalysis != null) {
                        item {
                            PlateCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.history_ai_summary_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.aiTrendAnalysis.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    item {
                        AnalysisBreakdown(
                            workoutStats = uiState.workoutStats,
                            durationTrend = uiState.durationTrend,
                            muscleGroupProgress = uiState.muscleGroupProgress,
                            volumeBalance = uiState.volumeBalance
                        )
                    }

                    item {
                        AIInsightsCard(
                            volumeTrend = uiState.volumeTrend,
                            muscleGroupProgress = uiState.muscleGroupProgress,
                            useMetric = useMetric,
                            onAnalyzeClick = onNavigateToAnalysis
                        )
                    }

                    item {
                        RecentPRsSection(
                            prsWithDetails = uiState.prsWithDetails,
                            useMetric = useMetric,
                            onNavigateToWorkoutDetail = onNavigateToWorkoutDetail,
                            onViewAllClick = onNavigateToPRList
                        )
                    }
                }
                1 -> {
                    if (uiState.exercises.isEmpty()) {
                        item {
                            EmptyStateMessage(message = stringResource(id = R.string.history_empty_exercises))
                        }
                    } else {
                        items(
                            items = uiState.exercises,
                            key = { exercise -> exercise.id }
                        ) { exercise ->
                            ExerciseHistoryItem(exercise)
                        }
                    }
                }
                2 -> {
                    item {
                        val comparisonData = uiState.compareData
                        if (comparisonData != null) {
                            ComparisonView(
                                data = comparisonData,
                                useMetric = useMetric
                            )
                        } else {
                            EmptyStateMessage(
                                message = stringResource(id = R.string.history_empty_comparison),
                                isPlaceholder = true
                            )
                        }
                    }
                }
            }
        }
    }
}
