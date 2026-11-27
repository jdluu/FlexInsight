package com.jdluu.flexinsight.ui.screens.platerack.parts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jdluu.flexinsight.domain.calc.LoadedPlate
import com.jdluu.flexinsight.domain.calc.PlateColorBand
import com.jdluu.flexinsight.domain.calc.PlateLoadout
import com.jdluu.flexinsight.domain.calc.WeightUnit
import com.jdluu.flexinsight.ui.theme.FlexTheme
import com.jdluu.flexinsight.ui.theme.MonoMetricMedium
import com.jdluu.flexinsight.ui.theme.MonoMetricSmall
import com.jdluu.flexinsight.ui.viewmodel.PlateRackViewModel

internal data class PlateBandPalette(
    val fill: Color,
    val onFill: Color,
    val border: Color
)

@Composable
internal fun plateBandPalette(band: PlateColorBand): PlateBandPalette = when (band) {
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
internal fun StepButton(
    icon: ImageVector,
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
internal fun BarbellSleeveDiagram(loadout: PlateLoadout) {
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
internal fun LoadedPlateBreakdownPill(
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
