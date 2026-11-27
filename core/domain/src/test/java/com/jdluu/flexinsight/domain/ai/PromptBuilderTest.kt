package com.jdluu.flexinsight.domain.ai

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

/**
 * Characterization tests: the expected strings are pinned byte-for-byte to the
 * prompt assembly that previously lived in HevyAiDataAccessor.
 */
class PromptBuilderTest {

    private lateinit var builder: PromptBuilder
    private var originalTimeZone: TimeZone? = null
    private var originalLocale: Locale? = null

    @Before
    fun setUp() {
        builder = PromptBuilder()
        originalTimeZone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
        Locale.setDefault(originalLocale)
    }

    // region Short-circuit contexts

    @Test
    fun `no api key context matches legacy text`() {
        val expected = """
            System Context - Hevy Data:
            - API key: Not configured. The user must add a Hevy API key in Settings to sync workout history.
            - Instruction: Explain that personalized coaching requires connecting Hevy, then offer general fitness guidance only.
        """.trimIndent()
        assertEquals(expected, builder.noApiKeyContext())
    }

    @Test
    fun `zero workout context matches legacy text`() {
        val expected = """
            System Context - Hevy Data:
            - API key: Connected.
            - Workouts synced: 0. Data may still be downloading — suggest the user pull to refresh or open Settings to sync.
            - Instruction: Offer general coaching; mention that insights will improve once Hevy workouts sync.
        """.trimIndent()
        assertEquals(expected, builder.zeroWorkoutContext())
    }

    // endregion

    // region Full training context

    @Test
    fun `full context with profile stats recent sessions renders byte identical output`() {
        val output = builder.buildTrainingContext(fullMetricInput())
        assertFalse(output.usesLiveExerciseHistory)
        assertEquals(FULL_METRIC_EXPECTED, output.text)
    }

    @Test
    fun `minimal context omits every optional section`() {
        val input = PromptBuilder.TrainingContextInput(
            workoutCount = 42,
            profile = PromptBuilder.ProfileInput(
                displayName = "Athlete",
                weeklyGoalSessions = 3,
                units = "Metric",
                memberSinceMillis = null
            )
        )
        val output = builder.buildTrainingContext(input)
        assertEquals(
            MINIMAL_EXPECTED,
            output.text
        )
    }

    // endregion

    // region Unit formatting

    @Test
    fun `metric units render kilos unconverted`() {
        val text = builder.buildTrainingContext(fullMetricInput()).text
        assertTrue(text.contains("- Total volume (all time): 123456 kg"))
        assertTrue(text.contains("   * Bench Press: 100 kg 8r | 102 kg 6r @RPE8.0"))
        assertTrue(text.contains("- Bench Press (Chest): 100 kg on 2024-01-01"))
    }

    @Test
    fun `imperial units convert kilos to pounds`() {
        val base = fullMetricInput()
        val imperial = base.copy(
            profile = base.profile.copy(units = "Imperial"),
            exerciseHistoryLookups = listOf(
                PromptBuilder.ExerciseHistoryLookup("Bench Press", promptSuccessOutcome())
            )
        )
        val text = builder.buildTrainingContext(imperial).text
        assertTrue(text.contains("- Preferred units: Imperial"))
        assertTrue(text.contains("- Total volume (all time): 272175 lbs"))
        assertTrue(text.contains("- Average session volume: 5171 lbs"))
        assertTrue(text.contains("   * Bench Press: 220 lbs 8r | 225 lbs 6r @RPE8.0"))
        assertTrue(text.contains("- Bench Press (Chest): 220 lbs on 2024-01-01"))
        assertTrue(text.contains("  2024-01-10: best set 225 lbs x 5 reps e1RM 253 lbs"))
    }

    // endregion

    // region Aggregate stats failure isolation

    @Test
    fun `partial aggregate stats failure keeps captured pieces`() {
        val input = PromptBuilder.TrainingContextInput(
            workoutCount = 5,
            profile = profileInput(),
            trainingSummary = PromptBuilder.TrainingSummary(
                base = PromptBuilder.BaseSummary(10.0, 5.0, 1, 2),
                fourWeekTrendPctChange = null,
                weeklyGoal = null
            )
        )
        val text = builder.buildTrainingContext(input).text
        assertTrue(
            text.contains(
                "\nTraining summary:\n" +
                    "- Total volume (all time): 10 kg\n" +
                    "- Average session volume: 5 kg\n" +
                    "- Current streak: 1 days\n" +
                    "- Longest streak: 2 days\n"
            )
        )
        assertFalse(text.contains("4-week volume trend"))
        assertFalse(text.contains("This week:"))
    }

    // endregion

    // region Exercise history injection

    @Test
    fun `exercise history lookups are injected with live api flag`() {
        val base = fullMetricInput()
        val withLookups = base.copy(
            exerciseHistoryLookups = listOf(
                PromptBuilder.ExerciseHistoryLookup("Bench Press", promptSuccessOutcome()),
                PromptBuilder.ExerciseHistoryLookup(
                    "Overhead Press",
                    PromptBuilder.ExerciseHistoryOutcome.Success(entries = emptyList())
                ),
                PromptBuilder.ExerciseHistoryLookup(
                    "Squat",
                    PromptBuilder.ExerciseHistoryOutcome.Failure(reason = "request timed out")
                ),
                PromptBuilder.ExerciseHistoryLookup(
                    "Deadlift",
                    PromptBuilder.ExerciseHistoryOutcome.Failure(reason = null)
                )
            )
        )
        val output = builder.buildTrainingContext(withLookups)
        assertTrue(output.usesLiveExerciseHistory)
        val expectedSection = listOf(
            "Exercise history for this question (from Hevy API):",
            "- Bench Press:",
            "  2024-01-10: best set 102 kg x 5 reps e1RM 115 kg",
            "  2024-01-03: best set 100 kg x 4 reps",
            "  2024-01-02: best set —",
            "- Overhead Press: no history entries",
            "- Squat: history unavailable (request timed out)",
            "- Deadlift: history unavailable (null)"
        ).joinToString("\n")
        assertTrue(output.text.contains(expectedSection))
    }

    @Test
    fun `history section placed between latest session and health connect`() {
        val input = fullMetricInput().copy(
            exerciseHistoryLookups = listOf(
                PromptBuilder.ExerciseHistoryLookup("Bench Press", promptSuccessOutcome())
            ),
            healthConnect = PromptBuilder.HealthConnectInput.Unavailable
        )
        val text = builder.buildTrainingContext(input).text
        val latestIndex = text.indexOf("Latest session (Jan 15")
        val historyIndex = text.indexOf("Exercise history for this question")
        val healthIndex = text.indexOf("- Health Connect: not available on this device")
        val instructionIndex = text.lastIndexOf("Instruction: You are an expert")
        assertTrue(latestIndex in 0 until historyIndex)
        assertTrue(historyIndex < healthIndex && healthIndex < instructionIndex)
    }

    @Test
    fun `more than five history entries are truncated to five`() {
        val entries = (1..7).map { i ->
            PromptBuilder.HistoryEntryData(
                dateIso = "2024-01-0$i" + "T10:00:00+00:00",
                sets = listOf(PromptBuilder.HistorySetData(weightKg = 50.0, reps = i)),
                oneRepMaxKg = null
            )
        }
        val input = fullMetricInput().copy(
            exerciseHistoryLookups = listOf(
                PromptBuilder.ExerciseHistoryLookup("Bench Press", PromptBuilder.ExerciseHistoryOutcome.Success(entries))
            )
        )
        val text = builder.buildTrainingContext(input).text
        assertTrue(text.contains("  2024-01-05: best set 50 kg x 5 reps"))
        assertFalse(text.contains("2024-01-06: best set"))
    }

    // endregion

    // region Health Connect tri-states

    @Test
    fun `health connect snapshot renders metrics section`() {
        val input = fullMetricInput().copy(
            healthConnect = PromptBuilder.HealthConnectInput.Snapshot(
                sleepHoursLastNight = 7.24,
                restingHeartRateBpm = 55L,
                stepsToday = 8500L,
                activeCaloriesToday = 420.6,
                cardioSessionsThisWeek = 2
            )
        )
        val text = builder.buildTrainingContext(input).text
        val expected = listOf(
            "",
            "Health Connect (last 24h / week):",
            "- Sleep last night: 7.2 hours",
            "- Resting heart rate: 55 bpm",
            "- Steps today: 8500",
            "- Active calories today: 420 kcal",
            "- Non-strength sessions this week: 2"
        ).joinToString("\n")
        assertTrue(text.contains(expected))
    }

    @Test
    fun `health connect permission missing renders single line`() {
        val input = fullMetricInput().copy(healthConnect = PromptBuilder.HealthConnectInput.PermissionMissing)
        val text = builder.buildTrainingContext(input).text
        assertTrue(text.contains("- Health Connect: enabled but permissions not granted"))
        assertFalse(text.contains("Health Connect (last 24h / week):"))
    }

    @Test
    fun `health connect unavailable renders single line`() {
        val input = fullMetricInput().copy(healthConnect = PromptBuilder.HealthConnectInput.Unavailable)
        val text = builder.buildTrainingContext(input).text
        assertTrue(text.contains("- Health Connect: not available on this device"))
        assertFalse(text.contains("Health Connect (last 24h / week):"))
    }

    @Test
    fun `disabled health connect renders nothing`() {
        val input = fullMetricInput().copy(healthConnect = PromptBuilder.HealthConnectInput.Disabled)
        val text = builder.buildTrainingContext(input).text
        assertFalse(text.contains("Health Connect"))
    }

    // endregion

    // region Routines cap

    @Test
    fun `routines are capped at eight entries`() {
        val routines = (1..9).map { i ->
            PromptBuilder.RoutineSection("Routine $i", listOf("Squat"))
        }
        val text = builder.buildTrainingContext(fullMetricInput().copy(routines = routines)).text
        assertTrue(text.contains("- Routine 8: Squat"))
        assertFalse(text.contains("- Routine 9: Squat"))
    }

    // endregion

    // region Truncation

    @Test
    fun `oversized context is cut at token budget with truncation marker`() {
        val longRoutineName = "R".repeat(13_000)
        val input = fullMetricInput().copy(
            routines = listOf(PromptBuilder.RoutineSection(longRoutineName, listOf("Squat")))
        )
        val output = builder.buildTrainingContext(input)
        val suffix = "\n…[context truncated for on-device token limit]"
        assertEquals(12_000 + suffix.length, output.text.length)
        assertTrue(output.text.endsWith(suffix))
        assertTrue(output.text.startsWith("System Context - Hevy Training Data"))
    }

    @Test
    fun `context under budget is never truncated`() {
        val output = builder.buildTrainingContext(fullMetricInput())
        assertFalse(output.text.contains("[context truncated"))
        assertTrue(output.text.length < 12_000)
    }

    // endregion

    // region Query matching

    @Test
    fun `query tokens match candidate exercises case insensitively`() {
        val candidates = listOf(
            PromptBuilder.ExerciseCandidate("Bench Press", "t1"),
            PromptBuilder.ExerciseCandidate("Incline Bench Press", "t2"),
            PromptBuilder.ExerciseCandidate("Leg Press", "t3"),
            PromptBuilder.ExerciseCandidate("Cable Row", "t4"),
            PromptBuilder.ExerciseCandidate("Bench Press Variant", "t1")
        )
        val matches = builder.matchExerciseTemplates("How is my BENCH press going?", candidates)
        assertEquals(listOf("Bench Press", "Incline Bench Press"), matches.map { it.name })
    }

    @Test
    fun `short tokens are ignored in matching`() {
        val candidates = listOf(
            PromptBuilder.ExerciseCandidate("Leg Press", "t3"),
            PromptBuilder.ExerciseCandidate("Leg Curl", "t5")
        )
        val matches = builder.matchExerciseTemplates("leg day was tough", candidates)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `non matching queries return no templates`() {
        val candidates = listOf(PromptBuilder.ExerciseCandidate("Bench Press", "t1"))
        assertTrue(builder.matchExerciseTemplates("what should I eat today?", candidates).isEmpty())
    }

    // endregion

    // region Fixtures

    private fun profileInput() = PromptBuilder.ProfileInput(
        displayName = "Jane Doe",
        weeklyGoalSessions = 4,
        units = "Metric",
        memberSinceMillis = 1_684_281_600_000L
    )

    private fun fullMetricInput(): PromptBuilder.TrainingContextInput {
        val recentWorkouts = listOf(
            PromptBuilder.RecentWorkout(
                startTimeMillis = 1_705_315_200_000L,
                name = "Push Day",
                exercises = listOf(
                    PromptBuilder.ExerciseSets(
                        name = "Bench Press",
                        sets = listOf(
                            PromptBuilder.SetData(weightKg = 100.0, reps = 8, rpe = null),
                            PromptBuilder.SetData(weightKg = 102.5, reps = 6, rpe = 8.0)
                        )
                    ),
                    PromptBuilder.ExerciseSets(name = "Cable Fly", sets = emptyList())
                )
            ),
            PromptBuilder.RecentWorkout(
                startTimeMillis = 1_705_228_800_000L,
                name = "Pull Day",
                exercises = listOf(
                    PromptBuilder.ExerciseSets(
                        name = "Barbell Row",
                        sets = listOf(PromptBuilder.SetData(weightKg = 80.0, reps = 10, rpe = null))
                    )
                )
            )
        )
        return PromptBuilder.TrainingContextInput(
            workoutCount = 42,
            profile = profileInput(),
            trainingSummary = PromptBuilder.TrainingSummary(
                base = PromptBuilder.BaseSummary(
                    totalVolumeKg = 123_456.7,
                    averageSessionVolumeKg = 2_345.9,
                    currentStreakDays = 3,
                    longestStreakDays = 10
                ),
                fourWeekTrendPctChange = 12.34,
                weeklyGoal = PromptBuilder.WeeklyGoalLine(completed = 2, target = 4, status = "On Track")
            ),
            recentWorkouts = recentWorkouts,
            consistencySessionsLast14Days = 9,
            personalRecords = listOf(
                PromptBuilder.PersonalRecord("Bench Press", "Chest", weightKg = 100.0, dateMillis = 1_704_067_200_000L),
                PromptBuilder.PersonalRecord("Squat", "Legs", weightKg = 140.0, dateMillis = 1_703_059_200_000L)
            ),
            highVolumeMuscles = listOf("Chest", "Back"),
            recoveringMuscles = listOf(
                PromptBuilder.RecoveringMuscle("LEGS", 0.3f),
                PromptBuilder.RecoveringMuscle("ARMS", 0.45f)
            ),
            volumeBalance = PromptBuilder.VolumeBalanceSplit(push = 0.4f, pull = 0.35f, legs = 0.25f),
            routines = listOf(
                PromptBuilder.RoutineSection("Push A", listOf("Bench Press", "Incline DB Press")),
                PromptBuilder.RoutineSection("Pull A", listOf("Barbell Row")),
                PromptBuilder.RoutineSection("Core Circuit", null)
            ),
            plannedToday = listOf(
                PromptBuilder.PlannedWorkoutSection("Leg Day", "High Intensity"),
                PromptBuilder.PlannedWorkoutSection("Evening Walk", null)
            ),
            latestSession = PromptBuilder.LatestSession(1_705_315_200_000L, "Push Day")
        )
    }

    private fun promptSuccessOutcome() = PromptBuilder.ExerciseHistoryOutcome.Success(
        listOf(
            PromptBuilder.HistoryEntryData(
                dateIso = "2024-01-10T18:30:00+00:00",
                sets = listOf(
                    PromptBuilder.HistorySetData(weightKg = 102.5, reps = 5),
                    PromptBuilder.HistorySetData(weightKg = 100.0, reps = 4)
                ),
                oneRepMaxKg = 115.0
            ),
            PromptBuilder.HistoryEntryData(
                dateIso = "2024-01-03T17:00:00+00:00",
                sets = listOf(PromptBuilder.HistorySetData(weightKg = 100.0, reps = 4)),
                oneRepMaxKg = null
            ),
            PromptBuilder.HistoryEntryData(
                dateIso = "2024-01-02T09:00:00+00:00",
                sets = null,
                oneRepMaxKg = null
            )
        )
    )

    // endregion

    companion object {
        /** Byte-pinned expected output for [fullMetricInput]; mirrors legacy accessor formatting. */
        private val FULL_METRIC_EXPECTED = listOf(
            "System Context - Hevy Training Data (synced from Hevy API):",
            "- Data source: Local cache of Hevy workouts; refreshed on app sync.",
            "- Total logged workouts: 42",
            "- Name: Jane Doe",
            "- Preferred units: Metric",
            "- Weekly frequency goal: 4 sessions",
            "- Member since: 2023-05-17",
            "",
            "Training summary:",
            "- Total volume (all time): 123456 kg",
            "- Average session volume: 2345 kg",
            "- Current streak: 3 days",
            "- Longest streak: 10 days",
            "- 4-week volume trend: 12.3% vs prior period",
            "- This week: 2/4 workouts (On Track)",
            "",
            "Recently completed workouts (last 7 sessions):",
            "1. 2024-01-15: Push Day",
            "   * Bench Press: 100 kg 8r | 102 kg 6r @RPE8.0",
            "   * Cable Fly: ",
            "2. 2024-01-14: Pull Day",
            "   * Barbell Row: 80 kg 10r",
            "- Consistency: 9 workouts in the last 14 days",
            "",
            "Personal records:",
            "- Bench Press (Chest): 100 kg on 2024-01-01",
            "- Squat (Legs): 140 kg on 2023-12-20",
            "- High recent volume muscles: Chest, Back",
            "- Muscles still recovering (<50%): LEGS (30% recovered), ARMS (45% recovered)",
            "- Push/Pull/Legs volume split (4 wks): Push 40%, Pull 35%, Legs 25%",
            "",
            "Saved Hevy routines:",
            "- Push A: Bench Press, Incline DB Press",
            "- Pull A: Barbell Row",
            "- Core Circuit: —",
            "",
            "Planned for today:",
            "- Leg Day (High Intensity)",
            "- Evening Walk (planned)",
            "",
            "Latest session (Jan 15 — Push Day):",
            "- Coach note: suggest +2.5% weight or +1 rep on main lifts vs that session when programming progress.",
            "",
            "Instruction: You are an expert strength coach. Reference the user's actual Hevy numbers, routines, PRs, and recovery state. Be specific — use exercise names, weights, and dates from above. If asked about data not listed, say what you do have and suggest they log it in Hevy."
        ).joinToString("\n") + "\n"

        private val MINIMAL_EXPECTED = listOf(
            "System Context - Hevy Training Data (synced from Hevy API):",
            "- Data source: Local cache of Hevy workouts; refreshed on app sync.",
            "- Total logged workouts: 42",
            "- Name: Athlete",
            "- Preferred units: Metric",
            "- Weekly frequency goal: 3 sessions",
            "",
            "Instruction: You are an expert strength coach. Reference the user's actual Hevy numbers, routines, PRs, and recovery state. Be specific — use exercise names, weights, and dates from above. If asked about data not listed, say what you do have and suggest they log it in Hevy."
        ).joinToString("\n") + "\n"
    }
}
