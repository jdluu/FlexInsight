package com.jdluu.flexinsight.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workoutId"]), Index(value = ["exerciseTemplateId"])]
)
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val workoutId: String,
    val exerciseTemplateId: String?,
    val name: String,
    val notes: String?,
    val restDuration: Int?,
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)
