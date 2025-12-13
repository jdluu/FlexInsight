package com.example.hevyinsight.data.local.dao

import androidx.room.*
import com.example.hevyinsight.data.model.Set
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY number")
    fun getSetsByExerciseIdFlow(exerciseId: String): Flow<List<Set>>
    
    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY number")
    suspend fun getSetsByExerciseId(exerciseId: String): List<Set>
    
    @Query("SELECT * FROM sets WHERE isPersonalRecord = 1 ORDER BY id DESC LIMIT :limit")
    fun getRecentPRsFlow(limit: Int): Flow<List<Set>>
    
    @Query("SELECT * FROM sets WHERE needsSync = 1")
    suspend fun getSetsNeedingSync(): List<Set>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: Set)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<Set>)
    
    @Update
    suspend fun updateSet(set: Set)
    
    @Delete
    suspend fun deleteSet(set: Set)
    
    @Query("DELETE FROM sets WHERE exerciseId = :exerciseId")
    suspend fun deleteSetsByExerciseId(exerciseId: String)
    
    @Query("UPDATE sets SET lastSynced = :timestamp, needsSync = 0 WHERE id = :setId")
    suspend fun markSetSynced(setId: String, timestamp: Long = System.currentTimeMillis())
}

