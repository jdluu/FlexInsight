package com.jdluu.flexinsight.data.repository

import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.data.cache.CacheManager
import com.jdluu.flexinsight.data.cache.CacheStrategy
import com.jdluu.flexinsight.data.local.dao.ExerciseDao
import com.jdluu.flexinsight.data.local.dao.SetDao
import com.jdluu.flexinsight.data.local.dao.WorkoutDao
import com.jdluu.flexinsight.data.local.entity.ExerciseEntity
import com.jdluu.flexinsight.data.local.entity.SetEntity
import com.jdluu.flexinsight.data.local.entity.WorkoutEntity
import com.jdluu.flexinsight.domain.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsQueryRepositoryBatchTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    private fun workout(id: String, startTime: Long, endTime: Long? = startTime + 3_600_000L) = WorkoutEntity(
        id = id,
        name = "Workout $id",
        startTime = startTime,
        endTime = endTime,
        notes = null,
        routineId = null,
        lastSynced = startTime,
        needsSync = false,
        isDeleted = false
    )

    private fun exercise(id: String, workoutId: String, name: String) = ExerciseEntity(
        id = id,
        workoutId = workoutId,
        exerciseTemplateId = "tpl_$id",
        name = name,
        notes = null,
        restDuration = 90,
        lastSynced = 1_000L,
        needsSync = false
    )

    private fun set(id: String, exerciseId: String, weight: Double, reps: Int, isPr: Boolean = false) = SetEntity(
        id = id,
        exerciseId = exerciseId,
        number = 1,
        weight = weight,
        reps = reps,
        rpe = 8.0,
        distance = null,
        duration = null,
        restDuration = null,
        notes = "normal",
        isPersonalRecord = isPr,
        lastSynced = 1_000L,
        needsSync = false
    )

    @Test
    fun `getPlannedWorkoutsForDay and getMuscleGroupProgress use batch IN queries`() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val workoutDao = mockk<WorkoutDao>()
        val exerciseDao = mockk<ExerciseDao>()
        val setDao = mockk<SetDao>()
        val exerciseRepository = mockk<ExerciseRepository>()

        val workouts = listOf(workout("w1", now - 10_000L), workout("w2", now - 20_000L))
        val exercises = listOf(
            exercise("e1", "w1", "Bench Press"),
            exercise("e2", "w2", "Barbell Row")
        )
        val sets = listOf(
            set("s1", "e1", weight = 100.0, reps = 5, isPr = true),
            set("s2", "e2", weight = 80.0, reps = 8)
        )

        every { workoutDao.getWorkoutsByDateRangeFlow(any(), any()) } returns flowOf(workouts)
        every { workoutDao.getWorkoutsSinceFlow(any()) } returns flowOf(workouts)
        every { setDao.getRecentPRsFlow(any()) } returns flowOf(listOf(sets.first()))
        coEvery { exerciseDao.getExercisesByWorkoutIds(listOf("w1", "w2")) } returns exercises
        coEvery { exerciseDao.getExercisesByIds(listOf("e1")) } returns listOf(exercises.first())
        coEvery { workoutDao.getWorkoutsByIds(listOf("w1")) } returns listOf(workouts.first())
        coEvery { setDao.getSetsByExerciseIds(listOf("e1", "e2")) } returns sets
        coEvery { exerciseRepository.getMuscleGroupForExercise(match { it.id == "e1" }) } returns "Chest"
        coEvery { exerciseRepository.getMuscleGroupForExercise(match { it.id == "e2" }) } returns "Back"

        val repo = StatsQueryRepositoryImpl(
            workoutDao = workoutDao,
            exerciseDao = exerciseDao,
            setDao = setDao,
            exerciseRepository = exerciseRepository,
            dispatcherProvider = dispatchers,
            cacheStrategy = CacheStrategy(CacheManager(), dispatchers)
        )

        val planned = repo.getPlannedWorkoutsForDay(now)
        val muscleProgress = repo.getMuscleGroupProgress(weeks = 4)
        val prDetails = repo.getPRsWithDetails(limit = 5)
        val recovery = repo.getMuscleRecoveryStatus()

        assertEquals(2, planned.size)
        assertEquals(2, muscleProgress.size)
        assertEquals(1, prDetails.size)
        assertTrue(recovery.isNotEmpty())

        coVerify(exactly = 0) { exerciseDao.getExercisesByWorkoutId(any()) }
        coVerify(exactly = 0) { setDao.getSetsByExerciseId(any()) }
        coVerify(exactly = 0) { exerciseDao.getExerciseById(any()) }
        coVerify(exactly = 0) { workoutDao.getWorkoutById(any()) }
        coVerify(atLeast = 1) { exerciseDao.getExercisesByWorkoutIds(listOf("w1", "w2")) }
        coVerify(atLeast = 1) { setDao.getSetsByExerciseIds(listOf("e1", "e2")) }
    }
}
