package com.jdluu.flexinsight.domain.calc

import com.jdluu.flexinsight.data.model.Set

/**
 * Pure personal-record math: best-set tonnage and PR-flag detection.
 */
object PersonalRecordCalculator {

    /** Heaviest single set by tonnage; empty input yields 0.0. */
    fun bestSetVolume(sets: List<Set>): Double =
        sets.maxOfOrNull { VolumeCalculator.setVolume(it.weight, it.reps) } ?: 0.0

    /** True when any set carries the personal-record flag. */
    fun hasPersonalRecord(sets: List<Set>): Boolean = sets.any { it.isPersonalRecord }
}
