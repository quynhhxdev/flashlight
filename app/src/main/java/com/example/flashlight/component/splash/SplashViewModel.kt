package com.example.flashlight.component.splash

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlight.component.startup.SplashStartupProgress
import com.example.flashlight.utils.SpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SplashDestination {
    MAIN,
    LANGUAGE
}

data class SplashUiState(
    val progress: Int = 0,
    val isProgressCompleted: Boolean = false,
    val isBannerFinished: Boolean = false,
    val isNativeFullscreenConfigReady: Boolean = false,
    val isNativeFullscreenEnabled: Boolean = false
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val spManager: SpManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private var splashStartTime = 0L
    private var splashStartRealtime = 0L
    private var hasNavigated = false
    private var loadingJob: Job? = null

    companion object {
        private const val TOTAL_TIMEOUT_MILLIS = 25_000L
        private const val SMOOTH_FILL_DURATION_MILLIS = 2_000L
        private const val STAGE_DURATION_MILLIS = 5_000L
        private const val LOOP_DELAY_MILLIS = 50L
    }

    fun initSplash() {
        if (splashStartTime == 0L) {
            splashStartTime = System.currentTimeMillis()
            splashStartRealtime = SystemClock.elapsedRealtime()
        }
    }

    fun getSplashStartTime(): Long = splashStartTime

    fun updateBannerFinished(finished: Boolean) {
        _uiState.update { it.copy(isBannerFinished = finished) }
    }

    fun updateNativeFullscreenConfig(ready: Boolean, enabled: Boolean) {
        _uiState.update {
            it.copy(
                isNativeFullscreenConfigReady = ready,
                isNativeFullscreenEnabled = enabled
            )
        }
    }

    fun startLoadingProcess(
        isSplashAdLoadedProvider: () -> Boolean,
        isNativeFullscreenFinishedProvider: (Boolean, Boolean) -> Boolean
    ) {
        if (loadingJob?.isActive == true) return

        loadingJob = viewModelScope.launch {
            if (splashStartRealtime == 0L) {
                splashStartRealtime = SystemClock.elapsedRealtime()
            }
            if (splashStartTime == 0L) {
                splashStartTime = System.currentTimeMillis()
            }

            var stageStartRealtime = splashStartRealtime
            var currentStage = 1
            var displayedProgress = 0f
            var timeoutStartTimeRealtime = 0L
            var isTimedOut = false

            while (!hasNavigated) {
                val currentRealtime = SystemClock.elapsedRealtime()
                val totalElapsed = currentRealtime - splashStartRealtime

                val rawFetchProgress = SplashStartupProgress.progress.value
                val isFetchDone = rawFetchProgress >= 100
                val isSplashAdLoaded = isSplashAdLoadedProvider()

                val currentState = _uiState.value
                val isBannerFinished = currentState.isBannerFinished
                val isNativeFullscreenReady = currentState.isNativeFullscreenConfigReady
                val isNativeFullscreenEnabled = currentState.isNativeFullscreenEnabled

                val isNativeFullscreenFinished = isNativeFullscreenFinishedProvider(
                    isNativeFullscreenReady,
                    isNativeFullscreenEnabled
                )

                val isNormalComplete = isFetchDone && isBannerFinished && isSplashAdLoaded && isNativeFullscreenFinished

                val targetProgress: Float = if (isNormalComplete || totalElapsed >= TOTAL_TIMEOUT_MILLIS) {
                    if (!isTimedOut && !isNormalComplete && totalElapsed >= TOTAL_TIMEOUT_MILLIS) {
                        isTimedOut = true
                        timeoutStartTimeRealtime = currentRealtime
                    }

                    if (isNormalComplete) {
                        100f
                    } else {
                        val elapsedSinceTimeout = currentRealtime - timeoutStartTimeRealtime
                        val progressAtTimeout = displayedProgress
                        val remainingProgress = 100f - progressAtTimeout
                        val fillPart = (elapsedSinceTimeout.toFloat() / SMOOTH_FILL_DURATION_MILLIS) * remainingProgress
                        (progressAtTimeout + fillPart).coerceIn(0f, 100f)
                    }
                } else {
                    val stageMin = (currentStage - 1) * 25f
                    val stageMax = currentStage * 25f

                    val elapsedInStage = (currentRealtime - stageStartRealtime).coerceAtLeast(0L)
                    val timeRatio = (elapsedInStage.toFloat() / STAGE_DURATION_MILLIS).coerceIn(0f, 1f)
                    val timeBasedProgress = stageMin + timeRatio * 25f

                    val taskRatio = when (currentStage) {
                        1 -> (rawFetchProgress / 100f).coerceIn(0f, 1f)
                        2 -> if (isBannerFinished) 1f else 0f
                        3 -> if (isSplashAdLoaded) 1f else 0f
                        4 -> if (isNativeFullscreenFinished) 1f else 0f
                        else -> 1f
                    }
                    val taskProgress = stageMin + taskRatio * 25f

                    val stageTarget = maxOf(timeBasedProgress, taskProgress).coerceIn(stageMin, stageMax)
                    maxOf(displayedProgress, stageTarget)
                }

                if (displayedProgress < targetProgress) {
                    val remainingToTarget = targetProgress - displayedProgress
                    val step = maxOf(0.25f, remainingToTarget / 15f).coerceAtMost(1.5f)
                    displayedProgress = (displayedProgress + step).coerceAtMost(targetProgress)
                } else if (displayedProgress > targetProgress) {
                    displayedProgress = targetProgress
                }

                val currentProgress = displayedProgress.toInt().coerceIn(0, 100)

                val isStageTaskComplete = when (currentStage) {
                    1 -> isFetchDone
                    2 -> isBannerFinished
                    3 -> isSplashAdLoaded
                    4 -> isNativeFullscreenFinished
                    else -> true
                }
                val isStageTimeBudgetExhausted = (currentRealtime - stageStartRealtime) >= STAGE_DURATION_MILLIS
                val stageMax = currentStage * 25f

                if (currentStage < 4 && !isNormalComplete && totalElapsed < TOTAL_TIMEOUT_MILLIS) {
                    if (displayedProgress >= stageMax ||
                        (isStageTaskComplete && displayedProgress >= stageMax - 0.5f) ||
                        (isStageTimeBudgetExhausted && displayedProgress >= stageMax - 0.5f)
                    ) {
                        displayedProgress = maxOf(displayedProgress, stageMax)
                        currentStage++
                        stageStartRealtime = currentRealtime
                    }
                }

                val reachedMax = currentProgress >= 100
                _uiState.update {
                    it.copy(
                        progress = currentProgress,
                        isProgressCompleted = it.isProgressCompleted || reachedMax
                    )
                }

                if (reachedMax) {
                    break
                }
                delay(LOOP_DELAY_MILLIS)
            }
        }
    }

    fun markNavigated(): Boolean {
        if (hasNavigated) return false
        hasNavigated = true
        return true
    }

    fun getNavigationDestination(): SplashDestination {
        return if (spManager.isOnboardingCompleted()) {
            SplashDestination.MAIN
        } else {
            SplashDestination.LANGUAGE
        }
    }
}

