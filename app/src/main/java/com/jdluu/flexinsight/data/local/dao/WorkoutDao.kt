package com.jdluu.flexinsight.data.local.dao

import androidx.room.*
import com.jdluu.flexinsight.data.model.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE isDeleted = 0 ORDER BY startTime DESC")
    fun getAllWorkoutsFlow(): Flow<List<Workout>>

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    fun getAllWorkoutsWithDetailsFlow(): Flow<List<com.jdluu.flexinsight.data.model.WorkoutWithDetails>>

    @Query("SELECT * FROM workouts WHERE isDeleted = 0 ORDER BY startTime DESC LIMIT :limit")
    fun getRecentWorkoutsFlow(limit: Int): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: String): Workout?

    @Query("SELECT id FROM workouts WHERE id IN (:ids)")
    suspend fun getExistingWorkoutIds(ids: List<String>): List<String>

    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?>

    @Query("SELECT COUNT(*) FROM workouts WHERE isDeleted = 0")
    fun getWorkoutCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workouts WHERE isDeleted = 0")
    suspend fun getWorkoutCount(): Int

    @Query("SELECT MAX(lastSynced) FROM workouts")
    suspend fun getMostRecentSyncedTimestamp(): Long?

    @Query("SELECT * FROM workouts WHERE startTime >= :startTimestamp AND startTime <= :endTimestamp ORDER BY startTime DESC")
    fun getWorkoutsByDateRangeFlow(startTimestamp: Long, endTimestamp: Long): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE startTime >= :startTimestamp ORDER BY startTime DESC")
    fun getWorkoutsSinceFlow(startTimestamp: Long): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE needsSync = 1")
    suspend fun getWorkoutsNeedingSync(): List<Workout>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<Workout>)

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("UPDATE workouts SET isDeleted = 1 WHERE id = :workoutId")
    suspend fun softDeleteWorkoutById(workoutId: String)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: String)

    @Query("UPDATE workouts SET lastSynced = :timestamp, needsSync = 0 WHERE id = :workoutId")
    suspend fun markWorkoutSynced(workoutId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Inserts or updates a complete workout with its related exercises and sets atomically.
     */
    @Transaction
    suspend fun insertWorkoutWithDetails(
        workout: Workout,
        exercises: List<com.jdluu.flexinsight.data.model.Exercise>,
        sets: List<com.jdluu.flexinsight.data.model.Set>,
        exerciseDao: com.jdluu.flexinsight.data.local.dao.ExerciseDao,
        setDao: com.jdluu.flexinsight.data.local.dao.SetDao
    ) {
        insertWorkout(workout)
        
        // Clear existing related data to ensure clean state on update
        exerciseDao.deleteExercisesByWorkoutId(workout.id)
        
        exerciseDao.insertExercises(exercises)
        setDao.insertSets(sets)
    }
}

