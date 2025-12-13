package com.example.hevyinsight.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Represents a single set in an exercise
 */
@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId"])]
)
data class Set(
    @PrimaryKey
    val id: String,
    val exerciseId: String,
    val number: Int,
    val weight: Double?,
    val reps: Int?,
    val rpe: Double?,
    val distance: Double?,
    val duration: Int?, // in seconds
    val restDuration: Int?, // in seconds
    val notes: String?,
    val isPersonalRecord: Boolean = false,
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)

/**
 * API response model for Set
 */
data class SetResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("number")
    val number: Int,
    @SerializedName("weight")
    val weight: Double?,
    @SerializedName("reps")
    val reps: Int?,
    @SerializedName("rpe")
    val rpe: Double?,
    @SerializedName("distance")
    val distance: Double?,
    @SerializedName("duration")
    val duration: Int?,
    @SerializedName("rest_duration")
    val restDuration: Int?,
    @SerializedName("notes")
    val notes: String?
) {
    fun toSet(exerciseId: String): Set {
        return Set(
            id = id,
            exerciseId = exerciseId,
            number = number,
            weight = weight,
            reps = reps,
            rpe = rpe,
            distance = distance,
            duration = duration,
            restDuration = restDuration,
            notes = notes,
            isPersonalRecord = false,
            lastSynced = System.currentTimeMillis(),
            needsSync = false
        )
    }
}

