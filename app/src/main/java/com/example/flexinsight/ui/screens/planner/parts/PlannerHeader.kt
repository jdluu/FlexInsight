package com.example.flexinsight.ui.screens.planner.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.WeeklyGoalProgress
import com.example.flexinsight.ui.theme.BackgroundDarkAlt
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.TextSecondary

@Composable
fun PlannerHeader(
    viewOnlyMode: Boolean = false,
    onAddWorkout: () -> Unit = {}
) {
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
                onClick = onAddWorkout,
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
fun WeeklyGoalCard(weeklyGoalProgress: WeeklyGoalProgress? = null) {
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
