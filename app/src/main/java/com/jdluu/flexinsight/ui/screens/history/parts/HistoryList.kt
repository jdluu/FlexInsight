package com.jdluu.flexinsight.ui.screens.history.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdluu.flexinsight.data.model.Exercise
import com.jdluu.flexinsight.data.model.PRDetails
import com.jdluu.flexinsight.ui.theme.Primary
import com.jdluu.flexinsight.ui.theme.SurfaceCardAlt
import com.jdluu.flexinsight.ui.theme.SurfaceHighlight
import com.jdluu.flexinsight.ui.theme.TextSecondary
import com.jdluu.flexinsight.ui.utils.UnitConverter
import com.jdluu.flexinsight.ui.screens.history.parts.formatDateShort

import com.jdluu.flexinsight.ui.components.EmptyStateIllustration

@Composable
fun RecentPRsSection(
    prsWithDetails: List<PRDetails> = emptyList(),
    useMetric: Boolean,
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent PRs",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (prsWithDetails.isEmpty()) {
            EmptyStateIllustration(
                icon = Icons.Default.FitnessCenter,
                title = "No PRs Yet",
                description = "Keep pushing your limits - your first PR is just one workout away.",
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            // ... grouping logic ...
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val exercisePRs = remember(prsWithDetails) { prsWithDetails.groupBy { it.exerciseName } }
                val newestPRPerExercise = remember(exercisePRs) { exercisePRs.mapValues { (_, prs) -> prs.maxByOrNull { it.date } } }

                prsWithDetails.forEach { prDetails ->
                    val isNewPR = newestPRPerExercise[prDetails.exerciseName]?.setId == prDetails.setId
                    val convertedWeight = UnitConverter.convertWeight(prDetails.weight, useMetric)
                    PRCard(
                        exercise = prDetails.exerciseName,
                        date = formatDateShort(prDetails.date),
                        muscle = prDetails.muscleGroup,
                        weight = convertedWeight?.toInt()?.toString() ?: "-",
                        unit = UnitConverter.getWeightUnit(useMetric),
                        isNewPR = isNewPR,
                        onClick = { onNavigateToWorkoutDetail(prDetails.workoutId) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String, isPlaceholder: Boolean = false) {
    EmptyStateIllustration(
        icon = Icons.Default.Info,
        title = if (isPlaceholder) "Working on it..." else "Empty",
        description = message
    )
}

@Composable
fun ExerciseHistoryItem(exercise: Exercise) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!exercise.notes.isNullOrBlank()) {
                    Text(
                        text = exercise.notes,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            if (exercise.restDuration != null && exercise.restDuration > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                ) {
                    Text(
                        text = "${exercise.restDuration}s rest",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PRCard(
    exercise: String,
    date: String,
    muscle: String,
    weight: String,
    unit: String,
    isNewPR: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (isNewPR) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = if (isNewPR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = exercise,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = muscle.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = weight,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (isNewPR) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "NEW PR",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
