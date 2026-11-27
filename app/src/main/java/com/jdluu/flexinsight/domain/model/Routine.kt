package com.jdluu.flexinsight.domain.model

data class RoutineExercise(
    val templateId: String,
    val name: String?
)

data class Routine(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val exercises: List<RoutineExercise>?
)

data class RoutineFolder(
    val id: Int,
    val title: String,
    val index: Int
)

data class CreateRoutineDraft(
    val title: String,
    val folderId: Int? = null,
    val exercises: List<CreateRoutineExerciseDraft>
)

data class CreateRoutineExerciseDraft(
    val exerciseTemplateId: String,
    val sets: List<CreateRoutineSetDraft>
)

data class CreateRoutineSetDraft(
    val type: String = "normal",
    val weightKg: Double? = null,
    val reps: Int? = null
)
