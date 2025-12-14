package com.example.flexinsight.ui.screens.dashboard.parts

import java.util.Locale

/**
 * Format duration in minutes as "Xm" or "Xh Xm"
 */
fun formatDuration(minutes: Long): String {
    if (minutes <= 0) return "0m"
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (remainingMinutes > 0) "${hours}h ${remainingMinutes}m" else "${hours}h"
}

/**
 * Format volume as "X.Xk" for thousands or "X" for regular numbers
 */
fun formatVolume(volume: Double): String {
    if (volume <= 0) return "0"
    if (volume >= 1000) {
        val thousands = volume / 1000.0
        return if (thousands % 1.0 == 0.0) {
            "${thousands.toInt()}k"
        } else {
            String.format(Locale.getDefault(), "%.1fk", thousands)
        }
    }
    return if (volume % 1.0 == 0.0) {
        volume.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", volume)
    }
}

/**
 * Format number with commas if needed
 */
fun formatNumber(value: Int): String {
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}
