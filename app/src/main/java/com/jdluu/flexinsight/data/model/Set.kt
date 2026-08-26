package com.jdluu.flexinsight.data.model

import androidx.compose.runtime.Stable
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
@Stable
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
    @SerializedName("index")
    val index: Int,
    @SerializedName("type")
    val type: String?, // "warmup", "normal", "failure", "dropset"
    @SerializedName("weight_kg")
    val weightKg: Double?,
    @SerializedName("reps")
    val reps: Int?,
    @SerializedName("rpe")
    val rpe: Double?,
    @SerializedName("distance_meters")
    val distanceMeters: Double?,
    @SerializedName("duration_seconds")
    val durationSeconds: Int?,
    @SerializedName("custom_metric")
    val customMetric: Double?,
    @SerializedName("personal_record")
    val personalRecord: Boolean? = false
)

