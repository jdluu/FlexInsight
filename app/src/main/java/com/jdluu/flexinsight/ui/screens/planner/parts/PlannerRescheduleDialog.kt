package com.jdluu.flexinsight.ui.screens.planner.parts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.data.model.PlannedWorkout
import com.jdluu.flexinsight.ui.theme.BackgroundDarkAlt
import com.jdluu.flexinsight.ui.theme.Primary
import com.jdluu.flexinsight.ui.theme.TextSecondary

@Composable
fun PlannerRescheduleDialog(
    workout: PlannedWorkout,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.planner_reschedule_dialog_title)) },
        text = { Text(stringResource(id = R.string.planner_reschedule_dialog_desc, workout.name)) },
        containerColor = BackgroundDarkAlt,
        titleContentColor = Color.White,
        textContentColor = TextSecondary,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(id = R.string.planner_reschedule_dialog_action), color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = TextSecondary)
            }
        }
    )
}
