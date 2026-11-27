package com.jdluu.flexinsight.ui.screens.dashboard.parts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
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
import com.jdluu.flexinsight.domain.model.MuscleGroup
import com.jdluu.flexinsight.domain.calc.RecoveryScoreCalculator
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.MuscleHeatmap
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.components.recoveryColorFor
import com.jdluu.flexinsight.ui.components.recoveryContainerFor
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.PlateNumeralLarge

@Composable
fun RecoveryPreviewCard(
    muscleRecovery: Map<MuscleGroup, Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val score = if (muscleRecovery.isNotEmpty()) {
        RecoveryScoreCalculator.overallRecoveryScore(muscleRecovery)
    } else {
        100
    }
    val fraction = score / 100f
    val statusColor = recoveryColorFor(fraction)
    val (pillBg, pillFg) = recoveryContainerFor(fraction)
    val statusLabel = when {
        fraction < 0.35f -> "Fatigued"
        fraction < 0.70f -> "Moderate"
        else -> "Ready"
    }

    PlateCard(
        modifier = modifier.padding(horizontal = 16.dp),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.recovery_composite_readiness),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$score%",
                        style = PlateNumeralLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PlateStatusPill(
                        label = statusLabel,
                        containerColor = pillBg,
                        contentColor = pillFg
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.recovery_open_details),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CalibratedPlateBar(
            progress = fraction,
            segments = 10,
            activeColor = statusColor,
            barHeight = 10.dp
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
        Spacer(modifier = Modifier.height(12.dp))

        MuscleHeatmap(
            recoveryMap = muscleRecovery,
            figureHeight = 136.dp
        )
    }
}
