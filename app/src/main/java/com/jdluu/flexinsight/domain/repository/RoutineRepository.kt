package com.jdluu.flexinsight.domain.repository

import com.jdluu.flexinsight.core.errors.Result
import com.jdluu.flexinsight.domain.model.CreateRoutineDraft
import com.jdluu.flexinsight.domain.model.Routine
import com.jdluu.flexinsight.domain.model.RoutineFolder
import kotlinx.coroutines.flow.Flow

/**
 * Interface for routine-related operations.
 */
interface RoutineRepository {
    fun invalidateApiService()

    suspend fun syncRoutines(): Result<Unit>

    fun getRoutines(): Flow<List<Routine>>

    suspend fun getRoutineById(routineId: String): Result<Routine>

    suspend fun getRoutineFolders(): Result<List<RoutineFolder>>

    /**
     * Creates a new routine on the user's Hevy account.
     * @param draft The domain routine creation payload.
     * @return The ID of the created routine, or an error.
     */
    suspend fun createRoutine(draft: CreateRoutineDraft): Result<String>
}
