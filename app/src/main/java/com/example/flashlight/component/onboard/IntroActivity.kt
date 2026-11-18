package com.example.flashlight.component.onboard

import android.os.Bundle
import com.example.flashlight.base.activity.BaseActivity
import com.kt.flashlight.databinding.ActivityOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroActivity : BaseActivity<ActivityOnboardingBinding>() {

    override fun provideViewBinding() = ActivityOnboardingBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startScreen = intent.getIntExtra(EXTRA_START_SCREEN, SCREEN_PERMISSION)
        if (savedInstanceState == null) {
            when (startScreen) {
                SCREEN_NO_INTERNET -> showNoInternetFragment()
                else -> showPermissionFragment()
            }
        }
    }

    fun showNoInternetFragment() {
        replaceFragment(binding.onboardingContainer.id, NoInternetFragment.newInstance())
    }

    fun showPermissionFragment() {
        replaceFragment(binding.onboardingContainer.id, PermissionFragment.newInstance())
    }

    companion object {
        const val EXTRA_START_SCREEN = "extra_start_screen"
        const val SCREEN_NO_INTERNET = 1
        const val SCREEN_PERMISSION = 2
    }
}
