package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.health.HealthConnectSnapshot
import com.jdluu.flexinsight.data.model.DayInfo
import com.jdluu.flexinsight.data.model.VolumeTrend
import com.jdluu.flexinsight.fakes.FakeFlexRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DetectDeloadUseCaseTest {

    private lateinit var flexRepo: FakeFlexRepository
    private val healthRepo = mockk<HealthConnectRepository>()
    private val originalLocale = Locale.getDefault()

    @Before
    fun setUp() {
        flexRepo = FakeFlexRepository()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    private fun health(sleepHours: Double? = null, restingHr: Long? = null) {
        coEvery { healthRepo.readSnapshot() } returns HealthConnectSnapshot(
            isAvailable = true,
            isPermissionGranted = true,
            sleepHoursLastNight = sleepHours,
            restingHeartRateBpm = restingHr
        )
    }

    private fun daysWithWorkouts(count: Int): List<DayInfo> =
        (1..count).map { DayInfo("Day$it", it, it.toLong(), hasWorkout = true, isCompleted = true, workoutCount = 1) }

    private fun fullyLoadedSetup(trendPct: Double = 20.0, sessions: Int = 10) {
        flexRepo.trendToReturn = VolumeTrend(1200.0, 1000.0, trendPct)
        flexRepo.consistencyToReturn = daysWithWorkouts(sessions)
        health(sleepHours = 5.0)
    }

    @Test
    fun `flags deload when all signals align`() = runTest {
        fullyLoadedSetup(trendPct = 20.0)

        val alert = DetectDeloadUseCase(flexRepo, healthRepo)()

        assertEquals(true, alert.shouldDeload)
        assertEquals(
            "Volume is up 20% while recovery signals are down. " +
                "Consider a deload week: reduce intensity 30\u201340% and prioritize sleep.",
            alert.message
        )
    }

    @Test
    fun `message rounds trend percentage to whole number`() = runTest {
        // %.0f on 7.6 renders as "8"
        fullyLoadedSetup(trendPct = 7.6)

        val alert = DetectDeloadUseCase(flexRepo, healthRepo)()

        assertEquals(true, alert.message.contains("Volume is up 8%"))
    }

    @Test
    fun `trend rise of exactly 5 percent does not trigger`() = runTest {
        // Threshold is strictly greater than 5.0
        fullyLoadedSetup(trendPct = 5.0, sessions = 10)

        val alert = DetectDeloadUseCase(flexRepo, healthRepo)()

        assertEquals(false, alert.shouldDeload)
    }

    @Test
    fun `exactly eight sessions counts as high load`() = runTest {
        fullyLoadedSetup(trendPct = 10.0, sessions = 8)

        assertEquals(true, DetectDeloadUseCase(flexRepo, healthRepo)().shouldDeload)
    }

    @Test
    fun `seven sessions is below high load threshold`() = runTest {
        fullyLoadedSetup(trendPct = 10.0, sessions = 7)

        assertEquals(false, DetectDeloadUseCase(flexRepo, healthRepo)().shouldDeload)
    }

    @Test
    fun `poor sleep alone satisfies recovery signal requirement`() = runTest {
        fullyLoadedSetup()
        health(sleepHours = 5.9, restingHr = null)

        assertEquals(true, DetectDeloadUseCase(flexRepo, healthRepo)().shouldDeload)
    }

    @Test
    fun `elevated resting heart rate alone satisfies recovery signal requirement`() = runTest {
        fullyLoadedSetup()
        health(sleepHours = 8.0, restingHr = 76L)

        assertEquals(true, DetectDeloadUseCase(flexRepo, healthRepo)().shouldDeload)
    }

    @Test
    fun `resting heart rate of exactly 75 is not considered elevated`() = runTest {
        fullyLoadedSetup()
        health(sleepHours = 8.0, restingHr = 75L)

        assertEquals(false, DetectDeloadUseCase(flexRepo, healthRepo)().shouldDeload)
    }

    @Test
    fun `no health data prevents deload even with strong volume signal`() = runTest {
        fullyLoadedSetup()
        health(sleepHours = null, restingHr = null)

        val alert = DetectDeloadUseCase(flexRepo, healthRepo)()

        assertEquals(false, alert.shouldDeload)
        assertEquals("", alert.message)
    }

    @Test
    fun `flat or falling volume never triggers regardless of recovery`() = runTest {
        flexRepo.trendToReturn = VolumeTrend(900.0, 1000.0, -10.0)
        flexRepo.consistencyToReturn = daysWithWorkouts(12)
        health(sleepHours = 4.0, restingHr = 90L)

        assertEquals(false, DetectDeloadUseCase(flexRepo, healthRepo)().shouldDeload)
    }

    @Test
    fun `volume trend failure treated as no trend`() = runTest {
        flexRepo.trendError = IllegalStateException("boom")
        flexRepo.consistencyToReturn = daysWithWorkouts(12)
        health(sleepHours = 4.0)

        assertEquals(false, DetectDeloadUseCase(flexRepo, healthRepo)().shouldDeload)
    }

    @Test
    fun `consistency failure treated as zero sessions`() = runTest {
        flexRepo.trendToReturn = VolumeTrend(1200.0, 1000.0, 15.0)
        flexRepo.consistencyError = IllegalStateException("boom")
        health(sleepHours = 4.0)

        assertEquals(false, DetectDeloadUseCase(flexRepo, healthRepo)().shouldDeload)
    }

    @Test
    fun `consistency window requested is 14 days`() = runTest {
        flexRepo.trendToReturn = VolumeTrend(1200.0, 1000.0, 15.0)
        flexRepo.consistencyToReturn = daysWithWorkouts(8)
        health(sleepHours = 5.0)

        DetectDeloadUseCase(flexRepo, healthRepo)()

        assertEquals(listOf(14), flexRepo.requestedConsistencyDays)
    }
}
