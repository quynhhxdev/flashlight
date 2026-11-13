package com.example.flashlight.utils.ads

import android.annotation.SuppressLint
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.kt.flashlight.BuildConfig
import com.example.flashlight.utils.FirebaseConfigManager
import com.example.flashlight.utils.Logger
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object NativeAdsUtils {

    private val processLifecycleOwner = ProcessLifecycleOwner.get()

    var nativeLanguage: NativeAdHelper? = null
        private set

    var nativeOnboarding: NativeAdHelper? = null
        private set

    var nativeFullscreen: NativeAdHelper? = null
        private set

    var nativeFullscreen2: NativeAdHelper? = null
        private set

    fun destroyAll() {
        nativeLanguage?.cancel()
        nativeOnboarding?.cancel()
        nativeFullscreen?.cancel()
        nativeFullscreen2?.cancel()

        nativeLanguage = null
        nativeOnboarding = null
        nativeFullscreen = null
        nativeFullscreen2 = null
    }

    fun preloadNativeLanguage(appActivity: AppCompatActivity) {

        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeLanguage

        if (!isEnable) return

        if (nativeLanguage != null) return // đã preload rồi

        val config = NativeAdConfig(
            idAds = BuildConfig.native_language,
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_ad_view_type1,
            adPlacement = "native_language",
            reloadIfFirstFail = true
        )

        nativeLanguage = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeLanguage?.requestAds(NativeAdParam.Request)

        Logger.e("preloadNativeLanguage()")
    }

    fun preloadNativeOnboarding(appActivity: AppCompatActivity) {

        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeOnBoarding

        if (!isEnable) return

        if (nativeOnboarding != null) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_onboarding,
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_ad_view_type1,
            adPlacement = "native_onboarding",
            reloadIfFirstFail = true
        )

        nativeOnboarding = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeOnboarding?.requestAds(NativeAdParam.Request)

        Logger.e("preloadNativeOnboarding()")
    }

    fun preloadNativeFullscreen(appActivity: AppCompatActivity) {

        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeFullscreen

        if (!isEnable) return

        if (nativeFullscreen != null) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_onboarding_fullscreen,
            idAdsPriority = BuildConfig.native_onboarding_fullscreen_2f,
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_fullscreen_view,
            adPlacement = "native_fullscreen",
            reloadIfFirstFail = true
        )

        nativeFullscreen = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeFullscreen?.requestAds(NativeAdParam.Request)

        Logger.e("preloadNativeFullscreen()")
    }

    fun preloadNativeFullscreen2(appActivity: AppCompatActivity) {

        val isEnable = FirebaseConfigManager.instance()
            .adConfig.enableNativeFullscreen

        if (!isEnable) return

        if (nativeFullscreen2 != null) return

        val config = NativeAdConfig(
            idAds = BuildConfig.native_onboarding_fullscreen,
            idAdsPriority = BuildConfig.native_onboarding_fullscreen_2f,
            canShowAds = true,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_fullscreen_view,
            adPlacement = "native_fullscreen_2",
            reloadIfFirstFail = true
        )

        nativeFullscreen2 = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeFullscreen2?.requestAds(NativeAdParam.Request)

        Logger.e("preloadNativeFullscreen2()")
    }

    private fun setNative(
        activity: FragmentActivity,
        config: NativeAdConfig,
        lifecycleOwner: LifecycleOwner,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout
    ) {
        val nativeAdHelper = NativeAdHelper(
            activity, lifecycleOwner, config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        activity.lifecycleScope.launch(Dispatchers.Main) {
            nativeAdHelper.setNativeContentView(nativeContentView)
            nativeAdHelper.setShimmerLayoutView(shimmerContainerNative)
            activity.lifecycleScope.launch(Dispatchers.IO) {
                nativeAdHelper.requestAds(NativeAdParam.Request)
            }
        }
    }

    fun loadAndShowNativeMain(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout
    ) {
        val config = NativeAdConfig(
            BuildConfig.native_main,
            canShowAds = FirebaseConfigManager.instance().adConfig.enableNativeMain,
            canReloadAds = false,
            layoutId = com.ads.admob.R.layout.layout_native_ad_view_type1,
            adPlacement = "native_home"
        )

        setNative(activity, config, lifecycleOwner, nativeContentView, shimmerContainerNative)
    }

}