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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.R
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.model.VolumeTrend
import com.example.flexinsight.data.model.WeeklyVolumeData
import com.example.flexinsight.data.model.WorkoutStats
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.SurfaceCardAlt
import com.example.flexinsight.ui.theme.TextSecondary
import com.example.flexinsight.ui.utils.UnitConverter

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
        StatCard("$workoutCount", stringResource(id = R.string.history_comparison_workouts), modifier = Modifier.weight(1f))
        StatCard(avgVolumeFormatted, stringResource(id = R.string.history_avg_vol_label, unitLabel), modifier = Modifier.weight(1f))
        StatCard(bestWeek, stringResource(id = R.string.history_best_week), modifier = Modifier.weight(1f), isHighlighted = true)
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier, isHighlighted: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun TotalVolumeCard(
    workoutStats: WorkoutStats? = null,
    volumeTrend: VolumeTrend? = null,
    weeklyVolumeData: List<WeeklyVolumeData> = emptyList(),
    consistencyData: List<com.example.flexinsight.data.model.DayInfo> = emptyList(),
    useMetric: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.history_comparison_total_volume),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = workoutStats?.let { formatVolumeWithCommas(UnitConverter.convertVolume(it.totalVolume, useMetric)) } ?: "0",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = UnitConverter.getWeightUnit(useMetric),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = workoutStats?.let { stringResource(id = R.string.history_across_workouts, it.totalWorkouts) } ?: stringResource(id = R.string.history_no_workouts_yet),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                if (volumeTrend != null && volumeTrend.percentageChange != 0.0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formatPercentageChange(volumeTrend.percentageChange),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Consistency Heatmap (Last 3 Months)
            if (consistencyData.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.history_consistency_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ConsistencyHeatmap(data = consistencyData)
                }
            }
        }
    }
}

@Composable
fun ConsistencyHeatmap(data: List<com.example.flexinsight.data.model.DayInfo>) {
    val columns = 13
    val rows = 7
    val totalDays = columns * rows
    val relevantData = data.takeLast(totalDays)
    
    if (relevantData.isEmpty()) return

    val lastItem = relevantData.last()
    val lastDayOfWeek = java.time.Instant.ofEpochMilli(lastItem.timestamp)
             .atZone(java.time.ZoneId.systemDefault())
             .dayOfWeek.value // 1(Mon)..7(Sun)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val months = remember(relevantData) {
                relevantData.map { 
                     java.time.Instant.ofEpochMilli(it.timestamp)
                         .atZone(java.time.ZoneId.systemDefault())
                         .month.name.take(3)
                }.distinct()
            }
            
            months.take(3).forEach { monthName ->
                Text(
                    text = monthName,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
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
                Text(stringResource(id = R.string.day_mon), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(id = R.string.day_thu), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(id = R.string.day_sun), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val grid = Array(columns) { Array<com.example.flexinsight.data.model.DayInfo?>(rows) { null } }
                
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
                            val color = if (dayInfo?.hasWorkout == true) {
                                MaterialTheme.colorScheme.primary
                            } else if (dayInfo != null) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color.Transparent
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.history_ai_insights_header),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = stringResource(id = R.string.history_ai_insights_desc),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
                Button(
                    onClick = onAnalyzeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.action_view_analysis),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
