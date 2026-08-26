package com.jdluu.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.data.model.PlannedWorkout
import com.jdluu.flexinsight.ui.theme.*
import com.jdluu.flexinsight.ui.viewmodel.PlannerViewModel
import com.jdluu.flexinsight.ui.viewmodel.SaveToHevyStatus
import com.jdluu.flexinsight.ui.screens.planner.parts.*
import kotlinx.coroutines.launch
import java.util.Calendar

import com.jdluu.flexinsight.ui.components.PlannerSkeleton

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = com.jdluu.flexinsight.ui.common.LocalSnackbarHostState.current

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            PlannerSkeleton()
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

    var showRescheduleDialog by remember { mutableStateOf<PlannedWorkout?>(null) }
    val movedTomorrowMessage = stringResource(R.string.planner_snackbar_moved_tomorrow)

    showRescheduleDialog?.let { workout ->
        PlannerRescheduleDialog(
            workout = workout,
            onConfirm = {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                workout.id?.let { id ->
                    viewModel.rescheduleWorkout(id, calendar.timeInMillis)
                }
                scope.launch { snackbarHostState.showSnackbar(movedTomorrowMessage) }
                showRescheduleDialog = null
            },
            onDismiss = { showRescheduleDialog = null }
        )
    }

    if (uiState.isGeneratingPlan) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(id = R.string.planner_toast_generating_title)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(id = R.string.planner_toast_analyzing))
                }
            },
            confirmButton = {}
        )
    }

    uiState.aiPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAIPlan() },
            title = { Text(stringResource(id = R.string.planner_dialog_ai_title)) },
            text = {
                LazyColumn {
                    item {
                        Text(plan)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (uiState.saveToHevyStatus) {
                        is SaveToHevyStatus.Saving -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        is SaveToHevyStatus.Success -> {
                            Text(
                                "Saved",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        else -> {
                            if (uiState.hevyEditingEnabled) {
                                TextButton(
                                    onClick = { viewModel.pushRoutineToHevy("AI Workout") }
                                ) {
                                    Text(stringResource(id = R.string.planner_save_to_hevy))
                                }
                            }
                        }
                    }

                    TextButton(onClick = { viewModel.clearAIPlan() }) {
                        Text(stringResource(id = R.string.action_close))
                    }
                }
            }
        )
    }

    val saveSuccessMessage: String? = when (val status = uiState.saveToHevyStatus) {
        is SaveToHevyStatus.Success -> when {
            status.usedPlaceholder ->
                stringResource(R.string.planner_save_success_placeholder)
            status.unmatchedNames.isEmpty() ->
                stringResource(R.string.planner_save_success_matched, status.matchedCount)
            status.matchedCount == 0 ->
                stringResource(
                    R.string.planner_save_success_unmatched,
                    status.unmatchedNames.joinToString(", ")
                )
            else ->
                stringResource(
                    R.string.planner_save_success_matched_and_unmatched,
                    status.matchedCount,
                    status.unmatchedNames.joinToString(", ")
                )
        }
        else -> null
    }

    LaunchedEffect(uiState.saveToHevyStatus) {
        when (val status = uiState.saveToHevyStatus) {
            is SaveToHevyStatus.Success -> {
                saveSuccessMessage?.let { message ->
                    snackbarHostState.showSnackbar(message)
                }
                viewModel.clearSaveStatus()
            }
            is SaveToHevyStatus.Error -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.clearSaveStatus()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiState.editBlockedMessage) {
        uiState.editBlockedMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearEditBlockedMessage()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            PlannerHeader()
        }
        item {
            WeeklyGoalCard(weeklyGoalProgress = uiState.weeklyGoalProgress)
        }
        item {
            WeekCalendar(
                weekCalendarData = uiState.weekCalendarData,
                selectedDayIndex = uiState.selectedDayIndex,
                onDaySelected = { viewModel.selectDay(it) }
            )
        }
        item {
            WorkoutListSection(
                selectedDayWorkouts = uiState.selectedDayWorkouts,
                selectedDayName = uiState.weekCalendarData.getOrNull(uiState.selectedDayIndex)?.name ?: "Day",
                hevyEditingEnabled = uiState.hevyEditingEnabled,
                onWorkoutComplete = { id, completed ->
                    viewModel.markWorkoutAsComplete(id, completed)
                },
                onReschedule = { workout ->
                    showRescheduleDialog = workout
                }
            )
        }
        item {
             AIInsightsSection(
                volumeBalance = uiState.volumeBalance,
                muscleGroupProgress = uiState.muscleGroupProgress,
                onGeneratePlan = {
                    viewModel.generateAIWorkout()
                }
            )
        }
    }
}
