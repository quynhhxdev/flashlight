package com.example.flashlight.component.main.fagment.led

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LedConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LedConfigRepository(application)
    private val _config = MutableStateFlow(repository.initialConfig)
    val config = _config.asStateFlow()

    init {
        viewModelScope.launch {
            repository.migrateLegacySettings()
            repository.config.collect { _config.value = it }
        }
    }

    fun setText(text: String) {
        _config.update { it.copy(text = text) }
        viewModelScope.launch { repository.setText(text) }
    }

    fun setFontSizeSp(size: Int) {
        persistFontSizeSp(size)
    }

    fun updateFontSizeInMemory(size: Int) {
        _config.update { it.copy(fontSizeSp = size) }
    }

    fun persistFontSizeSp(size: Int) {
        _config.update { it.copy(fontSizeSp = size) }
        viewModelScope.launch { repository.setFontSizeSp(size) }
    }

    fun setTextColor(color: Int) {
        _config.update { it.copy(textColor = color) }
        viewModelScope.launch { repository.setTextColor(color) }
    }

    fun setShadowColor(color: Int) {
        _config.update { it.copy(shadowColor = color) }
        viewModelScope.launch { repository.setShadowColor(color) }
    }

    fun setScrollSpeed(speed: Int) {
        persistScrollSpeed(speed)
    }

    fun updateScrollSpeedInMemory(speed: Int) {
        _config.update { it.copy(scrollSpeed = speed) }
    }

    fun persistScrollSpeed(speed: Int) {
        _config.update { it.copy(scrollSpeed = speed) }
        viewModelScope.launch { repository.setScrollSpeed(speed) }
    }

    fun setDirection(directionId: Int) {
        _config.update { it.copy(directionId = directionId) }
        viewModelScope.launch { repository.setDirection(directionId) }
    }

    fun setEffect(effectId: Int) {
        _config.update { it.copy(effectId = effectId) }
        viewModelScope.launch { repository.setEffect(effectId) }
    }
}
