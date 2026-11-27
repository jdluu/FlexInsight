package com.jdluu.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.PRDetails
import com.jdluu.flexinsight.domain.model.Workout
import com.jdluu.flexinsight.ui.components.EmptyStateIllustration
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateSectionHeader
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.theme.PlateNumeralSmall
import com.jdluu.flexinsight.ui.utils.UnitConverter

@Composable
fun RecentSessionsSection(
    workouts: List<Workout>,
    onWorkoutClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (workouts.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlateSectionHeader(title = stringResource(id = R.string.history_recent_sessions))

        workouts.take(6).forEach { workout ->
            val durationMinutes = workout.endTime?.let { (it - workout.startTime) / 60_000 } ?: 0L
            PlateCard(
                onClick = { onWorkoutClick(workout.id) },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = workout.name ?: stringResource(id = R.string.workout_default_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append(formatDateShort(workout.startTime))
                                if (durationMinutes > 0) append(" · ${durationMinutes}m")
                            },
                            style = MonoMetricSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RecentPRsSection(
    prsWithDetails: List<PRDetails> = emptyList(),
    useMetric: Boolean,
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlateSectionHeader(
            title = stringResource(id = R.string.pr_list_title),
            trailingActionLabel = if (prsWithDetails.isNotEmpty()) "View All" else null,
            onTrailingAction = onViewAllClick
        )

        if (prsWithDetails.isEmpty()) {
            EmptyStateIllustration(
                icon = Icons.Outlined.FitnessCenter,
                title = "No PRs Yet",
                description = "Complete workouts with tracked sets to surface your best lifts here."
            )
        } else {
            val exercisePRs = remember(prsWithDetails) { prsWithDetails.groupBy { it.exerciseName } }
            val newestPRPerExercise = remember(exercisePRs) {
                exercisePRs.mapValues { (_, prs) -> prs.maxByOrNull { it.date } }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                prsWithDetails.forEach { prDetails ->
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

@Composable
fun EmptyStateMessage(message: String, isPlaceholder: Boolean = false) {
    EmptyStateIllustration(
        icon = Icons.Outlined.Info,
        title = if (isPlaceholder) "Need More History" else "Nothing Here Yet",
        description = message,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun ExerciseHistoryItem(exercise: Exercise) {
    PlateCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val exerciseNotes = exercise.notes
                if (!exerciseNotes.isNullOrBlank()) {
                    Text(
                        text = exerciseNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val restDuration = exercise.restDuration
            if (restDuration != null && restDuration > 0) {
                PlateStatusPill(
                    label = "${restDuration}s rest",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    borderColor = FlexTheme.colors.hairline
                )
            }
        }
    }
}

@Composable
fun PRCard(
    exercise: String,
    date: String,
    muscle: String,
    weight: String,
    unit: String,
    isNewPR: Boolean,
    onClick: () -> Unit
) {
    PlateCard(
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.small,
                color = if (isNewPR) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = if (isNewPR) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlateStatusPill(
                        label = muscle,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        borderColor = FlexTheme.colors.hairline
                    )
                    Text(
                        text = date,
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = weight,
                        style = PlateNumeralSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = unit,
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (isNewPR) {
                    PlateStatusPill(
                        label = "PR",
                        containerColor = FlexTheme.colors.moderateContainer,
                        contentColor = FlexTheme.colors.onModerateContainer
                    )
                }
            }
        }
    }
}
