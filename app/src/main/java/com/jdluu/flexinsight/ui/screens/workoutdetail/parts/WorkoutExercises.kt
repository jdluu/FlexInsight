package com.jdluu.flexinsight.ui.screens.workoutdetail.parts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.ui.components.PlateSectionHeader
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.components.rememberReducedMotion
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.utils.UnitConverter
import com.jdluu.flexinsight.ui.viewmodel.ExerciseWithSets
import java.util.Locale

data class SetData(
    val number: Int,
    val weight: String,
    val reps: String,
    val rpe: String,
    val isPR: Boolean = false
)

@Composable
fun ExercisesSection(
    exercisesWithSets: List<ExerciseWithSets>,
    useMetric: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlateSectionHeader(title = "Exercises")

        exercisesWithSets.forEachIndexed { index, exerciseWithSets ->
            val exercise = exerciseWithSets.exercise
            val sets = exerciseWithSets.sets

            val setsData = remember(sets, useMetric) {
                sets.map { set ->
                    SetData(
                        number = set.number,
                        weight = UnitConverter.formatWeight(set.weight, useMetric),
                        reps = set.reps?.toString() ?: "-",
                        rpe = if (set.rpe != null) String.format(Locale.getDefault(), "%.1f", set.rpe) else "-",
                        isPR = set.isPersonalRecord
                    )
                }
            }

            val bestSet = sets.maxByOrNull { (it.weight ?: 0.0) * (it.reps ?: 0) }
            val bestSetText = bestSet?.let {
                val weight = UnitConverter.convertWeight(it.weight, useMetric) ?: 0.0
                val reps = it.reps ?: 0
                if (weight > 0 && reps > 0) {
                    "Best ${String.format(Locale.getDefault(), "%.0f", weight)}×$reps"
                } else null
            }

            ExpandableExerciseCard(
                number = index + 1,
                exerciseName = exercise.name,
                sets = sets.size,
                equipment = exercise.name.split(" ").firstOrNull() ?: "Exercise",
                isExpanded = index == 0,
                setsData = setsData,
                bestSet = bestSetText,
                useMetric = useMetric
            )
        }
    }
}

@Composable
fun ExpandableExerciseCard(
    number: Int,
    exerciseName: String,
    sets: Int,
    equipment: String,
    isExpanded: Boolean,
    setsData: List<SetData> = emptyList(),
    improvement: String? = null,
    bestSet: String? = null,
    useMetric: Boolean = false
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    val reducedMotion = rememberReducedMotion()
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = 200, easing = FastOutSlowInEasing)
        },
        label = "exercise_chevron_rotation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, FlexTheme.colors.hairline),
        tonalElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        style = MonoMetricMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$sets sets • $equipment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (bestSet != null) {
                    PlateStatusPill(
                        label = bestSet,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        borderColor = FlexTheme.colors.hairline
                    )
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation)
                )
            }

            AnimatedVisibility(
                visible = expanded && setsData.isNotEmpty(),
                enter = if (reducedMotion) {
                    EnterTransition.None
                } else {
                    expandVertically(tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(180))
                },
                exit = if (reducedMotion) {
                    ExitTransition.None
                } else {
                    shrinkVertically(tween(200, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
                }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "#",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(
                                text = UnitConverter.getWeightUnit(useMetric),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Reps",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "RPE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(64.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        setsData.forEach { set ->
                            SetRow(set)
                        }

                        if (improvement != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = FlexTheme.colors.recovered,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = improvement,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FlexTheme.colors.recovered
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetRow(set: SetData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                if (set.isPR) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = set.number.toString(),
                style = MonoMetricSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )
            Text(
                text = set.weight,
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = set.reps,
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier.width(64.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (set.isPR) {
                    PlateStatusPill(
                        label = "PR",
                        icon = Icons.Default.EmojiEvents,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = set.rpe,
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
