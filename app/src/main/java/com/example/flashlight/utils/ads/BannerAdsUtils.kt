package com.example.flashlight.utils.ads

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.BannerCollapsibleGravity
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.kt.flashlight.BuildConfig
import com.example.flashlight.utils.FirebaseConfigManager

object BannerAdsUtils {

    var bannerAdMain: BannerAdHelper? = null

    private fun setBanner(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, bannerContentView: FrameLayout, config: BannerAdConfig) {
        val bannerAdHelper = BannerAdHelper(activity = activity, lifecycleOwner = lifecycleOwner, config = config)
        bannerAdHelper.setBannerContentView(bannerContentView)
        bannerAdHelper.requestAds(BannerAdParam.Request)
    }

    fun initBannerMain(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, bannerContentView: FrameLayout) {
        val config = BannerAdConfig(
            idAds = BuildConfig.banner_main,
            canShowAds = FirebaseConfigManager.instance().adConfig.enableBannerMain,
            canReloadAds = false,
            adPlacement = "banner_main",
            reloadIfFirstFail = true
        ).apply { collapsibleGravity = BannerCollapsibleGravity.BOTTOM }
        bannerAdMain = BannerAdHelper(activity = activity, lifecycleOwner = lifecycleOwner, config = config)
        bannerAdMain?.setBannerContentView(bannerContentView)
        bannerAdMain?.requestAds(BannerAdParam.Request)
    }


    fun cancelAllAds(isPurchased: Boolean) {
        bannerAdMain?.cancelRequestAndShowAllAds(isPurchased)
        bannerAdMain?.cancel()

    }

    fun releaseBannerMain() {
        bannerAdMain?.cancel()
        bannerAdMain = null
    }
}