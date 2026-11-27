package com.jdluu.flexinsight.ui.screens.history.parts

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(ZoneId.systemDefault())

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
    return SHORT_DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp))
}
