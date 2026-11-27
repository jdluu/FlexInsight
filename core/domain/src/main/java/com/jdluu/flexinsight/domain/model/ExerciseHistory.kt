package com.jdluu.flexinsight.domain.model

data class ExerciseHistorySet(
    val weightKg: Double?,
    val reps: Int?
)

data class ExerciseHistoryEntry(
    val id: String,
    val workoutId: String,
    val exerciseTemplateId: String,
    val name: String,
    val date: String,
    val sets: List<ExerciseHistorySet>?,
    val volume: Double?,
    val oneRepMax: Double?
)

data class ExerciseHistory(
    val exerciseId: String,
    val history: List<ExerciseHistoryEntry>
)
