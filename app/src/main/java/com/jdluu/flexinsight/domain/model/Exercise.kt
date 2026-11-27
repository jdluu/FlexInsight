package com.jdluu.flexinsight.domain.model

data class Exercise(
    val id: String,
    val workoutId: String,
    val exerciseTemplateId: String?,
    val name: String,
    val notes: String?,
    val restDuration: Int?,
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

data class ExerciseTemplate(
    val id: String,
    val name: String,
    val muscleGroup: String?
)
