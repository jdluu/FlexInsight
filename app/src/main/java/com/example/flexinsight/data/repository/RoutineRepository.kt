package com.example.flexinsight.data.repository

import com.example.flexinsight.core.errors.Result
import com.example.flexinsight.data.model.Routine
import kotlinx.coroutines.flow.Flow

/**
 * Interface for routine-related operations.
 */
interface RoutineRepository {
    fun invalidateApiService()

    suspend fun syncRoutines(): Result<Unit>

    fun getRoutines(): Flow<List<Routine>>

    suspend fun getRoutineById(routineId: String): Result<Routine>

    suspend fun getRoutineFolders(): Result<List<com.example.flexinsight.data.model.RoutineFolder>>
}
