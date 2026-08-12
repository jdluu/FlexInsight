package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import com.jdluu.flexinsight.ui.components.WorkoutHistoryListSkeleton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.theme.*
import com.jdluu.flexinsight.ui.viewmodel.HistoryViewModel
import com.jdluu.flexinsight.ui.screens.history.parts.*
import com.jdluu.flexinsight.ui.screens.history.parts.formatDateShort

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    // Filter Dialog
    // Filter Dialog
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

    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HistoryHeader(onFilterClick = {})
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                WorkoutHistoryListSkeleton()
            }
        }
        return
    }

    if (uiState.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = uiState.error?.message ?: stringResource(id = R.string.error_unknown_fallback),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp
                )
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(id = R.string.action_retry), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        return
    }
    var selectedTab by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            HistoryHeader(onFilterClick = { showFilterDialog = true })
        }
        item {
            TabSelector(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }

        when (selectedTab) {
            0 -> { // Summary Tab
                if (uiState.isGeneratingTrend) {
                    item {
                         Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                             CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                         }
                    }
                }
                item {
                    RoutineDiffCard(comparison = uiState.routineComparison)
                }
                if (uiState.aiTrendAnalysis != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(id = R.string.history_ai_summary_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.aiTrendAnalysis!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
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
                item {
                    AnalysisBreakdown(
                        workoutStats = uiState.workoutStats,
                        durationTrend = uiState.durationTrend,
                        muscleGroupProgress = uiState.muscleGroupProgress,
                        volumeBalance = uiState.volumeBalance
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
            1 -> { // Exercises Tab
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
            2 -> { // Compare Tab
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
