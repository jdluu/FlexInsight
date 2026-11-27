package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.ExerciseHistoryEntryDto
import com.jdluu.flexinsight.data.model.ExerciseHistoryResponse
import com.jdluu.flexinsight.data.model.ExerciseTemplateResponse
import com.jdluu.flexinsight.data.model.SetResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseMapperTest {

    @Test
    fun toExerciseTemplate_mapsTitleToName() {
        val template = ExerciseMapper.toExerciseTemplate(
            ExerciseTemplateResponse(id = "tpl-1", title = "Barbell Bench Press", muscleGroup = "Chest")
        )

        assertEquals("tpl-1", template.id)
        assertEquals("Barbell Bench Press", template.name)
        assertEquals("Chest", template.muscleGroup)
    }

    @Test
    fun toExerciseTemplate_nullMuscleGroupStaysNull() {
        val template = ExerciseMapper.toExerciseTemplate(
            ExerciseTemplateResponse(id = "tpl-2", title = "Unknown Move", muscleGroup = null)
        )

        assertEquals("Unknown Move", template.name)
        assertNull(template.muscleGroup)
    }

    @Test
    fun toExerciseHistory_mapsEntriesAndSets() {
        val response = ExerciseHistoryResponse(
            exerciseId = "tpl-1",
            history = listOf(
                ExerciseHistoryEntryDto(
                    id = "h1",
                    workoutId = "w1",
                    exerciseTemplateId = "tpl-1",
                    name = "Bench Press",
                    date = "2026-01-10T10:00:00Z",
                    sets = listOf(
                        SetResponse(
                            index = 0,
                            type = "normal",
                            weightKg = 100.0,
                            reps = 5,
                            rpe = 8.0,
                            distanceMeters = null,
                            durationSeconds = null,
                            customMetric = null
                        )
                    ),
                    volume = 500.0,
                    oneRepMax = 116.7
                ),
                ExerciseHistoryEntryDto(
                    id = "h2",
                    workoutId = "w2",
                    exerciseTemplateId = "tpl-1",
                    name = "Bench Press",
                    date = "2026-01-15T10:00:00Z",
                    sets = null,
                    volume = 0.0,
                    oneRepMax = null
                )
            )
        )

        val domain = ExerciseMapper.toExerciseHistory(response)

        assertEquals("tpl-1", domain.exerciseId)
        assertEquals(2, domain.history.size)
        assertEquals(100.0, domain.history[0].sets!!.single().weightKg!!, 0.0)
        assertEquals(5, domain.history[0].sets!!.single().reps)
        assertEquals(116.7, domain.history[0].oneRepMax!!, 0.0)
        assertNull(domain.history[1].sets)
        assertNull(domain.history[1].oneRepMax)
    }
}
