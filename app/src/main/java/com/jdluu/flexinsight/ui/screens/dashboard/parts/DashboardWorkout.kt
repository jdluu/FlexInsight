package com.jdluu.flexinsight.ui.screens.dashboard.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Schedule
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
import com.jdluu.flexinsight.data.model.PlannedWorkout
import com.jdluu.flexinsight.data.model.SingleWorkoutStats
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.ui.components.EmptyStateIllustration
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.PlateNumeralSmall
import com.jdluu.flexinsight.ui.utils.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NextSessionCard(
    plannedWorkout: PlannedWorkout?,
    onOpenPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        onClick = onOpenPlanner,
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_next_session),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.dashboard_open_planner),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (plannedWorkout != null) {
            Text(
                text = plannedWorkout.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                plannedWorkout.intensity?.takeIf { it.isNotBlank() }?.let { intensity ->
                    PlateStatusPill(
                        label = intensity,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                PlateStatusPill(
                    label = "${plannedWorkout.duration}m",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = Icons.Outlined.Schedule,
                    borderColor = FlexTheme.colors.hairline
                )
                PlateStatusPill(
                    label = "${plannedWorkout.exerciseCount} exercises",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    borderColor = FlexTheme.colors.hairline
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.dashboard_no_planned_today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(id = R.string.dashboard_open_planner),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun LastSessionAnalysisCard(
    workout: Workout?,
    stats: SingleWorkoutStats?,
    useMetric: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    if (workout == null) {
        EmptyStateIllustration(
            icon = Icons.Outlined.FitnessCenter,
            title = stringResource(id = R.string.dashboard_no_workouts_title),
            description = stringResource(id = R.string.dashboard_no_workouts_desc),
            modifier = modifier.padding(horizontal = 16.dp)
        )
        return
    }

    val durationMinutes = workout.endTime?.let { (it - workout.startTime) / 60_000 } ?: 0L
    val volumeValue = stats?.totalVolume ?: 0.0
    val formattedVol = UnitConverter.formatVolume(volumeValue, useMetric)
    val unitLabel = UnitConverter.getWeightUnit(useMetric)
    val setsCount = stats?.totalSets ?: 0

    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp)
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
                    text = stringResource(id = R.string.dashboard_last_session),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = workout.name ?: stringResource(id = R.string.workout_default_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = dateFormatter.format(Date(workout.startTime)),
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SessionMetricCell(
                label = "Volume ($unitLabel)",
                value = formattedVol,
                modifier = Modifier.weight(1f)
            )
            SessionMetricCell(
                label = "Sets",
                value = setsCount.toString(),
                modifier = Modifier.weight(1f)
            )
            SessionMetricCell(
                label = "Duration",
                value = formatDuration(durationMinutes),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_view_details),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SessionMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = PlateNumeralSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
