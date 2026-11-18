package com.example.flashlight.component.main

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.helper.banner.params.AdBannerState
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.component.main.fagment.FlashAlertFragment
import com.example.flashlight.component.main.fagment.FlashlightFragment
import com.example.flashlight.component.main.fagment.LedFragment
import com.example.flashlight.component.main.fagment.ScreenLightFragment
import com.example.flashlight.utils.ads.BannerAdsUtils
import com.example.flashlight.utils.ads.manager.FeatureInterAdsManager
import com.kt.flashlight.R
import com.kt.flashlight.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var currentTab = TAB_FLASHLIGHT
    private var currentBannerState: AdBannerState = AdBannerState.None

    override fun provideViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FeatureInterAdsManager.preload(this)
        setupBannerAd()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            selectTab(TAB_FLASHLIGHT)
        } else {
            currentTab = savedInstanceState.getInt(KEY_SELECTED_TAB, TAB_FLASHLIGHT)
            updateNavState(currentTab)
            updateBannerVisibility()
        }
    }

    private fun setupBannerAd() {
        BannerAdsUtils.initBannerMain(
            activity = this,
            lifecycleOwner = this,
            bannerContentView = binding.bannerContent,
            shimmerLayoutView = binding.shimmerContainerBanner.root,
            placement = "banner_main",
            onReady = { bannerHelper ->
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        bannerHelper.getBannerState().collect { state ->
                            currentBannerState = state
                            updateBannerVisibility()
                        }
                    }
                }
            }
        )
    }

    private fun updateBannerVisibility() {
        val isSettingsTab = (currentTab == TAB_SETTINGS)
        val bannerHelper = BannerAdsUtils.getBannerHelper("banner_main")
        val isFailed = (currentBannerState == AdBannerState.Fail || currentBannerState == AdBannerState.Cancel)

        binding.flBannerContainer.isGone = isSettingsTab || isFailed || bannerHelper == null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, currentTab)
    }

    private fun setupBottomNavigation() = with(binding.layoutBottomNav) {
        btnNavFlashlight.setOnClickListener {
            if (currentTab != TAB_FLASHLIGHT) {
                triggerFeatureInterAd { selectTab(TAB_FLASHLIGHT) }
            }
        }

        btnNavScreenLight.setOnClickListener {
            if (currentTab != TAB_SCREEN_LIGHT) {
                triggerFeatureInterAd { selectTab(TAB_SCREEN_LIGHT) }
            }
        }

        btnNavLed.setOnClickListener {
            if (currentTab != TAB_LED) {
                triggerFeatureInterAd { selectTab(TAB_LED) }
            }
        }

        btnNavSettings.setOnClickListener {
            if (currentTab != TAB_SETTINGS) {
                triggerFeatureInterAd { selectTab(TAB_SETTINGS) }
            }
        }
    }

    fun selectTab(tab: Int) {
        val oldTab = currentTab
        currentTab = tab
        updateNavState(tab)
        updateBannerVisibility()

        val fragment: Fragment = when (tab) {
            TAB_FLASHLIGHT -> FlashlightFragment.newInstance()
            TAB_SCREEN_LIGHT -> ScreenLightFragment.newInstance()
            TAB_LED -> LedFragment.newInstance()
            TAB_SETTINGS -> FlashAlertFragment.newInstance()
            else -> FlashlightFragment.newInstance()
        }

        val transaction = supportFragmentManager.beginTransaction()
        if (oldTab != tab) {
            if (tab > oldTab) {
                transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
            } else {
                transaction.setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
            }
        }
        transaction.replace(R.id.fragmentContainer, fragment).commit()
    }

    private fun updateNavState(selectedTab: Int) = with(binding.layoutBottomNav) {
        updateNavItem(selectedTab == TAB_FLASHLIGHT, btnNavFlashlight, ivNavFlashlight, tvNavFlashlight)
        updateNavItem(selectedTab == TAB_SCREEN_LIGHT, btnNavScreenLight, ivNavScreenLight, tvNavScreenLight)
        updateNavItem(selectedTab == TAB_LED, btnNavLed, ivNavLed, tvNavLed)
        updateNavItem(selectedTab == TAB_SETTINGS, btnNavSettings, ivNavSettings, tvNavSettings)
    }

    private fun updateNavItem(
        isSelected: Boolean,
        container: LinearLayout,
        icon: ImageView,
        text: TextView
    ) {
        if (isSelected) {
            container.setBackgroundResource(R.drawable.bg_btn_flash_navigation)
            val activeColor = ContextCompat.getColor(this, R.color.bg_yellow_switch)
            icon.imageTintList = ColorStateList.valueOf(activeColor)
            text.setTextColor(activeColor)
        } else {
            container.setBackgroundResource(0)
            val inactiveColor = ContextCompat.getColor(this, R.color.bg_no_select_btn)
            icon.imageTintList = ColorStateList.valueOf(inactiveColor)
            text.setTextColor(inactiveColor)
        }
    }

    fun triggerFeatureInterAd(onAdFinished: () -> Unit = {}) {
        FeatureInterAdsManager.onFeatureUsed(
            activity = this,
            lifecycleOwner = this,
            onAdFinished = onAdFinished
        )
    }

    override fun onDestroy() {
        BannerAdsUtils.releaseBanner("banner_main")
        super.onDestroy()
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"

        const val TAB_FLASHLIGHT = 1
        const val TAB_SCREEN_LIGHT = 2
        const val TAB_LED = 3
        const val TAB_SETTINGS = 4
    }
}
