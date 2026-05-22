package com.jdluu.flexinsight.data.health

/**
 * Aggregated Health Connect metrics for recovery, AI context, and training load fusion.
 */
data class HealthConnectSnapshot(
    val isAvailable: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val sleepHoursLastNight: Double? = null,
    val restingHeartRateBpm: Long? = null,
    val stepsToday: Long? = null,
    val activeCaloriesToday: Double? = null,
    val cardioSessionsThisWeek: Int = 0,
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    val hasData: Boolean
        get() = sleepHoursLastNight != null || restingHeartRateBpm != null ||
            stepsToday != null || cardioSessionsThisWeek > 0
}
