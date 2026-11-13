package com.example.flashlight.component.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flashlight.data.model.FlashMode
import com.example.flashlight.data.repositories.FlashRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class FlashUiState(
    val selectedMode: FlashMode = FlashMode.FLASH,
    val isEnabled: Boolean = false,
    val error: String? = null
)

class MainViewModel(
    private val repository: FlashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlashUiState())
    val uiState = _uiState.asStateFlow()

    private var flashJob: Job? = null

    // Chuyển mode
    fun selectMode(mode: FlashMode) {
        stopFlash()

        _uiState.update {
            FlashUiState(
                selectedMode = mode,
                isEnabled = false
            )
        }
    }

    // Bật / tắt đèn
    fun toggleFlash() {
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

    // Flash Light: sáng liên tục
    private fun executeFlash(enabled: Boolean) {
        try {
            repository.setFlash(enabled)
        } catch (e: Exception) {
            handleFlashError()
        }
    }

    // SOS: 3 ngắn - 3 dài - 3 ngắn
    private fun startSosMode() {
        flashJob?.cancel()

        flashJob = viewModelScope.launch {
            try {
                while (isActive) {

                    // S: 3 nhịp ngắn
                    repeat(3) {
                        blink(200, 200)
                    }

                    delay(400)

                    // O: 3 nhịp dài
                    repeat(3) {
                        blink(600, 200)
                    }

                    delay(400)

                    // S: 3 nhịp ngắn
                    repeat(3) {
                        blink(200, 200)
                    }

                    // Khoảng nghỉ trước chu kỳ tiếp theo
                    delay(1200)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFlashError()
            }
        }
    }

    // DJ: nháy liên tục
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

    // Bật/tắt theo thời gian
    private suspend fun blink(
        onDuration: Long,
        offDuration: Long
    ) {
        repository.setFlash(true)
        delay(onDuration)

        repository.setFlash(false)
        delay(offDuration)
    }

    // Dừng tất cả hiệu ứng
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
