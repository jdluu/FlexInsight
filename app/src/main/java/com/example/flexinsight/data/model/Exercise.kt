package com.example.flexinsight.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Represents an exercise within a workout
 */
@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workoutId"]), Index(value = ["exerciseTemplateId"])]
)
data class Exercise(
    @PrimaryKey
    val id: String,
    val workoutId: String,
    val exerciseTemplateId: String?,
    val name: String,
    val notes: String?,
    val restDuration: Int?, // in seconds
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

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
) {
    fun toExercise(workoutId: String): Exercise {
        // Generate ID from workout ID and index, or use a hash if index is null
        val exerciseId = if (index != null) {
            "${workoutId}_exercise_$index"
        } else {
            "${workoutId}_exercise_${title.hashCode()}"
        }
        
        return Exercise(
            id = exerciseId,
            workoutId = workoutId,
            exerciseTemplateId = exerciseTemplateId,
            name = title, // API uses "title", we store as "name"
            notes = notes,
            restDuration = restSeconds, // API uses "rest_seconds", we store as "restDuration"
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }
}

/**
 * Exercise template from Hevy API
 */
data class ExerciseTemplate(
    val id: String,
    val name: String,
    val muscleGroup: String?
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
) {
    fun toExerciseTemplate(): ExerciseTemplate {
        return ExerciseTemplate(
            id = id,
            name = title,  // Map title to name for internal model
            muscleGroup = muscleGroup
        )
    }
}

/**
 * Paginated response wrapper for exercise templates
 * Actual API response structure: {"page":1,"page_count":5,"exercise_templates":[...]}
 */
data class PaginatedExerciseTemplatesResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_count")
    val pageCount: Int,
    @SerializedName("exercise_templates")
    val exerciseTemplates: List<ExerciseTemplateResponse>?
)

