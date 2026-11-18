package com.example.flashlight.component.main.fagment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlight.component.main.FlashUiState
import com.example.flashlight.data.model.FlashMode
import com.example.flashlight.data.repositories.FlashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class FlashlightViewModel @Inject constructor(
    private val repository: FlashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashUiState())
    val uiState: StateFlow<FlashUiState> = _uiState.asStateFlow()

    private var flashJob: Job? = null

    fun selectMode(mode: FlashMode) {
        setFlashMode(mode)
    }

    fun setFlashMode(mode: FlashMode) {
        stopFlash()
        _uiState.update {
            FlashUiState(
                selectedMode = mode,
                isEnabled = false
            )
        }
    }

    fun toggleFlash() {
        toggleFlashlight()
    }

    fun toggleFlashlight() {
        val currentState = _uiState.value

        if (currentState.isEnabled) {
            stopFlash()
            _uiState.update {
                it.copy(
                    isEnabled = false,
                    error = null
                )
            }
            return
        }

        if (!repository.isFlashAvailable) {
            updateError("Thiết bị không hỗ trợ đèn flash")
            return
        }

        _uiState.update {
            it.copy(
                isEnabled = true,
                error = null
            )
        }

        when (currentState.selectedMode) {
            FlashMode.FLASH -> {
                executeFlash(true)
            }
            FlashMode.SOS -> {
                startSosMode()
            }
            FlashMode.DJ -> {
                startDjMode()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun executeFlash(enabled: Boolean) {
        try {
            repository.setFlash(enabled)
        } catch (e: Exception) {
            handleFlashError()
        }
    }

    private fun startSosMode() {
        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            try {
                while (isActive) {
                    repeat(3) { blink(200, 200) }
                    delay(400)
                    repeat(3) { blink(600, 200) }
                    delay(400)
                    repeat(3) { blink(200, 200) }
                    delay(1200)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFlashError()
            }
        }
    }

    private fun startDjMode() {
        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            try {
                while (isActive) {
                    blink(250, 250)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFlashError()
            }
        }
    }

    private suspend fun blink(onDuration: Long, offDuration: Long) {
        repository.setFlash(true)
        delay(onDuration)
        repository.setFlash(false)
        delay(offDuration)
    }

    private fun stopFlash() {
        flashJob?.cancel()
        flashJob = null
        runCatching {
            repository.setFlash(false)
        }
    }

    private fun handleFlashError() {
        flashJob?.cancel()
        flashJob = null
        runCatching {
            repository.setFlash(false)
        }
        updateError("Không thể điều khiển đèn flash")
    }

    private fun updateError(message: String) {
        _uiState.update {
            it.copy(
                isEnabled = false,
                error = message
            )
        }
    }

    override fun onCleared() {
        stopFlash()
        super.onCleared()
    }
}
