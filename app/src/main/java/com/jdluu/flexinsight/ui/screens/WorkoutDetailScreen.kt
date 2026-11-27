package com.jdluu.flexinsight.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.calc.DurationCalculator
import com.jdluu.flexinsight.ui.components.EmptyStateIllustration
import com.jdluu.flexinsight.ui.components.ErrorBanner
import com.jdluu.flexinsight.ui.screens.workoutdetail.parts.*
import com.jdluu.flexinsight.ui.utils.UnitConverter
import com.jdluu.flexinsight.ui.viewmodel.WorkoutDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useMetric = uiState.units == "Metric"
    val context = LocalContext.current

    if (uiState.isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            WorkoutDetailTopBar(
                onNavigateBack = onNavigateBack,
                onShareClick = {},
                shareEnabled = false
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    if (uiState.error != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            WorkoutDetailTopBar(
                onNavigateBack = onNavigateBack,
                onShareClick = {},
                shareEnabled = false
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ErrorBanner(
                    error = uiState.error!!,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
        return
    }

    val workout = uiState.workout
    if (workout == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) {
            WorkoutDetailTopBar(
                onNavigateBack = onNavigateBack,
                onShareClick = {},
                shareEnabled = false
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateIllustration(
                    icon = Icons.Outlined.FitnessCenter,
                    title = stringResource(id = R.string.workout_not_found),
                    description = stringResource(id = R.string.error_unknown_fallback)
                )
            }
        }
        return
    }

    val dateString = remember(workout.startTime) {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(workout.startTime))
    }
    val workoutTitle = workout.name ?: stringResource(id = R.string.workout_default_title)
    val totalReps = remember(uiState.exercisesWithSets) {
        uiState.exercisesWithSets.sumOf { ex -> ex.sets.sumOf { it.reps ?: 0 } }
    }

    val shareWorkout: () -> Unit = {
        val duration = uiState.workoutStats?.durationMinutes?.let(DurationCalculator::durationLabel) ?: "0m"
        val vol = uiState.workoutStats?.totalVolume?.takeIf { it > 0 }?.let {
            UnitConverter.formatVolumeWithCommas(it, useMetric) + " " + UnitConverter.getWeightUnit(useMetric)
        } ?: "0 ${UnitConverter.getWeightUnit(useMetric)}"
        val setsCount = uiState.workoutStats?.totalSets ?: 0
        val shareText = buildString {
            appendLine("$workoutTitle — $dateString")
            appendLine("Duration: $duration | Volume: $vol | Sets: $setsCount | Reps: $totalReps")
            if (uiState.exercisesWithSets.isNotEmpty()) {
                appendLine()
                uiState.exercisesWithSets.forEach { ex ->
                    appendLine("• ${ex.exercise.name} (${ex.sets.size} sets)")
                }
            }
        }
        runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, workoutTitle)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, workoutTitle))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        WorkoutDetailTopBar(
            onNavigateBack = onNavigateBack,
            onShareClick = shareWorkout
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                WorkoutDetailSessionHeader(
                    title = workoutTitle,
                    date = dateString
                )
            }
            item {
                WorkoutStatsCard(
                    stats = uiState.workoutStats,
                    exercisesWithSets = uiState.exercisesWithSets,
                    totalReps = totalReps,
                    useMetric = useMetric
                )
            }
            item {
                ExercisesSection(
                    exercisesWithSets = uiState.exercisesWithSets,
                    useMetric = useMetric
                )
            }
            val workoutNotes = workout.notes
            if (!workoutNotes.isNullOrBlank()) {
                item {
                    NotesSection(notes = workoutNotes)
                }
            }
            item {
                WorkoutCoachSection(
                    aiReflection = uiState.aiReflection,
                    isGeneratingReflection = uiState.isGeneratingReflection,
                    workoutExplanation = uiState.workoutExplanation,
                    isExplainingWorkout = uiState.isExplainingWorkout,
                    onExplainWorkout = { viewModel.explainWorkout() }
                )
            }
        }
    }
}
