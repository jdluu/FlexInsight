package com.jdluu.flexinsight.domain.calc

import kotlin.math.abs
import kotlin.math.roundToInt

enum class WeightUnit(val label: String) {
    KG("kg"),
    LB("lb")
}

enum class PlateColorBand {
    RED,
    BLUE,
    YELLOW,
    GREEN,
    WHITE,
    BLACK,
    CHROME
}

data class LoadedPlate(
    val weight: Double,
    val countPerSide: Int,
    val colorBand: PlateColorBand
)

data class PlateLoadout(
    val targetWeight: Double,
    val barWeight: Double,
    val unit: WeightUnit,
    val platesPerSide: List<LoadedPlate>,
    val achievableWeight: Double,
    val remainder: Double,
    val isExact: Boolean
) {
    val totalPlatesPerSide: Int
        get() = platesPerSide.sumOf { it.countPerSide }
}

data class WarmupSet(
    val percentage: Int,
    val targetWeight: Double,
    val loadout: PlateLoadout
)

/**
 * Pure barbell plate-loading calculator using competition (kg) and standard gym (lb) sets.
 * Computes greedy per-side denominations with hundredth-unit integer math to eliminate float drift.
 */
object PlateCalculator {

    private val KG_PLATES = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    private val LB_PLATES = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    private val KG_BARS = listOf(20.0, 15.0, 10.0)
    private val LB_BARS = listOf(45.0, 35.0, 15.0)

    fun defaultBarWeight(unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> 20.0
        WeightUnit.LB -> 45.0
    }

    fun barWeightOptions(unit: WeightUnit): List<Double> = when (unit) {
        WeightUnit.KG -> KG_BARS
        WeightUnit.LB -> LB_BARS
    }

    fun availablePlates(unit: WeightUnit): List<Double> = when (unit) {
        WeightUnit.KG -> KG_PLATES
        WeightUnit.LB -> LB_PLATES
    }

    fun smallestStep(
        unit: WeightUnit,
        enabledPlates: Set<Double> = availablePlates(unit).toSet()
    ): Double {
        val minPlate = availablePlates(unit).filter { it in enabledPlates }.minOrNull()
        return if (minPlate != null) minPlate * 2.0 else 0.0
    }

    fun colorBandFor(weight: Double, unit: WeightUnit): PlateColorBand = when (unit) {
        WeightUnit.KG -> when (toHundredths(weight)) {
            2500 -> PlateColorBand.RED
            2000 -> PlateColorBand.BLUE
            1500 -> PlateColorBand.YELLOW
            1000 -> PlateColorBand.GREEN
            500 -> PlateColorBand.WHITE
            250 -> PlateColorBand.BLACK
            else -> PlateColorBand.CHROME
        }
        WeightUnit.LB -> when (toHundredths(weight)) {
            4500 -> PlateColorBand.BLUE
            3500 -> PlateColorBand.YELLOW
            2500 -> PlateColorBand.GREEN
            1000 -> PlateColorBand.WHITE
            500 -> PlateColorBand.BLACK
            else -> PlateColorBand.CHROME
        }
    }

    fun calculate(
        targetWeight: Double,
        barWeight: Double = defaultBarWeight(WeightUnit.KG),
        unit: WeightUnit = WeightUnit.KG,
        enabledPlates: Set<Double> = availablePlates(unit).toSet()
    ): PlateLoadout {
        val sanitizedBar = barWeight.coerceAtLeast(0.0)
        val sanitizedTarget = targetWeight.coerceAtLeast(0.0)

        if (sanitizedTarget <= sanitizedBar) {
            val diff = sanitizedTarget - sanitizedBar
            return PlateLoadout(
                targetWeight = sanitizedTarget,
                barWeight = sanitizedBar,
                unit = unit,
                platesPerSide = emptyList(),
                achievableWeight = sanitizedBar,
                remainder = diff,
                isExact = abs(diff) < 0.001
            )
        }

        val orderedPlates = availablePlates(unit)
            .filter { it in enabledPlates && it > 0.0 }
            .sortedDescending()

        var remainingPerSideHundredths = toHundredths((sanitizedTarget - sanitizedBar) / 2.0)
        val loaded = mutableListOf<LoadedPlate>()
        var loadedPerSideHundredths = 0

        for (plate in orderedPlates) {
            val plateHundredths = toHundredths(plate)
            if (plateHundredths <= 0 || remainingPerSideHundredths < plateHundredths) continue
            val count = remainingPerSideHundredths / plateHundredths
            if (count > 0) {
                loaded += LoadedPlate(
                    weight = plate,
                    countPerSide = count,
                    colorBand = colorBandFor(plate, unit)
                )
                val delta = count * plateHundredths
                remainingPerSideHundredths -= delta
                loadedPerSideHundredths += delta
            }
        }

        val achievable = (toHundredths(sanitizedBar) + 2 * loadedPerSideHundredths) / 100.0
        val remainder = (toHundredths(sanitizedTarget) - toHundredths(achievable)) / 100.0

        return PlateLoadout(
            targetWeight = sanitizedTarget,
            barWeight = sanitizedBar,
            unit = unit,
            platesPerSide = loaded,
            achievableWeight = achievable,
            remainder = remainder,
            isExact = abs(remainder) < 0.01
        )
    }

    fun buildWarmupRamp(
        workingWeight: Double,
        barWeight: Double,
        unit: WeightUnit,
        enabledPlates: Set<Double> = availablePlates(unit).toSet(),
        percentages: List<Int> = listOf(50, 70, 85)
    ): List<WarmupSet> {
        val sanitizedBar = barWeight.coerceAtLeast(0.0)
        val step = smallestStep(unit, enabledPlates)
        return percentages.map { pct ->
            val raw = workingWeight * (pct / 100.0)
            val quantized = if (raw <= sanitizedBar || step <= 0.0) {
                sanitizedBar
            } else {
                val stepsAboveBar = ((raw - sanitizedBar) / step).roundToInt().coerceAtLeast(0)
                sanitizedBar + stepsAboveBar * step
            }
            val loadout = calculate(
                targetWeight = quantized,
                barWeight = sanitizedBar,
                unit = unit,
                enabledPlates = enabledPlates
            )
            WarmupSet(
                percentage = pct,
                targetWeight = loadout.achievableWeight,
                loadout = loadout
            )
        }
    }

    private fun toHundredths(value: Double): Int = (value * 100.0).roundToInt()
}
