package com.example.flashlight.utils.ads

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager

object BannerAdsUtils {

    var bannerAdMain: BannerAdHelper? = null

    private fun setBanner(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, bannerContentView: FrameLayout, config: BannerAdConfig) {
        val bannerAdHelper = BannerAdHelper(activity = activity, lifecycleOwner = lifecycleOwner, config = config)
        bannerAdHelper.setBannerContentView(bannerContentView)
        bannerAdHelper.requestAds(BannerAdParam.Request)
    }

    fun initBannerMain(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, bannerContentView: FrameLayout) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("banner_main")
            if (resolved == null) return@doAfterFetch

            val config = BannerAdConfig(
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                adPlacement = resolved.placement,
                reloadIfFirstFail = true
            )
                //.apply { collapsibleGravity = BannerCollapsibleGravity.BOTTOM }
            bannerAdMain = BannerAdHelper(activity = activity, lifecycleOwner = lifecycleOwner, config = config)
            bannerAdMain?.setBannerContentView(bannerContentView)
            bannerAdMain?.requestAds(BannerAdParam.Request)
        }
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
