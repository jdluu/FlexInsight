package com.example.flexinsight.ui.screens.planner.components

/**
 * Format duration in minutes as "X min"
 */
fun formatDuration(minutes: Long?): String {
    if (minutes == null || minutes <= 0) return "0 min"
    return "${minutes} min"
}
