package com.example.flashlight.component.main.fagment.led

import com.kt.flashlight.R

data class LedConfig(
    val text: String = "HELLO WORLD",
    val fontSizeSp: Int = 32,
    val textColor: Int = 0xFFFDE047.toInt(),
    val scrollSpeed: Int = 5,
    val directionId: Int = R.id.btnDirLeft,
    val effectId: Int = R.id.btnEffectGlow
)
