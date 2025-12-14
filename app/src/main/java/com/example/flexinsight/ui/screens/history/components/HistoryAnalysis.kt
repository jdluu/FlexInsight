package com.example.flexinsight.ui.screens.history.components

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
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Analysis Breakdown",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnalysisCard(
                title = "Volume by Muscle",
                modifier = Modifier.weight(1f),
                workoutStats = workoutStats,
                durationTrend = durationTrend,
                muscleGroupProgress = muscleGroupProgress
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
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt)
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
                color = Color.White
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
                    color = Color.White
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
                                            if (isHighlighted) Primary
                                            else SurfaceHighlight
                                        )
                                )
                                Text(
                                    text = dayData.dayOfWeek,
                                    fontSize = 10.sp,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHighlighted) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No duration data",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            } else {
                // Volume by Muscle - simplified representation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (muscleGroupProgress.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val colors = listOf(Primary, PrimaryDark, Color(0xFF065F24))
                                muscleGroupProgress.take(3).forEachIndexed { index, muscle ->
                                    MuscleLegend(muscle.muscleGroup, colors.getOrElse(index) { Primary })
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No muscle group data",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
