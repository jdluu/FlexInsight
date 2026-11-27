package com.jdluu.flexinsight.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun `calculate kg competition loadout for 142_5kg uses minimal greedy plate count`() {
        // 142.5 - 20 = 122.5 -> 61.25 per side = 2x25 + 1x10 + 1x1.25
        val loadout = PlateCalculator.calculate(
            targetWeight = 142.5,
            barWeight = 20.0,
            unit = WeightUnit.KG
        )

        assertTrue(loadout.isExact)
        assertEquals(142.5, loadout.achievableWeight, 0.001)
        assertEquals(0.0, loadout.remainder, 0.001)
        assertEquals(4, loadout.totalPlatesPerSide)
        assertEquals(
            listOf(
                LoadedPlate(25.0, 2, PlateColorBand.RED),
                LoadedPlate(10.0, 1, PlateColorBand.GREEN),
                LoadedPlate(1.25, 1, PlateColorBand.CHROME)
            ),
            loadout.platesPerSide
        )
    }

    @Test
    fun `calculate kg handles blue yellow white and black plates cleanly`() {
        // 105 kg: 42.5 per side without 25s -> 2x20 + 2.5
        val doubleBlue = PlateCalculator.calculate(
            targetWeight = 105.0,
            barWeight = 20.0,
            unit = WeightUnit.KG,
            enabledPlates = setOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        )
        assertTrue(doubleBlue.isExact)
        assertEquals(
            listOf(
                LoadedPlate(20.0, 2, PlateColorBand.BLUE),
                LoadedPlate(2.5, 1, PlateColorBand.BLACK)
            ),
            doubleBlue.platesPerSide
        )

        // 95 kg: 37.5 per side without 25s -> 20 + 15 + 2.5
        val blueYellowBlack = PlateCalculator.calculate(
            targetWeight = 95.0,
            barWeight = 20.0,
            unit = WeightUnit.KG,
            enabledPlates = setOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        )
        assertTrue(blueYellowBlack.isExact)
        assertEquals(
            listOf(
                LoadedPlate(20.0, 1, PlateColorBand.BLUE),
                LoadedPlate(15.0, 1, PlateColorBand.YELLOW),
                LoadedPlate(2.5, 1, PlateColorBand.BLACK)
            ),
            blueYellowBlack.platesPerSide
        )

        // 30 kg: 5 per side -> 1x5 white
        val whitePlate = PlateCalculator.calculate(
            targetWeight = 30.0,
            barWeight = 20.0,
            unit = WeightUnit.KG
        )
        assertTrue(whitePlate.isExact)
        assertEquals(listOf(LoadedPlate(5.0, 1, PlateColorBand.WHITE)), whitePlate.platesPerSide)
    }

    @Test
    fun `calculate lb loadout for 315lb uses three 45s per side`() {
        val loadout = PlateCalculator.calculate(
            targetWeight = 315.0,
            barWeight = 45.0,
            unit = WeightUnit.LB
        )

        assertTrue(loadout.isExact)
        assertEquals(315.0, loadout.achievableWeight, 0.001)
        assertEquals(listOf(LoadedPlate(45.0, 3, PlateColorBand.BLUE)), loadout.platesPerSide)
        assertEquals(3, loadout.totalPlatesPerSide)
    }

    @Test
    fun `calculate respects disabled plates and reports remainder when not exact`() {
        // 135 lb - 45 = 90 -> 45 per side. Disable 45lb and 35lb -> uses 1x25 + 2x10? Wait: 25 + 10 + 10 = 45!
        val without45s = PlateCalculator.calculate(
            targetWeight = 135.0,
            barWeight = 45.0,
            unit = WeightUnit.LB,
            enabledPlates = setOf(25.0, 10.0, 5.0, 2.5)
        )
        assertTrue(without45s.isExact)
        assertEquals(
            listOf(
                LoadedPlate(25.0, 1, PlateColorBand.GREEN),
                LoadedPlate(10.0, 2, PlateColorBand.WHITE)
            ),
            without45s.platesPerSide
        )

        // Unachievable 101 kg without 1.25kg plates -> 100 kg achievable, 1.0 kg remainder
        val inexact = PlateCalculator.calculate(
            targetWeight = 101.0,
            barWeight = 20.0,
            unit = WeightUnit.KG,
            enabledPlates = setOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5)
        )
        assertFalse(inexact.isExact)
        assertEquals(100.0, inexact.achievableWeight, 0.001)
        assertEquals(1.0, inexact.remainder, 0.001)
    }

    @Test
    fun `calculate at or below bar weight returns empty plates`() {
        val exactBar = PlateCalculator.calculate(targetWeight = 20.0, barWeight = 20.0, unit = WeightUnit.KG)
        assertTrue(exactBar.isExact)
        assertTrue(exactBar.platesPerSide.isEmpty())
        assertEquals(20.0, exactBar.achievableWeight, 0.001)

        val belowBar = PlateCalculator.calculate(targetWeight = 12.0, barWeight = 20.0, unit = WeightUnit.KG)
        assertFalse(belowBar.isExact)
        assertTrue(belowBar.platesPerSide.isEmpty())
        assertEquals(-8.0, belowBar.remainder, 0.001)
    }

    @Test
    fun `color bands and bar options cover all standard denominations`() {
        assertEquals(20.0, PlateCalculator.defaultBarWeight(WeightUnit.KG), 0.001)
        assertEquals(45.0, PlateCalculator.defaultBarWeight(WeightUnit.LB), 0.001)
        assertEquals(listOf(20.0, 15.0, 10.0), PlateCalculator.barWeightOptions(WeightUnit.KG))
        assertEquals(listOf(45.0, 35.0, 15.0), PlateCalculator.barWeightOptions(WeightUnit.LB))
        assertEquals(2.5, PlateCalculator.smallestStep(WeightUnit.KG), 0.001)
        assertEquals(5.0, PlateCalculator.smallestStep(WeightUnit.LB), 0.001)
        assertEquals(0.0, PlateCalculator.smallestStep(WeightUnit.KG, emptySet()), 0.001)

        assertEquals(PlateColorBand.GREEN, PlateCalculator.colorBandFor(10.0, WeightUnit.KG))
        assertEquals(PlateColorBand.WHITE, PlateCalculator.colorBandFor(5.0, WeightUnit.KG))
        assertEquals(PlateColorBand.BLACK, PlateCalculator.colorBandFor(2.5, WeightUnit.KG))
        assertEquals(PlateColorBand.YELLOW, PlateCalculator.colorBandFor(35.0, WeightUnit.LB))
        assertEquals(PlateColorBand.BLACK, PlateCalculator.colorBandFor(5.0, WeightUnit.LB))
        assertEquals(PlateColorBand.CHROME, PlateCalculator.colorBandFor(2.5, WeightUnit.LB))
    }

    @Test
    fun `buildWarmupRamp quantizes percentages to valid plate steps and floors at bar weight`() {
        val ramp = PlateCalculator.buildWarmupRamp(
            workingWeight = 100.0,
            barWeight = 20.0,
            unit = WeightUnit.KG
        )
        assertEquals(3, ramp.size)
        assertEquals(50, ramp[0].percentage)
        assertEquals(50.0, ramp[0].targetWeight, 0.001)
        assertEquals(70.0, ramp[1].targetWeight, 0.001)
        assertEquals(85.0, ramp[2].targetWeight, 0.001)

        val lightRamp = PlateCalculator.buildWarmupRamp(
            workingWeight = 25.0,
            barWeight = 20.0,
            unit = WeightUnit.KG
        )
        assertEquals(20.0, lightRamp[0].targetWeight, 0.001)
    }
}
