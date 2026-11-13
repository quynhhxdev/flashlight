package com.example.flashlight.component.onboard

import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.utils.Utils
import com.kt.flashlight.databinding.FragmentNoInternetFullscreenBinding

class NoInternetFragment : BaseFragment<FragmentNoInternetFullscreenBinding>() {

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentNoInternetFullscreenBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        viewBinding.btnGoToSettings.setOnClickListener {
            runCatching {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
            }.onFailure {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }

        viewBinding.tvCancel.setOnClickListener {
            proceedToNextScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check internet when returning from Settings
        if (context?.let { Utils.isConnected(it) } == true) {
            proceedToNextScreen()
        }
    }

    private fun proceedToNextScreen() {
        (activity as? IntroActivity)?.showPermissionFragment()
    }

    companion object {
        fun newInstance() = NoInternetFragment()
    }
}
