package com.jdluu.flexinsight.data.local.dao

import androidx.room.*
import com.jdluu.flexinsight.data.local.entity.ExerciseEntity
import com.jdluu.flexinsight.data.local.entity.SetEntity
import com.jdluu.flexinsight.data.local.entity.WorkoutEntity
import com.jdluu.flexinsight.data.local.entity.WorkoutWithDetailsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE isDeleted = 0 ORDER BY startTime DESC")
    fun getAllWorkoutsFlow(): Flow<List<WorkoutEntity>>

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    fun getAllWorkoutsWithDetailsFlow(): Flow<List<WorkoutWithDetailsEntity>>

    @Query("SELECT * FROM workouts WHERE isDeleted = 0 ORDER BY startTime DESC LIMIT :limit")
    fun getRecentWorkoutsFlow(limit: Int): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: String): WorkoutEntity?

    @Query("SELECT id FROM workouts WHERE id IN (:ids)")
    suspend fun getExistingWorkoutIds(ids: List<String>): List<String>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutByIdFlow(workoutId: String): Flow<WorkoutEntity?>

    @Query("SELECT COUNT(*) FROM workouts WHERE isDeleted = 0")
    fun getWorkoutCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workouts WHERE isDeleted = 0")
    suspend fun getWorkoutCount(): Int

    @Query("SELECT MAX(lastSynced) FROM workouts")
    suspend fun getMostRecentSyncedTimestamp(): Long?

    @Query("SELECT * FROM workouts WHERE startTime >= :startTimestamp AND startTime <= :endTimestamp ORDER BY startTime DESC")
    fun getWorkoutsByDateRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE startTime >= :startTimestamp ORDER BY startTime DESC")
    fun getWorkoutsSinceFlow(startTimestamp: Long): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE needsSync = 1")
    suspend fun getWorkoutsNeedingSync(): List<WorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("UPDATE workouts SET isDeleted = 1 WHERE id = :workoutId")
    suspend fun softDeleteWorkoutById(workoutId: String)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: String)

    @Query("UPDATE workouts SET lastSynced = :timestamp, needsSync = 0 WHERE id = :workoutId")
    suspend fun markWorkoutSynced(workoutId: String, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun insertWorkoutWithDetails(
        workout: WorkoutEntity,
        exercises: List<ExerciseEntity>,
        sets: List<SetEntity>,
        exerciseDao: ExerciseDao,
        setDao: SetDao
    ) {
        insertWorkout(workout)
        exerciseDao.deleteExercisesByWorkoutId(workout.id)
        exerciseDao.insertExercises(exercises)
        setDao.insertSets(sets)
    }
}
