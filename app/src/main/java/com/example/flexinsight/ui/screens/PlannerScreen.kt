package com.example.flexinsight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.ui.theme.*
import com.example.flexinsight.ui.utils.rememberViewOnlyMode
import com.example.flexinsight.ui.viewmodel.PlannerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import java.util.*

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val viewOnlyMode = rememberViewOnlyMode()
    
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
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDarkAlt),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            PlannerHeader(viewOnlyMode = viewOnlyMode)
        }
        item {
            WeeklyGoalCard(weeklyGoalProgress = uiState.weeklyGoalProgress)
        }
        item {
            WeekCalendar(
                weekCalendarData = uiState.weekCalendarData,
                selectedDayIndex = uiState.selectedDayIndex,
                onDaySelected = { viewModel.selectDay(it) }
            )
        }
        item {
            WorkoutListSection(
                selectedDayWorkouts = uiState.selectedDayWorkouts,
                selectedDayName = uiState.weekCalendarData.getOrNull(uiState.selectedDayIndex)?.name ?: "Day"
            )
        }
        item {
            AIInsightsSection(
                volumeBalance = uiState.volumeBalance,
                muscleGroupProgress = uiState.muscleGroupProgress
            )
        }
    }
}

@Composable
fun PlannerHeader(viewOnlyMode: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Planner",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        if (!viewOnlyMode) {
            FloatingActionButton(
                onClick = {},
                modifier = Modifier.size(48.dp),
                containerColor = Primary,
                contentColor = BackgroundDarkAlt
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add workout",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun WeeklyGoalCard(weeklyGoalProgress: com.example.flexinsight.data.model.WeeklyGoalProgress? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8ECE9))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Weekly Goal",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${weeklyGoalProgress?.completed ?: 0}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "/${weeklyGoalProgress?.target ?: 5}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
                if (weeklyGoalProgress != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Primary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = weeklyGoalProgress.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                val progress = if (weeklyGoalProgress != null && weeklyGoalProgress.target > 0) {
                    (weeklyGoalProgress.completed.toFloat() / weeklyGoalProgress.target).coerceIn(0f, 1f)
                } else {
                    0f
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Primary)
                )
            }
            
            val remaining = weeklyGoalProgress?.let { it.target - it.completed } ?: 0
            Text(
                text = if (remaining > 0) {
                    "$remaining workout${if (remaining == 1) "" else "s"} left to hit your goal!"
                } else if (weeklyGoalProgress?.completed ?: 0 >= (weeklyGoalProgress?.target ?: 0)) {
                    "Goal achieved! Great work!"
                } else {
                    "Keep tracking your workouts!"
                },
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun WeekCalendar(
    weekCalendarData: List<com.example.flexinsight.data.model.DayInfo> = emptyList(),
    selectedDayIndex: Int = 0,
    onDaySelected: (Int) -> Unit = {}
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(weekCalendarData.size) { index ->
            val dayData = weekCalendarData[index]
            val dayInfo = DayInfo(
                name = dayData.name,
                date = dayData.date,
                hasWorkout = dayData.hasWorkout,
                isSelected = index == selectedDayIndex,
                isCompleted = dayData.isCompleted
            )
            DayCard(
                day = dayInfo,
                onClick = { onDaySelected(index) }
            )
        }
    }
}

@Composable
fun DayCard(day: DayInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(68.dp)
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (day.isSelected) Primary else Color(0xFFE8ECE9),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (day.isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                radius = 100f
                            )
                        )
                )
            }
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = day.name,
                    fontSize = 12.sp,
                    fontWeight = if (day.isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (day.isSelected) BackgroundDarkAlt else TextSecondary,
                    letterSpacing = if (day.isSelected) 1.sp else 0.sp
                )
                Text(
                    text = day.date.toString(),
                    fontSize = if (day.isSelected) 20.sp else 18.sp,
                    fontWeight = if (day.isSelected) FontWeight.Black else FontWeight.Bold,
                    color = if (day.isSelected) BackgroundDarkAlt else Color.Black
                )
                if (day.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BackgroundDarkAlt,
                        modifier = Modifier.size(12.dp)
                    )
                } else if (day.hasWorkout) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (day.isSelected) BackgroundDarkAlt else Primary)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.Gray, CircleShape)
                    )
                }
            }
        }
    }
}

data class DayInfo(
    val name: String,
    val date: Int,
    val hasWorkout: Boolean = false,
    val isSelected: Boolean = false,
    val isCompleted: Boolean = false
)

/**
 * Format duration in minutes as "X min"
 */
fun formatDuration(minutes: Long?): String {
    if (minutes == null || minutes <= 0) return "0 min"
    return "${minutes} min"
}

@Composable
fun WorkoutListSection(
    selectedDayWorkouts: List<com.example.flexinsight.data.model.PlannedWorkout> = emptyList(),
    selectedDayName: String = "Day"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedDayName}'s Plan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (selectedDayWorkouts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Text(
                        text = "${selectedDayWorkouts.size} Session${if (selectedDayWorkouts.size == 1) "" else "s"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        if (selectedDayWorkouts.isEmpty()) {
            Text(
                text = "No workouts planned for this day",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            selectedDayWorkouts.forEach { workout ->
                val icon = when {
                    workout.intensity?.contains("High", ignoreCase = true) == true -> Icons.Default.FitnessCenter
                    workout.intensity?.contains("Aerobic", ignoreCase = true) == true -> Icons.AutoMirrored.Filled.DirectionsRun
                    else -> Icons.Default.FitnessCenter
                }
                val iconColor = when {
                    workout.intensity?.contains("High", ignoreCase = true) == true -> Color(0xFF10B981)
                    workout.intensity?.contains("Aerobic", ignoreCase = true) == true -> Color(0xFF3B82F6)
                    else -> Primary
                }
                
                WorkoutItem(
                    title = workout.name,
                    duration = formatDuration(workout.duration),
                    intensity = workout.intensity ?: "Medium Intensity",
                    isCompleted = workout.isCompleted,
                    icon = icon,
                    iconColor = iconColor,
                    hasCheckbox = !workout.isCompleted
                )
            }
        }
        
        if (selectedDayWorkouts.isNotEmpty()) {
            Text(
                text = "Long press to reschedule",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun WorkoutItem(
    title: String,
    duration: String,
    intensity: String,
    isCompleted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    hasCheckbox: Boolean = false
) {
    var checked by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8ECE9)),
        border = BorderStroke(1.dp, if (isCompleted) Primary.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isCompleted) Modifier.border(2.dp, Primary, RoundedCornerShape(16.dp))
                    else Modifier
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = iconColor.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color.Black.copy(alpha = 0.6f) else Color.Black,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isCompleted && intensity == "Aerobic") {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF3B82F6).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = intensity,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF3B82F6),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = duration,
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                if (isCompleted) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = Primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = BackgroundDarkAlt,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (hasCheckbox) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Primary,
                            uncheckedColor = Color.Gray
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AIInsightsSection(
    volumeBalance: com.example.flexinsight.data.model.VolumeBalance? = null,
    muscleGroupProgress: List<com.example.flexinsight.data.model.MuscleGroupProgress> = emptyList()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1610)),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary
                    )
                    Text(
                        text = "AI Insights",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RecommendedWorkoutCard(muscleGroupProgress = muscleGroupProgress)
                    VolumeBalanceChart(volumeBalance = volumeBalance)
                }
            }
        }
    }
}

@Composable
fun RecommendedWorkoutCard(
    muscleGroupProgress: List<com.example.flexinsight.data.model.MuscleGroupProgress> = emptyList()
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE8ECE9)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Recommended Next",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "HIGH CONFIDENCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            val recommendation = when {
                muscleGroupProgress.isEmpty() -> {
                    "Active Recovery" to "Keep tracking your workouts to get personalized recommendations."
                }
                muscleGroupProgress.any { it.intensity == "HI" } -> {
                    "Active Recovery Yoga" to "Based on your high intensity load, a lighter session will optimize recovery."
                }
                else -> {
                    val lowest = muscleGroupProgress.minByOrNull { it.volume }
                    if (lowest != null) {
                        val focusArea = lowest.muscleGroup
                        "Focus on $focusArea" to "Your $focusArea volume is lower. Consider adding more exercises targeting this area."
                    } else {
                        "Balanced Training" to "Your volume distribution looks good. Continue with your current routine."
                    }
                }
            }
            
            Text(
                text = recommendation.first,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = recommendation.second,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun VolumeBalanceChart(
    volumeBalance: com.example.flexinsight.data.model.VolumeBalance? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Volume Balance",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val balance = volumeBalance ?: com.example.flexinsight.data.model.VolumeBalance(0.25f, 0.25f, 0.25f, 0.25f)
            val maxValue = maxOf(balance.push, balance.pull, balance.legs, balance.cardio)
            val isLegsHighlighted = balance.legs == maxValue
            
            VolumeBar("Push", balance.push, Color(0xFF60A5FA), modifier = Modifier.weight(1f))
            VolumeBar("Pull", balance.pull, Color(0xFF9333EA), modifier = Modifier.weight(1f))
            VolumeBar("Legs", balance.legs, Primary, isHighlighted = isLegsHighlighted, modifier = Modifier.weight(1f))
            VolumeBar("Cardio", balance.cardio, OrangeAccent, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun VolumeBar(label: String, percentage: Float, color: Color, isHighlighted: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE8ECE9)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(percentage)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
            ) {
                if (isHighlighted) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-24).dp),
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceCardAlt,
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) Primary else TextSecondary,
            letterSpacing = 1.sp
        )
    }
}
