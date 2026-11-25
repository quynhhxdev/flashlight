package com.example.flashlight.data.model

data class Language (
    val languageCode: String,
    val iconRes: Int,
    val nameRes: Int,
    val nativeName: String = "",
    var selected: Boolean = false
)
