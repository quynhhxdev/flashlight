package com.example.flashlight.component.splash

import androidx.lifecycle.ViewModel
import com.example.flashlight.component.startup.SplashStartupProgress

class SplashViewModel : ViewModel() {
    val progress = SplashStartupProgress.progress
}
