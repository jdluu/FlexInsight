package com.jdluu.flexinsight.domain.model

data class Workout(
    val id: String,
    val name: String?,
    val startTime: Long,
    val endTime: Long?,
    val notes: String?,
    val routineId: String?,
    val lastSynced: Long = System.currentTimeMillis(),
    val needsSync: Boolean = false,
    /** Soft-delete when Hevy reports a deleted event; hidden from UI but retained for audit. */
    val isDeleted: Boolean = false
)
