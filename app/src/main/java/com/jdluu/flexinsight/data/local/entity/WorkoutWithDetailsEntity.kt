package com.jdluu.flexinsight.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ExerciseWithSetsEntity(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val sets: List<SetEntity>
)

data class WorkoutWithDetailsEntity(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = ExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<ExerciseWithSetsEntity>
)
