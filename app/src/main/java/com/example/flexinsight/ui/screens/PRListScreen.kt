package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.ui.screens.history.parts.EmptyStateMessage
import com.example.flexinsight.ui.screens.history.parts.PRCard
import com.example.flexinsight.ui.screens.history.parts.formatDateShort
import com.example.flexinsight.ui.utils.UnitConverter
import com.example.flexinsight.ui.viewmodel.PRListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRListScreen(
    viewModel: PRListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWorkoutDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val useMetric = uiState.units == "Metric"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Records") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (uiState.loadingState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.prs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateMessage(message = "No Personal Records found yet. Keep training!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Group PRs by exercise? Or just list them chronologically descending?
                // Chronological descending (newest first) seems best for "Recent PRs" feel,
                // but let's confirm. The VM just returns list. Assuming repo returns them sorted.
                // Assuming `getAllPRsWithDetails` calls `getPRsWithDetails` which calls `setDao.getRecentPRsFlow`
                // which uses `ORDER BY id DESC`. Since ID contains index, it might not be strictly chronological across workouts unless ID implies time.
                // Actually `getRecentPRsFlow` orders by `id DESC`.
                // The API ID format is `workoutId_exerciseId_setIndex`.
                // This might not sort strictly by time if workoutIds aren't time-sortable.
                // But typically we want date sorting.
                // Let's sort client side to be safe.
                
                val sortedPrs = uiState.prs.sortedByDescending { it.date }

                // Determine "newest" PR per exercise for highlighting
                 val exercisePRs = sortedPrs.groupBy { it.exerciseName }
                 val newestPRPerExercise = exercisePRs.mapValues { (_, prs) -> prs.maxByOrNull { it.date } }

                items(sortedPrs) { prDetails ->
                    val isNewPR = newestPRPerExercise[prDetails.exerciseName]?.setId == prDetails.setId
                    val convertedWeight = UnitConverter.convertWeight(prDetails.weight, useMetric)
                    
                    PRCard(
                        exercise = prDetails.exerciseName,
                        date = formatDateShort(prDetails.date),
                        muscle = prDetails.muscleGroup,
                        weight = convertedWeight?.toInt()?.toString() ?: "-",
                        unit = UnitConverter.getWeightUnit(useMetric),
                        isNewPR = isNewPR,
                        onClick = { onNavigateToWorkoutDetail(prDetails.workoutId) }
                    )
                }
            }
        }
    }
}
