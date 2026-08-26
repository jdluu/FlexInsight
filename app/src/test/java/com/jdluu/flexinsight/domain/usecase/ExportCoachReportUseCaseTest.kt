package com.jdluu.flexinsight.domain.usecase

import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.data.ai.HevyAiDataAccessor
import com.jdluu.flexinsight.data.health.HealthConnectRepository
import com.jdluu.flexinsight.data.health.HealthConnectSnapshot
import com.jdluu.flexinsight.data.model.PRDetails
import com.jdluu.flexinsight.data.model.WeeklyGoalProgress
import com.jdluu.flexinsight.data.model.WorkoutStats
import com.jdluu.flexinsight.fakes.FakeFlexAIClient
import com.jdluu.flexinsight.fakes.FakeFlexRepository
import com.jdluu.flexinsight.domain.ai.AiContextProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportCoachReportUseCaseTest {

    private val flexRepo = FakeFlexRepository()
    private val aiClient = FakeFlexAIClient()
    private val accessor = mockk<AiContextProvider>()
    private val healthRepo = mockk<HealthConnectRepository>()
    private val originalLocale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        coEvery { healthRepo.readSnapshot() } returns HealthConnectSnapshot()
        coEvery { accessor.buildContext(any()) } returns HevyAiDataAccessor.ContextSnapshot(
            text = "AI CONTEXT",
            hasWorkoutData = true,
            hasApiKey = true,
            workoutCount = 5
        )
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    private fun useCase(): ExportCoachReportUseCase {
        val load = CalculateTrainingLoadUseCase(flexRepo, healthRepo)
        val context = BuildAiContextUseCase(accessor)
        return ExportCoachReportUseCase(flexRepo, context, aiClient, load)
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    @Test
    fun `full report includes summary load prs and ai notes sections`() = runTest {
        flexRepo.statsToReturn = WorkoutStats(
            totalWorkouts = 12, totalVolume = 1200.0, averageVolume = 100.0,
            totalSets = 120, totalDuration = 600L, averageDuration = 50L,
            currentStreak = 5, longestStreak = 9, bestWeekVolume = 800.0, bestWeekDate = null
        )
        flexRepo.goalToReturn = WeeklyGoalProgress(completed = 3, target = 5, status = "Behind")
        flexRepo.prsToReturn = listOf(
            PRDetails("Bench Press", 1700000000000L, "Chest", 100.0, "w1", "s1")
        )
        aiClient.available = true
        aiClient.response = Result.Success("Great week!")

        val report = useCase()()

        val expected =
            "# FlexInsight Weekly Coach Report\n" +
                "Generated: ${today()}\n" +
                "\n" +
                "## Summary\n" +
                "- Total workouts: 12\n" +
                "- Total volume: 1200 kg\n" +
                "- Current streak: 5 days\n" +
                "\n" +
                "## Training load\n" +
                "- Overall: 45/100 (Recovery focus)\n" + // 60*0.5 + 0*0.2 + 50*0.3
                "- Hevy volume 60/100\n" +
                "\n" +
                "## Recent PRs\n" +
                "- Bench Press: 100.0 kg\n" +
                "\n" +
                "## AI Coach Notes\n" +
                "Great week!\n"
        assertEquals(expected, report)
        // PR query uses a hard limit of 10
        assertEquals(listOf(10), flexRepo.requestedPrLimits)
    }

    @Test
    fun `stats failure omits summary section only`() = runTest {
        flexRepo.statsError = IllegalStateException("db down")
        flexRepo.goalToReturn = WeeklyGoalProgress(0, 5, "Behind")

        val report = useCase()()

        assertTrue(!report.contains("## Summary"))
        assertTrue(report.contains("## Training load"))
    }

    @Test
    fun `no PRs omits PR section`() = runTest {
        flexRepo.prsToReturn = emptyList()
        flexRepo.goalToReturn = WeeklyGoalProgress(0, 5, "Behind")

        val report = useCase()()

        assertTrue(!report.contains("## Recent PRs"))
    }

    @Test
    fun `ai unavailable skips notes section entirely`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(0, 5, "Behind")
        aiClient.available = false

        val report = useCase()()

        assertTrue(!report.contains("## AI Coach Notes"))
        // NOTE: possibly unintended - AI context is still built even when the model is unavailable
        coVerify(exactly = 1) { accessor.buildContext(null) }
    }

    @Test
    fun `ai error renders unavailable line with error message`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(0, 5, "Behind")
        aiClient.available = true
        aiClient.response = Result.Error(ApiError.Unknown("model not ready"))

        val report = useCase()()

        assertTrue(report.contains("## AI Coach Notes\nUnavailable (model not ready)\n"))
    }

    @Test
    fun `ai prompt embeds context and coaching instruction`() = runTest {
        flexRepo.goalToReturn = WeeklyGoalProgress(0, 5, "Behind")
        aiClient.available = true

        useCase()()

        assertEquals(
            "AI CONTEXT\n\nWrite a 3-paragraph weekly coaching summary based on this data. Be specific.",
            aiClient.prompts.single()
        )
    }
}
