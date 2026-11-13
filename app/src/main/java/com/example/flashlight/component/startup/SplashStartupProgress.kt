package com.example.flashlight.component.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SplashStartupProgress {
    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    fun update(progress: Int) {
        _progress.value = maxOf(_progress.value, progress.coerceIn(0, 100))
    }
}
