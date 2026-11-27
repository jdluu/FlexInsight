package com.jdluu.flexinsight.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.calc.LoadedPlate
import com.jdluu.flexinsight.domain.calc.PlateCalculator
import com.jdluu.flexinsight.domain.calc.PlateColorBand
import com.jdluu.flexinsight.domain.calc.PlateLoadout
import com.jdluu.flexinsight.domain.calc.RestTimerFormatter
import com.jdluu.flexinsight.domain.calc.WarmupSet
import com.jdluu.flexinsight.domain.calc.WeightUnit
import com.jdluu.flexinsight.ui.components.CalibratedPlateBar
import com.jdluu.flexinsight.ui.components.PlateCard
import com.jdluu.flexinsight.ui.components.PlateSectionHeader
import com.jdluu.flexinsight.ui.components.PlateStatusPill
import com.jdluu.flexinsight.ui.theme.FlexInsightTheme
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricLarge
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.theme.PlateNumeralLarge
import com.jdluu.flexinsight.ui.theme.PlateNumeralSmall
import com.jdluu.flexinsight.ui.viewmodel.PlateRackUiState
import com.jdluu.flexinsight.ui.viewmodel.PlateRackViewModel
import kotlin.math.abs

private data class PlateBandPalette(
    val fill: Color,
    val onFill: Color,
    val border: Color
)

@Composable
private fun plateBandPalette(band: PlateColorBand): PlateBandPalette = when (band) {
    PlateColorBand.RED -> PlateBandPalette(
        fill = FlexTheme.colors.plateCrimson,
        onFill = FlexTheme.colors.onStrained,
        border = FlexTheme.colors.plateCrimson
    )
    PlateColorBand.BLUE -> PlateBandPalette(
        fill = FlexTheme.colors.plateCobalt,
        onFill = MaterialTheme.colorScheme.onPrimary,
        border = FlexTheme.colors.plateCobalt
    )
    PlateColorBand.YELLOW -> PlateBandPalette(
        fill = FlexTheme.colors.plateAmber,
        onFill = FlexTheme.colors.onModerateFatigue,
        border = FlexTheme.colors.plateAmber
    )
    PlateColorBand.GREEN -> PlateBandPalette(
        fill = FlexTheme.colors.plateGreen,
        onFill = FlexTheme.colors.onRecovered,
        border = FlexTheme.colors.plateGreen
    )
    PlateColorBand.WHITE -> PlateBandPalette(
        fill = FlexTheme.colors.plateWhite,
        onFill = FlexTheme.colors.onPlateWhite,
        border = MaterialTheme.colorScheme.outline
    )
    PlateColorBand.BLACK -> PlateBandPalette(
        fill = FlexTheme.colors.plateBlack,
        onFill = FlexTheme.colors.onPlateBlack,
        border = MaterialTheme.colorScheme.outline
    )
    PlateColorBand.CHROME -> PlateBandPalette(
        fill = FlexTheme.colors.plateSteel,
        onFill = FlexTheme.colors.onPlateSteel,
        border = MaterialTheme.colorScheme.outline
    )
}

@Composable
fun PlateRackScreen(
    viewModel: PlateRackViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlateRackContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSelectUnit = viewModel::selectUnit,
        onWeightTextChanged = viewModel::onWeightTextChanged,
        onStepWeight = viewModel::stepWeight,
        onSetTargetWeight = viewModel::setTargetWeight,
        onSelectBarWeight = viewModel::selectBarWeight,
        onTogglePlate = viewModel::togglePlate,
        onStartTimerPreset = viewModel::startTimerPreset,
        onToggleTimerPause = viewModel::toggleTimerPause,
        onAddTimerSeconds = viewModel::addTimerSeconds,
        onResetTimer = viewModel::resetTimer
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlateRackContent(
    uiState: PlateRackUiState,
    onNavigateBack: () -> Unit,
    onSelectUnit: (WeightUnit) -> Unit,
    onWeightTextChanged: (String) -> Unit,
    onStepWeight: (Int) -> Unit,
    onSetTargetWeight: (Double) -> Unit,
    onSelectBarWeight: (Double) -> Unit,
    onTogglePlate: (Double) -> Unit,
    onStartTimerPreset: (Int) -> Unit,
    onToggleTimerPause: () -> Unit,
    onAddTimerSeconds: (Int) -> Unit,
    onResetTimer: () -> Unit
) {
    val unitLabel = uiState.unit.label

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.plate_rack_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.content_desc_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WeightUnit.entries.forEach { unitOption ->
                        val selected = uiState.unit == unitOption
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectUnit(unitOption) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            label = {
                                Text(
                                    text = unitOption.label,
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
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        HorizontalDivider(thickness = 1.dp, color = FlexTheme.colors.hairline)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Target Weight + Barbell Diagram + Per-Side Loadout
            item {
                LoadoutHeroCard(
                    uiState = uiState,
                    unitLabel = unitLabel,
                    onWeightTextChanged = onWeightTextChanged,
                    onStepWeight = onStepWeight,
                    onSetTargetWeight = onSetTargetWeight
                )
            }

            // Card 2: Bar Weight & Rack Inventory
            item {
                RackConfigCard(
                    uiState = uiState,
                    unitLabel = unitLabel,
                    onSelectBarWeight = onSelectBarWeight,
                    onTogglePlate = onTogglePlate
                )
            }

            // Card 3: Warm-Up Ramp (50% / 70% / 85%)
            item {
                WarmupRampCard(
                    warmupRamp = uiState.warmupRamp,
                    unitLabel = unitLabel,
                    onSelectWarmup = onSetTargetWeight
                )
            }

            // Card 4: Gym-Floor Rest Timer
            item {
                RestTimerCard(
                    uiState = uiState,
                    onStartPreset = onStartTimerPreset,
                    onTogglePause = onToggleTimerPause,
                    onAddSeconds = onAddTimerSeconds,
                    onReset = onResetTimer
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoadoutHeroCard(
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

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BarbellSleeveDiagram(loadout: PlateLoadout) {
    val expandedPlates = buildList {
        loadout.platesPerSide.forEach { loaded ->
            repeat(loaded.countPerSide) {
                add(loaded.weight to loaded.colorBand)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bar shaft leading into collar
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(10.dp)
                    .background(FlexTheme.colors.plateSteel)
            )
            // Machined inner collar
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )

            if (expandedPlates.isEmpty()) {
                // Empty sleeve
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(FlexTheme.colors.plateSteel)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Spacer(modifier = Modifier.width(2.dp))
                    expandedPlates.forEach { (weight, band) ->
                        val palette = plateBandPalette(band)
                        val height = plateHeightDp(weight, loadout.unit)
                        val width = plateWidthDp(weight, loadout.unit)
                        Box(
                            modifier = Modifier
                                .width(width)
                                .height(height)
                                .clip(RoundedCornerShape(3.dp))
                                .background(palette.fill)
                                .border(1.dp, palette.border, RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (width >= 18.dp) {
                                Text(
                                    text = PlateRackViewModel.formatWeightNumber(weight),
                                    style = MonoMetricSmall,
                                    color = palette.onFill,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    // Remaining steel sleeve tip
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(FlexTheme.colors.plateSteel)
                    )
                }
            }
        }
    }
}

private fun plateHeightDp(weight: Double, unit: WeightUnit): Dp = when (unit) {
    WeightUnit.KG -> when {
        weight >= 20.0 -> 96.dp
        weight >= 15.0 -> 86.dp
        weight >= 10.0 -> 74.dp
        weight >= 5.0 -> 58.dp
        weight >= 2.5 -> 48.dp
        else -> 38.dp
    }
    WeightUnit.LB -> when {
        weight >= 45.0 -> 96.dp
        weight >= 35.0 -> 84.dp
        weight >= 25.0 -> 72.dp
        weight >= 10.0 -> 56.dp
        weight >= 5.0 -> 46.dp
        else -> 38.dp
    }
}

private fun plateWidthDp(weight: Double, unit: WeightUnit): Dp = when (unit) {
    WeightUnit.KG -> when {
        weight >= 25.0 -> 24.dp
        weight >= 20.0 -> 22.dp
        weight >= 15.0 -> 20.dp
        weight >= 10.0 -> 18.dp
        weight >= 5.0 -> 14.dp
        else -> 11.dp
    }
    WeightUnit.LB -> when {
        weight >= 45.0 -> 24.dp
        weight >= 35.0 -> 22.dp
        weight >= 25.0 -> 20.dp
        weight >= 10.0 -> 18.dp
        weight >= 5.0 -> 14.dp
        else -> 11.dp
    }
}

@Composable
private fun LoadedPlateBreakdownPill(
    loaded: LoadedPlate,
    unitLabel: String
) {
    val palette = plateBandPalette(loaded.colorBand)
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(palette.fill)
                    .border(1.dp, palette.border, CircleShape)
            )
            Text(
                text = "${loaded.countPerSide} × ${PlateRackViewModel.formatWeightNumber(loaded.weight)} $unitLabel",
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RackConfigCard(
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
private fun WarmupRampCard(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimerCard(
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

@Preview(name = "Plate Rack Light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Plate Rack Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlateRackScreenPreview() {
    FlexInsightTheme {
        val sampleState = PlateRackUiState(
            unit = WeightUnit.KG,
            targetWeightText = "142.5",
            targetWeight = 142.5,
            barWeight = 20.0,
            loadout = PlateCalculator.calculate(142.5, 20.0, WeightUnit.KG),
            warmupRamp = PlateCalculator.buildWarmupRamp(142.5, 20.0, WeightUnit.KG)
        )
        PlateRackContent(
            uiState = sampleState,
            onNavigateBack = {},
            onSelectUnit = {},
            onWeightTextChanged = {},
            onStepWeight = {},
            onSetTargetWeight = {},
            onSelectBarWeight = {},
            onTogglePlate = {},
            onStartTimerPreset = {},
            onToggleTimerPause = {},
            onAddTimerSeconds = {},
            onResetTimer = {}
        )
    }
}
