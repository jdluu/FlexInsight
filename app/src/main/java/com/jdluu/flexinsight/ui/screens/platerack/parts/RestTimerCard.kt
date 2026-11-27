package com.jdluu.flexinsight.ui.screens.platerack.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.calc.RestTimerFormatter
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.PlateNumeralLarge
import com.jdluu.flexinsight.ui.viewmodel.PlateRackUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RestTimerCard(
    uiState: PlateRackUiState,
    onStartPreset: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onAddSeconds: (Int) -> Unit,
    onReset: () -> Unit
) {
    val timer = uiState.timerState
    val statusText = when {
        timer.hasCompleted -> stringResource(id = R.string.plate_rack_timer_complete)
        timer.isRunning -> stringResource(id = R.string.plate_rack_timer_running)
        timer.remainingSeconds < timer.totalSeconds -> stringResource(id = R.string.plate_rack_timer_paused)
        else -> stringResource(id = R.string.plate_rack_timer_ready)
    }
    val (pillBg, pillFg) = when {
        timer.hasCompleted -> FlexTheme.colors.recoveredContainer to FlexTheme.colors.onRecoveredContainer
        timer.isRunning -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val barColor = when {
        timer.hasCompleted -> FlexTheme.colors.recovered
        else -> MaterialTheme.colorScheme.primary
    }

    PlateCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.plate_rack_rest_timer),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                PlateStatusPill(
                    label = statusText,
                    containerColor = pillBg,
                    contentColor = pillFg
                )
            }

            Text(
                text = timer.formattedRemaining,
                style = PlateNumeralLarge,
                color = if (timer.hasCompleted) {
                    FlexTheme.colors.recovered
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            CalibratedPlateBar(
                progress = timer.progress,
                segments = 12,
                activeColor = barColor,
                barHeight = 10.dp
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RestTimerFormatter.DEFAULT_PRESETS_SECONDS.forEach { presetSeconds ->
                    val selected = timer.totalSeconds == presetSeconds
                    FilterChip(
                        selected = selected,
                        onClick = { onStartPreset(presetSeconds) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = {
                            Text(
                                text = RestTimerFormatter.presetLabel(presetSeconds),
                                style = MonoMetricMedium
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                FlexTheme.colors.hairline
                            }
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val primaryLabel = when {
                    timer.isRunning -> stringResource(id = R.string.plate_rack_timer_pause)
                    timer.hasCompleted -> stringResource(id = R.string.plate_rack_timer_restart)
                    timer.remainingSeconds < timer.totalSeconds -> stringResource(id = R.string.plate_rack_timer_resume)
                    else -> stringResource(id = R.string.plate_rack_timer_start)
                }
                Button(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = if (timer.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = { onAddSeconds(15) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = stringResource(id = R.string.plate_rack_timer_add_15),
                        style = MonoMetricMedium
                    )
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(id = R.string.plate_rack_timer_reset),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
