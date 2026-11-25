package com.example.flashlight.domain.layer

import com.google.gson.annotations.SerializedName

data class AdConfig(
    @SerializedName("config_ads") var configAds: Boolean = true,
    @SerializedName("ads_app_open") var adsAppOpen: Boolean = true,
    @SerializedName("ads_app_open_resume") var adsAppOpenResume: Boolean = true,
    @SerializedName("ads_banner") var adsBanner: Boolean = true,
    @SerializedName("ads_banner_all") var adsBannerAll: Boolean = true,
    @SerializedName("ads_banner_splash") var adsBannerSplash: Boolean = true,
    @SerializedName("ads_inter_back") var adsInterBack: Boolean = true,
    @SerializedName("ads_inter_feature_frequency") var adsInterFeatureFrequency: Long = 2L,
    @SerializedName("ads_inter_home") var adsInterHome: Boolean = true,
    @SerializedName("ads_inter_splash") var adsInterSplash: Boolean = true,
    @SerializedName("ads_inter_splash_2f") var adsInterSplash2f: Boolean = false,
    @SerializedName("ads_interstitial") var adsInterstitial: Boolean = true,
    @SerializedName("ads_native") var adsNative: Boolean = true,
    @SerializedName("ads_native_custom") var adsNativeCustom: Boolean = true,
    @SerializedName("ads_native_full_splash") var adsNativeFullSplash: Boolean = false,
    @SerializedName("ads_native_full_splash_2f") var adsNativeFullSplash2f: Boolean = false,
    @SerializedName("ads_native_language") var adsNativeLanguage: Boolean = true,
    @SerializedName("ads_native_language_2f") var adsNativeLanguage2f: Boolean = false,
    @SerializedName("ads_native_language_select") var adsNativeLanguageSelect: Boolean = true,
    @SerializedName("ads_native_language_select_2f") var adsNativeLanguageSelect2f: Boolean = false,
    @SerializedName("ads_native_obd1") var adsNativeObd1: Boolean = true,
    @SerializedName("ads_native_obd12_full") var adsNativeObd12Full: Boolean = true,
    @SerializedName("ads_native_obd1_2f") var adsNativeObd12f: Boolean = false,
    @SerializedName("ads_native_obd3") var adsNativeObd3: Boolean = true,
    @SerializedName("ads_native_obd34_full") var adsNativeObd34Full: Boolean = true,
    @SerializedName("ads_native_obd3_2f") var adsNativeObd32f: Boolean = false,
    @SerializedName("ads_native_permission") var adsNativePermission: Boolean = true,
    @SerializedName("interval_between_interstitial") var intervalBetweenInterstitial: Long = 30000L
) {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return when (key) {
            "config_ads" -> configAds
            "ads_app_open" -> adsAppOpen
            "ads_app_open_resume" -> adsAppOpenResume
            "ads_banner" -> adsBanner
            "ads_banner_all" -> adsBannerAll
            "ads_banner_splash" -> adsBannerSplash
            "ads_inter_back" -> adsInterBack
            "ads_inter_home" -> adsInterHome
            "ads_inter_splash" -> adsInterSplash
            "ads_inter_splash_2f" -> adsInterSplash2f
            "ads_interstitial" -> adsInterstitial
            "ads_native" -> adsNative
            "ads_native_custom" -> adsNativeCustom
            "ads_native_full_splash" -> adsNativeFullSplash
            "ads_native_full_splash_2f" -> adsNativeFullSplash2f
            "ads_native_language" -> adsNativeLanguage
            "ads_native_language_2f" -> adsNativeLanguage2f
            "ads_native_language_select" -> adsNativeLanguageSelect
            "ads_native_language_select_2f" -> adsNativeLanguageSelect2f
            "ads_native_obd1" -> adsNativeObd1
            "ads_native_obd12_full" -> adsNativeObd12Full
            "ads_native_obd1_2f" -> adsNativeObd12f
            "ads_native_obd3" -> adsNativeObd3
            "ads_native_obd34_full" -> adsNativeObd34Full
            "ads_native_obd3_2f" -> adsNativeObd32f
            "ads_native_permission" -> adsNativePermission
            else -> defaultValue
        }
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return when (key) {
            "ads_inter_feature_frequency" -> adsInterFeatureFrequency
            "interval_between_interstitial" -> intervalBetweenInterstitial
            else -> defaultValue
        }
    }

    companion object {
        fun normalizeDurationMs(duration: Long): Long {
            return if (duration in 1..999) duration * 1000L else duration
        }
    }
}
