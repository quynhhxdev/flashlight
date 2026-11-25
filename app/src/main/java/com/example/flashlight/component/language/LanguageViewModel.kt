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

    // Native ad state for the language screen
    private val _adUiState = MutableStateFlow(LanguageAdUiState())
    val adUiState: StateFlow<LanguageAdUiState> = _adUiState.asStateFlow()

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

    /**
     * Selects a language. Returns true if selection actually changed, false if same language.
     */
    fun selectLanguage(language: Language): Boolean {
        val currentSelected = _uiState.value.selectedLanguage
        // Guard: ignore tap on already-selected language
        if (currentSelected?.languageCode == language.languageCode) {
            return false
        }

        _uiState.value = _uiState.value.copy(
            selectedLanguage = language,
            isSaveVisible = true
        )
        return true
    }

    fun saveLanguage(): Language? {
        val language = _uiState.value.selectedLanguage ?: return null

        spManager.saveLanguage(language)
        spManager.setLanguageChosen()

        return language
    }

    // ── Ad State Management ──────────────────────────────────────────────────

    /** Call when user requests ad replacement (via language selection or ad cancel) */
    fun requestReplacement() {
        _adUiState.value = _adUiState.value.copy(replacementRequested = true)
    }

    /** Call when the select-phase ad request starts loading */
    fun onSelectAdLoading() {
        _adUiState.value = _adUiState.value.copy(
            isSelectAdLoading = true,
            isSelectAdReady = false
        )
    }

    /** Call when the select-phase ad has fully loaded (pending, not yet rendered) */
    fun onSelectAdLoaded() {
        _adUiState.value = _adUiState.value.copy(
            isSelectAdLoading = false,
            isSelectAdReady = true
        )
    }

    /** Call when the select-phase ad failed */
    fun onSelectAdFailed() {
        _adUiState.value = _adUiState.value.copy(
            isSelectAdLoading = false,
            isSelectAdReady = false
        )
    }

    /** Call after the pending ad has been committed and rendered */
    fun onSelectAdCommitted() {
        _adUiState.value = _adUiState.value.copy(
            replacementRequested = false,
            isSelectAdReady = false,
            isSelectAdLoading = false,
            isSelectAdDisplayed = true
        )
    }

    val isSelectAdDisplayed: Boolean
        get() = _adUiState.value.isSelectAdDisplayed

    val isSelectAdReady: Boolean
        get() = _adUiState.value.isSelectAdReady

    val isSelectAdLoading: Boolean
        get() = _adUiState.value.isSelectAdLoading

    val isReplacementPending: Boolean
        get() = _adUiState.value.replacementRequested
}

/**
 * Tracks native ad state for the language selection flow.
 *
 * [replacementRequested]  — user has selected a new language (or current ad was cancelled); needs ad swap
 * [isSelectAdLoading]     — the native_language_select request is in-flight
 * [isSelectAdReady]       — the select ad has loaded and is waiting in Pending
 * [isSelectAdDisplayed]   — the select ad has been committed and is currently displayed
 */
data class LanguageAdUiState(
    val replacementRequested: Boolean = false,
    val isSelectAdLoading: Boolean = false,
    val isSelectAdReady: Boolean = false,
    val isSelectAdDisplayed: Boolean = false
)
