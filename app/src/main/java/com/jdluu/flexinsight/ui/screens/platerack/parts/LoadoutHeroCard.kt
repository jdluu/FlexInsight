package com.jdluu.flexinsight.ui.screens.platerack.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.theme.PlateNumeralSmall
import com.jdluu.flexinsight.ui.viewmodel.PlateRackUiState
import com.jdluu.flexinsight.ui.viewmodel.PlateRackViewModel
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoadoutHeroCard(
    uiState: PlateRackUiState,
    unitLabel: String,
    onWeightTextChanged: (String) -> Unit,
    onStepWeight: (Int) -> Unit,
    onSetTargetWeight: (Double) -> Unit
) {
    val loadout = uiState.loadout
    PlateCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.plate_rack_target_weight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (loadout.isExact) {
                    PlateStatusPill(
                        label = stringResource(id = R.string.plate_rack_exact_match),
                        containerColor = FlexTheme.colors.recoveredContainer,
                        contentColor = FlexTheme.colors.onRecoveredContainer
                    )
                } else {
                    val achievableText = "${PlateRackViewModel.formatWeightNumber(loadout.achievableWeight)} $unitLabel"
                    val diffPrefix = if (loadout.remainder > 0) "+" else "-"
                    val diffText = "$diffPrefix${PlateRackViewModel.formatWeightNumber(abs(loadout.remainder))} $unitLabel"
                    PlateStatusPill(
                        label = stringResource(id = R.string.plate_rack_achievable_diff, achievableText, diffText),
                        containerColor = FlexTheme.colors.moderateContainer,
                        contentColor = FlexTheme.colors.onModerateContainer
                    )
                }
            }

            // Stepper + Numeric Weight Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepButton(
                    icon = Icons.Default.Remove,
                    contentDescription = stringResource(id = R.string.plate_rack_decrease_weight),
                    onClick = { onStepWeight(-1) }
                )

                OutlinedTextField(
                    value = uiState.targetWeightText,
                    onValueChange = onWeightTextChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = PlateNumeralSmall.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    suffix = {
                        Text(
                            text = unitLabel,
                            style = MonoMetricMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )

                StepButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.plate_rack_increase_weight),
                    onClick = { onStepWeight(1) }
                )
            }

            // Quick benchmark presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.quickWeights.forEach { preset ->
                    val isSelected = abs(uiState.targetWeight - preset) < 0.01
                    OutlinedButton(
                        onClick = { onSetTargetWeight(preset) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                FlexTheme.colors.hairline
                            }
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        Text(
                            text = PlateRackViewModel.formatWeightNumber(preset),
                            style = MonoMetricMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)

            // Interactive Barbell Sleeve Diagram
            BarbellSleeveDiagram(loadout = loadout)

            // Per-side plate breakdown
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.plate_rack_per_side_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val perSideTotal = ((loadout.achievableWeight - loadout.barWeight) / 2.0).coerceAtLeast(0.0)
                    Text(
                        text = "${PlateRackViewModel.formatWeightNumber(perSideTotal)} $unitLabel / side",
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (loadout.platesPerSide.isEmpty()) {
                    Text(
                        text = stringResource(
                            id = R.string.plate_rack_empty_bar,
                            "${PlateRackViewModel.formatWeightNumber(loadout.barWeight)} $unitLabel"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        loadout.platesPerSide.forEach { loaded ->
                            LoadedPlateBreakdownPill(loaded = loaded, unitLabel = unitLabel)
                        }
                    }
                }
            }
        }
    }
}
