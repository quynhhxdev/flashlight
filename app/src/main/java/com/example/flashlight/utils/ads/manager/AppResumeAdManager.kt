package com.example.flashlight.utils.ads.manager

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.admob.config.NetworkProvider
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.appoppen.AppResumeAdConfig
import com.ads.admob.helper.appoppen.AppResumeAdHelper
import com.ads.admob.listener.AppOpenAdCallBack
import com.example.flashlight.component.language.LanguageActivity
import com.example.flashlight.component.onboard.IntroActivity
import com.example.flashlight.component.onboard.OnboardingActivity
import com.example.flashlight.component.splash.SplashActivity
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.ads.AdPlacement
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

object AppResumeAdManager {

    private var appResumeAdHelper: AppResumeAdHelper? = null

    fun init(application: Application) {
        FirebaseConfigManager.instance().doAfterFetch {
            setupAppResumeAd(application)
        }
    }

    fun setupAppResumeAd(application: Application) {
        if (appResumeAdHelper != null) return

        val resolved = AdsIdResolver.resolve(AdPlacement.OPEN_RESUME)
        if (resolved == null) {
            Logger.e("AppResumeAdManager: App Resume Ad is disabled by config or resolver")
            return
        }

        val config = AppResumeAdConfig(
            idAds = resolved.adUnitId,
            networkProvider = NetworkProvider.ADMOB,
            listClassInValid = mutableListOf(
                SplashActivity::class.java,
                LanguageActivity::class.java,
                OnboardingActivity::class.java,
                IntroActivity::class.java
            ),
            canShowAds = true,
            canReloadAds = true,
            adPlacement = AdPlacement.OPEN_RESUME,
            reloadIfFirstFail = true,
            loadOnResume = false
        )

        val helper = AppResumeAdHelper(
            application = application,
            lifecycleOwner = ProcessLifecycleOwner.get(),
            config = config
        )

        helper.registerAdListener(object : AppOpenAdCallBack {
            override fun onAppOpenAdShow() {
                Logger.e("AppResumeAdManager: onAppOpenAdShow")
            }

            override fun onAppOpenAdClose() {
                Logger.e("AppResumeAdManager: onAppOpenAdClose")
            }

            override fun onAdLoaded(data: ContentAd) {
                Logger.e("AppResumeAdManager: onAdLoaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e("AppResumeAdManager: onAdFailedToLoad ${loadAdError.message}")
            }

            override fun onAdClicked() {
                Logger.e("AppResumeAdManager: onAdClicked")
            }

            override fun onAdImpression() {
                Logger.e("AppResumeAdManager: onAdImpression")
            }

            override fun onAdFailedToShow(adError: AdError) {
                Logger.e("AppResumeAdManager: onAdFailedToShow ${adError.message}")
            }
        })

        helper.requestAppOpenResume()
        appResumeAdHelper = helper
        Logger.e("AppResumeAdManager: AppResumeAdHelper initialized successfully with unitId: ${resolved.adUnitId}")
    }

    fun disableAppResumeOnScreen() {
        appResumeAdHelper?.setDisableAppResumeOnScreen()
    }

    fun enableAppResumeOnScreen() {
        appResumeAdHelper?.setEnableAppResumeOnScreen()
    }

    fun setDisableAppResumeByClickAction() {
        appResumeAdHelper?.setDisableAppResumeByClickAction()
    }

    fun cancelAllAds(isPurchased: Boolean) {
        appResumeAdHelper?.cancelRequestAndShowAllAds(isPurchased)
    }

    fun destroy() {
        appResumeAdHelper?.unregisterAllAdListener()
        appResumeAdHelper?.destroyAds()
        appResumeAdHelper = null
    }
}
