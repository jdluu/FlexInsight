package com.example.hevyinsight.ui.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hevyinsight.ui.theme.*
import com.example.hevyinsight.ui.utils.rememberViewOnlyMode
import com.example.hevyinsight.ui.utils.rememberUnitPreference
import com.example.hevyinsight.ui.utils.UnitConverter
import com.example.hevyinsight.ui.viewmodel.DashboardViewModel
import com.example.hevyinsight.ui.components.ErrorBanner
import com.example.hevyinsight.ui.components.NetworkStatusIndicator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Format duration in minutes as "Xm" or "Xh Xm"
 */
fun formatDuration(minutes: Long): String {
    if (minutes <= 0) return "0m"
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (remainingMinutes > 0) "${hours}h ${remainingMinutes}m" else "${hours}h"
}

/**
 * Format volume as "X.Xk" for thousands or "X" for regular numbers
 */
fun formatVolume(volume: Double): String {
    if (volume <= 0) return "0"
    if (volume >= 1000) {
        val thousands = volume / 1000.0
        return if (thousands % 1.0 == 0.0) {
            "${thousands.toInt()}k"
        } else {
            String.format("%.1fk", thousands)
        }
    }
    return if (volume % 1.0 == 0.0) {
        volume.toInt().toString()
    } else {
        String.format("%.1f", volume)
    }
}

/**
 * Format number with commas if needed
 */
fun formatNumber(value: Int): String {
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onNavigateToRecovery: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAITrainer: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewOnlyMode = rememberViewOnlyMode()
    val useMetric = rememberUnitPreference()
    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            isRefreshing = false
        }
    }
    
    if (uiState.isLoading && !isRefreshing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }
    
    // Network status indicator - get context and application outside LazyColumn
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as? com.example.hevyinsight.HevyInsightApplication
    val networkStateFlow = remember(application) {
        if (application != null) {
            application.networkMonitor.networkState
        } else {
            kotlinx.coroutines.flow.flowOf(com.example.hevyinsight.core.network.NetworkState.Unknown)
        }
    }
    val networkState by networkStateFlow.collectAsState(initial = com.example.hevyinsight.core.network.NetworkState.Unknown)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark),
            contentPadding = PaddingValues(
                top = if (isRefreshing) 60.dp else 0.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        if (networkState is com.example.hevyinsight.core.network.NetworkState.Unavailable) {
            item {
                NetworkStatusIndicator(
                    networkState = networkState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
        
        // Error banner
        uiState.error?.let { error ->
            item {
                ErrorBanner(
                    error = error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onDismiss = null
                )
            }
        }
        
        item {
            DashboardHeader(
                onNotificationsClick = { onNavigateToSettings() },
                onRefreshClick = { viewModel.sync() }
            )
        }
        item {
            StreakIndicator(streak = uiState.currentStreak)
        }
        item {
            uiState.latestWorkout?.let { workout ->
                FeaturedWorkoutCard(
                    workout = workout,
                    workoutStats = uiState.latestWorkoutStats,
                    useMetric = useMetric,
                    onClick = { onNavigateToWorkoutDetail(workout.id) }
                )
            } ?: run {
                // Show placeholder if no workout
                FeaturedWorkoutCardPlaceholder()
            }
        }
        item {
            WeeklyProgressSection(
                progress = uiState.weeklyProgress,
                muscleGroupProgress = uiState.muscleGroupProgress,
                useMetric = useMetric,
                onSeeAllClick = { onNavigateToHistory() }
            )
        }
        item {
            DailyInsightCard(
                onChatClick = { onNavigateToAITrainer() }
            )
        }
        if (!viewOnlyMode) {
            item {
                QuickActionsGrid(
                    onStartClick = { onNavigateToPlanner() },
                    onLogWeightClick = { /* TODO: Implement weight logging */ },
                    onAddNoteClick = { /* TODO: Implement note adding */ },
                    onAnalyticsClick = { onNavigateToHistory() }
                )
            }
        }
        }
        
        // Pull-to-refresh indicator
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardHeader(
    onNotificationsClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Primary)
                        .border(2.dp, BackgroundDark, CircleShape)
                )
            }
            Column {
                Text(
                    text = dateFormat.format(Date()).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$greeting, Jeffrey",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = TextSecondary
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun StreakIndicator(streak: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth(),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = OrangeAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (streak > 0) "Day $streak" else "No streak",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "streak",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                if (streak > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.Gray.copy(alpha = 0.3f))
                    ) {
                        // Progress bar based on streak (max 7 days for visual)
                        val progress = (streak.coerceAtMost(7) / 7f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(OrangeAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedWorkoutCard(
    workout: com.example.hevyinsight.data.model.Workout,
    workoutStats: com.example.hevyinsight.data.model.SingleWorkoutStats?,
    useMetric: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Background gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Gray.copy(alpha = 0.2f),
                                SurfaceCard
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = workout.name ?: "Workout",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(workout.startTime)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        "Time",
                        workoutStats?.let { formatDuration(it.durationMinutes) } ?: "0m",
                        Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        "Sets",
                        workoutStats?.totalSets?.toString() ?: "0",
                        Icons.Default.FitnessCenter,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        "Vol",
                        workoutStats?.let { UnitConverter.formatVolume(it.totalVolume, useMetric) } ?: "0",
                        Icons.Default.MonitorWeight,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // AI Summary
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundDark.copy(alpha = 0.5f),
                    border = BorderStroke(2.dp, Primary)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "AI SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Great intensity on the bench press today. You hit a PR! Try increasing rest times next session to 90s.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "View Details",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun WeeklyProgressSection(
    progress: List<com.example.hevyinsight.data.model.WeeklyProgress> = emptyList(),
    muscleGroupProgress: List<com.example.hevyinsight.data.model.MuscleGroupProgress> = emptyList(),
    useMetric: Boolean = false,
    onSeeAllClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Volume Trend",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val totalVolume = progress.sumOf { it.totalVolume }
                            Text(
                                text = UnitConverter.formatVolume(totalVolume, useMetric),
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Show last 5 weeks of progress
                        val weeksToShow = progress.takeLast(5)
                        val maxVolume = weeksToShow.maxOfOrNull { it.totalVolume } ?: 1.0
                        
                        repeat(5) { index ->
                            val weekIndex = weeksToShow.size - 5 + index
                            val weekVolume = if (weekIndex >= 0 && weekIndex < weeksToShow.size) {
                                weeksToShow[weekIndex].totalVolume
                            } else {
                                0.0
                            }
                            val height = if (maxVolume > 0) {
                                ((weekVolume / maxVolume) * 32).coerceAtLeast(4.0).dp
                            } else {
                                4.dp
                            }
                            val isCurrent = index == 4 && weekIndex >= 0
                            
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(height)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isCurrent) Primary
                                        else Primary.copy(alpha = 0.2f)
                                    )
                            )
                        }
                    }
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (muscleGroupProgress.isEmpty()) {
                        // Show placeholder if no data
                        Text(
                            text = "No muscle group data available",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val maxVolume = muscleGroupProgress.maxOfOrNull { it.volume } ?: 1.0
                        muscleGroupProgress.forEach { muscleGroup ->
                            val percentage = if (maxVolume > 0) {
                                ((muscleGroup.volume / maxVolume) * 100).toInt()
                            } else {
                                0
                            }
                            val icon = when (muscleGroup.muscleGroup.lowercase()) {
                                "chest" -> Icons.Default.AccessibilityNew
                                "back" -> Icons.Default.GridView
                                "legs" -> Icons.AutoMirrored.Filled.DirectionsWalk
                                "shoulders" -> Icons.Default.FitnessCenter
                                "arms" -> Icons.Default.FitnessCenter
                                "core" -> Icons.Default.FitnessCenter
                                else -> Icons.Default.FitnessCenter
                            }
                            MuscleProgressItem(
                                muscle = muscleGroup.muscleGroup,
                                percentage = percentage,
                                intensity = muscleGroup.intensity,
                                icon = icon
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MuscleProgressItem(muscle: String, percentage: Int, intensity: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = muscle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "$percentage%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percentage / 100f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (percentage >= 70) Primary
                            else if (percentage >= 40) Primary.copy(alpha = 0.7f)
                            else Primary.copy(alpha = 0.4f)
                        )
                )
            }
        }
        
        Text(
            text = intensity,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
    }
}

@Composable
fun DailyInsightCard(
    onChatClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = 200f
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF9333EA)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(
                    modifier = androidx.compose.ui.Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Daily Insight",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Did you know? Creatine absorption is optimized post-workout when combined with a source of carbohydrates.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                    TextButton(
                        onClick = onChatClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Chat with Trainer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsGrid(
    onStartClick: () -> Unit = {},
    onLogWeightClick: () -> Unit = {},
    onAddNoteClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Quick Actions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton("Start", Icons.Default.PlayArrow, Primary, onClick = onStartClick, modifier = Modifier.weight(1f))
            QuickActionButton("Log Weight", Icons.Default.MonitorWeight, Color.White, onClick = onLogWeightClick, modifier = Modifier.weight(1f))
            QuickActionButton("Add Note", Icons.Default.EditNote, Color.White, onClick = onAddNoteClick, modifier = Modifier.weight(1f))
            QuickActionButton("Analytics", Icons.Default.Analytics, Color.White, onClick = onAnalyticsClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceCard,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary
        )
    }
}
@Composable
fun FeaturedWorkoutCardPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No workouts yet. Start your first workout!",
                color = TextSecondary,
                fontSize = 16.sp
            )
        }
    }
}
