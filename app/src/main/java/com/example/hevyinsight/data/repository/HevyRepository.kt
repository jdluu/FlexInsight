package com.example.hevyinsight.data.repository

import com.example.hevyinsight.data.api.HevyApiClient
import com.example.hevyinsight.data.api.HevyApiService
import com.example.hevyinsight.data.local.HevyDatabase
import com.example.hevyinsight.data.local.dao.ExerciseDao
import com.example.hevyinsight.data.local.dao.SetDao
import com.example.hevyinsight.data.local.dao.WorkoutDao
import com.example.hevyinsight.data.model.*
import com.example.hevyinsight.data.model.ExerciseTemplate
import com.example.hevyinsight.data.preferences.ApiKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

class HevyRepository(
    private val database: HevyDatabase,
    private val apiKeyManager: ApiKeyManager
) {
    private val workoutDao: WorkoutDao = database.workoutDao()
    private val exerciseDao: ExerciseDao = database.exerciseDao()
    private val setDao: SetDao = database.setDao()
    
    private var apiService: HevyApiService? = null
    private var currentApiKey: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Cache for exercise templates (templateId -> muscleGroup)
    private var exerciseTemplateCache: Map<String, String>? = null
    private var exerciseTemplateCacheTime: Long = 0
    private val EXERCISE_TEMPLATE_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
    
    // Cache for routines
    private var routinesCache: List<Routine>? = null
    private var routinesCacheTime: Long = 0
    private val ROUTINES_CACHE_DURATION = 60 * 60 * 1000L // 1 hour
    
    /**
     * Initialize API service with current API key
     * Recreates the service if API key changes
     */
    private suspend fun getApiService(): HevyApiService? {
        val apiKey = apiKeyManager.getApiKey() ?: return null
        
        // Recreate service if API key has changed
        if (apiService == null || currentApiKey != apiKey) {
            apiService = HevyApiClient.createApiService(apiKey)
            currentApiKey = apiKey
        }
        return apiService
    }
    
    /**
     * Invalidate the API service (useful when API key is updated)
     */
    fun invalidateApiService() {
        apiService = null
        currentApiKey = null
        exerciseTemplateCache = null
        exerciseTemplateCacheTime = 0
    }
    
    /**
     * Fetch and cache exercise templates from API
     * Returns mapping of templateId -> muscleGroup
     */
    private suspend fun getExerciseTemplateMapping(): Map<String, String> {
        val now = System.currentTimeMillis()
        
        // Return cached data if still valid
        if (exerciseTemplateCache != null && (now - exerciseTemplateCacheTime) < EXERCISE_TEMPLATE_CACHE_DURATION) {
            return exerciseTemplateCache!!
        }
        
        val apiService = getApiService() ?: return emptyMap()
        
        return try {
            val response = apiService.getExerciseTemplates()
            if (response.isSuccessful && response.body() != null) {
                val templates = response.body()!!
                val mapping = templates.mapNotNull { templateResponse ->
                    val template = templateResponse.toExerciseTemplate()
                    template.muscleGroup?.let { template.id to it }
                }.toMap()
                exerciseTemplateCache = mapping
                exerciseTemplateCacheTime = now
                mapping
            } else {
                // Return empty map if API call fails, but keep old cache if available
                exerciseTemplateCache ?: emptyMap()
            }
        } catch (e: Exception) {
            // Return cached data if available, otherwise empty map
            exerciseTemplateCache ?: emptyMap()
        }
    }
    
    /**
     * Get muscle group for an exercise by looking up its template ID
     * Falls back to name-based mapping if template not found
     */
    private suspend fun getMuscleGroupForExercise(exercise: Exercise): String? {
        // First try to get from exercise template
        if (exercise.exerciseTemplateId != null) {
            val templateMapping = getExerciseTemplateMapping()
            templateMapping[exercise.exerciseTemplateId]?.let { return it }
        }
        
        // Fall back to name-based mapping
        return getMuscleGroupFromExerciseName(exercise.name)
    }
    
    /**
     * Get muscle group from exercise name (fallback method)
     */
    private fun getMuscleGroupFromExerciseName(exerciseName: String): String? {
        val name = exerciseName.lowercase()
        
        // Chest exercises
        if (name.contains("bench") || name.contains("chest") || name.contains("pec") ||
            name.contains("fly") || name.contains("press") && (name.contains("incline") || name.contains("decline"))) {
            return "Chest"
        }
        
        // Back exercises
        if (name.contains("row") || name.contains("pull") || name.contains("lat") ||
            name.contains("deadlift") || name.contains("shrug") || name.contains("rear delt")) {
            return "Back"
        }
        
        // Legs exercises
        if (name.contains("squat") || name.contains("leg") || name.contains("quad") ||
            name.contains("hamstring") || name.contains("calf") || name.contains("lunge") ||
            name.contains("extension") || name.contains("curl") && name.contains("leg")) {
            return "Legs"
        }
        
        // Shoulders
        if (name.contains("shoulder") || name.contains("delt") || name.contains("press") && name.contains("shoulder") ||
            name.contains("lateral") || name.contains("front raise")) {
            return "Shoulders"
        }
        
        // Arms
        if (name.contains("bicep") || name.contains("tricep") || name.contains("curl") ||
            name.contains("extension") && name.contains("tricep") || name.contains("preacher")) {
            return "Arms"
        }
        
        // Core/Abs
        if (name.contains("ab") || name.contains("core") || name.contains("crunch") ||
            name.contains("plank") || name.contains("sit-up")) {
            return "Core"
        }
        
        return null
    }
    
    /**
     * Get all workouts - returns Flow from Room immediately, syncs in background
     */
    fun getWorkouts(): Flow<List<Workout>> {
        // Return cached data immediately
        val cachedFlow = workoutDao.getAllWorkoutsFlow()
        
        // Sync in background
        scope.launch {
            syncWorkouts()
        }
        
        return cachedFlow
    }
    
    /**
     * Get recent workouts
     */
    fun getRecentWorkouts(limit: Int = 10): Flow<List<Workout>> {
        scope.launch {
            syncWorkouts()
        }
        return workoutDao.getRecentWorkoutsFlow(limit)
    }
    
    /**
     * Get workout by ID - checks Room first, then API if not found
     */
    suspend fun getWorkoutById(workoutId: String): Workout? {
        val cached = workoutDao.getWorkoutById(workoutId)
        if (cached != null) {
            return cached
        }
        
        // Try to fetch from API
        val apiService = getApiService() ?: return null
        try {
            val response = apiService.getWorkoutById(workoutId)
            if (response.isSuccessful && response.body() != null) {
                val workoutResponse = response.body()!!
                val workout = workoutResponse.toWorkout()
                workoutDao.insertWorkout(workout)
                
                // Insert exercises and sets
                workoutResponse.exercises?.forEach { exerciseResponse ->
                    val exercise = exerciseResponse.toExercise(workoutId)
                    exerciseDao.insertExercise(exercise)
                    
                    exerciseResponse.sets?.forEach { setResponse ->
                        val set = setResponse.toSet(exercise.id)
                        setDao.insertSet(set)
                    }
                }
                
                return workout
            } else {
                if (response.code() == 401) {
                    android.util.Log.e("HevyRepository", "API authentication failed: Invalid API key (401) for workout $workoutId")
                } else {
                    android.util.Log.w("HevyRepository", "API request failed for workout $workoutId: ${response.code()} ${response.message()}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HevyRepository", "Error fetching workout $workoutId: ${e.message}", e)
            // Handle error silently - return cached data if available
        }
        
        return null
    }
    
    /**
     * Get workout by ID as Flow
     */
    fun getWorkoutByIdFlow(workoutId: String): Flow<Workout?> {
        scope.launch {
            syncWorkout(workoutId)
        }
        return workoutDao.getWorkoutByIdFlow(workoutId)
    }
    
    /**
     * Get workout count
     */
    fun getWorkoutCount(): Flow<Int> {
        scope.launch {
            syncWorkouts()
        }
        return workoutDao.getWorkoutCountFlow()
    }
    
    /**
     * Sync workouts from API
     */
    private suspend fun syncWorkouts() {
        val apiService = getApiService() ?: return
        
        try {
            var page = 1
            var hasMore = true
            
            while (hasMore) {
                val response = apiService.getWorkouts(page, 50)
                if (response.isSuccessful && response.body() != null) {
                    val paginatedResponse = response.body()!!
                    val workouts = paginatedResponse.data.map { it.toWorkout() }
                    
                    // Insert workouts
                    workoutDao.insertWorkouts(workouts)
                    
                    // Insert exercises and sets
                    paginatedResponse.data.forEach { workoutResponse ->
                        workoutResponse.exercises?.forEach { exerciseResponse ->
                            val exercise = exerciseResponse.toExercise(workoutResponse.id)
                            exerciseDao.insertExercise(exercise)
                            
                            exerciseResponse.sets?.forEach { setResponse ->
                                val set = setResponse.toSet(exercise.id)
                                setDao.insertSet(set)
                            }
                        }
                    }
                    
                    // Check if there are more pages
                    val pagination = paginatedResponse.pagination
                    hasMore = pagination?.let { page < it.totalPages } ?: false
                    page++
                } else {
                    // Check if it's an authentication error
                    if (response.code() == 401) {
                        android.util.Log.e("HevyRepository", "API authentication failed: Invalid API key (401)")
                        android.util.Log.e("HevyRepository", "Response: ${response.message()}")
                        // Invalid API key - clear cached service to force recreation
                        this.apiService = null
                        this.currentApiKey = null
                        // Log error but don't crash - app will work offline
                    } else {
                        android.util.Log.w("HevyRepository", "API request failed: ${response.code()} ${response.message()}")
                    }
                    hasMore = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HevyRepository", "Error syncing workouts: ${e.message}", e)
            // Handle error silently - app will work offline
        }
    }
    
    /**
     * Sync a single workout
     */
    private suspend fun syncWorkout(workoutId: String) {
        val apiService = getApiService() ?: return
        
        try {
            val response = apiService.getWorkoutById(workoutId)
            if (response.isSuccessful && response.body() != null) {
                val workoutResponse = response.body()!!
                val workout = workoutResponse.toWorkout()
                workoutDao.insertWorkout(workout)
                
                // Insert exercises and sets
                workoutResponse.exercises?.forEach { exerciseResponse ->
                    val exercise = exerciseResponse.toExercise(workoutId)
                    exerciseDao.insertExercise(exercise)
                    
                    exerciseResponse.sets?.forEach { setResponse ->
                        val set = setResponse.toSet(exercise.id)
                        setDao.insertSet(set)
                    }
                }
            } else {
                // Check if it's an authentication error
                if (response.code() == 401) {
                    android.util.Log.e("HevyRepository", "API authentication failed: Invalid API key (401) for workout $workoutId")
                    // Invalid API key - clear cached service to force recreation
                    this.apiService = null
                    this.currentApiKey = null
                } else {
                    android.util.Log.w("HevyRepository", "API request failed for workout $workoutId: ${response.code()} ${response.message()}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HevyRepository", "Error syncing workout $workoutId: ${e.message}", e)
            // Handle error silently
        }
    }
    
    /**
     * Calculate workout statistics
     */
    suspend fun calculateStats(): WorkoutStats {
        return try {
            val workouts = workoutDao.getAllWorkoutsFlow().first()
            
            // Calculate stats from workouts
            val totalWorkouts = workouts.size
            val totalVolume = calculateTotalVolume(workouts)
            val averageVolume = if (totalWorkouts > 0) totalVolume / totalWorkouts else 0.0
            
            // Calculate total sets
            var totalSets = 0
            workouts.forEach { workout ->
                try {
                    val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
                    exercises.forEach { exercise ->
                        try {
                            val sets = setDao.getSetsByExerciseId(exercise.id)
                            totalSets += sets.size
                        } catch (e: Exception) {
                            // Skip if sets can't be loaded
                        }
                    }
                } catch (e: Exception) {
                    // Skip if exercises can't be loaded
                }
            }
            
            // Calculate duration
            var totalDuration = 0L
            workouts.forEach { workout ->
                try {
                    if (workout.endTime != null) {
                        totalDuration += (workout.endTime - workout.startTime) / (1000 * 60) // minutes
                    }
                } catch (e: Exception) {
                    // Skip if duration can't be calculated
                }
            }
            val averageDuration = if (totalWorkouts > 0) totalDuration / totalWorkouts else 0L
            
            val currentStreak = calculateStreak(workouts)
            val longestStreak = calculateLongestStreak(workouts)
            
            // Calculate best week
            val weeklyProgress = try {
                getWeeklyProgress(4)
            } catch (e: Exception) {
                emptyList()
            }
            val bestWeek = weeklyProgress.maxByOrNull { it.totalVolume }
            
            WorkoutStats(
                totalWorkouts = totalWorkouts,
                totalVolume = totalVolume,
                averageVolume = averageVolume,
                totalSets = totalSets,
                totalDuration = totalDuration,
                averageDuration = averageDuration,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                bestWeekVolume = bestWeek?.totalVolume ?: 0.0,
                bestWeekDate = bestWeek?.weekStartDate
            )
        } catch (e: Exception) {
            // Return empty stats if calculation fails
            WorkoutStats(
                totalWorkouts = 0,
                totalVolume = 0.0,
                averageVolume = 0.0,
                totalSets = 0,
                totalDuration = 0L,
                averageDuration = 0L,
                currentStreak = 0,
                longestStreak = 0,
                bestWeekVolume = 0.0,
                bestWeekDate = null
            )
        }
    }
    
    /**
     * Calculate statistics for a single workout
     * Returns duration in minutes, total sets count, and total volume
     */
    suspend fun calculateWorkoutStats(workout: Workout): SingleWorkoutStats {
        return try {
            val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
            var totalSets = 0
            var totalVolume = 0.0
            
            exercises.forEach { exercise ->
                val sets = setDao.getSetsByExerciseId(exercise.id)
                totalSets += sets.size
                sets.forEach { set ->
                    val volume = (set.weight ?: 0.0) * (set.reps ?: 0)
                    totalVolume += volume
                }
            }
            
            // Calculate duration in minutes
            val durationMinutes = if (workout.endTime != null) {
                (workout.endTime - workout.startTime) / (1000 * 60)
            } else {
                0L
            }
            
            SingleWorkoutStats(
                durationMinutes = durationMinutes,
                totalSets = totalSets,
                totalVolume = totalVolume
            )
        } catch (e: Exception) {
            SingleWorkoutStats(
                durationMinutes = 0L,
                totalSets = 0,
                totalVolume = 0.0
            )
        }
    }
    
    /**
     * Get recent PRs
     */
    fun getRecentPRs(limit: Int = 10): Flow<List<com.example.hevyinsight.data.model.Set>> {
        return setDao.getRecentPRsFlow(limit)
    }
    
    /**
     * Get PRs with exercise and workout details
     */
    suspend fun getPRsWithDetails(limit: Int = 10): List<PRDetails> {
        return try {
            val prSets = setDao.getRecentPRsFlow(limit).first()
            val prDetails = mutableListOf<PRDetails>()
            
            prSets.forEach { set ->
                try {
                    // Get exercise by exerciseId
                    val exercise = exerciseDao.getExerciseById(set.exerciseId)
                    
                    if (exercise != null) {
                        // Get workout
                        val workout = workoutDao.getWorkoutById(exercise.workoutId)
                        if (workout != null && set.weight != null) {
                            // Get muscle group
                            val muscleGroup = getMuscleGroupForExercise(exercise) ?: "Unknown"
                            
                            prDetails.add(
                                PRDetails(
                                    exerciseName = exercise.name,
                                    date = workout.startTime,
                                    muscleGroup = muscleGroup,
                                    weight = set.weight,
                                    workoutId = workout.id,
                                    setId = set.id
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Skip if can't load exercise/workout
                }
            }
            
            prDetails
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get muscle group progress for the last N weeks
     */
    suspend fun getMuscleGroupProgress(weeks: Int = 4): List<MuscleGroupProgress> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val startDate = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        // Map to track volume and sets per muscle group
        val muscleGroupData = mutableMapOf<String, Pair<Double, Int>>()
        
        workouts.forEach { workout ->
            try {
                val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
                exercises.forEach { exercise ->
                    val muscleGroup = getMuscleGroupForExercise(exercise) ?: return@forEach
                    
                    val sets = setDao.getSetsByExerciseId(exercise.id)
                    var exerciseVolume = 0.0
                    sets.forEach { set ->
                        val volume = (set.weight ?: 0.0) * (set.reps ?: 0)
                        exerciseVolume += volume
                    }
                    
                    val current = muscleGroupData[muscleGroup] ?: (0.0 to 0)
                    muscleGroupData[muscleGroup] = (current.first + exerciseVolume) to (current.second + sets.size)
                }
            } catch (e: Exception) {
                // Skip if can't load exercises/sets
            }
        }
        
        // Calculate average volume for intensity determination
        val totalVolume = muscleGroupData.values.sumOf { it.first }
        val averageVolume = if (muscleGroupData.isNotEmpty()) totalVolume / muscleGroupData.size else 0.0
        
        // Convert to MuscleGroupProgress and determine intensity
        return muscleGroupData.map { (muscleGroup, data) ->
            val (volume, sets) = data
            val intensity = when {
                volume >= averageVolume * 1.5 -> "HI"
                volume >= averageVolume * 0.7 -> "MD"
                else -> "LO"
            }
            MuscleGroupProgress(
                muscleGroup = muscleGroup,
                volume = volume,
                sets = sets,
                intensity = intensity
            )
        }.sortedByDescending { it.volume }.take(3) // Return top 3
    }
    
    /**
     * Calculate volume trend comparing current period to previous period
     */
    suspend fun calculateVolumeTrend(weeks: Int = 4): VolumeTrend {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Current period: last N weeks
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val currentPeriodStart = calendar.timeInMillis
        
        // Previous period: N weeks before that
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val previousPeriodStart = calendar.timeInMillis
        
        val currentWorkouts = workoutDao.getWorkoutsByDateRangeFlow(currentPeriodStart, now).first()
        val previousWorkouts = workoutDao.getWorkoutsByDateRangeFlow(previousPeriodStart, currentPeriodStart).first()
        
        val currentVolume = calculateTotalVolume(currentWorkouts)
        val previousVolume = calculateTotalVolume(previousWorkouts)
        
        val percentageChange = if (previousVolume > 0) {
            ((currentVolume - previousVolume) / previousVolume) * 100.0
        } else {
            if (currentVolume > 0) 100.0 else 0.0
        }
        
        return VolumeTrend(
            currentVolume = currentVolume,
            previousVolume = previousVolume,
            percentageChange = percentageChange
        )
    }
    
    /**
     * Get weekly volume data for chart display
     */
    suspend fun getWeeklyVolumeData(weeks: Int = 4): List<WeeklyVolumeData> {
        val weeklyProgress = getWeeklyProgress(weeks)
        return weeklyProgress.mapIndexed { index, progress ->
            WeeklyVolumeData(
                weekLabel = "W${index + 1}",
                volume = progress.totalVolume
            )
        }
    }
    
    /**
     * Get duration trend grouped by day of week
     */
    suspend fun getDurationTrend(weeks: Int = 6): List<DailyDurationData> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val startDate = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        // Group by day of week (1=Sunday, 2=Monday, ..., 7=Saturday)
        // We'll map to M, T, W, T, F, S (Monday-Saturday)
        val dayGroups = mutableMapOf<Int, MutableList<Long>>()
        
        workouts.forEach { workout ->
            if (workout.endTime != null) {
                val workoutCal = Calendar.getInstance().apply {
                    timeInMillis = workout.startTime
                }
                val dayOfWeek = workoutCal.get(Calendar.DAY_OF_WEEK)
                val durationMinutes = (workout.endTime - workout.startTime) / (1000 * 60)
                
                // Map Calendar day to our index (Monday=0, Tuesday=1, etc.)
                val dayIndex = when (dayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6 // We'll skip Sunday or add it
                    else -> -1
                }
                
                if (dayIndex >= 0) {
                    dayGroups.getOrPut(dayIndex) { mutableListOf() }.add(durationMinutes)
                }
            }
        }
        
        val dayLabels = listOf("M", "T", "W", "T", "F", "S")
        return dayLabels.mapIndexed { index, label ->
            val durations = dayGroups[index] ?: emptyList()
            val avgDuration = if (durations.isNotEmpty()) {
                durations.average().toLong()
            } else {
                0L
            }
            DailyDurationData(
                dayOfWeek = label,
                averageDuration = avgDuration
            )
        }
    }
    
    /**
     * Sync routines from API
     */
    private suspend fun syncRoutines() {
        val apiService = getApiService() ?: return
        
        try {
            val response = apiService.getRoutines()
            if (response.isSuccessful && response.body() != null) {
                val routineResponses = response.body()!!
                val exerciseTemplateMapping = getExerciseTemplateMapping()
                
                val routinesList = routineResponses.map { routineResponse: RoutineResponse ->
                    routineResponse.toRoutine(exerciseTemplateMapping)
                }
                
                routinesCache = routinesList
                routinesCacheTime = System.currentTimeMillis()
            } else {
                if (response.code() == 401) {
                    this.apiService = null
                    this.currentApiKey = null
                }
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    /**
     * Get all routines
     */
    fun getRoutines(): Flow<List<Routine>> {
        val now = System.currentTimeMillis()
        
        // Return cached data if still valid
        if (routinesCache != null && (now - routinesCacheTime) < ROUTINES_CACHE_DURATION) {
            return flow { emit(routinesCache!!) }
        }
        
        // Sync in background
        scope.launch {
            syncRoutines()
        }
        
        // Return cached data or empty list
        return flow { emit(routinesCache ?: emptyList()) }
    }
    
    /**
     * Get routine by ID
     */
    suspend fun getRoutineById(routineId: String): Routine? {
        val routines = getRoutines().first()
        return routines.firstOrNull { it.id == routineId }
    }
    
    /**
     * Get weekly goal progress
     */
    suspend fun getWeeklyGoalProgress(target: Int = 5): WeeklyGoalProgress {
        val calendar = Calendar.getInstance()
        
        // Get current week (Monday to Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val weekEnd = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(weekStart, weekEnd).first()
        val completed = workouts.size
        
        val status = when {
            completed >= target -> "On Track"
            completed >= target * 0.7 -> "On Track" // 70% or more is still on track
            completed >= target * 0.5 -> "Behind"
            else -> "Behind"
        }
        
        return WeeklyGoalProgress(
            completed = completed,
            target = target,
            status = status
        )
    }
    
    /**
     * Get week calendar data (Monday to Sunday)
     */
    suspend fun getWeekCalendarData(): List<DayInfo> {
        val calendar = Calendar.getInstance()
        
        // Get current week starting Monday
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> 6
            else -> dayOfWeek - Calendar.MONDAY
        }
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val days = mutableListOf<DayInfo>()
        
        for (i in 0..6) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val dayStart = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val dayEnd = calendar.timeInMillis
            
            val workouts = workoutDao.getWorkoutsByDateRangeFlow(dayStart, dayEnd).first()
            val hasWorkout = workouts.isNotEmpty()
            val isCompleted = workouts.any { it.endTime != null }
            val workoutCount = workouts.size
            
            days.add(
                DayInfo(
                    name = dayNames[i],
                    date = calendar.get(Calendar.DAY_OF_MONTH),
                    timestamp = dayStart,
                    hasWorkout = hasWorkout,
                    isCompleted = isCompleted,
                    workoutCount = workoutCount
                )
            )
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return days
    }
    
    /**
     * Get planned workouts for a specific day
     */
    suspend fun getPlannedWorkoutsForDay(timestamp: Long): List<PlannedWorkout> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dayStart = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val dayEnd = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(dayStart, dayEnd).first()
        
        return workouts.map { workout ->
            val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
            val duration = if (workout.endTime != null) {
                (workout.endTime - workout.startTime) / (1000 * 60)
            } else {
                null
            }
            
            // Determine intensity based on volume
            var totalVolume = 0.0
            exercises.forEach { exercise ->
                val sets = setDao.getSetsByExerciseId(exercise.id)
                sets.forEach { set ->
                    totalVolume += (set.weight ?: 0.0) * (set.reps ?: 0)
                }
            }
            val intensity = when {
                totalVolume > 5000 -> "High Intensity"
                totalVolume > 2000 -> "Medium Intensity"
                else -> "Aerobic"
            }
            
            PlannedWorkout(
                id = workout.id,
                name = workout.name ?: "Workout",
                duration = duration,
                intensity = intensity,
                isCompleted = workout.endTime != null,
                routineId = workout.routineId,
                exerciseCount = exercises.size
            )
        }
    }
    
    /**
     * Get volume balance across Push, Pull, Legs, Cardio
     */
    suspend fun getVolumeBalance(weeks: Int = 4): VolumeBalance {
        val muscleGroupProgress = getMuscleGroupProgress(weeks)
        
        // Map muscle groups to categories
        val pushGroups = listOf("Chest", "Shoulders", "Triceps")
        val pullGroups = listOf("Back", "Biceps")
        val legsGroups = listOf("Legs", "Quads", "Hamstrings", "Glutes", "Calves")
        val cardioGroups = listOf("Cardio")
        
        var pushVolume = 0.0
        var pullVolume = 0.0
        var legsVolume = 0.0
        var cardioVolume = 0.0
        
        muscleGroupProgress.forEach { progress ->
            val group = progress.muscleGroup
            when {
                pushGroups.any { group.contains(it, ignoreCase = true) } -> pushVolume += progress.volume
                pullGroups.any { group.contains(it, ignoreCase = true) } -> pullVolume += progress.volume
                legsGroups.any { group.contains(it, ignoreCase = true) } -> legsVolume += progress.volume
                cardioGroups.any { group.contains(it, ignoreCase = true) } -> cardioVolume += progress.volume
            }
        }
        
        val totalVolume = pushVolume + pullVolume + legsVolume + cardioVolume
        
        return if (totalVolume > 0) {
            VolumeBalance(
                push = (pushVolume / totalVolume).toFloat(),
                pull = (pullVolume / totalVolume).toFloat(),
                legs = (legsVolume / totalVolume).toFloat(),
                cardio = (cardioVolume / totalVolume).toFloat()
            )
        } else {
            VolumeBalance(
                push = 0.25f,
                pull = 0.25f,
                legs = 0.25f,
                cardio = 0.25f
            )
        }
    }
    
    /**
     * Get weekly progress
     */
    suspend fun getWeeklyProgress(weeks: Int = 4): List<WeeklyProgress> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        val startDate = calendar.timeInMillis
        
        val workouts = workoutDao.getWorkoutsByDateRangeFlow(startDate, endDate).first()
        
        // Group by week and calculate progress
        return workouts.groupBy { workout ->
            val workoutCal = Calendar.getInstance().apply {
                timeInMillis = workout.startTime
            }
            workoutCal.get(Calendar.WEEK_OF_YEAR)
        }.map { (_, weekWorkouts) ->
            val weekStart = weekWorkouts.minOfOrNull { it.startTime } ?: 0L
            val totalVolume = calculateTotalVolume(weekWorkouts)
            WeeklyProgress(
                weekStartDate = weekStart,
                totalVolume = totalVolume,
                workoutCount = weekWorkouts.size,
                averageVolume = if (weekWorkouts.isNotEmpty()) totalVolume / weekWorkouts.size else 0.0
            )
        }
    }
    
    /**
     * Get member since date (timestamp of first workout)
     */
    suspend fun getMemberSinceDate(): Long? {
        return try {
            val workouts = workoutDao.getAllWorkoutsFlow().first()
            workouts.minOfOrNull { it.startTime }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate account age in days
     */
    suspend fun calculateAccountAgeDays(): Int {
        val memberSince = getMemberSinceDate() ?: return 0
        val now = System.currentTimeMillis()
        val daysDiff = (now - memberSince) / (1000 * 60 * 60 * 24)
        return daysDiff.toInt().coerceAtLeast(0)
    }
    
    /**
     * Get profile information
     */
    suspend fun getProfileInfo(): ProfileInfo {
        val workouts = workoutDao.getAllWorkoutsFlow().first()
        val totalWorkouts = workouts.size
        val memberSince = getMemberSinceDate()
        val accountAgeDays = calculateAccountAgeDays()
        val isProMember = apiKeyManager.hasApiKey()
        
        return ProfileInfo(
            displayName = null, // Can be set by user later
            memberSince = memberSince,
            isProMember = isProMember,
            totalWorkouts = totalWorkouts,
            accountAgeDays = accountAgeDays
        )
    }
    
    /**
     * Sync all data from API (workouts, routines, exercise templates)
     */
    suspend fun syncAllData() {
        try {
            // Sync workouts
            syncWorkouts()
            
            // Sync routines
            syncRoutines()
            
            // Sync exercise templates (happens automatically when needed)
            getExerciseTemplateMapping()
        } catch (e: Exception) {
            // Handle error silently - app will work offline
        }
    }
    
    /**
     * Clear all cached data (exercise templates, routines)
     */
    fun clearCache() {
        exerciseTemplateCache = null
        exerciseTemplateCacheTime = 0
        routinesCache = null
        routinesCacheTime = 0
    }
    
    /**
     * Sync with cloud database (structure ready, implementation TBD)
     */
    suspend fun syncWithCloud() {
        // TODO: Implement cloud sync when database service is chosen
    }
    
    // Helper functions
    
    private suspend fun calculateTotalVolume(workouts: List<Workout>): Double {
        var totalVolume = 0.0
        workouts.forEach { workout ->
            val exercises = exerciseDao.getExercisesByWorkoutId(workout.id)
            exercises.forEach { exercise ->
                val sets = setDao.getSetsByExerciseId(exercise.id)
                sets.forEach { set ->
                    val volume = (set.weight ?: 0.0) * (set.reps ?: 0)
                    totalVolume += volume
                }
            }
        }
        return totalVolume
    }
    
    private fun calculateStreak(workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0
        
        val sortedWorkouts = workouts.sortedByDescending { it.startTime }
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        var streak = 0
        var currentDate = calendar.timeInMillis
        
        for (workout in sortedWorkouts) {
            val workoutDate = workout.startTime
            val workoutCal = Calendar.getInstance().apply {
                timeInMillis = workoutDate
            }
            workoutCal.set(Calendar.HOUR_OF_DAY, 0)
            workoutCal.set(Calendar.MINUTE, 0)
            workoutCal.set(Calendar.SECOND, 0)
            workoutCal.set(Calendar.MILLISECOND, 0)
            val workoutDateStart = workoutCal.timeInMillis
            
            if (workoutDateStart == currentDate) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                currentDate = calendar.timeInMillis
            } else if (workoutDateStart < currentDate) {
                break
            }
        }
        
        return streak
    }
    
    private fun calculateLongestStreak(workouts: List<Workout>): Int {
        if (workouts.isEmpty()) return 0
        
        val sortedWorkouts = workouts.sortedBy { it.startTime }
        var longestStreak = 0
        var currentStreak = 0
        var lastDate: Long? = null
        
        for (workout in sortedWorkouts) {
            val workoutCal = Calendar.getInstance().apply {
                timeInMillis = workout.startTime
            }
            workoutCal.set(Calendar.HOUR_OF_DAY, 0)
            workoutCal.set(Calendar.MINUTE, 0)
            workoutCal.set(Calendar.SECOND, 0)
            workoutCal.set(Calendar.MILLISECOND, 0)
            val workoutDate = workoutCal.timeInMillis
            
            if (lastDate == null) {
                currentStreak = 1
                lastDate = workoutDate
            } else {
                val daysDiff = (workoutDate - lastDate) / (1000 * 60 * 60 * 24)
                if (daysDiff == 1L) {
                    currentStreak++
                } else {
                    longestStreak = maxOf(longestStreak, currentStreak)
                    currentStreak = 1
                }
                lastDate = workoutDate
            }
        }
        
        return maxOf(longestStreak, currentStreak)
    }
}

