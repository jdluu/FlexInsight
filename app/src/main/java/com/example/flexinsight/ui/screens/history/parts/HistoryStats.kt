package com.example.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.model.VolumeTrend
import com.example.flexinsight.data.model.WeeklyVolumeData
import com.example.flexinsight.data.model.WorkoutStats
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.SurfaceCardAlt
import com.example.flexinsight.ui.theme.TextSecondary
import com.example.flexinsight.ui.utils.UnitConverter
import com.example.flexinsight.ui.screens.history.formatVolumeWithCommas
import com.example.flexinsight.ui.screens.history.formatPercentageChange

@Composable
fun StatsGrid(
    workoutCount: Int = 0,
    avgVolume: Int = 0,
    bestWeek: String = "N/A",
    useMetric: Boolean = false
) {
    val avgVolumeConverted = UnitConverter.convertVolume(avgVolume.toDouble(), useMetric)
    val avgVolumeFormatted = if (avgVolumeConverted >= 1000) {
        "${(avgVolumeConverted / 1000).toInt()}k"
    } else {
        avgVolumeConverted.toInt().toString()
    }
    val unitLabel = UnitConverter.getWeightUnit(useMetric)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("$workoutCount", "Workouts", modifier = Modifier.weight(1f))
        StatCard(avgVolumeFormatted, "Avg Vol ($unitLabel)", modifier = Modifier.weight(1f))
        StatCard(bestWeek, "Best Week", modifier = Modifier.weight(1f), isHighlighted = true)
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier, isHighlighted: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardAlt
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (value.contains("k")) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value.substringBefore("k"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) Primary else Color.White
                    )
                    Text(
                        text = "k",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) Primary else TextSecondary
                    )
                }
            } else {
                Text(
                    text = value,
                    fontSize = if (isHighlighted) 20.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) Primary else Color.White
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun TotalVolumeCard(
    workoutStats: WorkoutStats? = null,
    volumeTrend: VolumeTrend? = null,
    weeklyVolumeData: List<WeeklyVolumeData> = emptyList(),
    useMetric: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Total Volume",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = workoutStats?.let { formatVolumeWithCommas(UnitConverter.convertVolume(it.totalVolume, useMetric)) } ?: "0",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = UnitConverter.getWeightUnit(useMetric),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary
                        )
                    }
                }
                if (volumeTrend != null && volumeTrend.percentageChange != 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formatPercentageChange(volumeTrend.percentageChange),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }
            
            // Weekly volume chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                if (weeklyVolumeData.isNotEmpty()) {
                    val maxVolume = weeklyVolumeData.maxOfOrNull { it.volume } ?: 1.0
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyVolumeData.take(4).forEach { weekData ->
                            val height = if (maxVolume > 0) {
                                ((weekData.volume / maxVolume) * 128).coerceAtLeast(4.0).dp
                            } else {
                                4.dp
                            }
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(height)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Primary)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 200f
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI INSIGHTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "AI Insights coming soon! We're building tools to help you analyze your training patterns.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    lineHeight = 22.sp
                )
                Button(
                    onClick = onAnalyzeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary.copy(alpha = 0.1f),
                        contentColor = Primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = "View Analysis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
