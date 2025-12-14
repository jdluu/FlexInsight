package com.example.flexinsight.ui.screens.dashboard.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.model.WeeklyProgress
import com.example.flexinsight.ui.theme.OrangeAccent
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.SurfaceCard
import com.example.flexinsight.ui.theme.TextSecondary
import com.example.flexinsight.ui.utils.UnitConverter

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
fun WeeklyProgressSection(
    progress: List<WeeklyProgress> = emptyList(),
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
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
                                text = formatVolume(totalVolume),
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
fun MuscleProgressItem(muscle: String, percentage: Int, intensity: String, icon: ImageVector) {
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
