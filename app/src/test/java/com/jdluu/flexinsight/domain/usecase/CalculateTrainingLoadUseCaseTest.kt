package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.health.HealthConnectSnapshot
import com.jdluu.flexinsight.data.model.WeeklyGoalProgress
import com.jdluu.flexinsight.fakes.FakeFlexRepository
import com.jdluu.flexinsight.fakes.TestDefaults
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CalculateTrainingLoadUseCaseTest {

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

    private fun snapshot(
        sleepHours: Double? = null,
        restingHr: Long? = null,
        steps: Long? = null,
        cardioSessions: Int = 0
    ): HealthConnectSnapshot {
        val snap = HealthConnectSnapshot(
            isAvailable = true,
            isPermissionGranted = true,
            sleepHoursLastNight = sleepHours,
            restingHeartRateBpm = restingHr,
            stepsToday = steps,
            cardioSessionsThisWeek = cardioSessions
        )
        coEvery { healthRepo.readSnapshot() } returns snap
        return snap
    }

    // ---- Hevy volume score ----

    @Test
    fun `volume score derived from weekly goal completion`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(completed = 4, target = 4, status = "On Track")
        snapshot()

        val result = CalculateTrainingLoadUseCase(flexRepo, healthRepo)()

        assertEquals(100, result.hevyVolumeScore)
    }

    @Test
    fun `volume score truncates fractional percentage`() = runTest {
        // 2/3 of goal = 66.67%, integer division via toInt() truncates to 66
        flexRepo.goalToReturn = WeeklyGoalProgress(completed = 2, target = 3, status = "Behind")
        snapshot()

        assertEquals(66, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().hevyVolumeScore)
    }

    @Test
    fun `volume score coerced to 100 when goal exceeded`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(completed = 10, target = 5, status = "On Track")
        snapshot()

        assertEquals(100, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().hevyVolumeScore)
    }

    @Test
    fun `volume score coerced to 0 when nothing completed`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(completed = 0, target = 5, status = "Behind")
        snapshot()

        assertEquals(0, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().hevyVolumeScore)
    }

    @Test
    fun `zero target goal skips goal branch and uses workout count fallback`() = runTest {
        // NOTE: possibly unintended - a configured goal with target == 0 silently falls through
        flexRepo.goalToReturn = WeeklyGoalProgress(completed = 0, target = 0, status = "Behind")
        flexRepo.statsToReturn = TestDefaults.emptyStats.copy(totalWorkouts = 3)
        snapshot()

        assertEquals(65, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().hevyVolumeScore)
    }

    @Test
    fun `goal failure with existing workouts yields default 65`() = runTest {
        flexRepo.goalError = IllegalStateException("db closed")
        flexRepo.statsToReturn = TestDefaults.emptyStats.copy(totalWorkouts = 1)
        snapshot()

        assertEquals(65, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().hevyVolumeScore)
    }

    @Test
    fun `stats and goal failures yield floor score of 30`() = runTest {
        flexRepo.goalError = IllegalStateException("db closed")
        flexRepo.statsError = IllegalStateException("db closed")
        snapshot()

        assertEquals(30, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().hevyVolumeScore)
    }

    // ---- Cardio score ----

    @Test
    fun `cardio score is 15 points per session`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(0, 5, "Behind")
        snapshot(cardioSessions = 4)

        assertEquals(60, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().cardioScore)
    }

    @Test
    fun `cardio score capped at 100`() = runTest {
        // NOTE: possibly unintended - 7 sessions would naively be 105; capped at 100
        snapshot(cardioSessions = 7)

        assertEquals(100, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().cardioScore)
    }

    @Test
    fun `no cardio sessions scores zero`() = runTest {
        snapshot(cardioSessions = 0)

        assertEquals(0, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().cardioScore)
    }

    // ---- Sleep score bands ----

    @Test
    fun `missing sleep data scores neutral 50`() = runTest {
        snapshot(sleepHours = null)

        assertEquals(50, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().sleepScore)
    }

    @Test
    fun `sleep of exactly 7 hours scores 90`() = runTest {
        snapshot(sleepHours = 7.0)

        assertEquals(90, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().sleepScore)
    }

    @Test
    fun `sleep between 6 and 7 hours scores 70`() = runTest {
        snapshot(sleepHours = 6.5)

        assertEquals(70, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().sleepScore)
    }

    @Test
    fun `sleep of exactly 6 hours scores 70 at band lower bound`() = runTest {
        snapshot(sleepHours = 6.0)

        assertEquals(70, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().sleepScore)
    }

    @Test
    fun `sleep between 5 and 6 hours scores 50`() = runTest {
        snapshot(sleepHours = 5.5)

        assertEquals(50, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().sleepScore)
    }

    @Test
    fun `sleep below 5 hours scores 25`() = runTest {
        snapshot(sleepHours = 4.99)

        assertEquals(25, CalculateTrainingLoadUseCase(flexRepo, healthRepo)().sleepScore)
    }

    // ---- Overall score and label ----

    @Test
    fun `high load label at 80 or above`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(4, 4, "On Track")
        snapshot(sleepHours = 8.0, cardioSessions = 4) // 100*0.5 + 60*0.2 + 90*0.3 = 89

        val result = CalculateTrainingLoadUseCase(flexRepo, healthRepo)()

        assertEquals(89, result.overall)
        assertEquals("High load", result.label)
    }

    @Test
    fun `moderate load label between 55 and 79`() = runTest {
        flexRepo.goalError = IllegalStateException("no goal")
        flexRepo.statsToReturn = TestDefaults.emptyStats.copy(totalWorkouts = 3) // volume 65
        snapshot(sleepHours = 6.5, cardioSessions = 1) // 65*0.5 + 15*0.2 + 70*0.3 = 56.5 -> 56

        val result = CalculateTrainingLoadUseCase(flexRepo, healthRepo)()

        assertEquals(56, result.overall)
        assertEquals("Moderate load", result.label)
    }

    @Test
    fun `recovery focus label below 55`() = runTest {
        flexRepo.goalError = IllegalStateException("no goal")
        flexRepo.statsError = IllegalStateException("no stats") // volume 30
        snapshot() // cardio 0, sleep null -> 50
        // 30*0.5 + 0*0.2 + 50*0.3 = 30

        val result = CalculateTrainingLoadUseCase(flexRepo, healthRepo)()

        assertEquals(30, result.overall)
        assertEquals("Recovery focus", result.label)
    }

    @Test
    fun `overall stays recovery focused just under moderate threshold`() = runTest {
        flexRepo.goalError = IllegalStateException("no goal")
        flexRepo.statsError = IllegalStateException("no stats") // volume 30
        snapshot(cardioSessions = 7) // cardio capped 100 -> 30*0.5 + 100*0.2 + 50*0.3 = 50

        val result = CalculateTrainingLoadUseCase(flexRepo, healthRepo)()

        assertEquals(50, result.overall)
        assertEquals("Recovery focus", result.label)
    }

    // ---- Detail string ----

    @Test
    fun `detail omits health parts when snapshot has no data`() = runTest {
        flexRepo.goalError = IllegalStateException("no goal")
        flexRepo.statsToReturn = TestDefaults.emptyStats.copy(totalWorkouts = 2)
        snapshot() // all fields null/zero -> hasData false

        assertEquals(
            "Hevy volume 65/100",
            CalculateTrainingLoadUseCase(flexRepo, healthRepo)().detail
        )
    }

    @Test
    fun `detail includes sleep steps and cardio when present`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(3, 5, "Behind") // volume 60
        snapshot(sleepHours = 7.5, steps = 5000L, cardioSessions = 2)

        assertEquals(
            "Hevy volume 60/100, sleep 7.5h, steps 5000, 2 cardio sessions",
            CalculateTrainingLoadUseCase(flexRepo, healthRepo)().detail
        )
    }

    @Test
    fun `detail ignores resting heart rate presence`() = runTest {
        // hasData becomes true via restingHr alone, but detail never mentions it
        // NOTE: possibly unintended - resting HR counts as "has data" yet never appears in detail
        flexRepo.goalError = IllegalStateException("no goal")
        flexRepo.statsToReturn = TestDefaults.emptyStats.copy(totalWorkouts = 1)
        snapshot(restingHr = 58L)

        assertEquals(
            "Hevy volume 65/100",
            CalculateTrainingLoadUseCase(flexRepo, healthRepo)().detail
        )
    }

    @Test
    fun `detail omits cardio segment when zero sessions`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(1, 5, "Behind") // volume 20
        snapshot(sleepHours = 8.0, steps = 12000L, cardioSessions = 0)

        assertEquals(
            "Hevy volume 20/100, sleep 8.0h, steps 12000",
            CalculateTrainingLoadUseCase(flexRepo, healthRepo)().detail
        )
    }

    // ---- Error propagation asymmetry ----

    @Test
    fun `health snapshot failure is not caught and propagates`() {
        // Unlike the FlexRepository calls, readSnapshot() is not wrapped in runCatching
        coEvery { healthRepo.readSnapshot() } throws RuntimeException("provider crashed")

        assertThrows(RuntimeException::class.java) {
            kotlinx.coroutines.runBlocking {
                CalculateTrainingLoadUseCase(flexRepo, healthRepo)()
            }
        }
    }
}
