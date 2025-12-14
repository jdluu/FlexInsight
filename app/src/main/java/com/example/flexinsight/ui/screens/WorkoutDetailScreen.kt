package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.utils.rememberViewOnlyMode
import com.example.flexinsight.ui.utils.rememberUnitPreference
import com.example.flexinsight.ui.viewmodel.WorkoutDetailViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import java.text.SimpleDateFormat
import java.util.*
import com.example.flexinsight.ui.screens.workoutdetail.parts.*

@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewOnlyMode = rememberViewOnlyMode()
    val useMetric = rememberUnitPreference()
    
    if (uiState.isLoading) {
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
    
    if (uiState.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = uiState.error?.message ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp
                )
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        return
    }
    
    val workout = uiState.workout
    if (workout == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Workout not found",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }
        return
    }
    
    // Format date
    val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    val dateString = dateFormat.format(Date(workout.startTime))
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            WorkoutDetailHeader(
                title = workout.name ?: "Workout",
                date = dateString,
                onNavigateBack = onNavigateBack
            )
        }
        item {
            WorkoutStatsCard(
                stats = uiState.workoutStats,
                totalReps = uiState.exercisesWithSets.sumOf { it.sets.sumOf { set -> set.reps ?: 0 } },
                useMetric = useMetric
            )
        }
        item {
            ExercisesSection(
                exercisesWithSets = uiState.exercisesWithSets,
                viewOnlyMode = viewOnlyMode,
                useMetric = useMetric
            )
        }
        if (workout.notes != null && workout.notes.isNotBlank()) {
            item {
                NotesSection(notes = workout.notes)
            }
        }
        item {
            AICoachReflectionCard()
        }
    }
}

