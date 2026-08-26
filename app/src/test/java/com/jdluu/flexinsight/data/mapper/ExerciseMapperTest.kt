package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.model.ExerciseTemplateResponse
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
}
