package com.jdluu.flexinsight.ui.screens.planner.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.model.WeeklyGoalProgress
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.PlateNumeralMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerHeader(
    hevyEditingEnabled: Boolean = false,
    onOpenPlateRack: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.nav_planner),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = {
                if (!hevyEditingEnabled) {
                    PlateStatusPill(
                        label = "View-only",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        borderColor = FlexTheme.colors.hairline,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(onClick = onOpenPlateRack) {
                    Icon(
                        imageVector = Icons.Outlined.FitnessCenter,
                        contentDescription = stringResource(id = R.string.content_desc_plate_rack),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
    }
}

@Composable
fun WeeklyGoalCard(weeklyGoalProgress: WeeklyGoalProgress? = null) {
    val target = (weeklyGoalProgress?.target ?: 5).coerceAtLeast(1)
    val completed = (weeklyGoalProgress?.completed ?: 0).coerceAtLeast(0)
    val progress = (completed.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val isComplete = completed >= target
    val accentColor = if (isComplete) FlexTheme.colors.recovered else MaterialTheme.colorScheme.primary

    PlateCard(
        modifier = Modifier.padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_item_weekly_goal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$target workouts per week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (weeklyGoalProgress != null) {
                    val (pillBg, pillFg) = if (isComplete) {
                        FlexTheme.colors.recoveredContainer to FlexTheme.colors.onRecoveredContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    PlateStatusPill(
                        label = weeklyGoalProgress.status,
                        containerColor = pillBg,
                        contentColor = pillFg
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$completed",
                    style = PlateNumeralMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "/ $target",
                    style = MonoMetricMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            CalibratedPlateBar(
                progress = progress,
                segments = target.coerceIn(1, 7),
                activeColor = accentColor,
                barHeight = 10.dp
            )

            val remaining = (target - completed).coerceAtLeast(0)
            Text(
                text = when {
                    remaining > 0 -> "$remaining workout${if (remaining == 1) "" else "s"} remaining this week"
                    else -> "Weekly target reached"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
