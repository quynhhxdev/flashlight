package com.example.flashlight.component.main.fagment

import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.flashlight.utils.SpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ScreenLightUiState(
    val brightnessProgress: Int = 80,
    val baseColor: Int = Color.parseColor("#4FC3F7"),
    val adjustedColor: Int = Color.parseColor("#4FC3F7"),
    val brightnessText: String = "80%"
)

@HiltViewModel
class ScreenLightViewModel @Inject constructor(
    private val spManager: SpManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenLightUiState())
    val uiState: StateFlow<ScreenLightUiState> = _uiState.asStateFlow()

    init {
        loadSavedConfig()
    }

    private fun loadSavedConfig() {
        val defaultColor = Color.parseColor("#4FC3F7")
        val savedColor = spManager.getScreenLightColor(defaultColor)
        val savedBrightness = spManager.getScreenLightBrightness(80)
        val adjusted = adjustColorBrightness(savedColor, savedBrightness)

        _uiState.update {
            ScreenLightUiState(
                brightnessProgress = savedBrightness,
                baseColor = savedColor,
                adjustedColor = adjusted,
                brightnessText = "$savedBrightness%"
            )
        }
    }

    fun updateBrightness(progress: Int) {
        val currentColor = _uiState.value.baseColor
        val adjusted = adjustColorBrightness(currentColor, progress)
        spManager.saveScreenLightBrightness(progress)

        _uiState.update {
            it.copy(
                brightnessProgress = progress,
                adjustedColor = adjusted,
                brightnessText = "$progress%"
            )
        }
    }

    fun updateColor(newColor: Int) {
        val currentBrightness = _uiState.value.brightnessProgress
        val adjusted = adjustColorBrightness(newColor, currentBrightness)
        spManager.saveScreenLightColor(newColor)

        _uiState.update {
            it.copy(
                baseColor = newColor,
                adjustedColor = adjusted
            )
        }
    }

    fun adjustColorBrightness(color: Int, brightnessPercent: Int): Int {
        val factor = brightnessPercent / 100f
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
