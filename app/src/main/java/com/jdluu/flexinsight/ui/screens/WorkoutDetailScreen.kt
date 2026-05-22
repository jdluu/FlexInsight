package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdluu.flexinsight.ui.theme.*
import com.jdluu.flexinsight.ui.viewmodel.WorkoutDetailViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.jdluu.flexinsight.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.jdluu.flexinsight.ui.screens.workoutdetail.parts.*

@Composable
fun WorkoutDetailScreen(
    viewModel: WorkoutDetailViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val useMetric = uiState.units == "Metric"

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
                text = stringResource(id = R.string.workout_not_found),
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
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            WorkoutDetailHeader(
                title = workout.name ?: stringResource(id = R.string.workout_default_title),
                date = dateString,
                onNavigateBack = onNavigateBack
            )
        }
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                WorkoutStatsCard(
                    stats = uiState.workoutStats,
                    exercisesWithSets = uiState.exercisesWithSets,
                    totalReps = uiState.exercisesWithSets.sumOf { it.sets.sumOf { set -> set.reps ?: 0 } },
                    useMetric = useMetric
                )
            }
        }
        item {
            ExercisesSection(
                exercisesWithSets = uiState.exercisesWithSets,
                useMetric = useMetric
            )
        }
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = { viewModel.explainWorkout() },
                    enabled = !uiState.isExplainingWorkout,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isExplainingWorkout) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.workout_explain_button))
                    }
                }
            }
        }
        uiState.workoutExplanation?.let { explanation ->
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                stringResource(R.string.workout_explain_title),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(explanation, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        if (workout.notes != null && workout.notes.isNotBlank()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NotesSection(notes = workout.notes)
                }
            }
        }
        if (uiState.isGeneratingReflection) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(id = R.string.workout_coach_analyzing), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (uiState.aiReflection != null) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(stringResource(id = R.string.workout_coach_assessment), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(uiState.aiReflection!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer, lineHeight = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

