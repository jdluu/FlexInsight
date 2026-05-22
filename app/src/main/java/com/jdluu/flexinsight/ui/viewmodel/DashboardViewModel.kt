package com.jdluu.flexinsight.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdluu.flexinsight.data.ai.FlexAIClient
import com.jdluu.flexinsight.core.errors.ApiError
import com.jdluu.flexinsight.core.errors.ErrorHandler
import com.jdluu.flexinsight.data.model.Workout
import com.jdluu.flexinsight.data.model.WorkoutStats
import com.jdluu.flexinsight.data.model.WeeklyProgress
import com.jdluu.flexinsight.data.model.SingleWorkoutStats
import com.jdluu.flexinsight.data.model.ProfileInfo
import com.jdluu.flexinsight.data.repository.FlexRepository
import com.jdluu.flexinsight.core.network.NetworkMonitor
import com.jdluu.flexinsight.core.network.NetworkState
import com.jdluu.flexinsight.data.preferences.SyncPreferencesManager
import com.jdluu.flexinsight.data.preferences.UserPreferencesManager
import com.jdluu.flexinsight.data.sync.SyncCoordinator
import com.jdluu.flexinsight.widget.WidgetUpdater
import com.jdluu.flexinsight.domain.model.DeloadAlert
import com.jdluu.flexinsight.domain.model.TrainingLoadScore
import com.jdluu.flexinsight.domain.usecase.CalculateTrainingLoadUseCase
import com.jdluu.flexinsight.domain.usecase.DetectDeloadUseCase
import com.jdluu.flexinsight.ui.common.LoadingState
import com.jdluu.flexinsight.ui.common.UiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class DashboardUiState(
    val loadingState: LoadingState = LoadingState.Idle,
    val error: UiError? = null,
    val profileInfo: ProfileInfo? = null,
    val latestWorkout: Workout? = null,
    val latestWorkoutStats: SingleWorkoutStats? = null,
    val workoutStats: WorkoutStats? = null,
    val weeklyProgress: List<WeeklyProgress> = emptyList(),
    val currentStreak: Int = 0,
    val muscleGroupProgress: List<com.jdluu.flexinsight.data.model.MuscleGroupProgress> = emptyList(),
    val networkState: NetworkState = NetworkState.Unknown,
    val units: String = "Imperial",
    val dailyInsight: String? = null,
    val isGeneratingInsight: Boolean = false,
    val muscleRecovery: Map<com.jdluu.flexinsight.data.model.MuscleGroup, Float> = emptyMap(),
    val trainingLoad: TrainingLoadScore? = null,
    val deloadAlert: DeloadAlert? = null,
    val lastSyncAt: Long? = null,
    val isSyncing: Boolean = false
) {
    // Backward compatibility helper
    val isLoading: Boolean
        get() = loadingState.isLoading
}


@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FlexRepository,
    private val networkMonitor: NetworkMonitor,
    private val userPreferencesManager: UserPreferencesManager,
    private val aiClient: FlexAIClient,
    private val getWorkoutStatsUseCase: com.jdluu.flexinsight.domain.usecase.GetWorkoutStatsUseCase,
    private val getWeeklyProgressUseCase: com.jdluu.flexinsight.domain.usecase.GetWeeklyProgressUseCase,
    private val getMuscleRecoveryUseCase: com.jdluu.flexinsight.domain.usecase.GetMuscleRecoveryUseCase,
    private val calculateTrainingLoadUseCase: CalculateTrainingLoadUseCase,
    private val detectDeloadUseCase: DetectDeloadUseCase,
    private val syncPreferencesManager: SyncPreferencesManager,
    private val syncCoordinator: SyncCoordinator,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(loadingState = LoadingState.Loading))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Collect network state
        viewModelScope.launch {
            networkMonitor.networkState.collect { state ->
                _uiState.value = _uiState.value.copy(networkState = state)
            }
        }

        // Collect units preference
        viewModelScope.launch {
            userPreferencesManager.unitsFlow.collect { units ->
                _uiState.value = _uiState.value.copy(units = units)
            }
        }

        viewModelScope.launch {
            syncPreferencesManager.lastSyncAtFlow.collect { lastSync ->
                _uiState.value = _uiState.value.copy(lastSyncAt = lastSync)
            }
        }

        // Delay initialization slightly to ensure database is ready
        viewModelScope.launch {
            delay(100) // Small delay to ensure database is initialized
            loadDashboardData()
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(loadingState = LoadingState.Loading, error = null)

                // Load latest workout - use first() to get initial value instead of continuous collection
                val workouts = try {
                    repository.getRecentWorkouts(limit = 1).first()
                } catch (e: Exception) {
                    val apiError = ErrorHandler.handleError(e)
                    _uiState.value = _uiState.value.copy(
                        error = UiError.fromApiError(apiError),
                        loadingState = LoadingState.Error(apiError)
                    )
                    return@launch
                }

                try {
                    val latestWorkout = workouts.firstOrNull()

                    // Execute independent fetches concurrently using async
                    coroutineScope {
                        val profileInfoDeferred = async {
                            try { repository.getProfileInfo() } catch (e: Exception) { 
                                android.util.Log.e("DashboardViewModel", "Failed to load profile info", e)
                                null 
                            }
                        }
                        
                        val latestWorkoutStatsDeferred = async {
                            latestWorkout?.let {
                                try { repository.calculateWorkoutStats(it) } catch (e: Exception) { 
                                    android.util.Log.e("DashboardViewModel", "Failed to calculate latest workout stats", e)
                                    null 
                                }
                            }
                        }
                        
                        val statsDeferred = async {
                            try { getWorkoutStatsUseCase() } catch (e: Exception) {
                                android.util.Log.e("DashboardViewModel", "Failed to calculate overall workout stats", e)
                                WorkoutStats(
                                    totalWorkouts = 0, totalVolume = 0.0, averageVolume = 0.0,
                                    totalSets = 0, totalDuration = 0L, averageDuration = 0L,
                                    currentStreak = 0, longestStreak = 0, bestWeekVolume = 0.0,
                                    bestWeekDate = null
                                )
                            }
                        }
                        
                        val weeklyProgressDeferred = async {
                            try { getWeeklyProgressUseCase(weeks = 4) } catch (e: Exception) { 
                                android.util.Log.e("DashboardViewModel", "Failed to get weekly progress", e)
                                emptyList() 
                            }
                        }
                        
                        val muscleGroupProgressDeferred = async {
                            try { repository.getMuscleGroupProgress(weeks = 4) } catch (e: Exception) { 
                                android.util.Log.e("DashboardViewModel", "Failed to get muscle group progress", e)
                                emptyList() 
                            }
                        }
                        
                        val muscleRecoveryDeferred = async {
                            try { getMuscleRecoveryUseCase() } catch (e: Exception) { 
                                android.util.Log.e("DashboardViewModel", "Failed to get muscle recovery", e)
                                emptyMap() 
                            }
                        }

                        val trainingLoadDeferred = async {
                            runCatching { calculateTrainingLoadUseCase() }.getOrNull()
                        }

                        val deloadDeferred = async {
                            runCatching { detectDeloadUseCase() }.getOrNull()
                        }

                        // Await all results
                        val profileInfo = profileInfoDeferred.await()
                        val latestWorkoutStats = latestWorkoutStatsDeferred.await()
                        val stats = statsDeferred.await()
                        val weeklyProgress = weeklyProgressDeferred.await()
                        val muscleGroupProgress = muscleGroupProgressDeferred.await()
                        val muscleRecovery = muscleRecoveryDeferred.await()
                        val trainingLoad = trainingLoadDeferred.await()
                        val deloadAlert = deloadDeferred.await()

                        _uiState.value = _uiState.value.copy(
                            loadingState = LoadingState.Success,
                            profileInfo = profileInfo,
                            latestWorkout = latestWorkout,
                            latestWorkoutStats = latestWorkoutStats,
                            workoutStats = stats,
                            weeklyProgress = weeklyProgress,
                            currentStreak = stats.currentStreak,
                            muscleGroupProgress = muscleGroupProgress,
                            muscleRecovery = muscleRecovery,
                            trainingLoad = trainingLoad,
                            deloadAlert = deloadAlert,
                            error = null
                        )
                        
                        widgetUpdater.updateFromDashboard(
                            streak = stats.currentStreak,
                            recoveryScore = (muscleRecovery.values.average() * 100).toInt()
                                .coerceIn(0, 100),
                            nextWorkoutLabel = runCatching {
                                repository.getPlannedWorkoutsForDay(System.currentTimeMillis())
                                    .firstOrNull()?.name
                            }.getOrNull()
                        )

                        generateDailyInsight(profileInfo?.displayName ?: "User", stats.currentStreak)
                    }

                } catch (e: Exception) {
                    val apiError = ErrorHandler.handleError(e)
                    _uiState.value = _uiState.value.copy(
                        loadingState = LoadingState.Error(apiError),
                        error = UiError.fromApiError(apiError)
                    )
                }
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Error(apiError),
                    error = UiError.fromApiError(apiError)
                )
            }
        }
    }
    
    private suspend fun generateDailyInsight(userName: String, streak: Int) {
        if (!aiClient.isAvailable()) return
        if (_uiState.value.dailyInsight != null) return

        _uiState.value = _uiState.value.copy(isGeneratingInsight = true)

        val prompt = "Give a 1-sentence fitness tip for $userName who has a $streak day streak. Be brief and witty."
        val result = aiClient.generateResponse(prompt)

        _uiState.value = _uiState.value.copy(isGeneratingInsight = false)
        
        if (result is com.jdluu.flexinsight.core.errors.Result.Success) {
            _uiState.value = _uiState.value.copy(dailyInsight = result.data)
        }
    }

    fun refresh() {
        loadDashboardData()
    }

    fun sync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Loading,
                    isSyncing = true,
                    error = null
                )
                when (val result = repository.syncAllData()) {
                    is com.jdluu.flexinsight.core.errors.Result.Success -> {
                        syncCoordinator.onSyncComplete()
                        loadDashboardData()
                    }
                    is com.jdluu.flexinsight.core.errors.Result.Error -> {
                        val apiError = result.error
                        _uiState.value = _uiState.value.copy(
                            loadingState = LoadingState.Error(apiError),
                            error = UiError.fromApiError(apiError)
                        )
                    }
                }
            } catch (e: Exception) {
                val apiError = ErrorHandler.handleError(e)
                _uiState.value = _uiState.value.copy(
                    loadingState = LoadingState.Error(apiError),
                    error = UiError.fromApiError(apiError)
                )
            } finally {
                _uiState.value = _uiState.value.copy(isSyncing = false)
            }
        }
    }
}

