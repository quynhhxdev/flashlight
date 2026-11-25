package com.example.flashlight.utils.ads.manager

import com.ads.admob.admob.AdmobFactory
import com.ads.admob.billing.factory.IapFactory
import com.example.flashlight.component.startup.SplashStartupProgress
import com.example.flashlight.data.model.UserAttributionType
import com.example.flashlight.domain.layer.AdConfig
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.SpManager
import com.example.flashlight.utils.ads.AdPlacement
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson

class FirebaseConfigManager {

    companion object {
        const val KEY_ADS_CONFIG_ORGANIC = "ad_config_organic"
        const val KEY_ADS_CONFIG_PAID = "ad_config_paid_user"

        const val KEY_REMOTE_CONFIG_OGANIC = "remote_config_flashlight_oganic"
        const val KEY_REMOTE_CONFIG_PAID = "remote_config_flashlight_paid"

        const val KEY_VERSION_FORCE = "version_force"
        const val KEY_IS_FORCE_UPDATE = "is_force_update"

        // Cấu hình fix cứng Organic (Normal) cho Debug/Test
        val ORGANIC_CONFIG = AdConfig(
            configAds = true,
            adsAppOpen = true,
            adsAppOpenResume = true,
            adsBanner = true,
            adsBannerAll = true,
            adsBannerSplash = true,
            adsInterBack = true,
            adsInterFeatureFrequency = 2L,
            adsInterHome = true,
            adsInterSplash = true,
            adsInterSplash2f = false,
            adsInterstitial = true,
            adsNative = true,
            adsNativeCustom = true,
            adsNativeFullSplash = false,
            adsNativeFullSplash2f = false,
            adsNativeLanguage = true,
            adsNativeLanguage2f = false,
            adsNativeLanguageSelect = true,
            adsNativeLanguageSelect2f = false,
            adsNativeObd1 = true,
            adsNativeObd12Full = true,
            adsNativeObd12f = false,
            adsNativeObd3 = true,
            adsNativeObd34Full = true,
            adsNativeObd32f = false,
            adsNativePermission = true,
            intervalBetweenInterstitial = 30000L
        )

        // Cấu hình fix cứng Paid (Bật true HẾT tất cả các quảng cáo kể cả 2F) cho Debug/Test
        val PAID_CONFIG = AdConfig(
            configAds = true,
            adsAppOpen = true,
            adsAppOpenResume = true,
            adsBanner = true,
            adsBannerAll = true,
            adsBannerSplash = true,
            adsInterBack = true,
            adsInterFeatureFrequency = 2L,
            adsInterHome = true,
            adsInterSplash = true,
            adsInterSplash2f = true,
            adsInterstitial = true,
            adsNative = true,
            adsNativeCustom = true,
            adsNativeFullSplash = true,
            adsNativeFullSplash2f = true,
            adsNativeLanguage = true,
            adsNativeLanguage2f = true,
            adsNativeLanguageSelect = true,
            adsNativeLanguageSelect2f = true,
            adsNativeObd1 = true,
            adsNativeObd12Full = true,
            adsNativeObd12f = true,
            adsNativeObd3 = true,
            adsNativeObd34Full = true,
            adsNativeObd32f = true,
            adsNativePermission = true,
            intervalBetweenInterstitial = 30000L
        )

        private var mInstance: FirebaseConfigManager = FirebaseConfigManager()
        fun instance(): FirebaseConfigManager {
            return mInstance
        }
    }

    private val mMaxTryTime = 5
    private var mTryTime = 0

    private var isFetchDone = false
    var versionForce = "1.0.0"
    var isForceUpdate = false
    private val pendingActions: MutableList<() -> Unit> = mutableListOf()

    // Công tắc bật/tắt sử dụng cấu hình fix cứng local cho mục đích Debug/Test
    var isUseHardcodedLocalConfigForTest: Boolean = false
    private var hardcodedIsPaid: Boolean? = null

    var adConfig: AdConfig = AdConfig()

    /**
     * Hàm kích hoạt sử dụng cấu hình fix cứng (Organic hoặc Paid) cho bản Debug/Release Test mà không gọi Firebase
     * sử dụng gọi hàm này trong oncreate thay cho FirebaseConfigManager.instance().fetch(.......)
     */
    fun useHardcodedConfig(
        isPaid: Boolean = isPaidUser(),
        onFetchComplete: (() -> Unit)? = null,
        onFetchProgress: ((Int) -> Unit)? = null
    ) {
        isUseHardcodedLocalConfigForTest = true
        hardcodedIsPaid = isPaid
        adConfig = if (isPaid) PAID_CONFIG else ORGANIC_CONFIG
        Logger.e("[AdsConfig] Using hardcoded ${if (isPaid) "PAID" else "ORGANIC"} config for testing")

        updateAdmobFactorySettings()
        SplashStartupProgress.update(100)
        onFetchProgress?.invoke(100)
        onFetchFinished(onFetchComplete)
    }

    fun doAfterFetch(action: () -> Unit) {
        if (isFetchDone) {
            action()
        } else {
            pendingActions.add(action)
        }
    }

    fun getUserAttributionType(): UserAttributionType {
        if (isUseHardcodedLocalConfigForTest) {
            return if (hardcodedIsPaid == true) UserAttributionType.PAID else UserAttributionType.ORGANIC
        }
        return SpManager.getInstance().getUserAttributionType()
    }

    fun isPaidUser(): Boolean {
        if (isUseHardcodedLocalConfigForTest) {
            return hardcodedIsPaid ?: false
        }
        val type = getUserAttributionType()
        if (type == UserAttributionType.PAID) return true
        return try {
            IapFactory.getInstance().isProductPurchased()
        } catch (e: Exception) {
            false
        }
    }

    fun parseConfigJson() {
        if (isUseHardcodedLocalConfigForTest) {
            val isPaid = hardcodedIsPaid ?: isPaidUser()
            adConfig = if (isPaid) PAID_CONFIG else ORGANIC_CONFIG
            Logger.e("[AdsConfig] Hardcoded config applied (${if (isPaid) "PAID" else "ORGANIC"})")
            updateAdmobFactorySettings()
            return
        }

        val attributionType = getUserAttributionType()
        val isPaid = (attributionType == UserAttributionType.PAID)

        val primaryKey = if (isPaid) KEY_ADS_CONFIG_PAID else KEY_ADS_CONFIG_ORGANIC
        val fallbackKey = if (isPaid) KEY_REMOTE_CONFIG_PAID else KEY_REMOTE_CONFIG_OGANIC

        Logger.e("[AdsConfig] Current attribution status: $attributionType")
        Logger.e("[AdsConfig] Selected parameter: $primaryKey (fallback: $fallbackKey)")

        var jsonString = getStringFromRemote(primaryKey)
        if (jsonString.isBlank()) {
            jsonString = getStringFromRemote(fallbackKey)
        }

        if (jsonString.isNotBlank()) {
            val parsed = parseAndValidateJson(jsonString)
            if (parsed != null) {
                adConfig = parsed
                Logger.e("[AdsConfig] Config parsing: SUCCESS")
                Logger.e("[AdsConfig] Active config: ${if (isPaid) "PAID" else if (attributionType == UserAttributionType.ORGANIC) "ORGANIC" else "UNKNOWN (Default Organic Config)"}")
                updateAdmobFactorySettings()
                return
            } else {
                Logger.e("[AdsConfig] JSON parsing failed for $primaryKey, using safe fallback")
            }
        } else {
            Logger.e("[AdsConfig] Config JSON for $primaryKey is blank, using safe fallback")
        }

        if (isPaid) {
            var organicJson = getStringFromRemote(KEY_ADS_CONFIG_ORGANIC)
            if (organicJson.isBlank()) {
                organicJson = getStringFromRemote(KEY_REMOTE_CONFIG_OGANIC)
            }
            if (organicJson.isNotBlank()) {
                val parsedOrganic = parseAndValidateJson(organicJson)
                if (parsedOrganic != null) {
                    adConfig = parsedOrganic
                    Logger.e("[AdsConfig] Fallback to Organic Remote JSON for Paid user succeeded")
                    updateAdmobFactorySettings()
                    return
                }
            }
        }

        adConfig = if (isPaid) PAID_CONFIG else ORGANIC_CONFIG
        Logger.e("[AdsConfig] Using safe fallback (Hardcoded default selected: ${if (isPaid) "PAID" else "ORGANIC"})")
        updateAdmobFactorySettings()
    }

    private fun parseAndValidateJson(json: String): AdConfig? {
        return try {
            Gson().fromJson(json, AdConfig::class.java)
        } catch (e: Exception) {
            Logger.e("[AdsConfig] Invalid JSON parsing error: ${e.message}")
            null
        }
    }

    fun onAttributionUpdated(newType: UserAttributionType) {
        Logger.e("[Attribution] Attribution updated to $newType. Refreshing Ads Config...")
        parseConfigJson()
    }

    fun invalidateCache() {
        Logger.e("[AdsConfig] Invalidate cache called. Refreshing active config...")
        parseConfigJson()
    }

    fun updateAdmobFactorySettings() {
        val intervalMs = AdConfig.normalizeDurationMs(adConfig.intervalBetweenInterstitial)
        AdmobFactory.INSTANCE.updateIntervalBetweenInterstitial(intervalMs)
        Logger.e("[AdmobFactory] Interstitial interval updated: $intervalMs ms")
        Logger.e("[AdmobFactory] Ads configuration updated successfully")
    }

    fun fetch(
        onFetchComplete: (() -> Unit)? = null,
        onFetchProgress: ((Int) -> Unit)? = null
    ) {
        if (isUseHardcodedLocalConfigForTest) {
            parseConfigJson()
            SplashStartupProgress.update(100)
            onFetchProgress?.invoke(100)
            onFetchFinished(onFetchComplete)
            return
        }

        Logger.e("[AdsConfig] Remote Config fetch started")
        val longCache = 15 * 60L
        FirebaseRemoteConfig.getInstance().fetch(longCache).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Logger.e("[AdsConfig] Remote Config fetch succeeded")
                FirebaseRemoteConfig.getInstance().activate().addOnCompleteListener {
                    Logger.e("[AdsConfig] Remote Config activation completed")
                    versionForce = FirebaseRemoteConfig.getInstance().getString(KEY_VERSION_FORCE)
                    isForceUpdate = FirebaseRemoteConfig.getInstance().getBoolean(KEY_IS_FORCE_UPDATE)

                    parseConfigJson()

                    mTryTime = 0
                    onFetchProgress?.invoke(100)
                    onFetchFinished(onFetchComplete)
                }
            } else {
                Logger.e("[AdsConfig] Remote Config fetch failed")
                tryFetchAgain(onFetchComplete, onFetchProgress)
            }
        }
    }

    private fun onFetchFinished(onFetchComplete: (() -> Unit)?) {
        isFetchDone = true
        onFetchComplete?.invoke()
        pendingActions.forEach { it() }
        pendingActions.clear()
        Logger.e("[AdsConfig] Configuration update completed")
    }

    private fun tryFetchAgain(
        onFetchComplete: (() -> Unit)? = null,
        onFetchProgress: ((Int) -> Unit)? = null
    ) {
        ++mTryTime
        if (mTryTime < mMaxTryTime) {
            onFetchProgress?.invoke(mTryTime * 90 / mMaxTryTime)
            fetch(onFetchComplete, onFetchProgress)
        } else {
            mTryTime = 0
            parseConfigJson()
            onFetchProgress?.invoke(100)
            onFetchFinished(onFetchComplete)
        }
    }

    fun refreshConfig() {
        parseConfigJson()
    }

    fun getBoolean(key: String, defaultValue: Boolean = true): Boolean {
        if (!adConfig.configAds) {
            return false
        }
        if (key == AdPlacement.CONFIG_ADS) {
            return adConfig.configAds
        }
        return adConfig.getBoolean(key, defaultValue)
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return adConfig.getLong(key, defaultValue)
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return getStringFromRemote(key, defaultValue)
    }

    private fun getStringFromRemote(key: String, defaultValue: String = ""): String {
        return try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            if (remoteConfig.all.containsKey(key)) {
                remoteConfig.getString(key).ifEmpty { defaultValue }
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun isAdEnabled(formatKey: String, placementKey: String): Boolean {
        val global = getBoolean(AdPlacement.CONFIG_ADS, true)
        if (!global) return false
        val format = getBoolean(formatKey, true)
        if (!format) return false
        return getBoolean(placementKey, true)
    }

    fun isBannerAdEnabled(placementKey: String): Boolean {
        return isAdEnabled(AdPlacement.ADS_BANNER_ALL_ENABLED, placementKey)
    }

    fun isInterAdEnabled(placementKey: String): Boolean {
        return isAdEnabled(AdPlacement.ADS_INTERSTITIAL_ENABLED, placementKey)
    }

    fun isNativeAdEnabled(placementKey: String): Boolean {
        return isAdEnabled(AdPlacement.ADS_NATIVE_ENABLED, placementKey)
    }

    fun isAppOpenAdEnabled(placementKey: String): Boolean {
        return isAdEnabled(AdPlacement.ADS_APP_OPEN_ENABLED, placementKey)
    }
}
