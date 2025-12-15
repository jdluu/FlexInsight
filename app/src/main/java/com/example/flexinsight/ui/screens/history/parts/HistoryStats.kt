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
    // We want to render a grid: 7 rows (days), ~13 columns (weeks)
    // Plus labels: Month names on top, Day names on left.
    
    val columns = 13
    val rows = 7
    val totalDays = columns * rows
    val relevantData = data.takeLast(totalDays)
    
    // Day Labels (Mon, Wed, Fri) used by GitHub, we can use Mon, Wed, Fri or just M, W, F
    // The data loop in repository matches Mon(0)..Sun(6) if aligned?
    // Actually repository generates N days ending today.
    // So the last column ends on "Today".
    // Does that mean the rows align to Mon-Sun? No.
    // If the data is just a flat list of last 90 days, we need to be careful.
    // Ideally, for a calendar heatmap, rows SHOULD be fixed Day of Week (Mon, Tue...)
    
    // Let's verify alignment. 
    // If we fill columns from top to bottom, then left to right?
    // GitHub fills Column 1 (Mon-Sun), Column 2 (Mon-Sun).
    // Our data is a linear time series.
    // We need to determine the DayOfWeek of the FIRST data point to know where to start in the first column.
    
    if (relevantData.isEmpty()) return

    // Find the offset of the first item
    // DayInfo names are "Mon", "Tue" etc.
    // Let's rely on that.
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Month Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, bottom = 4.dp), // indent for day labels
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Simplified: Just show Start, Middle, End month? 
            // Or try to place them accurately.
            // Let's just place 3 labels for now spread out.
            val months = relevantData.map { 
                 java.time.Instant.ofEpochMilli(it.timestamp)
                     .atZone(java.time.ZoneId.systemDefault())
                     .month.name.take(3)
            }.distinct()
            
            // Show up to 3 distinct months
            months.take(3).forEach { monthName ->
                Text(
                    text = monthName,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth()) {
            // Day Labels Column
            Column(
                modifier = Modifier.padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp) // matches box spacing?
            ) {
                 // GitHub shows Mon, Wed, Fri. 
                 // We will show Mon, Wed, Fri, Sun to be helpful.
                 // We need to match the height of the boxes (10.dp) + spacing (4.dp)
                 // This is tricky with Text vs Box sizing.
                 
                 // Let's just show labels for specific rows: 1 (Mon), 3 (Wed), 5 (Fri)
                 // Assuming Row 0 is Mon?
                 // We need to align the data first so Row 0 IS Monday.
            }
            // Actually, calculating alignment is hard without data inspection.
            // Let's do a Grid Layout where we explicitly place items by (col, row).
            
            // Let's try a simpler robust layout:
            // Just columns of 7 dots.
            // But we need to shift the first column based on start day?
            // If today is Friday, the last dot is Friday (Row 4).
            // That means the current week column is partially filled up to Friday.
            
            // It's easier to verify:
            // Last item in `relevantData` is TODAY.
            // Let's verify day of week of last item.
            val lastItem = relevantData.last()
            val lastDayOfWeek = java.time.Instant.ofEpochMilli(lastItem.timestamp)
                     .atZone(java.time.ZoneId.systemDefault())
                     .dayOfWeek.value // 1(Mon)..7(Sun)
            
            // If last item is Fri (5), it should be at Row 4 (0-indexed).
            // So we fill backwards?
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Day Labels (Left)
                Column(
                   verticalArrangement = Arrangement.SpaceBetween,
                   modifier = Modifier.height((10 * 7 + 4 * 6).dp) // 7 boxes + 6 gaps
                ) {
                    Text("Mon", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Text("Wed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Thu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Sun", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // The Grid
                // We need to construct a grid 7 rows x N columns.
                // We map date -> (row, col)
                
                // Let's reconstruct the data into a grid.
                // We want ~13 columns.
                // The last column should contain Today.
                // Today is at row (headerDayOfWeek - 1).
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // We need to pad the START of the data so that the first item is a Monday?
                    // OR just fill the grid cells.
                    
                    // Simple logic: 
                    // Create a grid of empty cells (7 rows x 13 cols).
                    // Populate from bottom-right (Today) moving backwards.
                    
                    val grid = Array(columns) { Array<com.example.flexinsight.data.model.DayInfo?>(rows) { null } }
                    
                    // Fill backwards
                    var dataIdx = relevantData.lastIndex
                    var currentCol = columns - 1
                    // Today's Row
                    var currentRow = lastDayOfWeek - 1 // 0=Mon, 6=Sun
                    
                    while (dataIdx >= 0 && currentCol >= 0) {
                        grid[currentCol][currentRow] = relevantData[dataIdx]
                        dataIdx--
                        currentRow--
                        if (currentRow < 0) {
                            currentRow = 6
                            currentCol--
                        }
                    }
                    
                    // Render Grid
                    for (c in 0 until columns) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (r in 0 until rows) {
                                val dayInfo = grid[c][r]
                                val color = if (dayInfo?.hasWorkout == true) {
                                    MaterialTheme.colorScheme.primary
                                } else if (dayInfo != null) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    Color.Transparent // No data (future or pre-history padding)
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
