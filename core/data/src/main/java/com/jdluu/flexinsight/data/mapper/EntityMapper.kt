package com.jdluu.flexinsight.data.mapper

import com.jdluu.flexinsight.data.local.entity.ExerciseEntity
import com.jdluu.flexinsight.data.local.entity.SetEntity
import com.jdluu.flexinsight.data.local.entity.WorkoutEntity
import com.jdluu.flexinsight.domain.model.Exercise
import com.jdluu.flexinsight.domain.model.Set
import com.jdluu.flexinsight.domain.model.Workout

/**
 * Pure bidirectional mapper between Room database entities and domain models.
 */
object EntityMapper {

    fun toDomain(entity: WorkoutEntity): Workout = Workout(
        id = entity.id,
        name = entity.name,
        startTime = entity.startTime,
        endTime = entity.endTime,
        notes = entity.notes,
        routineId = entity.routineId,
        lastSynced = entity.lastSynced,
        needsSync = entity.needsSync,
        isDeleted = entity.isDeleted
    )

    fun toEntity(domain: Workout): WorkoutEntity = WorkoutEntity(
        id = domain.id,
        name = domain.name,
        startTime = domain.startTime,
        endTime = domain.endTime,
        notes = domain.notes,
        routineId = domain.routineId,
        lastSynced = domain.lastSynced,
        needsSync = domain.needsSync,
        isDeleted = domain.isDeleted
    )

    fun toDomain(entity: ExerciseEntity): Exercise = Exercise(
        id = entity.id,
        workoutId = entity.workoutId,
        exerciseTemplateId = entity.exerciseTemplateId,
        name = entity.name,
        notes = entity.notes,
        restDuration = entity.restDuration,
        lastSynced = entity.lastSynced,
        needsSync = entity.needsSync
    )

    fun toEntity(domain: Exercise): ExerciseEntity = ExerciseEntity(
        id = domain.id,
        workoutId = domain.workoutId,
        exerciseTemplateId = domain.exerciseTemplateId,
        name = domain.name,
        notes = domain.notes,
        restDuration = domain.restDuration,
        lastSynced = domain.lastSynced,
        needsSync = domain.needsSync
    )

    fun toDomain(entity: SetEntity): Set = Set(
        id = entity.id,
        exerciseId = entity.exerciseId,
        number = entity.number,
        weight = entity.weight,
        reps = entity.reps,
        rpe = entity.rpe,
        distance = entity.distance,
        duration = entity.duration,
        restDuration = entity.restDuration,
        notes = entity.notes,
        isPersonalRecord = entity.isPersonalRecord,
        lastSynced = entity.lastSynced,
        needsSync = entity.needsSync
    )

    fun toEntity(domain: Set): SetEntity = SetEntity(
        id = domain.id,
        exerciseId = domain.exerciseId,
        number = domain.number,
        weight = domain.weight,
        reps = domain.reps,
        rpe = domain.rpe,
        distance = domain.distance,
        duration = domain.duration,
        restDuration = domain.restDuration,
        notes = domain.notes,
        isPersonalRecord = domain.isPersonalRecord,
        lastSynced = domain.lastSynced,
        needsSync = domain.needsSync
    )
}
