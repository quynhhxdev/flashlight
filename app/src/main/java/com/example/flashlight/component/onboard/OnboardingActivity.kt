package com.example.flashlight.component.onboard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.ads.admob.helper.adnative.NativeAdHelper
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.example.flashlight.utils.dp
import com.example.flashlight.utils.Utils
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.example.flashlight.utils.ads.controller.NativeFullscreenAdController
import com.kt.flashlight.R
import com.kt.flashlight.databinding.ActivityOnboardingBinding

data class OnboardingData(
    val imageRes: Int,
    val title: String,
    val description: String
)

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {

    private val nativeFullscreenController = NativeFullscreenAdController()

    private var topSystemBarInset = 0
    private var currentPagePosition = 0
    private var defaultLightStatusBarAppearance = false
    private var defaultLayoutInDisplayCutoutMode: Int? = null

    private var isNativeOnboardingEnabled = false
    private var isNativeFullscreenEnabled = false
    private var isNativeFullscreen2Enabled = false
    private var hasShownNativeFullscreen1 = false
    private var hasShownNativeFullscreen2 = false

    private val onboardingItems = listOf(
        OnboardingData(
            imageRes = R.drawable.bg_onb1,
            title = "Bright Flashlight",
            description = "Turn your phone into a powerful\nflashlight anytime."
        ),
        OnboardingData(
            imageRes = R.drawable.bg_onb2,
            title = "SOS & DJ Mode",
            description = "Use SOS mode for emergencies for \n" +
                    "DJ mode for parties."
        ),
        OnboardingData(
            imageRes = R.drawable.bg_onb3,
            title = "LED Banner",
            description = "Display scrolling LED text with customizable\n" +
                    "colors and effects."
        ),
        OnboardingData(
            imageRes = R.drawable.bg_onb4,
            title = "Screen Light",
            description = "Use your display as a bright colored\n" +
                    "light source"
        )
    )

    override fun provideViewBinding() = ActivityOnboardingBinding.inflate(layoutInflater)

    override fun initViews() {
        defaultLightStatusBarAppearance =
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            defaultLayoutInDisplayCutoutMode = window.attributes.layoutInDisplayCutoutMode
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.onboardingContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            topSystemBarInset = insets.top
            val bottomSpacing = view.resources.getDimensionPixelSize(R.dimen._8dp)
            view.updatePadding(bottom = insets.bottom + bottomSpacing)
            updateNativeFullscreenCloseInset()
            updatePageInsets(currentPagePosition)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewPager()
        setupListeners()
        preloadAds()
    }

    private fun setupViewPager() {
        val adapter = OnboardingPagerAdapter(onboardingItems)
        binding.viewPagerOnboarding.adapter = adapter

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var lastPosition = 0

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)

                if (lastPosition == 0 && position == 1 && !hasShownNativeFullscreen1) {
                    hasShownNativeFullscreen1 = true
                    showNativeFullscreenAd(
                        NativeAdsUtils.nativeFullscreen,
                        isNativeFullscreenEnabled,
                        nextPage = 1
                    )
                } else if (lastPosition == 2 && position == 3 && !hasShownNativeFullscreen2) {
                    hasShownNativeFullscreen2 = true
                    showNativeFullscreenAd(
                        NativeAdsUtils.nativeFullscreen2,
                        isNativeFullscreen2Enabled,
                        nextPage = 3
                    )
                }

                lastPosition = position
                currentPagePosition = position

                when (position) {
                    1 -> NativeAdsUtils.preloadNativeFullscreen2(this@OnboardingActivity)
                    3 -> NativeAdsUtils.preloadNativePermission(this@OnboardingActivity)
                }

                val hasAds = (position == 0 || position == 2)
                binding.nativeAdContainer.isVisible = hasAds && isNativeOnboardingEnabled
                updatePageInsets(position)
            }
        })
        updatePageInsets(binding.viewPagerOnboarding.currentItem)
    }

    private fun updatePageInsets(position: Int) {
        val edgeToEdgeImage = isEdgeToEdgeImagePage(position)
        val layoutParams = binding.viewPagerOnboarding.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = if (edgeToEdgeImage) 0 else topSystemBarInset
        binding.viewPagerOnboarding.layoutParams = layoutParams

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
            defaultLightStatusBarAppearance && !edgeToEdgeImage

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowLayoutParams = window.attributes
            val cutoutMode = if (edgeToEdgeImage) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                defaultLayoutInDisplayCutoutMode
                    ?: WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            if (windowLayoutParams.layoutInDisplayCutoutMode != cutoutMode) {
                windowLayoutParams.layoutInDisplayCutoutMode = cutoutMode
                window.attributes = windowLayoutParams
            }
        }
    }

    private fun isEdgeToEdgeImagePage(position: Int) = (position == 0 || position == 2)

    private fun preloadAds() {
        FirebaseConfigManager.instance().doAfterFetch {
            if (isFinishing || isDestroyed) return@doAfterFetch

            isNativeOnboardingEnabled = AdsIdResolver.resolve("native_onboarding") != null
            binding.nativeAdContainer.isVisible =
                isNativeOnboardingEnabled && isEdgeToEdgeImagePage(currentPagePosition)
            if (isNativeOnboardingEnabled) {
                NativeAdsUtils.preloadNativeOnboarding(this)
                NativeAdsUtils.nativeOnboarding?.let { nativeAdHelper ->
                    nativeAdHelper.setNativeContentView(binding.nativeAdContent)
                    nativeAdHelper.setShimmerLayoutView(binding.shimmerContainerNative.root)
                }
            }

            isNativeFullscreenEnabled = AdsIdResolver.resolve("native_fullscreen") != null
            if (isNativeFullscreenEnabled) NativeAdsUtils.preloadNativeFullscreen(this)

            isNativeFullscreen2Enabled = AdsIdResolver.resolve("native_fullscreen_2") != null
            if (isNativeFullscreen2Enabled) NativeAdsUtils.preloadNativeFullscreen2(this)
        }
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            when (val current = binding.viewPagerOnboarding.currentItem) {
                0 -> {
                    if (!hasShownNativeFullscreen1) {
                        hasShownNativeFullscreen1 = true
                        showNativeFullscreenAd(
                            NativeAdsUtils.nativeFullscreen,
                            isNativeFullscreenEnabled,
                            nextPage = 1
                        )
                    } else {
                        binding.viewPagerOnboarding.currentItem = 1
                    }
                }
                2 -> {
                    if (!hasShownNativeFullscreen2) {
                        hasShownNativeFullscreen2 = true
                        showNativeFullscreenAd(
                            NativeAdsUtils.nativeFullscreen2,
                            isNativeFullscreen2Enabled,
                            nextPage = 3
                        )
                    } else {
                        binding.viewPagerOnboarding.currentItem = 3
                    }
                }
                else -> if (current < onboardingItems.size - 1) {
                    binding.viewPagerOnboarding.currentItem = current + 1
                } else {
                    onOnboardingFinished()
                }
            }
        }
    }

    private fun showNativeFullscreenAd(
        nativeAdHelper: NativeAdHelper?,
        adPlacementEnabled: Boolean,
        nextPage: Int
    ) {
        binding.viewPagerOnboarding.isUserInputEnabled = false
        nativeFullscreenController.show(
            activity = this,
            lifecycleOwner = this,
            nativeAdHelper = nativeAdHelper,
            enabled = adPlacementEnabled,
            overlay = binding.nativeFullscreenOverlay,
            content = binding.nativeFullscreenContent,
            shimmer = binding.shimmerNativeFullscreen.root,
            closeButton = binding.tvNativeFullscreenClose,
            onFinished = {
                binding.viewPagerOnboarding.isUserInputEnabled = true
                binding.viewPagerOnboarding.currentItem = nextPage
            }
        )
    }

    private fun updateNativeFullscreenCloseInset() {
        val layoutParams = binding.tvNativeFullscreenClose.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = topSystemBarInset + 12.dp
        binding.tvNativeFullscreenClose.layoutParams = layoutParams
    }

    private fun updateIndicators(position: Int) {
        val indicators = listOf(binding.indicator1, binding.indicator2, binding.indicator3, binding.indicator4)
        for (i in indicators.indices) {
            val indicator = indicators[i]
            val params = indicator.layoutParams
            if (i == position) {
                params.width = 20.dp
                indicator.layoutParams = params
                indicator.setBackgroundResource(R.drawable.bg_indicator_selected)
            } else {
                params.width = 8.dp
                indicator.layoutParams = params
                indicator.setBackgroundResource(R.drawable.bg_indicator_unselected)
            }
        }
    }

    private fun onOnboardingFinished() {
        NativeAdsUtils.preloadHomeNative(this)
        val isConnected = Utils.isConnected(this)
        val targetScreen = if (isConnected) {
            IntroActivity.SCREEN_PERMISSION
        } else {
            IntroActivity.SCREEN_NO_INTERNET
        }

        val intent = Intent(this, IntroActivity::class.java).apply {
            putExtra(IntroActivity.EXTRA_START_SCREEN, targetScreen)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        nativeFullscreenController.release()
        super.onDestroy()
    }
}
