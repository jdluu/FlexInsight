package com.jdluu.flexinsight.ui.screens.workoutdetail.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.data.model.SingleWorkoutStats
import com.jdluu.flexinsight.domain.calc.DurationCalculator
import com.jdluu.flexinsight.domain.calc.PersonalRecordCalculator
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.PlateNumeralMedium
import com.jdluu.flexinsight.ui.utils.UnitConverter
import com.jdluu.flexinsight.ui.viewmodel.ExerciseWithSets

@Composable
fun WorkoutStatsCard(
    stats: SingleWorkoutStats?,
    exercisesWithSets: List<ExerciseWithSets>,
    totalReps: Int,
    useMetric: Boolean = false
) {
    val durationText = stats?.durationMinutes?.let(DurationCalculator::durationLabel) ?: "0m"
    val volumeText = if (stats?.totalVolume != null && stats.totalVolume > 0) {
        UnitConverter.formatVolumeWithCommas(stats.totalVolume, useMetric)
    } else {
        "0"
    }
    val weightUnit = UnitConverter.getWeightUnit(useMetric)
    val setsText = (stats?.totalSets ?: 0).toString()
    val repsText = totalReps.toString()
    val hasPR = PersonalRecordCalculator.hasPersonalRecord(exercisesWithSets.flatMap { it.sets })

    PlateCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(durationText, "Duration", modifier = Modifier.weight(1f))
                StatItem(volumeText, "Volume ($weightUnit)", modifier = Modifier.weight(1f))
            }

            HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(setsText, "Sets", modifier = Modifier.weight(1f))
                StatItem(repsText, "Reps", modifier = Modifier.weight(1f))
            }

            if (hasPR || exercisesWithSets.isNotEmpty()) {
                HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasPR) {
                        PlateStatusPill(
                            label = "PR Day",
                            icon = Icons.Default.EmojiEvents,
                            containerColor = FlexTheme.colors.recoveredContainer,
                            contentColor = FlexTheme.colors.onRecoveredContainer
                        )
                    }
                    if (exercisesWithSets.isNotEmpty()) {
                        PlateStatusPill(
                            label = "${exercisesWithSets.size} Exercises",
                            icon = Icons.Default.FitnessCenter,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            borderColor = FlexTheme.colors.hairline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = PlateNumeralMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
