package com.jdluu.flexinsight.data.local.dao

import androidx.room.*
import com.jdluu.flexinsight.data.local.entity.SetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY number")
    fun getSetsByExerciseIdFlow(exerciseId: String): Flow<List<SetEntity>>

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY number")
    suspend fun getSetsByExerciseId(exerciseId: String): List<SetEntity>

    @Query("SELECT * FROM sets WHERE exerciseId IN (:exerciseIds) ORDER BY number")
    suspend fun getSetsByExerciseIds(exerciseIds: List<String>): List<SetEntity>

    @Query("SELECT * FROM sets WHERE isPersonalRecord = 1 ORDER BY id DESC LIMIT :limit")
    fun getRecentPRsFlow(limit: Int): Flow<List<SetEntity>>

    @Query("SELECT * FROM sets WHERE needsSync = 1")
    suspend fun getSetsNeedingSync(): List<SetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetEntity>)

    @Update
    suspend fun updateSet(set: SetEntity)

    @Delete
    suspend fun deleteSet(set: SetEntity)

    @Query("DELETE FROM sets WHERE exerciseId = :exerciseId")
    suspend fun deleteSetsByExerciseId(exerciseId: String)

    @Query("UPDATE sets SET lastSynced = :timestamp, needsSync = 0 WHERE id = :setId")
    suspend fun markSetSynced(setId: String, timestamp: Long = System.currentTimeMillis())
}
