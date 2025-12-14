package com.example.flexinsight.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flexinsight.data.model.PlannedWorkout
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.utils.rememberViewOnlyMode
import com.example.flexinsight.ui.viewmodel.PlannerViewModel
import com.example.flexinsight.ui.screens.planner.components.*
import java.util.Calendar

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewOnlyMode = rememberViewOnlyMode()
    
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
    
    val context = LocalContext.current
    var showRescheduleDialog by remember { mutableStateOf<PlannedWorkout?>(null) }
    
    if (showRescheduleDialog != null) {
        AlertDialog(
            onDismissRequest = { showRescheduleDialog = null },
            title = { Text("Reschedule Workout") },
            text = { Text("Move '${showRescheduleDialog?.name}' to tomorrow?") },
            containerColor = BackgroundDarkAlt,
            titleContentColor = Color.White,
            textContentColor = TextSecondary,
            confirmButton = {
                TextButton(onClick = {
                    showRescheduleDialog?.let { workout ->
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        workout.id?.let { id ->
                            viewModel.rescheduleWorkout(id, calendar.timeInMillis)
                        }
                        Toast.makeText(context, "Moved to tomorrow", Toast.LENGTH_SHORT).show()
                    }
                    showRescheduleDialog = null
                }) {
                    Text("Move", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescheduleDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDarkAlt),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            PlannerHeader(
                viewOnlyMode = viewOnlyMode,
                onAddWorkout = {
                    Toast.makeText(context, "Create Workout feature coming soon", Toast.LENGTH_SHORT).show()
                }
            )
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
                    Toast.makeText(context, "AI Plan Generation coming soon", Toast.LENGTH_SHORT).show()
                    viewModel.generateAIWorkout()
                }
            )
        }
    }
}
