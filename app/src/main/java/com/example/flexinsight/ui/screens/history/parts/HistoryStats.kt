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
import com.example.flexinsight.ui.screens.history.parts.formatVolumeWithCommas
import com.example.flexinsight.ui.screens.history.parts.formatPercentageChange

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
        color = MaterialTheme.colorScheme.secondaryContainer
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
                        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "k",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = value,
                    fontSize = if (isHighlighted) 20.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = UnitConverter.getWeightUnit(useMetric),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = workoutStats?.let { "Across ${it.totalWorkouts} workouts" } ?: "No workouts yet",
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
                        text = "Consistency (Last 3 Months)",
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
    // Assuming data contains ~90 days covering the last ~13 weeks
    // We want to render a grid of squares: 7 rows (days), ~13 columns (weeks)
    // The data likely comes as a flat list. We need to organize it.
    // The data is sorted by date ascending? Let's check repository implementation.
    // Repository getConsistencyData loops 0..days and adds to resultDays.
    // So data is sorted ascending: oldest -> newest.

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween 
    ) {
        // We can use a LazyRow for the columns or just calculate everything if fixed.
        // Let's use a fixed grid for stability.
        val columns = 13 // approx 3 months (90 days / 7 = 12.8)
        
        // Take the last 7 * columns days to ensure full columns
        val totalDays = columns * 7
        val relevantData = data.takeLast(totalDays)
        
        // Split into columns (weeks)
        for (col in 0 until columns) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0 until 7) {
                    // Index calculation:
                    // Data is oldest to newest.
                    // We want to fill columns left to right? or rows?
                    // Standard is columns = weeks. Rows = days (Mon-Sun).
                    
                    // index = col * 7 + row
                    // But we need to make sure the start day aligns with Monday?
                    // Repository logic loops 0..days. The start date is arbitrary relative to Mon.
                    // This creates a misalignment if we blindly map to a grid.
                    // Ideally the heatmap should show dates correctly aligned.
                    
                    // Simpler approach for "visual consistency":
                    // Just show the last 91 days as a continuous stream wrapped into columns,
                    // without strict alignment to "Monday" on row 0, unless we want to be fancy.
                    // Github does align Monday to row 1.
                    
                    // Let's stick to a simpler "Activity Grid" where each column is a chunk of 7 days,
                    // regardless of actual day of week, for purely showing frequency.
                    // OR, better: Align to bottom right (today).
                    
                    val index = relevantData.size - 1 - ((columns - 1 - col) * 7 + (6 - row))
                    
                    val dayInfo = relevantData.getOrNull(index)
                    val color = if (dayInfo?.hasWorkout == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
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
                        text = "AI INSIGHTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Complete 10+ workouts to unlock AI-powered insights about your training patterns, muscle balance, and recovery recommendations.",
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
                        text = "View Analysis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
