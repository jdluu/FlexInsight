package com.example.flexinsight.ui.screens.planner.parts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.flexinsight.data.model.PlannedWorkout
import com.example.flexinsight.ui.theme.BackgroundDarkAlt
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.TextSecondary

@Composable
fun PlannerRescheduleDialog(
    workout: PlannedWorkout,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Workout") },
        text = { Text("Move '${workout.name}' to tomorrow?") },
        containerColor = BackgroundDarkAlt,
        titleContentColor = Color.White,
        textContentColor = TextSecondary,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Move", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
