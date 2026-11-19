package com.example.flashlight.utils.ads.resolver

import com.example.flashlight.data.ads.model.ResolvedAds
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.example.flashlight.utils.ads.AdPlacement
import com.kt.flashlight.BuildConfig

object AdsIdResolver {

    fun resolve(placement: String): ResolvedAds? {
        val remoteManager = FirebaseConfigManager.instance()

        // Tier 1: Global Master Switch
        val globalEnabled = remoteManager.getBoolean(AdPlacement.CONFIG_ADS, true)
        if (!globalEnabled) return null

        // Tier 2: Format Switch
        val formatKey = when {
            placement.contains("banner", ignoreCase = true) -> AdPlacement.ADS_BANNER_ENABLED
            placement.contains("inter", ignoreCase = true) -> AdPlacement.ADS_INTERSTITIAL_ENABLED
            placement.contains("native", ignoreCase = true) -> AdPlacement.ADS_NATIVE_ENABLED
            placement.contains("app_open", ignoreCase = true) || placement.contains("resume", ignoreCase = true) || placement.contains("open", ignoreCase = true) -> AdPlacement.ADS_APP_OPEN_ENABLED
            else -> AdPlacement.ADS_INTERSTITIAL_ENABLED
        }
        val formatEnabled = remoteManager.getBoolean(formatKey, true)
        if (!formatEnabled) return null

        // Tier 3: Placement Switch & 2F Switch resolution
        val config = getPlacementConfig(placement)

        val normalEnabled = remoteManager.getBoolean(config.normalSwitch, true)
        val twoFEnabled = remoteManager.getBoolean(config.twoFSwitch, false)

        // Matrix evaluation:
        // OFF / OFF -> null
        if (!normalEnabled && !twoFEnabled) return null

        // Try 2F if twoFEnabled == true
        if (twoFEnabled) {
            val remoteTwoFId = remoteManager.getString(config.twoFIdKey, "")
            val resolvedTwoFId = if (remoteTwoFId.isNotBlank()) remoteTwoFId else config.defaultTwoFId
            if (resolvedTwoFId.isNotBlank()) {
                return ResolvedAds(
                    adUnitId = resolvedTwoFId,
                    placement = placement,
                    is2F = true
                )
            }
        }

        // Try Normal if normalEnabled == true
        if (normalEnabled) {
            val remoteNormalId = remoteManager.getString(config.normalIdKey, "")
            val resolvedNormalId = if (remoteNormalId.isNotBlank()) remoteNormalId else config.defaultNormalId
            if (resolvedNormalId.isNotBlank()) {
                return ResolvedAds(
                    adUnitId = resolvedNormalId,
                    placement = placement,
                    is2F = false
                )
            }
        }

        // If twoFEnabled was true but failed ID resolution and normal is not available, try defaultTwoFId BuildConfig as last resort
        if (twoFEnabled && config.defaultTwoFId.isNotBlank()) {
            return ResolvedAds(
                adUnitId = config.defaultTwoFId,
                placement = placement,
                is2F = true
            )
        }

        return null
    }

    private data class PlacementConfig(
        val normalSwitch: String,
        val twoFSwitch: String,
        val normalIdKey: String,
        val twoFIdKey: String,
        val defaultNormalId: String,
        val defaultTwoFId: String
    )

    private fun getPlacementConfig(placement: String): PlacementConfig {
        return when (placement) {
            AdPlacement.INTER_SPLASH, "inter_splash" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_INTER_SPLASH_ENABLED,
                twoFSwitch = AdPlacement.ADS_INTER_SPLASH_2F_ENABLED,
                normalIdKey = AdPlacement.INTER_SPLASH,
                twoFIdKey = "inter_splash_2f",
                defaultNormalId = BuildConfig.inter_splash,
                defaultTwoFId = BuildConfig.inter_splash_2f
            )
            AdPlacement.INTER_MAIN, "inter_main" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_INTER_HOME_ENABLED,
                twoFSwitch = AdPlacement.ADS_INTER_HOME_ENABLED,
                normalIdKey = AdPlacement.INTER_MAIN,
                twoFIdKey = "inter_main_2f",
                defaultNormalId = BuildConfig.inter_main,
                defaultTwoFId = BuildConfig.inter_main
            )
            AdPlacement.INTER_FEATURE, "inter_feature" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_INTER_FEATURE_ENABLED,
                twoFSwitch = AdPlacement.ADS_INTER_FEATURE_ENABLED,
                normalIdKey = AdPlacement.INTER_FEATURE,
                twoFIdKey = "inter_feature_2f",
                defaultNormalId = BuildConfig.inter_main,
                defaultTwoFId = BuildConfig.inter_main
            )
            "banner_main", AdPlacement.ADS_BANNER_SPLASH_ENABLED -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_BANNER_ALL_ENABLED,
                twoFSwitch = AdPlacement.ADS_BANNER_ALL_ENABLED,
                normalIdKey = "banner_main",
                twoFIdKey = "banner_main_2f",
                defaultNormalId = BuildConfig.banner_main,
                defaultTwoFId = BuildConfig.banner_main
            )
            AdPlacement.NATIVE_LANGUAGE, "native_language" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_NATIVE_LANGUAGE_ENABLED,
                twoFSwitch = AdPlacement.ADS_NATIVE_LANGUAGE_2F_ENABLED,
                normalIdKey = AdPlacement.NATIVE_LANGUAGE,
                twoFIdKey = AdPlacement.NATIVE_LANGUAGE_2F,
                defaultNormalId = BuildConfig.native_language,
                defaultTwoFId = BuildConfig.native_language_2f
            )
            "native_onboarding" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_NATIVE_OBD1_ENABLED,
                twoFSwitch = AdPlacement.ADS_NATIVE_OBD1_2F_ENABLED,
                normalIdKey = "native_onboarding",
                twoFIdKey = "native_onboarding_2f",
                defaultNormalId = BuildConfig.native_onboarding,
                defaultTwoFId = BuildConfig.native_onboarding
            )
            "native_permission" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_NATIVE_PERMISSION_ENABLED,
                twoFSwitch = AdPlacement.ADS_NATIVE_PERMISSION_ENABLED,
                normalIdKey = "native_permission",
                twoFIdKey = "native_permission_2f",
                defaultNormalId = BuildConfig.native_onboarding,
                defaultTwoFId = BuildConfig.native_onboarding
            )
            "native_splash" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_NATIVE_FULL_SPLASH_ENABLED,
                twoFSwitch = AdPlacement.ADS_NATIVE_FULL_SPLASH_2F_ENABLED,
                normalIdKey = "native_splash",
                twoFIdKey = "native_splash_2f",
                defaultNormalId = BuildConfig.native_splash,
                defaultTwoFId = BuildConfig.native_splash
            )
            "native_fullscreen" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_NATIVE_OBD12_FULL_ENABLED,
                twoFSwitch = AdPlacement.ADS_NATIVE_FULL_SPLASH_2F_ENABLED,
                normalIdKey = "native_onboarding_fullscreen",
                twoFIdKey = "native_onboarding_fullscreen_2f",
                defaultNormalId = BuildConfig.native_onboarding_fullscreen,
                defaultTwoFId = BuildConfig.native_onboarding_fullscreen_2f
            )
            "native_fullscreen_2" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_NATIVE_OBD34_FULL_ENABLED,
                twoFSwitch = AdPlacement.ADS_NATIVE_FULL_SPLASH_2F_ENABLED,
                normalIdKey = "native_onboarding_fullscreen",
                twoFIdKey = "native_onboarding_fullscreen_2f",
                defaultNormalId = BuildConfig.native_onboarding_fullscreen,
                defaultTwoFId = BuildConfig.native_onboarding_fullscreen_2f
            )
            "native_home", "native_main" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_NATIVE_CUSTOM_ENABLED,
                twoFSwitch = AdPlacement.ADS_NATIVE_CUSTOM_ENABLED,
                normalIdKey = "native_main",
                twoFIdKey = "native_main_2f",
                defaultNormalId = BuildConfig.native_main,
                defaultTwoFId = BuildConfig.native_main
            )
            AdPlacement.REWARD_UNLOCK, "reward_unlock" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_INTERSTITIAL_ENABLED,
                twoFSwitch = AdPlacement.ADS_INTERSTITIAL_ENABLED,
                normalIdKey = AdPlacement.REWARD_UNLOCK,
                twoFIdKey = "reward_unlock_2f",
                defaultNormalId = BuildConfig.reward_unlock,
                defaultTwoFId = BuildConfig.reward_unlock
            )
            "app_open", "app_open_main", "open_resume" -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_APP_OPEN_RESUME_ENABLED,
                twoFSwitch = AdPlacement.ADS_APP_OPEN_RESUME_ENABLED,
                normalIdKey = "appopen_resume",
                twoFIdKey = "appopen_resume_2f",
                defaultNormalId = BuildConfig.appopen_resume,
                defaultTwoFId = BuildConfig.appopen_resume
            )
            else -> PlacementConfig(
                normalSwitch = AdPlacement.ADS_INTERSTITIAL_ENABLED,
                twoFSwitch = AdPlacement.ADS_INTERSTITIAL_ENABLED,
                normalIdKey = placement,
                twoFIdKey = "${placement}_2f",
                defaultNormalId = BuildConfig.inter_splash,
                defaultTwoFId = BuildConfig.inter_splash
            )
        }
    }
}
