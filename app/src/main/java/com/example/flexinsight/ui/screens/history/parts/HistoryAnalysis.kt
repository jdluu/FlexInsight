package com.example.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.DailyDurationData
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.model.WorkoutStats
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.PrimaryDark
import com.example.flexinsight.ui.theme.SurfaceCardAlt
import com.example.flexinsight.ui.theme.SurfaceHighlight
import com.example.flexinsight.ui.theme.TextSecondary

@Composable
fun AnalysisBreakdown(
    workoutStats: WorkoutStats? = null,
    durationTrend: List<DailyDurationData> = emptyList(),
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    volumeBalance: com.example.flexinsight.data.model.VolumeBalance? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Training Insights",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Training Split (Balance)
            VolumeSplitCard(
                modifier = Modifier.weight(1f),
                volumeBalance = volumeBalance
            )
            
            // Duration Trend
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
    volumeBalance: com.example.flexinsight.data.model.VolumeBalance?
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Training Split",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (volumeBalance != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SplitItem("Push", volumeBalance.push, MaterialTheme.colorScheme.primary)
                    SplitItem("Pull", volumeBalance.pull, MaterialTheme.colorScheme.secondary)
                    SplitItem("Legs", volumeBalance.legs, MaterialTheme.colorScheme.tertiary)
                    if (volumeBalance.cardio > 0) {
                       SplitItem("Cardio", volumeBalance.cardio, MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                 Text(
                    text = "No split data",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(percentage * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (title == "Duration Trend") {
                val avgDuration = workoutStats?.averageDuration ?: 0L
                val durationStr = if (avgDuration > 0) {
                    "${avgDuration} min avg"
                } else {
                    "No data"
                }
                Text(
                    text = durationStr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (durationTrend.isNotEmpty()) {
                    val maxDuration = durationTrend.maxOfOrNull { it.averageDuration } ?: 1L
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        durationTrend.forEachIndexed { index, dayData ->
                            val height = if (maxDuration > 0) {
                                ((dayData.averageDuration.toFloat() / maxDuration) * 64).coerceAtLeast(4f).dp
                            } else {
                                4.dp
                            }
                            val isHighlighted = dayData.averageDuration == maxDuration
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(height)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isHighlighted) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                                Text(
                                    text = dayData.dayOfWeek,
                                    fontSize = 10.sp,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No duration data",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MuscleLegend(name: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
