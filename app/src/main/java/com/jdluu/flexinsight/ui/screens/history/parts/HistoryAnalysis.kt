package com.jdluu.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.domain.model.DailyDurationData
import com.jdluu.flexinsight.domain.model.MuscleGroupProgress
import com.jdluu.flexinsight.domain.model.VolumeBalance
import com.jdluu.flexinsight.domain.model.WorkoutStats
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.theme.PlateNumeralSmall

@Composable
fun AnalysisBreakdown(
    workoutStats: WorkoutStats? = null,
    durationTrend: List<DailyDurationData> = emptyList(),
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    volumeBalance: VolumeBalance? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Training Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VolumeSplitCard(
                modifier = Modifier.weight(1f),
                volumeBalance = volumeBalance
            )
            AnalysisCard(
                title = "Duration Trend",
                modifier = Modifier.weight(1f),
                workoutStats = workoutStats,
                durationTrend = durationTrend,
                muscleGroupProgress = muscleGroupProgress
            )
        }
    }
}

@Composable
fun VolumeSplitCard(
    modifier: Modifier = Modifier,
    volumeBalance: VolumeBalance?
) {
    PlateCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = "Training Split",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (volumeBalance != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SplitItem("Push", volumeBalance.push, FlexTheme.colors.plateCobalt)
                SplitItem("Pull", volumeBalance.pull, FlexTheme.colors.plateGreen)
                SplitItem("Legs", volumeBalance.legs, FlexTheme.colors.plateAmber)
                if (volumeBalance.cardio > 0f) {
                    SplitItem("Cardio", volumeBalance.cardio, FlexTheme.colors.plateCrimson)
                }
            }
        } else {
            Text(
                text = "No split data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SplitItem(name: String, percentage: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(percentage * 100).toInt()}%",
                style = MonoMetricSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        CalibratedPlateBar(
            progress = percentage.coerceIn(0f, 1f),
            segments = 6,
            activeColor = color,
            barHeight = 6.dp
        )
    }
}

@Composable
fun AnalysisCard(
    title: String,
    modifier: Modifier = Modifier,
    workoutStats: WorkoutStats? = null,
    durationTrend: List<DailyDurationData> = emptyList(),
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList()
) {
    PlateCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        val avgDuration = workoutStats?.averageDuration ?: 0L
        Text(
            text = if (avgDuration > 0) "$avgDuration min avg" else "No data",
            style = PlateNumeralSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (durationTrend.isNotEmpty()) {
            val maxDuration = durationTrend.maxOfOrNull { it.averageDuration } ?: 1L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                durationTrend.forEach { dayData ->
                    val height = if (maxDuration > 0) {
                        ((dayData.averageDuration.toFloat() / maxDuration) * 56f).coerceAtLeast(4f).dp
                    } else {
                        4.dp
                    }
                    val isPeak = dayData.averageDuration == maxDuration
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(height)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isPeak) MaterialTheme.colorScheme.primary
                                    else FlexTheme.colors.chalkTrack
                                )
                        )
                        Text(
                            text = dayData.dayOfWeek.take(1),
                            style = MonoMetricSmall,
                            color = if (isPeak) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Text(
                text = "No duration data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
