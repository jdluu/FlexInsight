package com.jdluu.flexinsight.data.model

import com.google.gson.annotations.SerializedName

/**
 * API response model for Exercise
 */
data class ExerciseResponse(
    @SerializedName("index")
    val index: Int?,
    @SerializedName("title")
    val title: String,
    @SerializedName("exercise_template_id")
    val exerciseTemplateId: String?,
    @SerializedName("notes")
    val notes: String?,
    @SerializedName("rest_seconds")
    val restSeconds: Int?,
    @SerializedName("sets")
    val sets: List<SetResponse>?
)

/**
 * Exercise template API response
 */
data class ExerciseTemplateResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("muscle_group")
    val muscleGroup: String?
)

/**
 * Paginated response wrapper for exercise templates
 */
data class PaginatedExerciseTemplatesResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_count")
    val pageCount: Int,
    @SerializedName("exercise_templates")
    val exerciseTemplates: List<ExerciseTemplateResponse>?
)
