package com.example.flashlight.utils.ads

object AdPlacement {
    // Existing constants
    const val INTER_SPLASH = "inter_splash"
    const val APP_OPEN_MAIN = "app_open_main"
    const val INTER_MAIN = "inter_main"
    const val OPEN_RESUME = "open_resume"
    const val REWARD_UNLOCK = "reward_unlock"

    const val NATIVE_LANGUAGE = "native_language"
    const val NATIVE_LANGUAGE_2F = "native_language_2f"

    // A. Công tắc tổng
    const val CONFIG_ADS = "config_ads"

    // B. Công tắc theo định dạng quảng cáo
    const val ADS_BANNER_ENABLED = "ads_banner"
    const val ADS_INTERSTITIAL_ENABLED = "ads_interstitial"
    const val ADS_NATIVE_ENABLED = "ads_native"
    const val ADS_APP_OPEN_ENABLED = "ads_app_open"

    // C. Banner Ads Placements
    const val ADS_BANNER_SPLASH_ENABLED = "ads_banner_splash"
    const val ADS_BANNER_ALL_ENABLED = "ads_banner_all"

    // D. Interstitial Ads Placements
    const val ADS_INTER_SPLASH_ENABLED = "ads_inter_splash"
    const val ADS_INTER_SPLASH_2F_ENABLED = "ads_inter_splash_2f"
    const val ADS_INTER_HOME_ENABLED = "ads_inter_home"
    const val ADS_INTER_BACK_ENABLED = "ads_inter_back"
    const val ADS_INTER_FEATURE_ENABLED = "ads_inter_feature"

    // Feature inter placement (Flashlight / ScreenLight / LED / FlashAlert)
    const val INTER_FEATURE = "inter_feature"

    // Firebase Remote Config keys for feature inter frequency
    const val REMOTE_ADS_INTER_FEATURE_FREQUENCY = "ads_inter_feature_frequency"
    const val REMOTE_INTERVAL_BETWEEN_INTERSTITIAL = "interval_between_interstitial"

    // Default values
    const val DEFAULT_INTER_FEATURE_FREQUENCY = 2L   // show on every 2nd usage
    const val DEFAULT_INTERVAL_BETWEEN_INTER_MS = 0L // 0 = no minimum interval

    // E. App Open Ads Placements
    const val ADS_APP_OPEN_RESUME_ENABLED = "ads_app_open_resume"

    // F. Native Ads Placements
    const val ADS_NATIVE_FULL_SPLASH_ENABLED = "ads_native_full_splash"
    const val ADS_NATIVE_FULL_SPLASH_2F_ENABLED = "ads_native_full_splash_2f"
    const val ADS_NATIVE_LANGUAGE_ENABLED = "ads_native_language"
    const val ADS_NATIVE_LANGUAGE_2F_ENABLED = "ads_native_language_2f"
    const val ADS_NATIVE_LANGUAGE_SELECT_ENABLED = "ads_native_language_select"
    const val ADS_NATIVE_LANGUAGE_SELECT_2F_ENABLED = "ads_native_language_select_2f"
    const val ADS_NATIVE_OBD1_ENABLED = "ads_native_obd1"
    const val ADS_NATIVE_OBD1_2F_ENABLED = "ads_native_obd1_2f"
    const val ADS_NATIVE_OBD12_FULL_ENABLED = "ads_native_obd12_full"
    const val ADS_NATIVE_OBD34_FULL_ENABLED = "ads_native_obd34_full"
    const val ADS_NATIVE_OBD3_ENABLED = "ads_native_obd3"
    const val ADS_NATIVE_OBD3_2F_ENABLED = "ads_native_obd3_2f"
    const val ADS_NATIVE_PERMISSION_ENABLED = "ads_native_permission"
    const val ADS_NATIVE_CUSTOM_ENABLED = "ads_native_custom"
}
