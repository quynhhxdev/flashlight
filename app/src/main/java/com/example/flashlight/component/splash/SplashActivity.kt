package com.example.flashlight.component.splash

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.helper.adnative.params.AdNativeState
import com.ads.admob.helper.banner.params.AdBannerState
import com.ads.admob.helper.interstitial.InterstitialAdsHelper
import com.kt.flashlight.R
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.component.language.LanguageActivity
import com.example.flashlight.component.main.MainActivity
import com.kt.flashlight.databinding.ActivitySplashBinding
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.dp
import com.example.flashlight.utils.ads.AdPlacement
import com.example.flashlight.utils.ads.BannerAdsUtils
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.example.flashlight.utils.ads.controller.NativeFullscreenAdController
import com.example.flashlight.utils.ads.manager.AdsDisplayManager
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val viewModel: SplashViewModel by viewModels()
    private val nativeFullscreenController = NativeFullscreenAdController()
    private var topSystemBarInset = 0

    override fun provideViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.e("SplashAdsFlow: Splash started")
        super.onCreate(savedInstanceState)
        viewModel.initSplash()
    }

    override fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            topSystemBarInset = insets.top
            updateNativeFullscreenCloseInset()
            windowInsets
        }

        BannerAdsUtils.initBannerMain(
            activity = this,
            lifecycleOwner = this,
            bannerContentView = binding.bannerContent,
            shimmerLayoutView = binding.shimmerBannerSplash.root,
            placement = AdPlacement.ADS_BANNER_SPLASH_ENABLED,
            onReady = { bannerHelper ->
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        bannerHelper.getBannerState().collect { state: AdBannerState ->
                            binding.bannerSlot.isGone =
                                (state == AdBannerState.Fail || state == AdBannerState.Cancel)
                            when (state) {
                                is AdBannerState.Loaded,
                                is AdBannerState.Fail,
                                is AdBannerState.Cancel -> {
                                    Logger.e("SplashAdsFlow: Banner state = $state")
                                    viewModel.updateBannerFinished(true)
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            },
            onUnavailable = {
                viewModel.updateBannerFinished(true)
            }
        )
    }

    private fun updateNativeFullscreenCloseInset() {
        val layoutParams = binding.tvNativeFullscreenClose.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = topSystemBarInset + 12.dp
        binding.tvNativeFullscreenClose.layoutParams = layoutParams
    }

    override fun initData() {
        NativeAdsUtils.preloadNativeLanguage(this)
        preloadNativeFullscreen()

        AdsDisplayManager.prepareInterstitial(
            activity = this,
            placement = AdPlacement.INTER_SPLASH
        )

        observeViewModelState()

        viewModel.startLoadingProcess(
            isSplashAdLoadedProvider = {
                val interHelper = InterstitialAdsHelper.getInstance(AdPlacement.INTER_SPLASH)
                interHelper.interstitialAdValue != null
            },
            isNativeFullscreenFinishedProvider = { isConfigReady, isEnabled ->
                val nativeHelper = NativeAdsUtils.nativeFullscreen
                isConfigReady && (
                    !isEnabled || nativeHelper == null || run {
                        val state = nativeHelper.getAdNativeState().value
                        nativeHelper.nativeAd != null ||
                                state is AdNativeState.Loaded ||
                                state is AdNativeState.Fail ||
                                state is AdNativeState.Cancel
                    }
                )
            }
        )
    }

    private fun observeViewModelState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingPercentage.text =
                        getString(R.string.splash_loading_percentage, state.progress)
                    binding.loadingProgress.progress = state.progress

                    if (state.isProgressCompleted) {
                        if (viewModel.markNavigated()) {
                            Logger.e("SplashAdsFlow: Progress completed")
                            checkAndShowSplashAd()
                        }
                    }
                }
            }
        }
    }



    private fun checkAndShowSplashAd() {
        AdsDisplayManager.showInterstitial(
            activity = this,
            lifecycleOwner = this,
            placement = AdPlacement.INTER_SPLASH,
            splashStartTime = viewModel.getSplashStartTime(),
            isProgressCompleted = viewModel.uiState.value.isProgressCompleted,
            onAdFinished = {
                showNativeFullscreenAd()
            }
        )
    }

    private fun showNativeFullscreenAd() {
        if (isFinishing || isDestroyed) return

        lifecycleScope.launch {
            val configTimeout = System.currentTimeMillis() + 10_000L
            while (!viewModel.uiState.value.isNativeFullscreenConfigReady && System.currentTimeMillis() < configTimeout) {
                delay(100L)
            }

            nativeFullscreenController.show(
                activity = this@SplashActivity,
                lifecycleOwner = this@SplashActivity,
                nativeAdHelper = NativeAdsUtils.nativeFullscreen,
                enabled = viewModel.uiState.value.isNativeFullscreenEnabled,
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
        when (viewModel.getNavigationDestination()) {
            SplashDestination.MAIN -> {
                NativeAdsUtils.preloadHomeNative(this)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            SplashDestination.LANGUAGE -> {
                val intent = Intent(this, LanguageActivity::class.java).apply {
                    putExtra(LanguageActivity.EXTRA_FROM_SETTINGS, false)
                }
                startActivity(intent)
            }
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

            val isEnabled = AdsIdResolver.resolve("native_fullscreen") != null
            viewModel.updateNativeFullscreenConfig(ready = true, enabled = isEnabled)

            if (isEnabled) {
                Logger.e("SplashAdsFlow: Preloading Native Fullscreen")
                NativeAdsUtils.preloadNativeFullscreen(this)
            } else {
                Logger.e("SplashAdsFlow: Native Fullscreen disabled")
            }
        }
    }

    override fun onDestroy() {
        nativeFullscreenController.release()
        BannerAdsUtils.releaseBanner("banner_splash")
        super.onDestroy()
    }
}
