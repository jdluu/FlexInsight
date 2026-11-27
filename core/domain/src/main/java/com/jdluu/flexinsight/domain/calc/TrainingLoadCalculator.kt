package com.jdluu.flexinsight.domain.calc

/**
 * Pure composite training-load scoring: Hevy volume attainment, cardio frequency,
 * sleep quality bands, and the weighted blend shown as the daily load score.
 */
object TrainingLoadCalculator {

    private const val VOLUME_WEIGHT = 0.5f
    private const val CARDIO_WEIGHT = 0.2f
    private const val SLEEP_WEIGHT = 0.3f

    private const val FALLBACK_SCORE_WITH_WORKOUTS = 65
    private const val FALLBACK_SCORE_NO_DATA = 30

    /**
     * Hevy volume score. Prefers weekly goal completion (clamped to 0-100); falls back
     * to a fixed score when any workouts exist, and to the floor score otherwise.
     */
    fun hevyVolumeScore(goalCompleted: Int?, goalTarget: Int?, totalWorkouts: Int?): Int = when {
        goalCompleted != null && goalTarget != null && goalTarget > 0 ->
            ((goalCompleted.toFloat() / goalTarget) * 100).toInt().coerceIn(0, 100)
        totalWorkouts != null && totalWorkouts > 0 -> FALLBACK_SCORE_WITH_WORKOUTS
        else -> FALLBACK_SCORE_NO_DATA
    }

    /** Cardio score: 15 points per session this week, capped at 100. */
    fun cardioScore(cardioSessionsThisWeek: Int): Int =
        (cardioSessionsThisWeek * 15).coerceIn(0, 100)

    /** Sleep score banding; missing data scores a neutral 50. */
    fun sleepScore(sleepHoursLastNight: Double?): Int = when (sleepHoursLastNight) {
        null -> 50
        in 7.0..Double.MAX_VALUE -> 90
        in 6.0..7.0 -> 70
        in 5.0..6.0 -> 50
        else -> 25
    }

    /** Weighted blend of volume (50%), cardio (20%), and sleep (30%), clamped to 0-100. */
    fun overall(volumeScore: Int, cardioScore: Int, sleepScore: Int): Int =
        ((volumeScore * VOLUME_WEIGHT) + (cardioScore * CARDIO_WEIGHT) + (sleepScore * SLEEP_WEIGHT))
            .toInt()
            .coerceIn(0, 100)

    /** Label bucket for an overall score. */
    fun label(overall: Int): String = when {
        overall >= 80 -> "High load"
        overall >= 55 -> "Moderate load"
        else -> "Recovery focus"
    }

    /** Human-readable breakdown appended to the load card. */
    fun detail(
        volumeScore: Int,
        hasHealthData: Boolean,
        sleepHoursLastNight: Double?,
        stepsToday: Long?,
        cardioSessionsThisWeek: Int
    ): String = buildString {
        append("Hevy volume $volumeScore/100")
        if (hasHealthData) {
            sleepHoursLastNight?.let { append(", sleep ${"%.1f".format(it)}h") }
            stepsToday?.let { append(", steps $it") }
            if (cardioSessionsThisWeek > 0) {
                append(", $cardioSessionsThisWeek cardio sessions")
            }
        }
    }

    /** Weekly goal status label based on completion ratio. */
    fun goalStatus(completed: Int, target: Int): String = when {
        completed >= target -> "On Track"
        completed >= target * 0.7 -> "On Track"
        completed >= target * 0.5 -> "Behind"
        else -> "Behind"
    }
}
