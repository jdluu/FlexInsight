package com.example.hevyinsight.data.model

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
    @SerializedName("id")
    val id: String,
    @SerializedName("exercise_template_id")
    val exerciseTemplateId: String?,
    @SerializedName("name")
    val name: String,
    @SerializedName("notes")
    val notes: String?,
    @SerializedName("rest_duration")
    val restDuration: Int?,
    @SerializedName("sets")
    val sets: List<SetResponse>?
) {
    fun toExercise(workoutId: String): Exercise {
        return Exercise(
            id = id,
            workoutId = workoutId,
            exerciseTemplateId = exerciseTemplateId,
            name = name,
            notes = notes,
            restDuration = restDuration,
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
    @SerializedName("name")
    val name: String,
    @SerializedName("muscle_group")
    val muscleGroup: String?
) {
    fun toExerciseTemplate(): ExerciseTemplate {
        return ExerciseTemplate(
            id = id,
            name = name,
            muscleGroup = muscleGroup
        )
    }
}

