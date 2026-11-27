package com.jdluu.flexinsight.ui.screens.workoutdetail.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateSectionHeader
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme

@Composable
fun WorkoutCoachSection(
    aiReflection: String?,
    isGeneratingReflection: Boolean,
    workoutExplanation: String?,
    isExplainingWorkout: Boolean,
    onExplainWorkout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlateSectionHeader(title = stringResource(id = R.string.workout_coach_assessment))

        OutlinedButton(
            onClick = onExplainWorkout,
            enabled = !isExplainingWorkout,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.small
        ) {
            if (isExplainingWorkout) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.workout_coach_analyzing))
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.workout_explain_button))
            }
        }

        if (workoutExplanation != null) {
            PlateCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.workout_explain_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        PlateStatusPill(
                            label = stringResource(id = R.string.ai_trainer_offline_badge),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = workoutExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (isGeneratingReflection) {
            PlateCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(id = R.string.workout_coach_analyzing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (aiReflection != null) {
            PlateCard(
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = FlexTheme.colors.hairline
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.ai_coach_reflection),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        PlateStatusPill(
                            label = stringResource(id = R.string.ai_trainer_offline_badge),
                            icon = Icons.Default.AutoAwesome,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            borderColor = FlexTheme.colors.hairline
                        )
                    }
                    Text(
                        text = aiReflection,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
