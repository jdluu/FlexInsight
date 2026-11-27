package com.jdluu.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.model.DayInfo
import com.jdluu.flexinsight.domain.model.MuscleGroupProgress
import com.jdluu.flexinsight.domain.model.VolumeTrend
import com.jdluu.flexinsight.domain.model.WeeklyVolumeData
import com.jdluu.flexinsight.domain.model.WorkoutStats
import com.jdluu.flexinsight.domain.calc.VolumeCalculator
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.theme.PlateNumeralLarge
import com.jdluu.flexinsight.ui.theme.PlateNumeralMedium
import com.jdluu.flexinsight.ui.utils.UnitConverter
import java.time.Instant
import java.time.ZoneId

@Composable
fun StatsGrid(
    workoutCount: Int = 0,
    avgVolume: Int = 0,
    bestWeek: String = "N/A",
    useMetric: Boolean = false
) {
    val avgVolumeConverted = UnitConverter.convertVolume(avgVolume.toDouble(), useMetric)
    val avgVolumeFormatted = VolumeCalculator.compactThousandsLabel(avgVolumeConverted)
    val unitLabel = UnitConverter.getWeightUnit(useMetric)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value = "$workoutCount",
            label = stringResource(id = R.string.history_comparison_workouts),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = avgVolumeFormatted,
            label = stringResource(id = R.string.history_avg_vol_label, unitLabel),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = bestWeek,
            label = stringResource(id = R.string.history_best_week),
            modifier = Modifier.weight(1f),
            isHighlighted = true
        )
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    PlateCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = value,
            style = PlateNumeralMedium,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TotalVolumeCard(
    workoutStats: WorkoutStats? = null,
    volumeTrend: VolumeTrend? = null,
    weeklyVolumeData: List<WeeklyVolumeData> = emptyList(),
    consistencyData: List<DayInfo> = emptyList(),
    useMetric: Boolean = false
) {
    PlateCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.history_comparison_total_volume),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = workoutStats?.let {
                            formatVolumeWithCommas(UnitConverter.convertVolume(it.totalVolume, useMetric))
                        } ?: "0",
                        style = PlateNumeralLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = UnitConverter.getWeightUnit(useMetric),
                        style = MonoMetricMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = workoutStats?.let {
                        stringResource(id = R.string.history_across_workouts, it.totalWorkouts)
                    } ?: stringResource(id = R.string.history_no_workouts_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (volumeTrend != null && volumeTrend.percentageChange != 0.0) {
                val positive = volumeTrend.percentageChange >= 0.0
                PlateStatusPill(
                    label = formatPercentageChange(volumeTrend.percentageChange),
                    containerColor = if (positive) FlexTheme.colors.recoveredContainer
                    else FlexTheme.colors.moderateContainer,
                    contentColor = if (positive) FlexTheme.colors.onRecoveredContainer
                    else FlexTheme.colors.onModerateContainer,
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
            }
        }

        if (consistencyData.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.history_consistency_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            ConsistencyHeatmap(data = consistencyData)
        }
    }
}

@Composable
fun ConsistencyHeatmap(data: List<DayInfo>) {
    val columns = 13
    val rows = 7
    val totalDays = columns * rows
    val relevantData = data.takeLast(totalDays)
    if (relevantData.isEmpty()) return

    val lastItem = relevantData.last()
    val lastDayOfWeek = Instant.ofEpochMilli(lastItem.timestamp)
        .atZone(ZoneId.systemDefault())
        .dayOfWeek.value // 1(Mon)..7(Sun)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val months = remember(relevantData) {
                relevantData.map {
                    Instant.ofEpochMilli(it.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .month.name.take(3)
                }.distinct()
            }
            months.take(3).forEach { monthName ->
                Text(
                    text = monthName,
                    style = MonoMetricSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height((10 * 7 + 4 * 6).dp)
            ) {
                Text(
                    stringResource(id = R.string.day_mon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(id = R.string.day_thu),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(id = R.string.day_sun),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val grid = Array(columns) { Array<DayInfo?>(rows) { null } }
                var dataIdx = relevantData.lastIndex
                var currentCol = columns - 1
                var currentRow = lastDayOfWeek - 1

                while (dataIdx >= 0 && currentCol >= 0) {
                    grid[currentCol][currentRow] = relevantData[dataIdx]
                    dataIdx--
                    currentRow--
                    if (currentRow < 0) {
                        currentRow = 6
                        currentCol--
                    }
                }

                for (c in 0 until columns) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (r in 0 until rows) {
                            val dayInfo = grid[c][r]
                            val color = when {
                                dayInfo?.hasWorkout == true -> MaterialTheme.colorScheme.primary
                                dayInfo != null -> FlexTheme.colors.chalkTrack
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AIInsightsCard(
    volumeTrend: VolumeTrend? = null,
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    useMetric: Boolean = false,
    onAnalyzeClick: () -> Unit = {}
) {
    PlateCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(id = R.string.ai_insights_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.history_ai_insights_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onAnalyzeClick,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = stringResource(id = R.string.action_view_analysis),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
