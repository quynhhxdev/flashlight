package com.example.flashlight.utils.ads.manager

import com.example.flashlight.domain.layer.AdConfig
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.ads.AdPlacement
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson


class FirebaseConfigManager {

    companion object {
        const val KEY_ADS_STATUS_ENABLE = "ad_config"
        const val KEY_VERSION_FORCE = "version_force"
        const val KEY_IS_FORCE_UPDATE = "is_force_update"

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

    var adConfig: AdConfig = AdConfig()

    fun doAfterFetch(action: () -> Unit) {
        if (isFetchDone) {
            action()
        } else {
            pendingActions.add(action)
        }
    }

    fun fetch(
        onFetchComplete: (() -> Unit)? = null,
        onFetchProgress: ((Int) -> Unit)? = null
    ) {
        val longCache = 15 * 60L
        FirebaseRemoteConfig.getInstance().fetch(longCache).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Logger.e("load config success")
                FirebaseRemoteConfig.getInstance().activate()
                versionForce = FirebaseRemoteConfig.getInstance().getString(KEY_VERSION_FORCE)
                isForceUpdate = FirebaseRemoteConfig.getInstance().getBoolean(KEY_IS_FORCE_UPDATE)
                val json = FirebaseRemoteConfig.getInstance().getString(KEY_ADS_STATUS_ENABLE)
                Logger.e("FirebaseConfigManager : $json")
                if (json.isEmpty()) {
                    tryFetchAgain(onFetchComplete, onFetchProgress)
                } else {
                    try {
                        adConfig = Gson().fromJson(json, AdConfig::class.java)
                        mTryTime = 0
                        onFetchProgress?.invoke(100)
                        onFetchFinished(onFetchComplete)
                    } catch (e: Exception) {
                        tryFetchAgain(onFetchComplete, onFetchProgress)
                        Logger.e(e.message)
                    }
                }

            } else {
                tryFetchAgain(onFetchComplete, onFetchProgress)
                Logger.e("load config fail")
            }
        }
    }

    private fun onFetchFinished(onFetchComplete: (() -> Unit)?) {
        isFetchDone = true
        onFetchComplete?.invoke()
        pendingActions.forEach { it() }
        pendingActions.clear()
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
            onFetchProgress?.invoke(100)
            onFetchFinished(onFetchComplete)
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean = true): Boolean {
        return try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            if (remoteConfig.all.containsKey(key)) {
                remoteConfig.getBoolean(key)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
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

    fun getLong(key: String, defaultValue: Long): Long {
        return try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            if (remoteConfig.all.containsKey(key)) {
                remoteConfig.getLong(key)
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