package com.example.flashlight.component.language

import com.example.flashlight.base.viewmodel.BaseViewModel
import com.example.flashlight.data.model.Language
import com.example.flashlight.data.sources.LocalDataSource
import com.example.flashlight.utils.SpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val spManager: SpManager
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    fun loadLanguage(isFromSettings: Boolean) {
        val languages = localDataSource.getListLanguage()
        val hasSaveLanguage = spManager.hasLanguageSaved()

        val currentLanguage = if (hasSaveLanguage || isFromSettings) {
            spManager.getLanguage()
        } else {
            null
        }

        _uiState.value = LanguageUiState(
            languages = languages,
            selectedLanguage = currentLanguage,
            isSaveVisible = hasSaveLanguage || isFromSettings
        )

    }

    fun selectLanguage(language: Language) {
        _uiState.value = _uiState.value.copy(
            selectedLanguage = language,
            isSaveVisible = true
        )
    }

    fun saveLanguage(): Language? {
        val language = _uiState.value.selectedLanguage ?: return null

        spManager.saveLanguage(language)
        spManager.setLanguageChosen()

        return language
    }

}