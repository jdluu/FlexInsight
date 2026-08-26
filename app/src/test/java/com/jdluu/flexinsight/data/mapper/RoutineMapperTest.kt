package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.RoutineExerciseResponse
import com.jdluu.flexinsight.data.model.RoutineFolderResponse
import com.jdluu.flexinsight.data.model.RoutineResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineMapperTest {

    // region toRoutine

    @Test
    fun toRoutine_mapsBasicFields() {
        val response = RoutineResponse(
            id = "r1",
            name = "Push Day",
            exerciseCount = 2,
            exercises = null
        )

        val routine = RoutineMapper.toRoutine(response)

        assertEquals("r1", routine.id)
        assertEquals("Push Day", routine.name)
        assertEquals(2, routine.exerciseCount)
        assertNull(routine.exercises)
    }

    @Test
    fun toRoutine_prefersApiTitleOverTemplateMapping() {
        val response = RoutineResponse(
            id = "r1",
            name = "Legs",
            exerciseCount = 1,
            exercises = listOf(
                RoutineExerciseResponse(templateId = "t1", title = "API Title")
            )
        )

        val routine = RoutineMapper.toRoutine(response, mapOf("t1" to "Mapped Title"))

        assertEquals("API Title", routine.exercises?.single()?.name)
        assertEquals("t1", routine.exercises?.single()?.templateId)
    }

    @Test
    fun toRoutine_fallsBackToTemplateMappingWhenTitleNull() {
        val response = RoutineResponse(
            id = "r2",
            name = "Pull Day",
            exerciseCount = 1,
            exercises = listOf(
                RoutineExerciseResponse(templateId = "t2", title = null)
            )
        )

        val routine = RoutineMapper.toRoutine(response, mapOf("t2" to "Mapped Name"))

        assertEquals("Mapped Name", routine.exercises?.single()?.name)
    }

    @Test
    fun toRoutine_nullExercisesStaysNull() {
        val response = RoutineResponse(id = "r3", name = "Rest", exerciseCount = 0, exercises = null)

        val routine = RoutineMapper.toRoutine(response, mapOf("t1" to "Unused"))

        assertNull(routine.exercises)
    }

    @Test
    fun toRoutine_defaultMappingYieldsNullExerciseNames() {
        val response = RoutineResponse(
            id = "r4",
            name = "Arms",
            exerciseCount = 1,
            exercises = listOf(RoutineExerciseResponse(templateId = "t9", title = null))
        )

        val routine = RoutineMapper.toRoutine(response)

        assertNull(routine.exercises?.single()?.name)
    }

    // endregion

    // region toRoutineExercise

    @Test
    fun toRoutineExercise_prefersApiTitle() {
        val exercise = RoutineMapper.toRoutineExercise(
            RoutineExerciseResponse(templateId = "t1", title = "Bench Press"),
            exerciseName = "Fallback"
        )

        assertEquals("t1", exercise.templateId)
        assertEquals("Bench Press", exercise.name)
    }

    @Test
    fun toRoutineExercise_usesFallbackWhenTitleNull() {
        val exercise = RoutineMapper.toRoutineExercise(
            RoutineExerciseResponse(templateId = "t1", title = null),
            exerciseName = "Fallback"
        )

        assertEquals("Fallback", exercise.name)
    }

    // endregion

    // region toRoutineFolder

    @Test
    fun toRoutineFolder_copiesFields() {
        val folder = RoutineMapper.toRoutineFolder(
            RoutineFolderResponse(id = 7, title = "Strength", index = 3, createdAt = "", updatedAt = "")
        )

        assertEquals(7, folder.id)
        assertEquals("Strength", folder.title)
        assertEquals(3, folder.index)
    }

    // endregion
}
