package com.example.flashlight.utils.ads

import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.facebook.shimmer.ShimmerFrameLayout
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import java.util.concurrent.ConcurrentHashMap

object BannerAdsUtils {

    private val bannerHelpers = ConcurrentHashMap<String, BannerAdHelper>()

    fun initBannerMain(
        activity: AppCompatActivity,
        lifecycleOwner: LifecycleOwner,
        bannerContentView: FrameLayout,
        shimmerLayoutView: View? = null,
        placement: String = "banner_main",
        onReady: (BannerAdHelper) -> Unit,
        onUnavailable: (() -> Unit)? = null
    ) {
        bannerHelpers[placement]?.let {
            onReady(it)
            return
        }

        FirebaseConfigManager.instance().doAfterFetch {
            if (activity.isFinishing || activity.isDestroyed) {
                onUnavailable?.invoke()
                return@doAfterFetch
            }
            val resolved = AdsIdResolver.resolve(placement)
            if (resolved == null) {
                shimmerLayoutView?.isVisible = false
                onUnavailable?.invoke()
                return@doAfterFetch
            }

            val config = BannerAdConfig(
                idAdsPriority = resolved.adUnitIdPriority,
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                adPlacement = resolved.placement,
                reloadIfFirstFail = true
            )
            val bannerAdHelper = BannerAdHelper(activity = activity, lifecycleOwner = lifecycleOwner, config = config)
            bannerAdHelper.setBannerContentView(bannerContentView)
            if (shimmerLayoutView is ShimmerFrameLayout) {
                bannerAdHelper.setShimmerLayoutView(shimmerLayoutView)
            }
            bannerAdHelper.requestAds(BannerAdParam.Request)

            bannerHelpers[placement] = bannerAdHelper
            onReady(bannerAdHelper)
        }
    }

    fun getBannerHelper(placement: String = "banner_main"): BannerAdHelper? {
        return bannerHelpers[placement]
            ?: if (placement == "banner_splash") bannerHelpers[AdPlacement.ADS_BANNER_SPLASH_ENABLED]
            else if (placement == AdPlacement.ADS_BANNER_SPLASH_ENABLED) bannerHelpers["banner_splash"]
            else null
    }

    fun cancelAllAds(isPurchased: Boolean) {
        bannerHelpers.values.forEach {
            it.cancelRequestAndShowAllAds(isPurchased)
            it.cancel()
        }
        bannerHelpers.clear()
    }

    fun releaseBanner(placement: String = "banner_main") {
        bannerHelpers[placement]?.cancel()
        bannerHelpers.remove(placement)
        if (placement == "banner_splash") {
            bannerHelpers[AdPlacement.ADS_BANNER_SPLASH_ENABLED]?.cancel()
            bannerHelpers.remove(AdPlacement.ADS_BANNER_SPLASH_ENABLED)
        } else if (placement == AdPlacement.ADS_BANNER_SPLASH_ENABLED) {
            bannerHelpers["banner_splash"]?.cancel()
            bannerHelpers.remove("banner_splash")
        }
    }

    fun releaseBannerMain() {
        releaseBanner("banner_main")
    }
}
