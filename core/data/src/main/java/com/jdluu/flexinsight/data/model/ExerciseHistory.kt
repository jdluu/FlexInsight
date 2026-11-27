package com.jdluu.flexinsight.data.model

import com.google.gson.annotations.SerializedName

/**
 * Exercise history entry DTO from Hevy API
 */
data class ExerciseHistoryEntryDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("workout_id")
    val workoutId: String,
    @SerializedName("exercise_template_id")
    val exerciseTemplateId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("sets")
    val sets: List<SetResponse>?,
    @SerializedName("volume")
    val volume: Double?,
    @SerializedName("one_rep_max")
    val oneRepMax: Double?
)

/**
 * Exercise history API response
 */
data class ExerciseHistoryResponse(
    @SerializedName("exercise_id")
    val exerciseId: String,
    @SerializedName("history")
    val history: List<ExerciseHistoryEntryDto>
)
