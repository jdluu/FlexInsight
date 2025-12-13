package com.example.hevyinsight.data.api

import com.example.hevyinsight.data.model.ExerciseHistoryResponse
import com.example.hevyinsight.data.model.ExerciseTemplateResponse
import com.example.hevyinsight.data.model.PaginatedWorkoutResponse
import com.example.hevyinsight.data.model.RoutineResponse
import com.example.hevyinsight.data.model.WorkoutResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HevyApiService {
    /**
     * Get a paginated list of workouts
     * API key is added automatically by the interceptor
     * @param page Page number (default: 1)
     * @param perPage Items per page (default: 20)
     */
    @GET("v1/workouts")
    suspend fun getWorkouts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
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
     * Get all exercise templates
     * API key is added automatically by the interceptor
     */
    @GET("v1/exercise-templates")
    suspend fun getExerciseTemplates(): Response<List<ExerciseTemplateResponse>>

    /**
     * Get all workout routines (templates) saved by user
     * API key is added automatically by the interceptor
     */
    @GET("v1/routines")
    suspend fun getRoutines(): Response<List<RoutineResponse>>

    /**
     * Get a single routine by ID
     * API key is added automatically by the interceptor
     * @param routineId The routine ID
     */
    @GET("v1/routines/{routineId}")
    suspend fun getRoutineById(
        @Path("routineId") routineId: String
    ): Response<RoutineResponse>
}

/**
 * Response model for workout count
 */
data class WorkoutCountResponse(
    val count: Int
)

