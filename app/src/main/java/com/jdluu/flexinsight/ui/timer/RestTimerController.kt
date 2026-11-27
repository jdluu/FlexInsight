package com.jdluu.flexinsight.ui.timer

import com.jdluu.flexinsight.core.dispatchers.DispatcherProvider
import com.jdluu.flexinsight.domain.calc.RestTimerFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class RestTimerState(
    val totalSeconds: Int = 90,
    val remainingSeconds: Int = 90,
    val isRunning: Boolean = false,
    val hasCompleted: Boolean = false,
    val isVisibleInBanner: Boolean = false
) {
    val progress: Float
        get() = RestTimerFormatter.progressFraction(remainingSeconds, totalSeconds)

    val formattedRemaining: String
        get() = RestTimerFormatter.formatCountdown(remainingSeconds)
}

/**
 * Process-scoped rest timer controller so active rest intervals survive screen transitions
 * and surface in the bottom live bar across Training, Planner, and Workout Detail flows.
 */
@Singleton
class RestTimerController @Inject constructor(
    dispatcherProvider: DispatcherProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(RestTimerState())
    val state: StateFlow<RestTimerState> = _state.asStateFlow()

    @Synchronized
    fun startPreset(seconds: Int) {
        val clamped = seconds.coerceAtLeast(1)
        tickerJob?.cancel()
        _state.update {
            RestTimerState(
                totalSeconds = clamped,
                remainingSeconds = clamped,
                isRunning = true,
                hasCompleted = false,
                isVisibleInBanner = true
            )
        }
        launchTicker()
    }

    @Synchronized
    fun togglePause() {
        val current = _state.value
        if (current.isRunning) {
            tickerJob?.cancel()
            tickerJob = null
            _state.update { it.copy(isRunning = false) }
        } else {
            tickerJob?.cancel()
            val restartSeconds = if (current.remainingSeconds <= 0) {
                current.totalSeconds.coerceAtLeast(1)
            } else {
                current.remainingSeconds
            }
            _state.update {
                it.copy(
                    remainingSeconds = restartSeconds,
                    isRunning = true,
                    hasCompleted = false,
                    isVisibleInBanner = true
                )
            }
            launchTicker()
        }
    }

    @Synchronized
    fun addSeconds(delta: Int) {
        val current = _state.value
        val nextRemaining = (current.remainingSeconds + delta).coerceAtLeast(0)
        val nextTotal = maxOf(current.totalSeconds, nextRemaining).coerceAtLeast(1)

        if (nextRemaining == 0) {
            tickerJob?.cancel()
            tickerJob = null
            _state.update {
                it.copy(
                    totalSeconds = nextTotal,
                    remainingSeconds = 0,
                    isRunning = false,
                    hasCompleted = true
                )
            }
            return
        }

        val shouldResume = delta > 0 && (current.isRunning || current.hasCompleted)
        _state.update {
            it.copy(
                totalSeconds = nextTotal,
                remainingSeconds = nextRemaining,
                isRunning = shouldResume,
                hasCompleted = false,
                isVisibleInBanner = if (shouldResume) true else it.isVisibleInBanner
            )
        }
        if (shouldResume && tickerJob?.isActive != true) {
            launchTicker()
        }
    }

    @Synchronized
    fun reset() {
        tickerJob?.cancel()
        tickerJob = null
        _state.update { current ->
            current.copy(
                remainingSeconds = current.totalSeconds,
                isRunning = false,
                hasCompleted = false,
                isVisibleInBanner = false
            )
        }
    }

    @Synchronized
    fun dismissBanner() {
        tickerJob?.cancel()
        tickerJob = null
        _state.update { current ->
            current.copy(
                remainingSeconds = if (current.hasCompleted) current.totalSeconds else current.remainingSeconds,
                isRunning = false,
                hasCompleted = false,
                isVisibleInBanner = false
            )
        }
    }

    private fun launchTicker() {
        tickerJob = scope.launch {
            while (isActive) {
                delay(1_000L)
                var finished = false
                _state.update { current ->
                    if (!current.isRunning || current.remainingSeconds <= 0) {
                        finished = true
                        current
                    } else {
                        val next = (current.remainingSeconds - 1).coerceAtLeast(0)
                        finished = next == 0
                        current.copy(
                            remainingSeconds = next,
                            isRunning = next > 0,
                            hasCompleted = next == 0
                        )
                    }
                }
                if (finished) break
            }
        }
    }
}
