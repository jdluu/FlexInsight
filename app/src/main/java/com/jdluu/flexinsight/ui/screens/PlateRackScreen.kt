package com.jdluu.flexinsight.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jdluu.flexinsight.R
import com.jdluu.flexinsight.domain.calc.PlateCalculator
import com.jdluu.flexinsight.domain.calc.WeightUnit
import com.jdluu.flexinsight.ui.screens.platerack.parts.LoadoutHeroCard
import com.jdluu.flexinsight.ui.screens.platerack.parts.RackConfigCard
import com.jdluu.flexinsight.ui.screens.platerack.parts.RestTimerCard
import com.jdluu.flexinsight.ui.screens.platerack.parts.WarmupRampCard
import com.jdluu.flexinsight.ui.theme.FlexInsightTheme
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.viewmodel.PlateRackUiState
import com.jdluu.flexinsight.ui.viewmodel.PlateRackViewModel

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

@OptIn(ExperimentalMaterial3Api::class)
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
            item {
                LoadoutHeroCard(
                    uiState = uiState,
                    unitLabel = unitLabel,
                    onWeightTextChanged = onWeightTextChanged,
                    onStepWeight = onStepWeight,
                    onSetTargetWeight = onSetTargetWeight
                )
            }

            item {
                RackConfigCard(
                    uiState = uiState,
                    unitLabel = unitLabel,
                    onSelectBarWeight = onSelectBarWeight,
                    onTogglePlate = onTogglePlate
                )
            }

            item {
                WarmupRampCard(
                    warmupRamp = uiState.warmupRamp,
                    unitLabel = unitLabel,
                    onSelectWarmup = onSetTargetWeight
                )
            }

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
