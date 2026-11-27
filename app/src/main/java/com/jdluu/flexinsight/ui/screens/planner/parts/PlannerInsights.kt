package com.jdluu.flexinsight.ui.screens.planner.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.data.model.MuscleGroupProgress
import com.jdluu.flexinsight.data.model.VolumeBalance
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateSectionHeader
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall

@Composable
fun AIInsightsSection(
    volumeBalance: VolumeBalance? = null,
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    onGeneratePlan: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlateSectionHeader(title = stringResource(id = R.string.ai_insights_title))

        RecommendedWorkoutCard(
            muscleGroupProgress = muscleGroupProgress,
            onGeneratePlan = onGeneratePlan
        )

        VolumeBalanceChart(volumeBalance = volumeBalance)
    }
}

@Composable
fun RecommendedWorkoutCard(
    muscleGroupProgress: List<MuscleGroupProgress> = emptyList(),
    onGeneratePlan: () -> Unit = {}
) {
    val recommendation = when {
        muscleGroupProgress.isEmpty() -> {
            stringResource(id = R.string.planner_insights_active_recovery_title) to
                stringResource(id = R.string.planner_insights_active_recovery_desc)
        }
        muscleGroupProgress.any { it.intensity == "HI" } -> {
            stringResource(id = R.string.planner_insights_recovery_yoga_title) to
                stringResource(id = R.string.planner_insights_recovery_yoga_desc)
        }
        else -> {
            val lowest = muscleGroupProgress.minByOrNull { it.volume }
            if (lowest != null) {
                val focusArea = lowest.muscleGroup
                stringResource(id = R.string.planner_insights_focus_area_title, focusArea) to
                    stringResource(id = R.string.planner_insights_focus_area_desc, focusArea)
            } else {
                stringResource(id = R.string.planner_insights_balanced_title) to
                    stringResource(id = R.string.planner_insights_balanced_desc)
            }
        }
    }

    PlateCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.planner_insights_recommended_next),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                PlateStatusPill(
                    label = stringResource(id = R.string.planner_insights_high_confidence),
                    icon = Icons.Default.AutoAwesome,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = recommendation.first,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = recommendation.second,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onGeneratePlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.action_generate_optimized_plan),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = stringResource(id = R.string.planner_insights_generate_plan_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun VolumeBalanceChart(
    volumeBalance: VolumeBalance? = null
) {
    val balance = volumeBalance ?: VolumeBalance(0.25f, 0.25f, 0.25f, 0.25f)
    val maxValue = maxOf(balance.push, balance.pull, balance.legs, balance.cardio)

    PlateCard(containerColor = MaterialTheme.colorScheme.surface) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(id = R.string.planner_insights_volume_balance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                VolumeBar(
                    label = stringResource(id = R.string.muscle_group_push),
                    percentage = balance.push,
                    color = FlexTheme.colors.plateCobalt,
                    isHighlighted = balance.push == maxValue,
                    modifier = Modifier.weight(1f)
                )
                VolumeBar(
                    label = stringResource(id = R.string.muscle_group_pull),
                    percentage = balance.pull,
                    color = FlexTheme.colors.plateSteel,
                    isHighlighted = balance.pull == maxValue,
                    modifier = Modifier.weight(1f)
                )
                VolumeBar(
                    label = stringResource(id = R.string.muscle_group_legs),
                    percentage = balance.legs,
                    color = FlexTheme.colors.plateGreen,
                    isHighlighted = balance.legs == maxValue,
                    modifier = Modifier.weight(1f)
                )
                VolumeBar(
                    label = stringResource(id = R.string.muscle_group_cardio),
                    percentage = balance.cardio,
                    color = FlexTheme.colors.plateAmber,
                    isHighlighted = balance.cardio == maxValue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VolumeBar(
    label: String,
    percentage: Float,
    color: Color,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clamped = percentage.coerceIn(0f, 1f)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${(clamped * 100).toInt()}%",
            style = MonoMetricSmall,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(MaterialTheme.shapes.small)
                .background(FlexTheme.colors.chalkTrack),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(clamped.coerceAtLeast(0.06f))
                    .clip(MaterialTheme.shapes.small)
                    .background(color)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
