package com.jdluu.flexinsight.data.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutCountResponseTest {

    private val gson = Gson()

    @Test
    fun `parses workout_count field from Hevy API JSON`() {
        val json = """{"workout_count": 342}"""
        val response = gson.fromJson(json, WorkoutCountResponse::class.java)
        assertEquals(342, response.workoutCount)
    }
}
