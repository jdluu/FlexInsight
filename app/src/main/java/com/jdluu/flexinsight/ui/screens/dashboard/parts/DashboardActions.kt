package com.jdluu.flexinsight.ui.screens.dashboard.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.model.MuscleGroupProgress
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall

@Composable
fun QuickInsightCard(
    insight: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (insight == null && !isLoading) return

    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(id = R.string.dashboard_quick_insight),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.ai_trainer_offline_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.recovery_smart_analysis_running),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        } else if (insight != null) {
            Text(
                text = insight,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun MuscleGroupProgressSection(
    muscleGroupProgress: List<MuscleGroupProgress>,
    modifier: Modifier = Modifier
) {
    if (muscleGroupProgress.isEmpty()) return

    val topGroups = muscleGroupProgress.sortedByDescending { it.sets }.take(4)
    val maxSets = topGroups.maxOfOrNull { it.sets }?.takeIf { it > 0 } ?: 1

    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = "4-Week Muscle Focus",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            topGroups.forEach { progress ->
                val ratio = (progress.sets.toFloat() / maxSets.toFloat()).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = progress.muscleGroup,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${progress.sets} sets · ${progress.intensity}",
                            style = MonoMetricSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CalibratedPlateBar(
                        progress = ratio,
                        segments = 8,
                        activeColor = MaterialTheme.colorScheme.primary,
                        barHeight = 6.dp
                    )
                }
            }
        }
    }
}
