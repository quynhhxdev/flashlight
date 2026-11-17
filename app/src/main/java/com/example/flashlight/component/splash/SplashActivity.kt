package com.example.flashlight.component.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.helper.adnative.params.AdNativeState
import com.ads.admob.helper.banner.params.AdBannerState
import com.kt.flashlight.R
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.utils.ads.BannerAdsUtils
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.example.flashlight.component.main.MainActivity
import com.example.flashlight.utils.SpManager
import com.example.flashlight.utils.ads.manager.AdsDisplayManager
import com.example.flashlight.utils.ads.AdPlacement
import com.example.flashlight.utils.Logger
import com.example.flashlight.component.startup.SplashStartupProgress
import com.kt.flashlight.databinding.ActivitySplashBinding
import com.example.flashlight.utils.ads.controller.NativeFullscreenAdController
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private val viewModel: SplashViewModel by viewModels()
    private val nativeFullscreenController = NativeFullscreenAdController()

    private var hasNavigated = false
    private var splashStartTime = 0L
    private var isProgressCompleted = false
    private var isBannerFinished = false

    // Native Fullscreen
    private var isNativeFullscreenEnabled = false
    private var isNativeFullscreenConfigReady = false

    override fun provideViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        splashStartTime = System.currentTimeMillis()
        Logger.e("SplashAdsFlow: Splash started")
        super.onCreate(savedInstanceState)
    }

    override fun initViews() {
        setupNativeFullscreenInsets()

        BannerAdsUtils.initBannerMain(
            this,
            this,
            binding.bannerContent,
        )
        val bannerHelper = BannerAdsUtils.bannerAdMain
        if (bannerHelper == null) {
            isBannerFinished = true
        } else {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    bannerHelper.getBannerState().collect { state: AdBannerState ->
                        binding.bannerSlot.isGone =
                            (state == AdBannerState.Fail || state == AdBannerState.Cancel)
                        when (state) {
                            is AdBannerState.Loaded -> {
                                Logger.e("SplashAdsFlow: Banner loaded")
                                isBannerFinished = true
                            }
                            is AdBannerState.Fail -> {
                                Logger.e("SplashAdsFlow: Banner failed")
                                isBannerFinished = true
                            }
                            is AdBannerState.Cancel -> {
                                Logger.e("SplashAdsFlow: Banner canceled")
                                isBannerFinished = true
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun initData() {
        NativeAdsUtils.preloadNativeLanguage(this)

        // Preload Native Fullscreen song song với Interstitial
        preloadNativeFullscreen()

        AdsDisplayManager.prepareInterstitial(
            activity = this,
            placement = AdPlacement.INTER_SPLASH
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val totalTimeoutMillis = 25_000L
                val smoothFillDurationMillis = 2_000L
                var timeoutStartTime = 0L
                var isTimedOut = false
                var displayedProgress = 0f

                while (!hasNavigated) {
                    val currentTime = System.currentTimeMillis()
                    val totalElapsed = currentTime - splashStartTime

                    val rawFetchProgress = SplashStartupProgress.progress.value // 0 to 100
                    val isFetchDone = rawFetchProgress >= 100
                    val interHelper = com.ads.admob.helper.interstitial.InterstitialAdsHelper.getInstance(AdPlacement.INTER_SPLASH)
                    val isSplashAdLoaded = interHelper.interstitialAdValue != null

                    if (!isBannerFinished && BannerAdsUtils.bannerAdMain == null) {
                        isBannerFinished = true
                    }

                    val nativeHelper = NativeAdsUtils.nativeFullscreen
                    val isNativeFullscreenFinished = isNativeFullscreenConfigReady && (
                        !isNativeFullscreenEnabled || nativeHelper == null || run {
                            val state = nativeHelper.getAdNativeState().value
                            nativeHelper.nativeAd != null ||
                                    state is AdNativeState.Loaded ||
                                    state is AdNativeState.Fail ||
                                    state is AdNativeState.Cancel
                        }
                    )

                    val isNormalComplete = isFetchDone && isBannerFinished && isSplashAdLoaded && isNativeFullscreenFinished

                    val targetProgress = if (isNormalComplete || totalElapsed >= totalTimeoutMillis) {
                        if (!isTimedOut && !isNormalComplete && totalElapsed >= totalTimeoutMillis) {
                            isTimedOut = true
                            timeoutStartTime = currentTime
                            Logger.e("SplashAdsFlow: Total 25s timeout reached, starting smooth fill to 100%")
                        }

                        if (isNormalComplete) {
                            100
                        } else {
                            val elapsedSinceTimeout = currentTime - timeoutStartTime
                            val progressAtTimeout = displayedProgress.toInt()
                            val remainingProgress = 100 - progressAtTimeout
                            val fillPart = ((elapsedSinceTimeout.toFloat() / smoothFillDurationMillis) * remainingProgress).toInt()
                            (progressAtTimeout + fillPart).coerceIn(0, 100)
                        }
                    } else {
                        // Chia đều 4 giai đoạn, mỗi giai đoạn 25%:
                        // 1. Remote Config (0% - 25%)
                        val stage1 = (rawFetchProgress * 0.25f).toInt().coerceIn(0, 25)
                        // 2. Banner (25% - 50%)
                        val stage2 = if (stage1 >= 25 && isBannerFinished) 50 else if (stage1 >= 25) 37 else stage1
                        // 3. Interstitial (50% - 75%)
                        val stage3 = if (stage2 >= 50 && isSplashAdLoaded) 75 else if (stage2 >= 50) 62 else stage2
                        // 4. Native Fullscreen (75% - 100%)
                        val stage4 = if (stage3 >= 75 && isNativeFullscreenFinished) 100 else if (stage3 >= 75) 87 else stage3
                        stage4.coerceIn(0, 99)
                    }

                    if (displayedProgress < targetProgress) {
                        displayedProgress = (displayedProgress + 1.2f).coerceAtMost(targetProgress.toFloat())
                    } else if (displayedProgress > targetProgress) {
                        displayedProgress = targetProgress.toFloat()
                    }

                    val currentProgress = displayedProgress.toInt().coerceIn(0, 100)

                    binding.loadingPercentage.text =
                        getString(R.string.splash_loading_percentage, currentProgress)
                    binding.loadingProgress.progress = currentProgress

                    val reachedMax = currentProgress >= 100
                    if (reachedMax) {
                        if (!isProgressCompleted) {
                            isProgressCompleted = true
                            Logger.e("SplashAdsFlow: Progress completed (Normal=$isNormalComplete, TimedOut=$isTimedOut)")
                            checkAndShowSplashAd()
                            break
                        }
                    }
                    delay(100L)
                }
            }
        }
    }

    private fun setupNativeFullscreenInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
            binding.nativeFullscreenOverlay
        ) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.updatePadding(
                top = systemBars.top
            )

            insets
        }

        // Yêu cầu hệ thống gửi WindowInsets
        ViewCompat.requestApplyInsets(binding.nativeFullscreenOverlay)
    }

    private fun checkAndShowSplashAd() {
        if (hasNavigated) return
        hasNavigated = true

        AdsDisplayManager.showInterstitial(
            activity = this,
            lifecycleOwner = this,
            placement = AdPlacement.INTER_SPLASH,
            splashStartTime = splashStartTime,
            isProgressCompleted = isProgressCompleted,
            onAdFinished = {
                showNativeFullscreenAd()
            }
        )
    }

    private fun showNativeFullscreenAd() {
        if (isFinishing || isDestroyed) return

        lifecycleScope.launch {
            // Chờ Firebase Remote Config xác định trạng thái quảng cáo.
            val configTimeout = System.currentTimeMillis() + 10_000L
            while (!isNativeFullscreenConfigReady && System.currentTimeMillis() < configTimeout) {
                delay(100L)
            }

            nativeFullscreenController.show(
                activity = this@SplashActivity,
                lifecycleOwner = this@SplashActivity,
                nativeAdHelper = NativeAdsUtils.nativeFullscreen,
                enabled = isNativeFullscreenEnabled,
                overlay = binding.nativeFullscreenOverlay,
                content = binding.nativeFullscreenContent,
                shimmer = binding.shimmerNativeFullscreen.root,
                closeButton = binding.tvNativeFullscreenClose,
                hideOverlayOnFinish = false,
                onFinished = {
                    navigateToHome()
                }
            )
        }
    }

    private fun navigateToHome() {
        val spManager = SpManager.getInstance(this)
        if (spManager.isOnboardingCompleted()) {
            NativeAdsUtils.preloadHomeNative(this)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, com.example.flashlight.component.language.LanguageActivity::class.java).apply {
                putExtra(com.example.flashlight.component.language.LanguageActivity.EXTRA_FROM_SETTINGS, false)
            }
            startActivity(intent)
        }
        finish()
        applySmoothTransition()
    }

    private fun applySmoothTransition() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun preloadNativeFullscreen() {
        FirebaseConfigManager.instance().doAfterFetch {
            if (isFinishing || isDestroyed) return@doAfterFetch

            isNativeFullscreenEnabled =
                AdsIdResolver.resolve("native_fullscreen") != null

            isNativeFullscreenConfigReady = true

            if (isNativeFullscreenEnabled) {
                Logger.e("SplashAdsFlow: Preloading Native Fullscreen")
                NativeAdsUtils.preloadNativeFullscreen(this)
            } else {
                Logger.e("SplashAdsFlow: Native Fullscreen disabled")
            }
        }
    }

    override fun onDestroy() {
        nativeFullscreenController.release()
        BannerAdsUtils.releaseBannerMain()
        super.onDestroy()
    }
}
