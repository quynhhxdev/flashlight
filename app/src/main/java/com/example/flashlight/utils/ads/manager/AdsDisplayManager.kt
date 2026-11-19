package com.example.flashlight.utils.ads.manager

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.helper.banner.params.AdBannerState
import com.ads.admob.helper.interstitial.InterstitialAdsHelper
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.ads.BannerAdsUtils
import com.example.flashlight.utils.ads.InterAdsUtils
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

object AdsDisplayManager {

    private const val MIN_SPLASH_DURATION = 1000L

    fun prepareInterstitial(
        activity: Activity,
        placement: String
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        Logger.e("SplashAdsFlow: Interstitial preload started")
        FirebaseConfigManager.instance().doAfterFetch {
            if (activity.isFinishing || activity.isDestroyed) return@doAfterFetch

            val resolved = AdsIdResolver.resolve(placement)
            if (resolved == null) {
                Logger.e("SplashAdsFlow: Interstitial resolve failed or disabled")
                return@doAfterFetch
            }

            val helper = InterstitialAdsHelper.getInstance(resolved.placement)
            if (helper.interstitialAdValue == null) {
                InterAdsUtils.loadInterAds(
                    activity = activity,
                    idAds = resolved.adUnitId,
                    adPlacement = resolved.placement,
                    isEnable = true,
                    isLoadAndShow = false,
                    onAdLoaded = {
                        Logger.e("SplashAdsFlow: Interstitial ready (loaded)")
                    },
                    onAdLoadFail = {
                        Logger.e("SplashAdsFlow: Interstitial load failed")
                    }
                )
            } else {
                Logger.e("SplashAdsFlow: Interstitial ready (cached)")
            }
        }
    }

    fun showInterstitial(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        placement: String,
        splashStartTime: Long,
        isProgressCompleted: Boolean,
        onAdFinished: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onAdFinished()
            return
        }

        var hasFinished = false
        fun finishOnce() {
            if (!hasFinished) {
                hasFinished = true
                Logger.e("SplashAdsFlow: Navigate Home / Splash Ads Flow finished")
                onAdFinished()
            }
        }

        lifecycleOwner.lifecycleScope.launch {
            // 1. Ensure minimum 3 seconds splash duration
            val elapsed = System.currentTimeMillis() - splashStartTime
            if (elapsed < MIN_SPLASH_DURATION) {
                val remaining = MIN_SPLASH_DURATION - elapsed
                delay(remaining)
            }
            Logger.e("SplashAdsFlow: Minimum splash duration reached")

            if (activity.isFinishing || activity.isDestroyed) {
                finishOnce()
                return@launch
            }

            // 2. Banner check (Step 1 & Step 2)
            val bannerHelper = BannerAdsUtils.getBannerHelper("banner_splash")
            if (bannerHelper != null) {
                Logger.e("SplashAdsFlow: Checking banner status")
                val bannerReady = withTimeoutOrNull(1000L.milliseconds) {
                    try {
                        bannerHelper.getBannerState().first { it is AdBannerState.Loaded }
                        true
                    } catch (e: Exception) {
                        false
                    }
                } ?: false

                if (bannerReady) {
                    Logger.e("SplashAdsFlow: Banner displayed / ready")
                } else {
                    Logger.e("SplashAdsFlow: Banner failed or timed out waiting")
                }
            } else {
                Logger.e("SplashAdsFlow: Banner helper is null")
            }

            if (activity.isFinishing || activity.isDestroyed) {
                finishOnce()
                return@launch
            }

            // 3. Interstitial check (Step 3)
            FirebaseConfigManager.instance().doAfterFetch {
                if (activity.isFinishing || activity.isDestroyed) {
                    finishOnce()
                    return@doAfterFetch
                }

                val resolved = AdsIdResolver.resolve(placement)
                if (resolved == null) {
                    Logger.e("SplashAdsFlow: Interstitial disabled or not resolved")
                    finishOnce()
                    return@doAfterFetch
                }

                val helper = InterstitialAdsHelper.getInstance(resolved.placement)
                if (helper.interstitialAdValue == null) {
                    Logger.e("SplashAdsFlow: Interstitial not ready yet, waiting briefly...")
                }

                lifecycleOwner.lifecycleScope.launch {
                    val ready = withTimeoutOrNull(3000L.milliseconds) {
                        var isReady = false
                        while (!isReady) {
                            if (activity.isFinishing || activity.isDestroyed) break
                            if (helper.interstitialAdValue != null) {
                                isReady = true
                                break
                            }
                            delay(100L.milliseconds)
                        }
                        isReady
                    } ?: false

                    if (ready && !activity.isFinishing && !activity.isDestroyed) {
                        Logger.e("SplashAdsFlow: Interstitial show started")
                        InterAdsUtils.showInterAds(
                            activity = activity,
                            lifecycleOwner = lifecycleOwner,
                            adPlacement = resolved.placement,
                            isReloadWhenClose = false,
                            action = {
                                Logger.e("SplashAdsFlow: Interstitial dismissed / show finished")
                                finishOnce()
                            }
                        )
                    } else {
                        Logger.e("SplashAdsFlow: Interstitial not ready or timed out")
                        finishOnce()
                    }
                }
            }
        }
    }
}
