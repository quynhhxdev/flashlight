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
import com.kt.flashlight.databinding.FragmentSettingsBinding

class FlashAlertFragment : BaseFragment<FragmentSettingsBinding>() {

    private val languageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateCurrentLanguageText()
            (activity as? MainActivity)?.selectTab(MainActivity.TAB_SETTINGS)
        }
    }

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentSettingsBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        viewBinding.layoutBottomNav.root.isVisible = false

        updateCurrentLanguageText()

        viewBinding.btnSettingLanguage.setOnClickListener {
            activity?.let { act ->
                val intent = Intent(act, LanguageActivity::class.java).apply {
                    putExtra(LanguageActivity.EXTRA_FROM_SETTINGS, true)
                }
                languageLauncher.launch(intent)
            }
        }

        viewBinding.btnSettingShare.setOnClickListener {
            context?.let { ctx ->
                ctx.share("https://play.google.com/store/apps/details?id=${ctx.packageName}")
            }
        }

        viewBinding.btnSettingRate.setOnClickListener {
            activity?.let { act ->
                val dialog = RatingDialog(act)
                dialog.show()
            }
        }

        viewBinding.btnSettingPrivacy.setOnClickListener {
            activity?.openBrowser("https://google.com")
        }

        viewBinding.btnSettingPolicy.setOnClickListener {
            activity?.openBrowser("https://google.com")
        }
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLanguageText()
        (activity as? MainActivity)?.selectTab(MainActivity.TAB_SETTINGS)
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
