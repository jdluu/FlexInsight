package com.jdluu.flexinsight.ui.screens.dashboard.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
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
import com.jdluu.flexinsight.domain.model.TrainingLoadScore
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.theme.PlateNumeralMedium

@Composable
fun TrainingLoadCard(
    score: TrainingLoadScore?,
    modifier: Modifier = Modifier
) {
    if (score == null) return

    val loadFraction = (score.overall / 100f).coerceIn(0f, 1f)
    val accentColor = when {
        score.overall >= 85 -> FlexTheme.colors.strained
        score.overall >= 60 -> FlexTheme.colors.moderateFatigue
        else -> MaterialTheme.colorScheme.primary
    }

    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_training_load_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = score.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${score.overall}",
                    style = PlateNumeralMedium,
                    color = accentColor
                )
                Text(
                    text = score.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CalibratedPlateBar(
            progress = loadFraction,
            segments = 10,
            activeColor = accentColor,
            barHeight = 8.dp
        )

        Spacer(modifier = Modifier.height(10.dp))

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlateStatusPill(
                label = "Strength ${score.hevyVolumeScore}",
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                borderColor = FlexTheme.colors.hairline
            )
            PlateStatusPill(
                label = "Cardio ${score.cardioScore}",
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                borderColor = FlexTheme.colors.hairline
            )
            PlateStatusPill(
                label = "Sleep ${score.sleepScore}",
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                borderColor = FlexTheme.colors.hairline
            )
        }
    }
}

@Composable
fun DeloadBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    if (message.isNullOrBlank()) return

    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        containerColor = FlexTheme.colors.moderateContainer,
        borderColor = FlexTheme.colors.moderateFatigue.copy(alpha = 0.45f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = FlexTheme.colors.moderateFatigue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = FlexTheme.colors.onModerateContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
