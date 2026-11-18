package com.example.flashlight.component.onboard

import androidx.lifecycle.ViewModel
import com.example.flashlight.utils.SpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PermissionUiState(
    val isCameraGranted: Boolean = false,
    val isContinueEnabled: Boolean = false
)

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val spManager: SpManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    fun updateCameraPermission(isGranted: Boolean) {
        _uiState.update {
            it.copy(
                isCameraGranted = isGranted,
                isContinueEnabled = isGranted
            )
        }
    }

    fun completePermissionAndMarkOnboarding() {
        spManager.setOnboardingCompleted(true)
    }
}
