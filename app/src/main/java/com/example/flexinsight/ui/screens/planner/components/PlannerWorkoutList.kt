package com.example.flexinsight.ui.screens.planner.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.PlannedWorkout
import com.example.flexinsight.ui.theme.BackgroundDarkAlt
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.TextSecondary

@Composable
fun WorkoutListSection(
    selectedDayWorkouts: List<PlannedWorkout> = emptyList(),
    selectedDayName: String = "Day",
    onWorkoutComplete: (String, Boolean) -> Unit = { _, _ -> },
    onReschedule: (PlannedWorkout) -> Unit = {}
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
                    hasCheckbox = !workout.isCompleted,
                    onCheckedChange = { isChecked -> 
                        workout.id?.let { id -> onWorkoutComplete(id, isChecked) }
                    },
                    onLongClick = { onReschedule(workout) }
                )
            }
        }
        
        if (selectedDayWorkouts.isNotEmpty()) {
            Text(
                text = "Long press to reschedule",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutItem(
    title: String,
    duration: String,
    intensity: String,
    isCompleted: Boolean,
    icon: ImageVector,
    iconColor: Color,
    hasCheckbox: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {}, // No-op, just for ripple
                onLongClick = onLongClick
            ),
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
                        checked = isCompleted,
                        onCheckedChange = onCheckedChange,
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
