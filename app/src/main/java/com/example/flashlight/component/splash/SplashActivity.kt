package com.example.flashlight.component.splash

import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.helper.banner.params.AdBannerState
import com.kt.flashlight.R
import com.example.flashlight.base.activity.BaseActivity
import com.kt.flashlight.databinding.ActivitySplashBinding
import com.example.flashlight.utils.ads.BannerAdsUtils
import com.example.flashlight.component.main.MainActivity
import com.example.flashlight.utils.SpManager
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    private val viewModel: SplashViewModel by viewModels()

    override fun provideViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun initViews() {
        BannerAdsUtils.initBannerMain(
            this,
            this,
            binding.bannerContent,
        )
        val bannerHelper = BannerAdsUtils.bannerAdMain ?: return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bannerHelper.getBannerState().collect { state ->
                    binding.adIndicator.isVisible = state is AdBannerState.Loaded
                    binding.bannerSlot.isGone =
                        (state == AdBannerState.Fail || state == AdBannerState.Cancel)
                }
            }
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.progress.collect { progress ->
                    binding.loadingPercentage.text =
                        getString(R.string.splash_loading_percentage, progress)
                    binding.loadingProgress.progress = progress
                    if (progress >= 100) {
                        navigateToHome()
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        val spManager = SpManager.getInstance(this)
        if (spManager.isOnboardingCompleted()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, com.example.flashlight.component.language.LanguageActivity::class.java).apply {
                putExtra(com.example.flashlight.component.language.LanguageActivity.EXTRA_FROM_SETTINGS, false)
            }
            startActivity(intent)
        }
        finish()
    }

    override fun onDestroy() {
        BannerAdsUtils.releaseBannerMain()
        super.onDestroy()
    }
}
