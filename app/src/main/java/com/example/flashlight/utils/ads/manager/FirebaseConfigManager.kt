package com.example.flashlight.utils.ads.manager

import com.ads.admob.billing.factory.IapFactory
import com.example.flashlight.component.startup.SplashStartupProgress
import com.example.flashlight.domain.layer.AdConfig
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.ads.AdPlacement
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson

class FirebaseConfigManager {

    companion object {
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
        Logger.e("FirebaseConfigManager: Using hardcoded ${if (isPaid) "PAID" else "ORGANIC"} config for testing")

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

    fun isPaidUser(): Boolean {
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
            Logger.e("FirebaseConfigManager: Hardcoded config applied (${if (isPaid) "PAID" else "ORGANIC"})")
            return
        }

        val isPaid = isPaidUser()
        val configKey = if (isPaid) KEY_REMOTE_CONFIG_PAID else KEY_REMOTE_CONFIG_OGANIC
        val jsonString = getStringFromRemote(configKey)

        Logger.e("FirebaseConfigManager: Selected configuration type = ${if (isPaid) "Paid" else "Normal"} ($configKey)")

        if (jsonString.isNotBlank()) {
            val parsed = parseAndValidateJson(jsonString)
            if (parsed != null) {
                adConfig = parsed
                Logger.e("FirebaseConfigManager: JSON parsing succeeded for $configKey. Master switch config_ads = ${adConfig.configAds}")
                return
            } else {
                Logger.e("FirebaseConfigManager: JSON parsing failed for $configKey, falling back")
            }
        } else {
            Logger.e("FirebaseConfigManager: Config JSON for $configKey is blank")
        }

        if (isPaid) {
            val normalJson = getStringFromRemote(KEY_REMOTE_CONFIG_OGANIC)
            if (normalJson.isNotBlank()) {
                val parsedNormal = parseAndValidateJson(normalJson)
                if (parsedNormal != null) {
                    adConfig = parsedNormal
                    Logger.e("FirebaseConfigManager: Fallback to Normal JSON for Paid user succeeded")
                    return
                }
            }
        }

        adConfig = if (isPaid) PAID_CONFIG else ORGANIC_CONFIG
        Logger.e("FirebaseConfigManager: Configuration fallback to hardcoded default selected")
    }

    private fun parseAndValidateJson(json: String): AdConfig? {
        return try {
            Gson().fromJson(json, AdConfig::class.java)
        } catch (e: Exception) {
            Logger.e("FirebaseConfigManager parse error: ${e.message}")
            null
        }
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

        Logger.e("FirebaseConfigManager: Remote Config fetch started")
        val longCache = 15 * 60L
        FirebaseRemoteConfig.getInstance().fetch(longCache).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Logger.e("FirebaseConfigManager: Remote Config fetch succeeded")
                FirebaseRemoteConfig.getInstance().activate().addOnCompleteListener {
                    Logger.e("FirebaseConfigManager: Remote Config activation completed")
                    versionForce = FirebaseRemoteConfig.getInstance().getString(KEY_VERSION_FORCE)
                    isForceUpdate = FirebaseRemoteConfig.getInstance().getBoolean(KEY_IS_FORCE_UPDATE)

                    parseConfigJson()

                    mTryTime = 0
                    onFetchProgress?.invoke(100)
                    onFetchFinished(onFetchComplete)
                }
            } else {
                Logger.e("FirebaseConfigManager: Remote Config fetch failed")
                tryFetchAgain(onFetchComplete, onFetchProgress)
            }
        }
    }

    private fun onFetchFinished(onFetchComplete: (() -> Unit)?) {
        isFetchDone = true
        onFetchComplete?.invoke()
        pendingActions.forEach { it() }
        pendingActions.clear()
        Logger.e("FirebaseConfigManager: Configuration update completed")
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
        return isAdEnabled(AdPlacement.ADS_BANNER_ENABLED, placementKey)
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
