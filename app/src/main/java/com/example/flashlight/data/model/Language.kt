package com.example.flashlight.data.model

data class Language (
    val languageCode: String,
    val iconRes: Int,
    var selected: Boolean = false
)