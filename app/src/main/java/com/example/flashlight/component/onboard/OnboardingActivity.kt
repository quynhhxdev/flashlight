package com.example.flashlight.component.onboard

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.utils.dp
import com.example.flashlight.utils.Utils
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.kt.flashlight.R
import com.kt.flashlight.databinding.ActivityOnboardingBinding

data class OnboardingData(
    val imageRes: Int,
    val title: String,
    val description: String
)

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {

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
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                // Screen 1 & 3 (index 0 & 2) have native ads; Screen 2 & 4 (index 1 & 3) are full screen without ads
                val hasAds = (position == 0 || position == 2)
                binding.nativeAdContainer.isVisible = hasAds
            }
        })
    }

    private fun preloadAds() {
        NativeAdsUtils.preloadNativeOnboarding(this)
        NativeAdsUtils.nativeOnboarding?.setNativeContentView(binding.nativeAdContainer)
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            val current = binding.viewPagerOnboarding.currentItem
            if (current < onboardingItems.size - 1) {
                binding.viewPagerOnboarding.currentItem = current + 1
            } else {
                onOnboardingFinished()
            }
        }
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
}
