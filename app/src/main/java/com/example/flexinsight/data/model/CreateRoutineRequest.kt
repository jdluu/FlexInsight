package com.example.flexinsight.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for creating a routine via POST /v1/routines.
 * Wraps the routine data in a top-level "routine" key as required by the Hevy API.
 */
data class CreateRoutineRequest(
    @SerializedName("routine")
    val routine: CreateRoutineBody
)

/**
 * Inner routine body containing the routine details.
 *
 * @param title The display name for the routine.
 * @param folderId Optional folder ID to organize the routine.
 * @param exercises The list of exercises with target sets.
 */
data class CreateRoutineBody(
    @SerializedName("title")
    val title: String,
    @SerializedName("folder_id")
    val folderId: Int? = null,
    @SerializedName("exercises")
    val exercises: List<CreateRoutineExercise>
)

/**
 * An exercise entry within a routine creation request.
 *
 * @param exerciseTemplateId The Hevy exercise template ID.
 * @param sets The target sets for this exercise.
 */
data class CreateRoutineExercise(
    @SerializedName("exercise_template_id")
    val exerciseTemplateId: String,
    @SerializedName("sets")
    val sets: List<CreateRoutineSet>
)

/**
 * A single set within a routine exercise.
 *
 * @param type The set type (e.g., "normal", "warmup", "dropset").
 * @param weightKg Target weight in kilograms.
 * @param reps Target repetitions.
 */
data class CreateRoutineSet(
    @SerializedName("type")
    val type: String = "normal",
    @SerializedName("weight_kg")
    val weightKg: Double? = null,
    @SerializedName("reps")
    val reps: Int? = null
)

/**
 * Response body from POST /v1/routines.
 */
data class CreateRoutineResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("exercise_count")
    val exerciseCount: Int
)
