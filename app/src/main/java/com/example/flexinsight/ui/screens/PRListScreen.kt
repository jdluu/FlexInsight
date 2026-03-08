package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.R

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
                title = { Text(stringResource(id = R.string.pr_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_back))
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
                EmptyStateMessage(message = stringResource(id = R.string.pr_list_empty_state))
            }
        } else {
            val sortedPrs = remember(uiState.prs) { uiState.prs.sortedByDescending { it.date } }
            val exercisePRs = remember(sortedPrs) { sortedPrs.groupBy { it.exerciseName } }
            val newestPRPerExercise = remember(exercisePRs) { exercisePRs.mapValues { (_, prs) -> prs.maxByOrNull { it.date } } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {


                items(
                    items = sortedPrs,
                    key = { prDetails -> prDetails.setId }
                ) { prDetails ->
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
