package com.jdluu.flexinsight.domain.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class RestTimerFormatterTest {

    @Test
    fun `formatCountdown zero pads minutes and seconds and clamps negatives`() {
        assertEquals("00:00", RestTimerFormatter.formatCountdown(-10))
        assertEquals("00:00", RestTimerFormatter.formatCountdown(0))
        assertEquals("00:45", RestTimerFormatter.formatCountdown(45))
        assertEquals("01:30", RestTimerFormatter.formatCountdown(90))
        assertEquals("05:00", RestTimerFormatter.formatCountdown(300))
    }

    @Test
    fun `progressFraction bounds between 0 and 1 and handles zero total`() {
        assertEquals(0f, RestTimerFormatter.progressFraction(30, 0), 0.001f)
        assertEquals(0.5f, RestTimerFormatter.progressFraction(45, 90), 0.001f)
        assertEquals(1f, RestTimerFormatter.progressFraction(120, 90), 0.001f)
        assertEquals(0f, RestTimerFormatter.progressFraction(-5, 90), 0.001f)
    }

    @Test
    fun `presetLabel formats short presets in seconds and whole multi-minutes in m`() {
        assertEquals("60s", RestTimerFormatter.presetLabel(60))
        assertEquals("90s", RestTimerFormatter.presetLabel(90))
        assertEquals("2m", RestTimerFormatter.presetLabel(120))
        assertEquals("3m", RestTimerFormatter.presetLabel(180))
        assertEquals("5m", RestTimerFormatter.presetLabel(300))
    }
}
