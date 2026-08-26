package com.jdluu.flexinsight.domain.usecase

import app.cash.turbine.test
import com.jdluu.flexinsight.fakes.FakeWorkoutRepository
import com.jdluu.flexinsight.fakes.TestDefaults.exercise
import com.jdluu.flexinsight.fakes.TestDefaults.set
import com.jdluu.flexinsight.fakes.TestDefaults.workout
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareRoutineSessionsUseCaseTest {

    private val repo = FakeWorkoutRepository()
    private val useCase = CompareRoutineSessionsUseCase(repo)

    private companion object {
        const val ROUTINE = "r1"
        const val ROUTINE_NAME = "Push Day"
        const val T0 = 1_700_000_000_000L
        const val T1 = 1_700_086_400_000L
        const val T2 = 1_700_172_800_000L
        const val T3 = 1_700_259_200_000L
    }

    private fun seedTwoSessionScenario(
        recentName: String = "Bench Press",
        previousName: String = "Bench Press",
        recentSets: List<com.jdluu.flexinsight.data.model.Set>,
        previousSets: List<com.jdluu.flexinsight.data.model.Set>
    ) {
        repo.workoutsFlow.value = listOf(workout("rw", startTime = T2, routineId = ROUTINE))
        repo.workoutsFlow.value += workout("pw", startTime = T1, routineId = ROUTINE)
        repo.exercisesByWorkout["rw"] = listOf(exercise("re", "rw", recentName))
        repo.exercisesByWorkout["pw"] = listOf(exercise("pe", "pw", previousName))
        repo.setsByExercise["re"] = recentSets
        repo.setsByExercise["pe"] = previousSets
    }

    // ---- Guard clauses ----

    @Test
    fun `null routine id returns null without comparing`() = runTest {
        assertNull(useCase(routineId = null, routineName = ROUTINE_NAME))
    }

    @Test
    fun `single session for routine returns null`() = runTest {
        repo.workoutsFlow.value = listOf(workout("w1", startTime = T1, routineId = ROUTINE))

        assertNull(useCase(ROUTINE, ROUTINE_NAME))
    }

    @Test
    fun `no sessions at all returns null`() = runTest {
        assertNull(useCase(ROUTINE, ROUTINE_NAME))
    }

    @Test
    fun `deleted workouts are excluded from pairing`() = runTest {
        repo.workoutsFlow.value = listOf(
            workout("deleted-recent", startTime = T3, routineId = ROUTINE, isDeleted = true),
            workout("recent", startTime = T2, routineId = ROUTINE),
            workout("previous", startTime = T1, routineId = ROUTINE)
        )
        repo.exercisesByWorkout["recent"] = listOf(exercise("re", "recent", "Bench Press"))
        repo.exercisesByWorkout["previous"] = listOf(exercise("pe", "previous", "Bench Press"))
        repo.setsByExercise["re"] = listOf(set("s1", "re", 100.0, 10)) // 1000
        repo.setsByExercise["pe"] = listOf(set("s2", "pe", 100.0, 10)) // 1000

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertTrue(result!!.improvements.isEmpty() && result.regressions.isEmpty())
    }

    // ---- Pairing ----

    @Test
    fun `compares the two most recent sessions only`() = runTest {
        repo.workoutsFlow.value = listOf(
            workout("old", startTime = T0, routineId = ROUTINE),
            workout("previous", startTime = T1, routineId = ROUTINE),
            workout("recent", startTime = T2, routineId = ROUTINE)
        )
        repo.exercisesByWorkout["recent"] = listOf(exercise("re", "recent", "Bench Press"))
        repo.exercisesByWorkout["previous"] = listOf(exercise("pe", "previous", "Bench Press"))
        repo.setsByExercise["re"] = listOf(set("s1", "re", 120.0, 10)) // 1200
        repo.setsByExercise["pe"] = listOf(set("s2", "pe", 100.0, 10)) // 1000

        // If "old" were used as baseline instead of "previous", the outcome would differ;
        // with correct pairing this is a clean improvement against the immediate prior run.
        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertEquals(listOf("Bench Press: volume up"), result!!.improvements)
    }

    @Test
    fun `workouts from other routines are ignored`() = runTest {
        repo.workoutsFlow.value = listOf(
            workout("other-routine", startTime = T2, routineId = "r2"),
            workout("recent", startTime = T2 - 1000, routineId = ROUTINE),
            workout("previous", startTime = T1, routineId = ROUTINE)
        )
        repo.exercisesByWorkout["recent"] = listOf(exercise("re", "recent", "Bench Press"))
        repo.exercisesByWorkout["previous"] = listOf(exercise("pe", "previous", "Bench Press"))
        repo.setsByExercise["re"] = listOf(set("s1", "re", 100.0, 10))
        repo.setsByExercise["pe"] = listOf(set("s2", "pe", 100.0, 10))

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertTrue(result != null && result.improvements.isEmpty() && result.regressions.isEmpty())
    }

    @Test
    fun `exercise names match ignoring case`() = runTest {
        seedTwoSessionScenario(
            recentName = "INCLINE BENCH PRESS",
            previousName = "Incline Bench Press",
            recentSets = listOf(set("s1", "re", 80.0, 8)), // 640
            previousSets = listOf(set("s2", "pe", 60.0, 8)) // 480
        )

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertEquals(listOf("INCLINE BENCH PRESS: volume up"), result!!.improvements)
    }

    @Test
    fun `recent exercise without previous counterpart is skipped`() = runTest {
        seedTwoSessionScenario(
            recentName = "Brand New Lift",
            previousName = "Something Else",
            recentSets = listOf(set("s1", "re", 500.0, 5)),
            previousSets = listOf(set("s2", "pe", 100.0, 10))
        )

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertTrue(result!!.improvements.isEmpty() && result.regressions.isEmpty())
    }

    // ---- Best-set thresholds ----

    @Test
    fun `more than two percent above baseline counts as improvement`() = runTest {
        seedTwoSessionScenario(
            recentSets = listOf(set("s1", "re", 102.1, 10)), // 1021
            previousSets = listOf(set("s2", "pe", 100.0, 10)) // 1000
        )

        assertEquals(listOf("Bench Press: volume up"), useCase(ROUTINE, ROUTINE_NAME)!!.improvements)
    }

    @Test
    fun `exactly two percent above baseline is considered stable`() = runTest {
        seedTwoSessionScenario(
            recentSets = listOf(set("s1", "re", 102.0, 10)), // 1020 == 1000 * 1.02
            previousSets = listOf(set("s2", "pe", 100.0, 10)) // 1000
        )

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertTrue(result!!.improvements.isEmpty())
        assertEquals("Performance stable between last two $ROUTINE_NAME sessions.", result.summary)
    }

    @Test
    fun `more than two percent below baseline counts as regression`() = runTest {
        seedTwoSessionScenario(
            recentSets = listOf(set("s1", "re", 97.9, 10)), // 979
            previousSets = listOf(set("s2", "pe", 100.0, 10)) // 1000
        )

        assertEquals(
            listOf("Bench Press: volume down"),
            useCase(ROUTINE, ROUTINE_NAME)!!.regressions
        )
    }

    @Test
    fun `exactly two percent below baseline is considered stable`() = runTest {
        seedTwoSessionScenario(
            recentSets = listOf(set("s1", "re", 98.0, 10)), // 980 == 1000 * 0.98
            previousSets = listOf(set("s2", "pe", 100.0, 10)) // 1000
        )

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertTrue(result!!.regressions.isEmpty())
    }

    @Test
    fun `best set across multiple sets determines direction`() = runTest {
        seedTwoSessionScenario(
            recentSets = listOf(
                set("s1", "re", 80.0, 10),   // 800
                set("s2", "re", 91.0, 10)    // 910 <- best
            ),
            previousSets = listOf(
                set("s3", "pe", 90.0, 10),   // 900 <- best
                set("s4", "pe", 70.0, 12)    // 840
            )
        )
        // 910 vs 900: within +/-2% -> stable

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertTrue(result!!.improvements.isEmpty() && result.regressions.isEmpty())
    }

    // ---- Null / zero data handling ----

    @Test
    fun `sets with null weight or reps contribute zero volume`() = runTest {
        // NOTE: possibly unintended - bodyweight/duration-only sets score as 0 kg-volume,
        // so an all-bodyweight session always reads as "volume down" against weighted work
        seedTwoSessionScenario(
            recentSets = listOf(set("s1", "re", null, 15)),
            previousSets = listOf(set("s2", "pe", 50.0, 10))
        )

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertEquals(listOf("Bench Press: volume down"), result!!.regressions)
    }

    @Test
    fun `sessions with no sets compare as zeros and are stable`() = runTest {
        seedTwoSessionScenario(recentSets = emptyList(), previousSets = emptyList())

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertTrue(result!!.improvements.isEmpty() && result.regressions.isEmpty())
        assertEquals("Performance stable between last two $ROUTINE_NAME sessions.", result.summary)
    }

    // ---- Summary wording ----

    @Test
    fun `summary reports regressions when nothing improved`() = runTest {
        seedTwoSessionScenario(
            recentSets = listOf(set("s1", "re", 50.0, 10)), // 500
            previousSets = listOf(set("s2", "pe", 100.0, 10)) // 1000
        )

        assertEquals(
            "Latest $ROUTINE_NAME session shows regressions vs the prior run.",
            useCase(ROUTINE, ROUTINE_NAME)!!.summary
        )
    }

    @Test
    fun `summary reports improvement when nothing regressed`() = runTest {
        seedTwoSessionScenario(
            recentSets = listOf(set("s1", "re", 110.0, 10)), // 1100
            previousSets = listOf(set("s2", "pe", 100.0, 10)) // 1000
        )

        assertEquals(
            "Latest $ROUTINE_NAME session improved across key lifts.",
            useCase(ROUTINE, ROUTINE_NAME)!!.summary
        )
    }

    @Test
    fun `summary reports mixed performance when both directions occur`() = runTest {
        repo.workoutsFlow.value = listOf(
            workout("rw", startTime = T2, routineId = ROUTINE),
            workout("pw", startTime = T1, routineId = ROUTINE)
        )
        repo.exercisesByWorkout["rw"] = listOf(
            exercise("up", "rw", "Overhead Press"),
            exercise("down", "rw", "Squat")
        )
        repo.exercisesByWorkout["pw"] = listOf(
            exercise("up-prev", "pw", "Overhead Press"),
            exercise("down-prev", "pw", "Squat")
        )
        repo.setsByExercise["up"] = listOf(set("s1", "up", 60.0, 10)) // 600
        repo.setsByExercise["up-prev"] = listOf(set("s2", "up-prev", 50.0, 10)) // 500 -> up
        repo.setsByExercise["down"] = listOf(set("s3", "down", 80.0, 10)) // 800
        repo.setsByExercise["down-prev"] = listOf(set("s4", "down-prev", 100.0, 10)) // 1000 -> down

        val result = useCase(ROUTINE, ROUTINE_NAME)

        assertEquals(
            "Mixed performance on $ROUTINE_NAME \u2014 review flagged lifts.",
            result!!.summary
        )
        assertEquals(listOf("Overhead Press: volume up"), result.improvements)
        assertEquals(listOf("Squat: volume down"), result.regressions)
    }

    @Test
    fun `comparison payload echoes routine name`() = runTest {
        seedTwoSessionScenario(
            recentSets = listOf(set("s1", "re", 100.0, 10)),
            previousSets = listOf(set("s2", "pe", 100.0, 10))
        )

        val result = useCase(ROUTINE, "Heavy Upper")

        assertEquals("Heavy Upper", result!!.routineName)
    }

    // ---- Flow consumption ----

    @Test
    fun `use case consumes latest emission of repository workouts flow`() = runTest {
        repo.workoutsFlow.test {
            assertEquals(emptyList<com.jdluu.flexinsight.data.model.Workout>(), awaitItem())

            repo.workoutsFlow.value = listOf(
                workout("recent", startTime = T2, routineId = ROUTINE),
                workout("previous", startTime = T1, routineId = ROUTINE)
            )
            assertEquals(2, awaitItem().size)
        }
        // first() inside the use case must observe the emitted pair
        val result = useCase(ROUTINE, ROUTINE_NAME)
        assertTrue(result != null)
    }
}
