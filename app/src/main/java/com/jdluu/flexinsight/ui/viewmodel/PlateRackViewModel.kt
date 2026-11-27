package com.jdluu.flexinsight.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.domain.calc.PlateCalculator
import com.jdluu.flexinsight.domain.calc.PlateLoadout
import com.jdluu.flexinsight.domain.calc.WarmupSet
import com.jdluu.flexinsight.domain.calc.WeightUnit
import com.jdluu.flexinsight.ui.timer.RestTimerController
import com.jdluu.flexinsight.ui.timer.RestTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

data class PlateRackUiState(
    val unit: WeightUnit = WeightUnit.KG,
    val targetWeightText: String = "100",
    val targetWeight: Double = 100.0,
    val barWeight: Double = 20.0,
    val barOptions: List<Double> = PlateCalculator.barWeightOptions(WeightUnit.KG),
    val availablePlates: List<Double> = PlateCalculator.availablePlates(WeightUnit.KG),
    val enabledPlates: Set<Double> = PlateCalculator.availablePlates(WeightUnit.KG).toSet(),
    val quickWeights: List<Double> = listOf(60.0, 100.0, 140.0, 180.0),
    val loadout: PlateLoadout = PlateCalculator.calculate(100.0, 20.0, WeightUnit.KG),
    val warmupRamp: List<WarmupSet> = PlateCalculator.buildWarmupRamp(100.0, 20.0, WeightUnit.KG),
    val timerState: RestTimerState = RestTimerState(),
    val selectedPresetSeconds: Int = 90
)

@HiltViewModel
class PlateRackViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val restTimerController: RestTimerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val weightArg: Double? = savedStateHandle.get<String>("weight")
        ?.toDoubleOrNull()
        ?.takeIf { it > 0.0 }
    private val unitArg: WeightUnit? = savedStateHandle.get<String>("unit")
        ?.let { raw -> runCatching { WeightUnit.valueOf(raw) }.getOrNull() }

    private var hasManualUnitOrWeightEdit = unitArg != null || weightArg != null

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<PlateRackUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            restTimerController.state.collect { timer ->
                _uiState.update { it.copy(timerState = timer) }
            }
        }

        viewModelScope.launch {
            userPreferencesManager.unitsFlow.collect { unitsPref ->
                if (!hasManualUnitOrWeightEdit) {
                    val preferredUnit = if (unitsPref == "Metric") WeightUnit.KG else WeightUnit.LB
                    if (_uiState.value.unit != preferredUnit) {
                        applyUnitSwitch(preferredUnit, keepTargetFromConversion = false)
                    }
                }
            }
        }
    }

    private fun buildInitialState(): PlateRackUiState {
        val unit = unitArg ?: WeightUnit.KG
        val bar = PlateCalculator.defaultBarWeight(unit)
        val target = weightArg ?: defaultTargetFor(unit)
        val available = PlateCalculator.availablePlates(unit)
        val enabled = available.toSet()
        return PlateRackUiState(
            unit = unit,
            targetWeightText = formatWeightNumber(target),
            targetWeight = target,
            barWeight = bar,
            barOptions = PlateCalculator.barWeightOptions(unit),
            availablePlates = available,
            enabledPlates = enabled,
            quickWeights = quickWeightsFor(unit),
            loadout = PlateCalculator.calculate(target, bar, unit, enabled),
            warmupRamp = PlateCalculator.buildWarmupRamp(target, bar, unit, enabled),
            timerState = restTimerController.state.value
        )
    }

    fun onWeightTextChanged(rawInput: String) {
        hasManualUnitOrWeightEdit = true
        val filtered = buildString {
            var seenDot = false
            for (ch in rawInput) {
                when {
                    ch.isDigit() -> append(ch)
                    ch == '.' && !seenDot -> {
                        seenDot = true
                        append(ch)
                    }
                }
            }
        }.take(7)

        val parsed = filtered.toDoubleOrNull()
        _uiState.update { current ->
            val nextTarget = parsed?.coerceIn(0.0, 2000.0) ?: current.targetWeight
            current.copy(
                targetWeightText = filtered,
                targetWeight = nextTarget,
                loadout = PlateCalculator.calculate(
                    targetWeight = nextTarget,
                    barWeight = current.barWeight,
                    unit = current.unit,
                    enabledPlates = current.enabledPlates
                ),
                warmupRamp = PlateCalculator.buildWarmupRamp(
                    workingWeight = nextTarget,
                    barWeight = current.barWeight,
                    unit = current.unit,
                    enabledPlates = current.enabledPlates
                )
            )
        }
    }

    fun stepWeight(steps: Int) {
        hasManualUnitOrWeightEdit = true
        _uiState.update { current ->
            val step = PlateCalculator.smallestStep(current.unit, current.enabledPlates)
                .takeIf { it > 0.0 }
                ?: PlateCalculator.smallestStep(current.unit)
            val nextTarget = (current.targetWeight + steps * step).coerceIn(current.barWeight, 2000.0)
            current.copy(
                targetWeight = nextTarget,
                targetWeightText = formatWeightNumber(nextTarget),
                loadout = PlateCalculator.calculate(
                    targetWeight = nextTarget,
                    barWeight = current.barWeight,
                    unit = current.unit,
                    enabledPlates = current.enabledPlates
                ),
                warmupRamp = PlateCalculator.buildWarmupRamp(
                    workingWeight = nextTarget,
                    barWeight = current.barWeight,
                    unit = current.unit,
                    enabledPlates = current.enabledPlates
                )
            )
        }
    }

    fun setTargetWeight(weight: Double) {
        hasManualUnitOrWeightEdit = true
        _uiState.update { current ->
            val clamped = weight.coerceIn(current.barWeight, 2000.0)
            current.copy(
                targetWeight = clamped,
                targetWeightText = formatWeightNumber(clamped),
                loadout = PlateCalculator.calculate(
                    targetWeight = clamped,
                    barWeight = current.barWeight,
                    unit = current.unit,
                    enabledPlates = current.enabledPlates
                ),
                warmupRamp = PlateCalculator.buildWarmupRamp(
                    workingWeight = clamped,
                    barWeight = current.barWeight,
                    unit = current.unit,
                    enabledPlates = current.enabledPlates
                )
            )
        }
    }

    fun selectUnit(unit: WeightUnit) {
        hasManualUnitOrWeightEdit = true
        if (_uiState.value.unit == unit) return
        applyUnitSwitch(unit, keepTargetFromConversion = true)
    }

    fun selectBarWeight(barWeight: Double) {
        hasManualUnitOrWeightEdit = true
        _uiState.update { current ->
            current.copy(
                barWeight = barWeight,
                loadout = PlateCalculator.calculate(
                    targetWeight = current.targetWeight,
                    barWeight = barWeight,
                    unit = current.unit,
                    enabledPlates = current.enabledPlates
                ),
                warmupRamp = PlateCalculator.buildWarmupRamp(
                    workingWeight = current.targetWeight,
                    barWeight = barWeight,
                    unit = current.unit,
                    enabledPlates = current.enabledPlates
                )
            )
        }
    }

    fun togglePlate(plateWeight: Double) {
        hasManualUnitOrWeightEdit = true
        _uiState.update { current ->
            val updatedEnabled = if (plateWeight in current.enabledPlates) {
                current.enabledPlates - plateWeight
            } else {
                current.enabledPlates + plateWeight
            }
            current.copy(
                enabledPlates = updatedEnabled,
                loadout = PlateCalculator.calculate(
                    targetWeight = current.targetWeight,
                    barWeight = current.barWeight,
                    unit = current.unit,
                    enabledPlates = updatedEnabled
                ),
                warmupRamp = PlateCalculator.buildWarmupRamp(
                    workingWeight = current.targetWeight,
                    barWeight = current.barWeight,
                    unit = current.unit,
                    enabledPlates = updatedEnabled
                )
            )
        }
    }

    fun startTimerPreset(seconds: Int) {
        _uiState.update { it.copy(selectedPresetSeconds = seconds) }
        restTimerController.startPreset(seconds)
    }

    fun toggleTimerPause() {
        restTimerController.togglePause()
    }

    fun addTimerSeconds(delta: Int) {
        restTimerController.addSeconds(delta)
    }

    fun resetTimer() {
        restTimerController.reset()
    }

    private fun applyUnitSwitch(newUnit: WeightUnit, keepTargetFromConversion: Boolean) {
        _uiState.update { current ->
            val available = PlateCalculator.availablePlates(newUnit)
            val enabled = available.toSet()
            val bar = PlateCalculator.defaultBarWeight(newUnit)
            val step = PlateCalculator.smallestStep(newUnit, enabled).coerceAtLeast(1.0)
            val newTarget = if (keepTargetFromConversion) {
                val rawConverted = when {
                    current.unit == WeightUnit.KG && newUnit == WeightUnit.LB -> current.targetWeight * 2.20462
                    current.unit == WeightUnit.LB && newUnit == WeightUnit.KG -> current.targetWeight / 2.20462
                    else -> current.targetWeight
                }
                val snapped = (rawConverted / step).roundToInt() * step
                snapped.coerceAtLeast(bar)
            } else {
                defaultTargetFor(newUnit)
            }

            current.copy(
                unit = newUnit,
                targetWeight = newTarget,
                targetWeightText = formatWeightNumber(newTarget),
                barWeight = bar,
                barOptions = PlateCalculator.barWeightOptions(newUnit),
                availablePlates = available,
                enabledPlates = enabled,
                quickWeights = quickWeightsFor(newUnit),
                loadout = PlateCalculator.calculate(newTarget, bar, newUnit, enabled),
                warmupRamp = PlateCalculator.buildWarmupRamp(newTarget, bar, newUnit, enabled)
            )
        }
    }

    private fun defaultTargetFor(unit: WeightUnit): Double = when (unit) {
        WeightUnit.KG -> 100.0
        WeightUnit.LB -> 225.0
    }

    private fun quickWeightsFor(unit: WeightUnit): List<Double> = when (unit) {
        WeightUnit.KG -> listOf(60.0, 100.0, 140.0, 180.0)
        WeightUnit.LB -> listOf(135.0, 225.0, 315.0, 405.0)
    }

    companion object {
        fun formatWeightNumber(value: Double): String {
            val rounded = value.roundToInt()
            return if (abs(value - rounded) < 0.005) {
                rounded.toString()
            } else {
                String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
            }
        }
    }
}
