package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.utils.rememberUnitPreference
import com.example.flexinsight.ui.utils.UnitConverter
import com.example.flexinsight.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Format volume with commas (e.g., "124,500")
 */
fun formatVolumeWithCommas(volume: Double): String {
    val volumeInt = volume.toInt()
    return volumeInt.toString().reversed().chunked(3).joinToString(",").reversed()
}

/**
 * Format percentage change with sign (e.g., "+12%", "-5%")
 */
fun formatPercentageChange(change: Double): String {
    val sign = if (change >= 0) "+" else ""
    return "$sign${change.toInt()}%"
}

/**
 * Format date as "MMM d" (e.g., "Oct 14")
 */
fun formatDateShort(timestamp: Long): String {
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToWorkoutDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val useMetric = rememberUnitPreference()
    
    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDarkAlt),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }
    
    if (uiState.error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDarkAlt)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = uiState.error?.message ?: "Unknown error",
                    color = RedAccent,
                    fontSize = 16.sp
                )
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Retry", color = BackgroundDark)
                }
            }
        }
        return
    }
    var selectedTab by remember { mutableStateOf(0) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDarkAlt),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            HistoryHeader()
        }
        item {
            TabSelector(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
        item {
            AIInsightsCard(
                volumeTrend = uiState.volumeTrend,
                muscleGroupProgress = uiState.muscleGroupProgress,
                useMetric = useMetric
            )
        }
        item {
            StatsGrid(
                workoutCount = uiState.workoutCount,
                avgVolume = uiState.workoutStats?.averageVolume?.toInt() ?: 0,
                bestWeek = uiState.workoutStats?.bestWeekDate?.let { 
                    java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(it))
                } ?: "N/A",
                useMetric = useMetric
            )
        }
        item {
            TotalVolumeCard(
                workoutStats = uiState.workoutStats,
                volumeTrend = uiState.volumeTrend,
                weeklyVolumeData = uiState.weeklyVolumeData,
                useMetric = useMetric
            )
        }
        item {
            AnalysisBreakdown(
                workoutStats = uiState.workoutStats,
                durationTrend = uiState.durationTrend,
                muscleGroupProgress = uiState.muscleGroupProgress
            )
        }
        item {
            RecentPRsSection(
                prsWithDetails = uiState.prsWithDetails,
                onNavigateToWorkoutDetail = onNavigateToWorkoutDetail
            )
        }
    }
}

@Composable
fun HistoryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Workout History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filter",
                tint = Primary
            )
        }
    }
}

@Composable
fun TabSelector(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf("Summary", "Exercises", "Compare")
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardAlt
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedTab == index) Primary else Color.Transparent,
                    onClick = { onTabSelected(index) }
                ) {
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == index) BackgroundDarkAlt else TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AIInsightsCard(
    volumeTrend: com.example.flexinsight.data.model.VolumeTrend? = null,
    muscleGroupProgress: List<com.example.flexinsight.data.model.MuscleGroupProgress> = emptyList(),
    useMetric: Boolean = false
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
                    text = when {
                        volumeTrend != null && volumeTrend.percentageChange > 0 -> {
                            "You've increased your total volume by ${volumeTrend.percentageChange.toInt()}% this month! Keep pushing on compound movements."
                        }
                        muscleGroupProgress.isNotEmpty() -> {
                            val topMuscle = muscleGroupProgress.first()
                            val volumeStr = UnitConverter.formatVolume(topMuscle.volume, useMetric)
                            val unit = UnitConverter.getWeightUnit(useMetric)
                            "Your ${topMuscle.muscleGroup} volume is $volumeStr $unit. Focus on balanced training across all muscle groups."
                        }
                        else -> {
                            "Keep tracking your workouts to see insights and progress over time!"
                        }
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    lineHeight = 22.sp
                )
                Button(
                    onClick = {},
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
    workoutStats: com.example.flexinsight.data.model.WorkoutStats? = null,
    volumeTrend: com.example.flexinsight.data.model.VolumeTrend? = null,
    weeklyVolumeData: List<com.example.flexinsight.data.model.WeeklyVolumeData> = emptyList(),
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
                                text = workoutStats?.let { UnitConverter.formatVolumeWithCommas(it.totalVolume, useMetric) } ?: "0",
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
                } else {
                    Text(
                        text = "No volume data available",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyVolumeData.take(4).forEach { weekData ->
                    Text(
                        text = weekData.weekLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
                // Fill remaining slots if less than 4 weeks
                repeat(4 - weeklyVolumeData.size.coerceAtMost(4)) {
                    Text(
                        text = "",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisBreakdown(
    workoutStats: com.example.flexinsight.data.model.WorkoutStats? = null,
    durationTrend: List<com.example.flexinsight.data.model.DailyDurationData> = emptyList(),
    muscleGroupProgress: List<com.example.flexinsight.data.model.MuscleGroupProgress> = emptyList()
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
    workoutStats: com.example.flexinsight.data.model.WorkoutStats? = null,
    durationTrend: List<com.example.flexinsight.data.model.DailyDurationData> = emptyList(),
    muscleGroupProgress: List<com.example.flexinsight.data.model.MuscleGroupProgress> = emptyList()
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

@Composable
fun RecentPRsSection(
    prsWithDetails: List<com.example.flexinsight.data.model.PRDetails> = emptyList(),
    onNavigateToWorkoutDetail: (String) -> Unit = {}
) {
    val useMetric = rememberUnitPreference()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent PRs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = {}) {
                Text(
                    text = "View All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Primary
                )
            }
        }
        
        if (prsWithDetails.isEmpty()) {
            Text(
                text = "No personal records yet. Keep training to set PRs!",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Track which exercises have been seen to mark newest PR per exercise
                val exercisePRs = prsWithDetails.groupBy { it.exerciseName }
                val newestPRPerExercise = exercisePRs.mapValues { (_, prs) -> prs.maxByOrNull { it.date } }
                
                prsWithDetails.forEach { prDetails ->
                    val isNewPR = newestPRPerExercise[prDetails.exerciseName]?.setId == prDetails.setId
                    val convertedWeight = UnitConverter.convertWeight(prDetails.weight, useMetric)
                    PRCard(
                        exercise = prDetails.exerciseName,
                        date = formatDateShort(prDetails.date),
                        muscle = prDetails.muscleGroup,
                        weight = convertedWeight?.toInt()?.toString() ?: "-",
                        unit = UnitConverter.getWeightUnit(useMetric),
                        isNewPR = isNewPR,
                        onClick = { onNavigateToWorkoutDetail(prDetails.workoutId) }
                    )
                }
            }
        }
    }
}

@Composable
fun PRCard(
    exercise: String,
    date: String,
    muscle: String,
    weight: String,
    unit: String,
    isNewPR: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardAlt)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = SurfaceHighlight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (muscle == "Chest") Icons.Default.FitnessCenter else Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = null,
                        tint = if (isNewPR) Primary else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$date • $muscle",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            
            // Mini chart placeholder
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            )
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = weight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    )
                }
                if (isNewPR) {
                    Text(
                        text = "NEW 1RM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                } else {
                    Text(
                        text = "Vol: 4.2k",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
