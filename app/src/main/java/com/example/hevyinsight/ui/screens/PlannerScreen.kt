package com.example.hevyinsight.ui.screens

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
import com.example.hevyinsight.ui.theme.*
import java.util.*

@Composable
fun PlannerScreen() {
    var selectedDay by remember { mutableStateOf(1) } // Tuesday
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDarkAlt),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            PlannerHeader()
        }
        item {
            WeeklyGoalCard()
        }
        item {
            WeekCalendar(selectedDay = selectedDay, onDaySelected = { selectedDay = it })
        }
        item {
            WorkoutListSection(selectedDay = selectedDay)
        }
        item {
            AIInsightsSection()
        }
    }
}

@Composable
fun PlannerHeader() {
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

@Composable
fun WeeklyGoalCard() {
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
                            text = "3",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "/5",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "On Track",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Primary)
                )
            }
            
            Text(
                text = "2 workouts left to hit your streak!",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun WeekCalendar(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    val days = listOf(
        DayInfo("Mon", 11, hasWorkout = true),
        DayInfo("Tue", 12, hasWorkout = true, isSelected = selectedDay == 1, isCompleted = true),
        DayInfo("Wed", 13, hasWorkout = true),
        DayInfo("Thu", 14, hasWorkout = false),
        DayInfo("Fri", 15, hasWorkout = true),
        DayInfo("Sat", 16, hasWorkout = false),
        DayInfo("Sun", 17, hasWorkout = false)
    )
    
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(days.size) { index ->
            val day = days[index]
            DayCard(
                day = day,
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

@Composable
fun WorkoutListSection(selectedDay: Int) {
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
                text = "Tuesday's Plan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Text(
                    text = "2 Sessions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        
        WorkoutItem(
            title = "Upper Body Power",
            duration = "45 min",
            intensity = "High Intensity",
            isCompleted = true,
            icon = Icons.Default.FitnessCenter,
            iconColor = Color(0xFF10B981)
        )
        
        WorkoutItem(
            title = "Zone 2 Cardio",
            duration = "30 min",
            intensity = "Aerobic",
            isCompleted = false,
            icon = Icons.Default.DirectionsRun,
            iconColor = Color(0xFF3B82F6),
            hasCheckbox = true
        )
        
        Text(
            text = "Long press to reschedule",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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
fun AIInsightsSection() {
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
                    RecommendedWorkoutCard()
                    VolumeBalanceChart()
                }
            }
        }
    }
}

@Composable
fun RecommendedWorkoutCard() {
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
            Text(
                text = "Active Recovery Yoga",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = "Based on your high intensity load yesterday, a lighter session will optimize recovery.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun VolumeBalanceChart() {
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
            VolumeBar("Push", 0.45f, Color(0xFF60A5FA), modifier = Modifier.weight(1f))
            VolumeBar("Pull", 0.30f, Color(0xFF9333EA), modifier = Modifier.weight(1f))
            VolumeBar("Legs", 0.85f, Primary, isHighlighted = true, modifier = Modifier.weight(1f))
            VolumeBar("Cardio", 0.60f, OrangeAccent, modifier = Modifier.weight(1f))
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
