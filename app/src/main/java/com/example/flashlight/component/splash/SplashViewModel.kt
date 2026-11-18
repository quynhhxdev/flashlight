package com.example.flashlight.component.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlight.component.startup.SplashStartupProgress
import com.example.flashlight.utils.SpManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private var hasNavigated = false

    fun initSplash() {
        if (splashStartTime == 0L) {
            splashStartTime = System.currentTimeMillis()
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
        viewModelScope.launch {
            val totalTimeoutMillis = 25_000L
            val smoothFillDurationMillis = 2_000L
            var timeoutStartTime = 0L
            var isTimedOut = false
            var displayedProgress = 0f

            while (!hasNavigated) {
                val currentTime = System.currentTimeMillis()
                val totalElapsed = currentTime - splashStartTime

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

                val targetProgress = if (isNormalComplete || totalElapsed >= totalTimeoutMillis) {
                    if (!isTimedOut && !isNormalComplete && totalElapsed >= totalTimeoutMillis) {
                        isTimedOut = true
                        timeoutStartTime = currentTime
                    }

                    if (isNormalComplete) {
                        100
                    } else {
                        val elapsedSinceTimeout = currentTime - timeoutStartTime
                        val progressAtTimeout = displayedProgress.toInt()
                        val remainingProgress = 100 - progressAtTimeout
                        val fillPart = ((elapsedSinceTimeout.toFloat() / smoothFillDurationMillis) * remainingProgress).toInt()
                        (progressAtTimeout + fillPart).coerceIn(0, 100)
                    }
                } else {
                    val stage1 = (rawFetchProgress * 0.25f).toInt().coerceIn(0, 25)
                    val stage2 = if (stage1 >= 25 && isBannerFinished) 50 else if (stage1 >= 25) 37 else stage1
                    val stage3 = if (stage2 >= 50 && isSplashAdLoaded) 75 else if (stage2 >= 50) 62 else stage2
                    val stage4 = if (stage3 >= 75 && isNativeFullscreenFinished) 100 else if (stage3 >= 75) 87 else stage3
                    stage4.coerceIn(0, 99)
                }

                if (displayedProgress < targetProgress) {
                    displayedProgress = (displayedProgress + 1.2f).coerceAtMost(targetProgress.toFloat())
                } else if (displayedProgress > targetProgress) {
                    displayedProgress = targetProgress.toFloat()
                }

                val currentProgress = displayedProgress.toInt().coerceIn(0, 100)

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
                delay(100L)
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
