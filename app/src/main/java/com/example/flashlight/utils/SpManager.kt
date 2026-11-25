package com.example.flashlight.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.example.flashlight.App
import com.example.flashlight.data.model.Language
import com.example.flashlight.data.model.LanguageOptions
import com.example.flashlight.data.model.UserAttributionType
import com.kt.flashlight.R
import java.util.Locale

class SpManager(private val preferences: SharedPreferences) {
    companion object {
        private var instance: SpManager? = null

        fun getInstance(context: Context): SpManager {
            if (instance == null) {
                instance = SpManager(PreferenceManager.getDefaultSharedPreferences(context))
            }
            return instance!!
        }

        fun getInstance(): SpManager {
            if (instance == null) {
                instance = App.context?.let {
                    PreferenceManager.getDefaultSharedPreferences(
                        it
                    )
                }?.let { SpManager(it) }
            }
            return instance!!
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        preferences.edit {
            putBoolean(key, value)
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    fun getInt(key: String, defaultValue: Int): Int = preferences.getInt(key, defaultValue)

    fun putInt(key: String, value: Int) {
        preferences.edit {
            putInt(key, value)
        }
    }

    fun getLong(key: String, defaultValue: Long): Long =
        preferences.getLong(key, defaultValue)

    fun putLong(key: String, value: Long) {
        preferences.edit {
            putLong(key, value)
        }
    }

    fun getString(key: String, defaultValue: String): String? =
        preferences.getString(key, defaultValue)

    fun putString(key: String, value: String) {
        preferences.edit {
            putString(key, value)
        }
    }

    fun saveLanguage(language: Language) {
        preferences.edit {
            putString(Constant.KEY_SP_CURRENT_LANGUAGE, language.toJson())
        }
    }

    fun getLanguage(): Language {
        val savedLanguage = preferences.getString(Constant.KEY_SP_CURRENT_LANGUAGE, "")?.toLanguageModel()
        if (savedLanguage != null) return savedLanguage

        val deviceLanguageCode = App.context?.getDeviceLanguage() ?: Locale.getDefault().language
        val matchedLanguage = LanguageOptions.all.firstOrNull { it.languageCode.equals(deviceLanguageCode, ignoreCase = true) }

        return matchedLanguage ?: Language("en", R.drawable.ic_eng, R.string.english)
    }

    fun hasLanguageSaved(): Boolean {
        return preferences.contains(Constant.KEY_SP_CURRENT_LANGUAGE)
    }

    fun setLanguageChosen() {
        preferences.edit { putBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, true) }
    }

    fun isLanguageChosen(): Boolean {
        return preferences.getBoolean(Constant.KEY_SP_LANGUAGE_CHOSEN, false)
    }

    fun saveApplicationStop(isStop: Boolean) {
        preferences.edit { putBoolean(Constant.KEY_SP_IS_APPLICATION_STOP, isStop) }
    }

    fun isApplicationStopped(): Boolean {
        return preferences.getBoolean(Constant.KEY_SP_IS_APPLICATION_STOP, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        preferences.edit { putBoolean("KEY_SP_ONBOARDING_COMPLETED", completed) }
    }

    fun isOnboardingCompleted(): Boolean {
        return preferences.getBoolean("KEY_SP_ONBOARDING_COMPLETED", false)
    }

    // User Attribution (Paid / Organic)
    fun setUserAttributionType(type: UserAttributionType) {
        preferences.edit {
            putString(Constant.KEY_SP_USER_ATTRIBUTION_TYPE, type.name)
            when (type) {
                UserAttributionType.PAID -> putBoolean(Constant.KEY_SP_IS_PAID_USER, true)
                UserAttributionType.ORGANIC -> putBoolean(Constant.KEY_SP_IS_PAID_USER, false)
                UserAttributionType.UNKNOWN -> remove(Constant.KEY_SP_IS_PAID_USER)
            }
        }
    }

    fun getUserAttributionType(): UserAttributionType {
        val savedTypeStr = preferences.getString(Constant.KEY_SP_USER_ATTRIBUTION_TYPE, null)
        if (!savedTypeStr.isNullOrEmpty()) {
            return try {
                UserAttributionType.valueOf(savedTypeStr)
            } catch (e: Exception) {
                UserAttributionType.UNKNOWN
            }
        }
        if (preferences.contains(Constant.KEY_SP_IS_PAID_USER)) {
            return if (preferences.getBoolean(Constant.KEY_SP_IS_PAID_USER, false)) {
                UserAttributionType.PAID
            } else {
                UserAttributionType.ORGANIC
            }
        }
        return UserAttributionType.UNKNOWN
    }

    fun setPaidUser(isPaid: Boolean) {
        val type = if (isPaid) UserAttributionType.PAID else UserAttributionType.ORGANIC
        setUserAttributionType(type)
    }

    fun isPaidUser(): Boolean {
        return getUserAttributionType() == UserAttributionType.PAID
    }

    // Screen Light Color & Brightness
    fun saveScreenLightColor(color: Int) {
        preferences.edit { putInt("KEY_SCREEN_LIGHT_COLOR", color) }
    }
    fun getScreenLightColor(defaultColor: Int): Int {
        return preferences.getInt("KEY_SCREEN_LIGHT_COLOR", defaultColor)
    }

    fun saveScreenLightBrightness(brightness: Int) {
        preferences.edit { putInt("KEY_SCREEN_LIGHT_BRIGHTNESS", brightness) }
    }
    fun getScreenLightBrightness(defaultBrightness: Int): Int {
        return preferences.getInt("KEY_SCREEN_LIGHT_BRIGHTNESS", defaultBrightness)
    }

    // LED Settings
    fun saveLedText(text: String) {
        preferences.edit { putString("KEY_LED_TEXT", text) }
    }
    fun getLedText(defaultText: String): String {
        return preferences.getString("KEY_LED_TEXT", defaultText) ?: defaultText
    }

    fun saveLedFontSize(size: Int) {
        preferences.edit { putInt("KEY_LED_FONT_SIZE", size) }
    }
    fun getLedFontSize(defaultSize: Int): Int {
        return preferences.getInt("KEY_LED_FONT_SIZE", defaultSize)
    }

    fun saveLedScrollSpeed(speed: Int) {
        preferences.edit { putInt("KEY_LED_SCROLL_SPEED", speed) }
    }
    fun getLedScrollSpeed(defaultSpeed: Int): Int {
        return preferences.getInt("KEY_LED_SCROLL_SPEED", defaultSpeed)
    }

    fun saveLedColor(color: Int) {
        preferences.edit { putInt("KEY_LED_COLOR", color) }
    }
    fun getLedColor(defaultColor: Int): Int {
        return preferences.getInt("KEY_LED_COLOR", defaultColor)
    }

    fun saveLedShadowColor(color: Int) {
        preferences.edit { putInt("KEY_LED_SHADOW_COLOR", color) }
    }
    fun getLedShadowColor(defaultColor: Int): Int {
        return preferences.getInt("KEY_LED_SHADOW_COLOR", defaultColor)
    }
    fun hasLedShadowColor(): Boolean {
        return preferences.contains("KEY_LED_SHADOW_COLOR")
    }
    fun getLedShadowColorOrNull(): Int? {
        return if (preferences.contains("KEY_LED_SHADOW_COLOR")) {
            preferences.getInt("KEY_LED_SHADOW_COLOR", 0)
        } else {
            null
        }
    }

    fun saveLedDirection(direction: Int) {
        preferences.edit { putInt("KEY_LED_DIRECTION", direction) }
    }
    fun getLedDirection(defaultDirection: Int): Int {
        return preferences.getInt("KEY_LED_DIRECTION", defaultDirection)
    }

    fun saveLedEffect(effect: Int) {
        preferences.edit { putInt("KEY_LED_EFFECT", effect) }
    }
    fun getLedEffect(defaultEffect: Int): Int {
        return preferences.getInt("KEY_LED_EFFECT", defaultEffect)
    }

}
