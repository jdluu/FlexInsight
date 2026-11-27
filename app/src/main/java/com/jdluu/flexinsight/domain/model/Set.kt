package com.jdluu.flexinsight.domain.model

data class Set(
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
