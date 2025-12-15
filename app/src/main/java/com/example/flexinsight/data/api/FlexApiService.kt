package com.example.flexinsight.data.api

import com.example.flexinsight.data.model.ExerciseHistoryResponse
import com.example.flexinsight.data.model.ExerciseTemplateResponse
import com.example.flexinsight.data.model.PaginatedExerciseTemplatesResponse
import com.example.flexinsight.data.model.PaginatedRoutineResponse
import com.example.flexinsight.data.model.PaginatedWorkoutResponse
import com.example.flexinsight.data.model.RoutineResponse
import com.example.flexinsight.data.model.WorkoutResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FlexApiService {
    /**
     * Get a paginated list of workouts
     * API key is added automatically by the interceptor
     * Note: Both page and per_page are mandatory query parameters
     * @param page Page number (required)
     * @param perPage Items per page (required)
     */
    @GET("v1/workouts")
    suspend fun getWorkouts(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<PaginatedWorkoutResponse>

    /**
     * Get the total number of workouts on the account
     * API key is added automatically by the interceptor
     */
    @GET("v1/workouts/count")
    suspend fun getWorkoutCount(): Response<WorkoutCountResponse>

    /**
     * Get a single workout's complete details by the workoutId
     * API key is added automatically by the interceptor
     * @param workoutId The workout ID
     */
    @GET("v1/workouts/{workoutId}")
    suspend fun getWorkoutById(
        @Path("workoutId") workoutId: String
    ): Response<WorkoutResponse>

    /**
     * Get exercise history for a specific exercise template
     * API key is added automatically by the interceptor
     * @param exerciseTemplateId The exercise template ID
     */
    @GET("v1/exercise_history/{exerciseTemplateId}")
    suspend fun getExerciseHistory(
        @Path("exerciseTemplateId") exerciseTemplateId: String
    ): Response<ExerciseHistoryResponse>

    /**
     * Get all exercise templates (paginated)
     * API key is added automatically by the interceptor
     * @param page Page number (default: 1)
     * @param pageSize Number of items per page (default: 50)
     */
    @GET("v1/exercise_templates")
    suspend fun getExerciseTemplates(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<PaginatedExerciseTemplatesResponse>

    /**
     * Get workout events (created, updated, deleted) since a specific date
     * API key is added automatically by the interceptor
     * @param page Page number (default: 1)
     * @param pageSize Number of items per page (default: 5, max: 10)
     * @param since ISO 8601 date string (e.g., "2023-10-01T00:00:00Z")
     */
    @GET("v1/workouts/events")
    suspend fun getWorkoutEvents(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 5,
        @Query("since") since: String? = null
    ): Response<com.example.flexinsight.data.model.PaginatedWorkoutEventsResponse>

    /**
     * Get all workout routines (templates) saved by user (paginated)
     * API key is added automatically by the interceptor
     * @param page Page number (default: 1)
     * @param pageSize Number of items per page (default: 50)
     */
    @GET("v1/routines")
    suspend fun getRoutines(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<PaginatedRoutineResponse>

    /**
     * Get a single routine by ID
     * API key is added automatically by the interceptor
     * @param routineId The routine ID
     */
    @GET("v1/routines/{routineId}")
    suspend fun getRoutineById(
        @Path("routineId") routineId: String
    ): Response<RoutineResponse>

    /**
     * Get routine folders
     * API key is added automatically by the interceptor
     * @param page Page number (default: 1)
     * @param pageSize Number of items per page (default: 50)
     */
    @GET("v1/routine_folders")
    suspend fun getRoutineFolders(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<com.example.flexinsight.data.model.PaginatedRoutineFolderResponse>
}

/**
 * Response model for workout count
 */
data class WorkoutCountResponse(
    val count: Int
)

