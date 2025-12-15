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
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (streak > 0) "Day $streak" else "No streak",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "streak",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (streak > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        // Progress bar based on streak (max 7 days for visual)
                        val progress = (streak.coerceAtMost(7) / 7f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.tertiary)
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
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = UnitConverter.getWeightUnit(useMetric),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val totalVolume = progress.sumOf { it.totalVolume }
                        muscleGroupProgress.forEach { muscleGroup ->
                            // Calculate percentage of total volume
                            val percentage = if (totalVolume > 0) {
                                ((muscleGroup.volume / totalVolume) * 100).toInt()
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$percentage%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            // Determine relative width for visual emphasis (relative to the largest group shown)
                            // or just use the raw percentage?
                            // User request implies "accurate percentages".
                            // If we use raw percentage (e.g. 30%), the bar will be 30% width.
                            // Previously it was full width (100%) for the top one.
                            // Let's stick to raw percentage for accuracy, but scale it slightly if they are all small?
                            // Actually, let's keep it simple: raw percentage width.
                            .fillMaxWidth(percentage / 100f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                // Color coding based on intensity instead of arbitrary percentage thresholds
                                when(intensity) {
                                    "HI" -> MaterialTheme.colorScheme.primary
                                    "MD" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                }
                            )
                    )
            }
        }

        // Intensity label removed per user request
//        Text(
//            text = intensity,
//            fontSize = 12.sp,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
    }
}
