package com.jdluu.flexinsight.ui.screens.platerack.parts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.calc.PlateCalculator
import com.jdluu.flexinsight.domain.calc.WarmupSet
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateSectionHeader
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricLarge
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.viewmodel.PlateRackUiState
import com.jdluu.flexinsight.ui.viewmodel.PlateRackViewModel
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RackConfigCard(
    uiState: PlateRackUiState,
    unitLabel: String,
    onSelectBarWeight: (Double) -> Unit,
    onTogglePlate: (Double) -> Unit
) {
    PlateCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(id = R.string.plate_rack_bar_weight),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.barOptions.forEach { barOption ->
                    val selected = abs(uiState.barWeight - barOption) < 0.01
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectBarWeight(barOption) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        label = {
                            Text(
                                text = "${PlateRackViewModel.formatWeightNumber(barOption)} $unitLabel",
                                style = MonoMetricMedium,
                                maxLines = 1
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

            HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)

            Text(
                text = stringResource(id = R.string.plate_rack_available_plates),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availablePlates.forEach { plateWeight ->
                    val enabled = plateWeight in uiState.enabledPlates
                    val band = PlateCalculator.colorBandFor(plateWeight, uiState.unit)
                    val palette = plateBandPalette(band)
                    FilterChip(
                        selected = enabled,
                        onClick = { onTogglePlate(plateWeight) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (enabled) palette.fill else MaterialTheme.colorScheme.outlineVariant)
                                    .border(1.dp, palette.border, CircleShape)
                            )
                        },
                        label = {
                            Text(
                                text = PlateRackViewModel.formatWeightNumber(plateWeight),
                                style = MonoMetricMedium
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(
                            width = if (enabled) 1.5.dp else 1.dp,
                            color = if (enabled) {
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
        }
    }
}

@Composable
internal fun WarmupRampCard(
    warmupRamp: List<WarmupSet>,
    unitLabel: String,
    onSelectWarmup: (Double) -> Unit
) {
    PlateCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlateSectionHeader(title = stringResource(id = R.string.plate_rack_warmup_title))
            Text(
                text = stringResource(id = R.string.plate_rack_warmup_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            warmupRamp.forEachIndexed { index, set ->
                if (index > 0) {
                    HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onSelectWarmup(set.targetWeight) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlateStatusPill(
                        label = "${set.percentage}%",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        borderColor = FlexTheme.colors.hairline
                    )
                    Text(
                        text = "${PlateRackViewModel.formatWeightNumber(set.targetWeight)} $unitLabel",
                        style = MonoMetricLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val plateSummary = if (set.loadout.platesPerSide.isEmpty()) {
                        stringResource(
                            id = R.string.plate_rack_empty_bar,
                            "${PlateRackViewModel.formatWeightNumber(set.loadout.barWeight)} $unitLabel"
                        )
                    } else {
                        set.loadout.platesPerSide.joinToString(" + ") {
                            "${it.countPerSide}×${PlateRackViewModel.formatWeightNumber(it.weight)}"
                        }
                    }
                    Text(
                        text = plateSummary,
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
