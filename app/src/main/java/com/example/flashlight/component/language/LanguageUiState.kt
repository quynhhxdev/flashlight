package com.example.flashlight.component.language

import com.example.flashlight.data.model.Language

data class LanguageUiState(
    val languages: List<Language> = emptyList(),
    val selectedLanguage: Language? = null,
    val isSaveVisible: Boolean = false
)
