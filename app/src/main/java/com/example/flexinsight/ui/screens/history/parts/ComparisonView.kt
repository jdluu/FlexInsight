package com.example.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.R
import com.example.flexinsight.data.model.VolumeTrend
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.utils.UnitConverter
import com.example.flexinsight.ui.viewmodel.ComparisonData

@Composable
fun ComparisonView(
    data: ComparisonData,
    useMetric: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.history_comparison_period_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stringResource(id = R.string.history_comparison_period_label, data.currentPeriodLabel, data.previousPeriodLabel),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Comparison Cards
        ComparisonCard(
            title = stringResource(id = R.string.history_comparison_total_volume),
            currentValue = formatVolume(data.totalVolumeCurrent, useMetric),
            previousValue = formatVolume(data.totalVolumePrevious, useMetric),
            unit = UnitConverter.getWeightUnit(useMetric),
            positiveIsGood = true
        )

        ComparisonCard(
            title = stringResource(id = R.string.history_comparison_workouts),
            currentValue = data.totalWorkoutsCurrent.toString(),
            previousValue = data.totalWorkoutsPrevious.toString(),
            unit = "",
            positiveIsGood = true
        )

        ComparisonCard(
            title = stringResource(id = R.string.history_comparison_avg_duration),
            currentValue = "${data.avgDurationCurrent / 60}m",
            previousValue = "${data.avgDurationPrevious / 60}m",
            unit = "",
            positiveIsGood = true
        )
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    currentValue: String,
    previousValue: String,
    unit: String,
    positiveIsGood: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = currentValue,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (unit.isNotEmpty()) {
                            Text(
                                text = " $unit",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Text(stringResource(id = R.string.history_comparison_current), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Previous
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = previousValue,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(stringResource(id = R.string.history_comparison_previous), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}

private fun formatVolume(volume: Double, useMetric: Boolean): String {
    val converted = UnitConverter.convertVolume(volume, useMetric)
    return if (converted >= 1000) {
        String.format("%.1fk", converted / 1000)
    } else {
        converted.toInt().toString()
    }
}
