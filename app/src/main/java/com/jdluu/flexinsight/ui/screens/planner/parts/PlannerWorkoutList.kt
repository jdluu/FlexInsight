package com.jdluu.flexinsight.ui.screens.planner.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.data.model.PlannedWorkout
import com.jdluu.flexinsight.ui.components.EmptyStateIllustration
import com.jdluu.flexinsight.ui.components.PlateSectionHeader
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall

@Composable
fun WorkoutListSection(
    selectedDayWorkouts: List<PlannedWorkout> = emptyList(),
    selectedDayName: String = "Day",
    hevyEditingEnabled: Boolean = false,
    onWorkoutComplete: (String, Boolean) -> Unit = { _, _ -> },
    onReschedule: (PlannedWorkout) -> Unit = {},
    onGeneratePlan: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlateSectionHeader(
                title = stringResource(id = R.string.planner_list_day_plan, selectedDayName),
                modifier = Modifier.weight(1f)
            )
            if (selectedDayWorkouts.isNotEmpty()) {
                val countLabel = if (selectedDayWorkouts.size == 1) {
                    stringResource(id = R.string.planner_list_session_singular, selectedDayWorkouts.size)
                } else {
                    stringResource(id = R.string.planner_list_session_plural, selectedDayWorkouts.size)
                }
                PlateStatusPill(
                    label = countLabel,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    borderColor = FlexTheme.colors.hairline
                )
            }
        }

        if (selectedDayWorkouts.isEmpty()) {
            EmptyStateIllustration(
                icon = Icons.Default.EventAvailable,
                title = stringResource(id = R.string.planner_list_empty_title),
                description = stringResource(id = R.string.planner_list_empty_desc),
                action = {
                    Button(
                        onClick = onGeneratePlan,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.action_ai_generate_plan),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            )
        } else {
            selectedDayWorkouts.forEach { workout ->
                val isHigh = workout.intensity?.contains("High", ignoreCase = true) == true
                val isAerobic = workout.intensity?.contains("Aerobic", ignoreCase = true) == true
                val icon = if (isAerobic) {
                    Icons.AutoMirrored.Filled.DirectionsRun
                } else {
                    Icons.Default.FitnessCenter
                }
                val (badgeBg, badgeFg) = when {
                    isHigh -> FlexTheme.colors.recoveredContainer to FlexTheme.colors.onRecoveredContainer
                    isAerobic -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.primary
                }

                WorkoutItem(
                    title = workout.name,
                    duration = formatDuration(workout.duration),
                    intensity = workout.intensity ?: stringResource(id = R.string.planner_list_medium_intensity),
                    isCompleted = workout.isCompleted,
                    icon = icon,
                    badgeContainer = badgeBg,
                    badgeContent = badgeFg,
                    hasCheckbox = hevyEditingEnabled && !workout.isCompleted,
                    onCheckedChange = { isChecked ->
                        workout.id?.let { id -> onWorkoutComplete(id, isChecked) }
                    },
                    onLongClick = {
                        if (hevyEditingEnabled) onReschedule(workout)
                    }
                )
            }

            Text(
                text = if (hevyEditingEnabled) {
                    stringResource(id = R.string.planner_list_long_press_reschedule)
                } else {
                    stringResource(id = R.string.planner_list_view_only_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutItem(
    title: String,
    duration: String,
    intensity: String,
    isCompleted: Boolean,
    icon: ImageVector,
    badgeContainer: Color,
    badgeContent: Color,
    hasCheckbox: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isCompleted) 1.5.dp else 1.dp,
            color = if (isCompleted) FlexTheme.colors.recovered else FlexTheme.colors.hairline
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.small,
                color = badgeContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeContent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = duration,
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PlateStatusPill(
                        label = intensity,
                        containerColor = badgeContainer,
                        contentColor = badgeContent
                    )
                }
            }

            if (isCompleted) {
                PlateStatusPill(
                    label = "Done",
                    icon = Icons.Default.Check,
                    containerColor = FlexTheme.colors.recoveredContainer,
                    contentColor = FlexTheme.colors.onRecoveredContainer
                )
            } else if (hasCheckbox) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}
