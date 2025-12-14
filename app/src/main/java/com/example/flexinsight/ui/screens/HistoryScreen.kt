package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.utils.rememberUnitPreference
import com.example.flexinsight.ui.viewmodel.HistoryViewModel
import com.example.flexinsight.ui.screens.history.parts.*
import com.example.flexinsight.ui.screens.history.parts.formatDateShort

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToPRList: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val useMetric = rememberUnitPreference()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDarkAlt),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }
    
    if (uiState.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDarkAlt)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = uiState.error?.message ?: "Unknown error",
                    color = RedAccent,
                    fontSize = 16.sp
                )
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Retry", color = BackgroundDark)
                }
            }
        }
        return
    }
    var selectedTab by remember { mutableStateOf(0) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDarkAlt),
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
                        useMetric = useMetric
                    )
                }
                item {
                    AnalysisBreakdown(
                        workoutStats = uiState.workoutStats,
                        durationTrend = uiState.durationTrend,
                        muscleGroupProgress = uiState.muscleGroupProgress
                    )
                }
                item {
                    RecentPRsSection(
                        prsWithDetails = uiState.prsWithDetails,
                        onNavigateToWorkoutDetail = onNavigateToWorkoutDetail,
                        onViewAllClick = onNavigateToPRList
                    )
                }
            }
            1 -> { // Exercises Tab
                if (uiState.exercises.isEmpty()) {
                    item {
                        EmptyStateMessage(message = "No exercises found in history.")
                    }
                } else {
                    items(uiState.exercises) { exercise ->
                        ExerciseHistoryItem(exercise)
                    }
                }
            }
            2 -> { // Compare Tab
                item {
                    EmptyStateMessage(
                        message = "Comparison Tool Coming Soon!\nCompare your progress across different time periods.",
                        isPlaceholder = true
                    )
                }
            }
        }
    }
}
