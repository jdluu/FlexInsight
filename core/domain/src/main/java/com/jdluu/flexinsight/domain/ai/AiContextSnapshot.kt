package com.jdluu.flexinsight.domain.ai

data class AiContextSnapshot(
    val text: String,
    val hasWorkoutData: Boolean,
    val hasApiKey: Boolean,
    val workoutCount: Int,
    /** True when live Hevy exercise_history API was queried for this turn. */
    val usesLiveExerciseHistory: Boolean = false
)
