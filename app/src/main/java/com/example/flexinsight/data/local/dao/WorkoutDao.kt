package com.example.flexinsight.data.local.dao

import androidx.room.*
import com.example.flexinsight.data.model.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    fun getAllWorkoutsFlow(): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts ORDER BY startTime DESC LIMIT :limit")
    fun getRecentWorkoutsFlow(limit: Int): Flow<List<Workout>>
    
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: String): Workout?
    
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?>
    
    @Query("SELECT COUNT(*) FROM workouts")
    fun getWorkoutCountFlow(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM workouts")
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
    
    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: String)
    
    @Query("UPDATE workouts SET lastSynced = :timestamp, needsSync = 0 WHERE id = :workoutId")
    suspend fun markWorkoutSynced(workoutId: String, timestamp: Long = System.currentTimeMillis())
}

