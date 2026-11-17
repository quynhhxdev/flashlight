package com.example.flashlight.component.main

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.component.main.fagment.FlashlightFragment
import com.example.flashlight.component.main.fagment.LedFragment
import com.example.flashlight.component.main.fagment.ScreenLightFragment
import com.example.flashlight.component.main.fagment.FlashAlertFragment
import com.example.flashlight.utils.ads.manager.FeatureInterAdsManager
import com.kt.flashlight.R
import com.kt.flashlight.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var currentTab = TAB_FLASHLIGHT

    override fun provideViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload inter quảng cáo cho các tính năng ngay khi vào MainActivity
        FeatureInterAdsManager.preload(this)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            selectTab(TAB_FLASHLIGHT)
        } else {
            currentTab = savedInstanceState.getInt(KEY_SELECTED_TAB, TAB_FLASHLIGHT)
            updateNavState(currentTab)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, currentTab)
    }

    private fun setupBottomNavigation() = with(binding.layoutBottomNav) {
        btnNavFlashlight.setOnClickListener {
            selectTab(TAB_FLASHLIGHT)
        }

        btnNavScreenLight.setOnClickListener {
            selectTab(TAB_SCREEN_LIGHT)
        }

        btnNavLed.setOnClickListener {
            selectTab(TAB_LED)
        }

        btnNavSettings.setOnClickListener {
            selectTab(TAB_SETTINGS)
        }
    }

    fun selectTab(tab: Int) {
        currentTab = tab
        updateNavState(tab)

        val fragment: Fragment = when (tab) {
            TAB_FLASHLIGHT -> FlashlightFragment.newInstance()
            TAB_SCREEN_LIGHT -> ScreenLightFragment.newInstance()
            TAB_LED -> LedFragment.newInstance()
            TAB_SETTINGS -> FlashAlertFragment.newInstance()
            else -> FlashlightFragment.newInstance()
        }

        replaceFragment(R.id.fragmentContainer, fragment)
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

    /**
     * Điểm vào tập trung để các Fragment kích hoạt inter quảng cáo tính năng.
     * Fragment chỉ cần gọi hàm này, KHÔNG tự quản lý counter hay logic show ads.
     *
     * @param onAdFinished Callback được gọi sau khi ads đóng hoặc bị bỏ qua
     *                     (Fragment tiến hành hành động thực sự sau đây, vd: bật đèn)
     */
    fun triggerFeatureInterAd(onAdFinished: () -> Unit = {}) {
        FeatureInterAdsManager.onFeatureUsed(
            activity = this,
            lifecycleOwner = this,
            onAdFinished = onAdFinished
        )
    }

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"

        const val TAB_FLASHLIGHT = 1
        const val TAB_SCREEN_LIGHT = 2
        const val TAB_LED = 3
        const val TAB_SETTINGS = 4
    }
}
