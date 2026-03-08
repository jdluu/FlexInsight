package com.example.flexinsight.ui.screens.planner.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.flexinsight.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexinsight.data.model.MuscleGroupProgress
import com.example.flexinsight.data.model.VolumeBalance
import com.example.flexinsight.ui.theme.BackgroundDark
import com.example.flexinsight.ui.theme.OrangeAccent
import com.example.flexinsight.ui.theme.Primary
import com.example.flexinsight.ui.theme.SurfaceCardAlt
import com.example.flexinsight.ui.theme.TextSecondary

@Composable
fun AIInsightsSection(
    volumeBalance: VolumeBalance? = null,
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    onGeneratePlan: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
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
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.ai_insights_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RecommendedWorkoutCard(
                        muscleGroupProgress = muscleGroupProgress,
                        onGeneratePlan = onGeneratePlan
                    )
                    VolumeBalanceChart(volumeBalance = volumeBalance)
                }
            }
        }
    }
}

@Composable
fun RecommendedWorkoutCard(
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    onGeneratePlan: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
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
                    text = stringResource(id = R.string.planner_insights_recommended_next),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = stringResource(id = R.string.planner_insights_high_confidence),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            val recommendation = when {
                muscleGroupProgress.isEmpty() -> {
                    stringResource(id = R.string.planner_insights_active_recovery_title) to stringResource(id = R.string.planner_insights_active_recovery_desc)
                }
                muscleGroupProgress.any { it.intensity == "HI" } -> {
                    stringResource(id = R.string.planner_insights_recovery_yoga_title) to stringResource(id = R.string.planner_insights_recovery_yoga_desc)
                }
                else -> {
                    val lowest = muscleGroupProgress.minByOrNull { it.volume }
                    if (lowest != null) {
                        val focusArea = lowest.muscleGroup
                        stringResource(id = R.string.planner_insights_focus_area_title, focusArea) to stringResource(id = R.string.planner_insights_focus_area_desc, focusArea)
                    } else {
                        stringResource(id = R.string.planner_insights_balanced_title) to stringResource(id = R.string.planner_insights_balanced_desc)
                    }
                }
            }

            Text(
                text = recommendation.first,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = recommendation.second,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Button(
                onClick = onGeneratePlan,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.action_generate_optimized_plan), color = MaterialTheme.colorScheme.onPrimary)
            }

            Text(
                text = stringResource(id = R.string.planner_insights_generate_plan_desc),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                lineHeight = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun VolumeBalanceChart(
    volumeBalance: VolumeBalance? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.planner_insights_volume_balance),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val balance = volumeBalance ?: VolumeBalance(0.25f, 0.25f, 0.25f, 0.25f)
            val maxValue = maxOf(balance.push, balance.pull, balance.legs, balance.cardio)
            val isLegsHighlighted = balance.legs == maxValue

            VolumeBar(stringResource(id = R.string.muscle_group_push), balance.push, Color(0xFF60A5FA), modifier = Modifier.weight(1f))
            VolumeBar(stringResource(id = R.string.muscle_group_pull), balance.pull, Color(0xFF9333EA), modifier = Modifier.weight(1f))
            VolumeBar(stringResource(id = R.string.muscle_group_legs), balance.legs, MaterialTheme.colorScheme.primary, isHighlighted = isLegsHighlighted, modifier = Modifier.weight(1f))
            VolumeBar(stringResource(id = R.string.muscle_group_cardio), balance.cardio, MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
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
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
