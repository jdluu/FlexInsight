package com.jdluu.flexinsight.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId"])]
)
data class SetEntity(
    @PrimaryKey
    val id: String,
    val exerciseId: String,
    val number: Int,
    val weight: Double?,
    val reps: Int?,
    val rpe: Double?,
    val distance: Double?,
    val duration: Int?,
    val restDuration: Int?,
    val notes: String?,
    val isPersonalRecord: Boolean = false,
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false
)
