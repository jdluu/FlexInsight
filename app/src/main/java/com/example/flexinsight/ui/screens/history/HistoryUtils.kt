package com.example.flexinsight.ui.screens.history

import java.text.SimpleDateFormat
import java.util.*

/**
 * Format volume with commas (e.g., "124,500")
 */
fun formatVolumeWithCommas(volume: Double): String {
    return String.format(Locale.US, "%,.0f", volume)
}

/**
 * Format percentage change with sign (e.g., "+12%", "-5%")
 */
fun formatPercentageChange(change: Double): String {
    val sign = if (change > 0) "+" else ""
    return "$sign${String.format(Locale.US, "%.0f", change)}%"
}

/**
 * Format date as "MMM d" (e.g., "Oct 14")
 */
fun formatDateShort(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d", Locale.US)
    return sdf.format(Date(timestamp))
}
