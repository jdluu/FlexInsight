package com.jdluu.flexinsight.data.model

import androidx.compose.runtime.Stable
import androidx.room.Embedded
import androidx.room.Relation

/**
 * Relational DTO mapping an Exercise to its Sets.
 */
@Stable
data class ExerciseWithSets(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<Set>
)

/**
 * Relational DTO mapping a Workout to its Exercises and their Sets.
 * This resolves the N+1 query issue for fetching complete workout history.
 */
@Stable
data class WorkoutWithDetails(
    @Embedded val workout: Workout,
    @Relation(
        entity = Exercise::class,
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<ExerciseWithSets>
)
