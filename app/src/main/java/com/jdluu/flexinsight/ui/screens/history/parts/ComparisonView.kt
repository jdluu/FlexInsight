package com.jdluu.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.theme.PlateNumeralMedium
import com.jdluu.flexinsight.ui.utils.UnitConverter
import com.jdluu.flexinsight.ui.viewmodel.ComparisonData
import java.util.Locale

@Composable
fun ComparisonView(
    data: ComparisonData,
    useMetric: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.history_comparison_period_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            PlateStatusPill(
                label = stringResource(
                    id = R.string.history_comparison_period_label,
                    data.currentPeriodLabel,
                    data.previousPeriodLabel
                ),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        ComparisonCard(
            title = stringResource(id = R.string.history_comparison_total_volume),
            currentValue = formatVolume(data.totalVolumeCurrent, useMetric),
            previousValue = formatVolume(data.totalVolumePrevious, useMetric),
            unit = UnitConverter.getWeightUnit(useMetric),
            deltaPercent = percentDelta(data.totalVolumeCurrent, data.totalVolumePrevious)
        )

        ComparisonCard(
            title = stringResource(id = R.string.history_comparison_workouts),
            currentValue = data.totalWorkoutsCurrent.toString(),
            previousValue = data.totalWorkoutsPrevious.toString(),
            unit = "",
            deltaPercent = percentDelta(
                data.totalWorkoutsCurrent.toDouble(),
                data.totalWorkoutsPrevious.toDouble()
            )
        )

        ComparisonCard(
            title = stringResource(id = R.string.history_comparison_avg_duration),
            currentValue = "${data.avgDurationCurrent / 60}m",
            previousValue = "${data.avgDurationPrevious / 60}m",
            unit = "",
            deltaPercent = percentDelta(
                data.avgDurationCurrent.toDouble(),
                data.avgDurationPrevious.toDouble()
            )
        )
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    currentValue: String,
    previousValue: String,
    unit: String,
    deltaPercent: Int?
) {
    PlateCard(contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (deltaPercent != null) {
                val positive = deltaPercent >= 0
                val sign = if (positive) "+" else ""
                PlateStatusPill(
                    label = "$sign$deltaPercent%",
                    containerColor = if (positive) FlexTheme.colors.recoveredContainer
                    else FlexTheme.colors.moderateContainer,
                    contentColor = if (positive) FlexTheme.colors.onRecoveredContainer
                    else FlexTheme.colors.onModerateContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = currentValue,
                        style = PlateNumeralMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (unit.isNotEmpty()) {
                        Text(
                            text = unit,
                            style = MonoMetricSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(id = R.string.history_comparison_current),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (unit.isNotEmpty()) "$previousValue $unit" else previousValue,
                    style = MonoMetricMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(id = R.string.history_comparison_previous),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun percentDelta(current: Double, previous: Double): Int? {
    if (previous <= 0.0) return null
    return (((current - previous) / previous) * 100.0).toInt()
}

private fun formatVolume(volume: Double, useMetric: Boolean): String {
    val converted = UnitConverter.convertVolume(volume, useMetric)
    return if (converted >= 1000) {
        String.format(Locale.US, "%.1fk", converted / 1000.0)
    } else {
        converted.toInt().toString()
    }
}
