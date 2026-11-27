package com.jdluu.flexinsight.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workouts",
    indices = [Index(value = ["startTime"]), Index(value = ["lastSynced"])]
)
data class WorkoutEntity(
    @PrimaryKey
    val id: String,
    val name: String?,
    val startTime: Long,
    val endTime: Long?,
    val notes: String?,
    val routineId: String?,
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false,
    val isDeleted: Boolean = false
)
