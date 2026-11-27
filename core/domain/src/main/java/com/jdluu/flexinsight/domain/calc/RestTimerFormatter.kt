package com.jdluu.flexinsight.domain.calc

/**
 * Pure formatting and progress math for gym-floor rest timers.
 */
object RestTimerFormatter {

    val DEFAULT_PRESETS_SECONDS: List<Int> = listOf(60, 90, 120, 180, 300)

    fun formatCountdown(totalSeconds: Int): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        val minutes = clamped / 60
        val seconds = clamped % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun progressFraction(remainingSeconds: Int, totalSeconds: Int): Float {
        if (totalSeconds <= 0) return 0f
        return (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }

    fun presetLabel(seconds: Int): String {
        val clamped = seconds.coerceAtLeast(0)
        return if (clamped >= 120 && clamped % 60 == 0) {
            "${clamped / 60}m"
        } else {
            "${clamped}s"
        }
    }
}
