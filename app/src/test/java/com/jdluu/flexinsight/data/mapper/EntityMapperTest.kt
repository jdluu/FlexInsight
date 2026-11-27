package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.local.entity.ExerciseEntity
import com.jdluu.flexinsight.data.local.entity.SetEntity
import com.jdluu.flexinsight.data.local.entity.WorkoutEntity
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMapperTest {

    @Test
    fun workout_roundTripsBetweenEntityAndDomain() {
        val domain = Workout(
            id = "w1",
            name = "Push Day",
            startTime = 1_700_000_000_000L,
            endTime = 1_700_003_600_000L,
            notes = "Felt strong",
            routineId = "r1",
            lastSynced = 12345L,
            needsSync = true,
            isDeleted = false
        )

        val entity: WorkoutEntity = EntityMapper.toEntity(domain)
        val back: Workout = EntityMapper.toDomain(entity)

        assertEquals(domain, back)
    }

    @Test
    fun workout_nullableFieldsPreserveNullOnRoundTrip() {
        val entity = WorkoutEntity(
            id = "w2",
            name = null,
            startTime = 100L,
            endTime = null,
            notes = null,
            routineId = null,
            lastSynced = 200L,
            needsSync = false,
            isDeleted = true
        )

        val domain = EntityMapper.toDomain(entity)

        assertNull(domain.name)
        assertNull(domain.endTime)
        assertNull(domain.notes)
        assertNull(domain.routineId)
        assertTrue(domain.isDeleted)
        assertEquals(entity, EntityMapper.toEntity(domain))
    }

    @Test
    fun exercise_roundTripsBetweenEntityAndDomain() {
        val domain = Exercise(
            id = "e1",
            workoutId = "w1",
            exerciseTemplateId = "tpl-1",
            name = "Bench Press",
            notes = "Pause",
            restDuration = 90,
            lastSynced = 999L,
            needsSync = true
        )

        val entity: ExerciseEntity = EntityMapper.toEntity(domain)
        val back: Exercise = EntityMapper.toDomain(entity)

        assertEquals(domain, back)
    }

    @Test
    fun set_roundTripsBetweenEntityAndDomain() {
        val domain = Set(
            id = "s1",
            exerciseId = "e1",
            number = 2,
            weight = 102.5,
            reps = 5,
            rpe = 8.5,
            distance = null,
            duration = 45,
            restDuration = 120,
            notes = "working",
            isPersonalRecord = true,
            lastSynced = 777L,
            needsSync = false
        )

        val entity: SetEntity = EntityMapper.toEntity(domain)
        val back: Set = EntityMapper.toDomain(entity)

        assertEquals(domain, back)
    }
}
