package com.example.flashlight.component.onboard

import android.content.Intent
import android.provider.Settings
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ads.admob.helper.banner.params.AdBannerState
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.utils.Utils
import com.example.flashlight.utils.ads.BannerAdsUtils
import com.kt.flashlight.databinding.FragmentNoInternetFullscreenBinding
import kotlinx.coroutines.launch

class NoInternetFragment : BaseFragment<FragmentNoInternetFullscreenBinding>() {

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentNoInternetFullscreenBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        val act = activity as? AppCompatActivity
        if (act != null) {
            BannerAdsUtils.initBannerMain(
                activity = act,
                lifecycleOwner = viewLifecycleOwner,
                bannerContentView = viewBinding.bannerContent,
                shimmerLayoutView = viewBinding.shimmerContainerBanner.root,
                placement = "banner_main",
                onReady = { bannerHelper ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            bannerHelper.getBannerState().collect { state ->
                                viewBinding.bannerSlot.isGone =
                                    (state == AdBannerState.Fail || state == AdBannerState.Cancel || state == AdBannerState.None)
                            }
                        }
                    }
                }
            )
        }

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
        if (context?.let { Utils.isConnected(it) } == true) {
            proceedToNextScreen()
        }
    }

    private fun proceedToNextScreen() {
        (activity as? IntroActivity)?.showPermissionFragment()
    }

    override fun onDestroyView() {
        BannerAdsUtils.releaseBanner("banner_main")
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = NoInternetFragment()
    }
}
