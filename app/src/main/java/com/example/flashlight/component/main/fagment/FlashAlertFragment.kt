package com.example.flashlight.component.main.fagment

import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.language.LanguageActivity
import com.example.flashlight.component.main.MainActivity
import com.example.flashlight.component.main.dialog.RatingDialog
import com.example.flashlight.utils.SpManager
import com.example.flashlight.utils.openBrowser
import com.example.flashlight.utils.share
import com.kt.flashlight.databinding.FragmentFlashAlertBinding

class FlashAlertFragment : BaseFragment<FragmentFlashAlertBinding>() {

    private val languageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateCurrentLanguageText()
        }
    }

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentFlashAlertBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        viewBinding.layoutBottomNav.root.isVisible = false

        updateCurrentLanguageText()

        viewBinding.btnSettingLanguage.setOnClickListener {
            triggerFeatureAdThenRun {
                activity?.let { act ->
                    val intent = Intent(act, LanguageActivity::class.java).apply {
                        putExtra(LanguageActivity.EXTRA_FROM_SETTINGS, true)
                    }
                    languageLauncher.launch(intent)
                }
            }
        }

        viewBinding.btnSettingShare.setOnClickListener {
            triggerFeatureAdThenRun {
                context?.let { ctx ->
                    ctx.share("https://play.google.com/store/apps/details?id=${ctx.packageName}")
                }
            }
        }

        viewBinding.btnSettingRate.setOnClickListener {
            triggerFeatureAdThenRun {
                activity?.let { act ->
                    val dialog = RatingDialog(act)
                    dialog.show()
                }
            }
        }

        viewBinding.btnSettingPrivacy.setOnClickListener {
            triggerFeatureAdThenRun {
                activity?.openBrowser("https://sites.google.com/view/flashlight-led-banner-policy/")
            }
        }

        viewBinding.btnSettingPolicy.setOnClickListener {
            triggerFeatureAdThenRun {
                activity?.openBrowser("https://google.com")
            }
        }
    }

    /**
     * Kích hoạt bộ đếm inter chung qua MainActivity rồi thực thi [action].
     * Nếu không tìm thấy MainActivity thì chạy [action] ngay.
     */
    private fun triggerFeatureAdThenRun(action: () -> Unit) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            mainActivity.triggerFeatureInterAd { action() }
        } else {
            action()
        }
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLanguageText()
    }

    private fun updateCurrentLanguageText() {
        context?.let { ctx ->
            val lang = SpManager.getInstance(ctx).getLanguage()
            viewBinding.tvCurrentLanguage.text = getString(lang.nameRes)
        }
    }

    companion object {
        fun newInstance() = FlashAlertFragment()
    }
}
