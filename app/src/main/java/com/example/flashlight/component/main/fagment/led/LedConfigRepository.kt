package com.example.flashlight.component.main.fagment.led

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flashlight.utils.SpManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ledSettingsDataStore by preferencesDataStore(name = "led_settings")

class LedConfigRepository(private val context: Context) {
    private val dataStore = context.applicationContext.ledSettingsDataStore
    private val legacyConfig = SpManager.getInstance(context.applicationContext).let { preferences ->
        LedConfig(
            text = preferences.getLedText(LedConfig().text),
            fontSizeSp = preferences.getLedFontSize(LedConfig().fontSizeSp).coerceAtLeast(14),
            textColor = preferences.getLedColor(LedConfig().textColor),
            shadowColor = preferences.getLedShadowColorOrNull(),
            scrollSpeed = preferences.getLedScrollSpeed(LedConfig().scrollSpeed).coerceIn(1, 10),
            directionId = preferences.getLedDirection(LedConfig().directionId),
            effectId = preferences.getLedEffect(LedConfig().effectId)
        )
    }

    val initialConfig: LedConfig
        get() = legacyConfig

    val config: Flow<LedConfig> = dataStore.data.map { preferences ->
        val hasCustomInSp = SpManager.getInstance(context.applicationContext).hasLedShadowColor()
        val rawShadowColor = preferences[Keys.SHADOW_COLOR] ?: legacyConfig.shadowColor
        val shadowColor = if (hasCustomInSp) rawShadowColor else null
        LedConfig(
            text = preferences[Keys.TEXT] ?: legacyConfig.text,
            fontSizeSp = preferences[Keys.FONT_SIZE_SP] ?: legacyConfig.fontSizeSp,
            textColor = preferences[Keys.TEXT_COLOR] ?: legacyConfig.textColor,
            shadowColor = shadowColor,
            scrollSpeed = preferences[Keys.SCROLL_SPEED] ?: legacyConfig.scrollSpeed,
            directionId = preferences[Keys.DIRECTION_ID] ?: legacyConfig.directionId,
            effectId = preferences[Keys.EFFECT_ID] ?: legacyConfig.effectId
        )
    }

    suspend fun migrateLegacySettings() {
        dataStore.edit { preferences ->
            if (preferences[Keys.TEXT] == null) preferences[Keys.TEXT] = legacyConfig.text
            if (preferences[Keys.FONT_SIZE_SP] == null) preferences[Keys.FONT_SIZE_SP] = legacyConfig.fontSizeSp
            if (preferences[Keys.TEXT_COLOR] == null) preferences[Keys.TEXT_COLOR] = legacyConfig.textColor
            legacyConfig.shadowColor?.let {
                if (preferences[Keys.SHADOW_COLOR] == null) preferences[Keys.SHADOW_COLOR] = it
            }
            if (preferences[Keys.SCROLL_SPEED] == null) preferences[Keys.SCROLL_SPEED] = legacyConfig.scrollSpeed
            if (preferences[Keys.DIRECTION_ID] == null) preferences[Keys.DIRECTION_ID] = legacyConfig.directionId
            if (preferences[Keys.EFFECT_ID] == null) preferences[Keys.EFFECT_ID] = legacyConfig.effectId
        }
    }

    suspend fun setText(text: String) = dataStore.edit { it[Keys.TEXT] = text }
    suspend fun setFontSizeSp(size: Int) = dataStore.edit { it[Keys.FONT_SIZE_SP] = size }
    suspend fun setTextColor(color: Int) = dataStore.edit { it[Keys.TEXT_COLOR] = color }
    suspend fun setShadowColor(color: Int) {
        SpManager.getInstance(context.applicationContext).saveLedShadowColor(color)
        dataStore.edit { it[Keys.SHADOW_COLOR] = color }
    }
    suspend fun setScrollSpeed(speed: Int) = dataStore.edit { it[Keys.SCROLL_SPEED] = speed }
    suspend fun setDirection(directionId: Int) = dataStore.edit { it[Keys.DIRECTION_ID] = directionId }
    suspend fun setEffect(effectId: Int) = dataStore.edit { it[Keys.EFFECT_ID] = effectId }

    private object Keys {
        val TEXT = stringPreferencesKey("text")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val TEXT_COLOR = intPreferencesKey("text_color")
        val SHADOW_COLOR = intPreferencesKey("shadow_color")
        val SCROLL_SPEED = intPreferencesKey("scroll_speed")
        val DIRECTION_ID = intPreferencesKey("direction_id")
        val EFFECT_ID = intPreferencesKey("effect_id")
    }
}
