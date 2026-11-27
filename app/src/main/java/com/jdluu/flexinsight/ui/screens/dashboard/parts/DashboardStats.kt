package com.jdluu.flexinsight.ui.screens.dashboard.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.data.model.WeeklyGoalProgress
import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.data.model.WorkoutStats
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.PlateNumeralMedium
import com.jdluu.flexinsight.ui.utils.UnitConverter

@Composable
fun WeeklyVolumePlateCard(
    workoutStats: WorkoutStats?,
    weeklyProgress: List<WeeklyProgress>,
    weeklyGoalProgress: WeeklyGoalProgress?,
    currentStreak: Int,
    useMetric: Boolean,
    modifier: Modifier = Modifier
) {
    val thisWeekVol = weeklyProgress.firstOrNull()?.totalVolume ?: 0.0
    val goalCompleted = weeklyGoalProgress?.completed ?: weeklyProgress.firstOrNull()?.workoutCount ?: 0
    val goalTarget = (weeklyGoalProgress?.target ?: 5).coerceAtLeast(1)
    val goalFraction = (goalCompleted.toFloat() / goalTarget.toFloat()).coerceIn(0f, 1f)
    val totalWorkouts = workoutStats?.totalWorkouts ?: 0
    val unitLabel = UnitConverter.getWeightUnit(useMetric)

    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_weekly_volume),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            PlateStatusPill(
                label = "$goalCompleted / $goalTarget sessions",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        CalibratedPlateBar(
            progress = goalFraction,
            segments = goalTarget.coerceIn(3, 7),
            activeColor = MaterialTheme.colorScheme.primary,
            barHeight = 10.dp
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricColumn(
                value = UnitConverter.formatVolume(thisWeekVol, useMetric),
                unit = unitLabel,
                label = "Week Vol",
                modifier = Modifier.weight(1f)
            )
            MetricColumn(
                value = "$currentStreak",
                unit = "d",
                label = "Streak",
                modifier = Modifier.weight(1f)
            )
            MetricColumn(
                value = formatNumber(totalWorkouts),
                unit = "",
                label = "Workouts",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricColumn(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                style = PlateNumeralMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MonoMetricMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
