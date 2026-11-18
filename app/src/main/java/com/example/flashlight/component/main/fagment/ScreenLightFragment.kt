package com.example.flashlight.component.main.fagment

import android.app.Activity
import android.app.Dialog
import android.content.pm.ActivityInfo
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.MainActivity
import com.example.flashlight.component.main.dialog.ColorPickerDialog
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.kt.flashlight.R
import com.kt.flashlight.databinding.FragmentScreenLightBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScreenLightFragment : BaseFragment<FragmentScreenLightBinding>() {

    private val viewModel: ScreenLightViewModel by viewModels()

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentScreenLightBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        viewBinding.layoutBottomNav.root.isVisible = false

        NativeAdsUtils.bindHomeNative(
            activity = requireActivity(),
            lifecycleOwner = viewLifecycleOwner,
            nativeContentView = viewBinding.nativeAdContainer,
            shimmerContainerNative = viewBinding.shimmerContainerNative.root
        )

        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        viewBinding.sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.updateBrightness(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        viewBinding.cardColorPreview.setOnClickListener {
            val currentColor = viewModel.uiState.value.baseColor
            val dialog = ColorPickerDialog(requireContext(), currentColor) { newColor ->
                viewModel.updateColor(newColor)
            }
            dialog.show()
        }

        viewBinding.btnFullScreen.setOnClickListener {
            toggleFullScreenPreview()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (viewBinding.sbBrightness.progress != state.brightnessProgress) {
                        viewBinding.sbBrightness.progress = state.brightnessProgress
                    }
                    viewBinding.tvBrightnessValue.text = state.brightnessText
                    viewBinding.cardColorPreview.setCardBackgroundColor(state.adjustedColor)
                }
            }
        }
    }

    private fun toggleFullScreenPreview() {
        val act = activity ?: return

        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val dialog = Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val adjustedColor = viewModel.uiState.value.adjustedColor

        val fullscreenView = FrameLayout(act).apply {
            setBackgroundColor(adjustedColor)
        }
        val closeButton = ImageView(act).apply {
            setImageResource(R.drawable.ic_close_play)
            contentDescription = "Close full screen"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                handleClosePlay(act, dialog)
            }
        }
        fullscreenView.addView(
            closeButton,
            FrameLayout.LayoutParams(dpToPx(28), dpToPx(28), Gravity.TOP or Gravity.START).apply {
                leftMargin = dpToPx(24)
                topMargin = dpToPx(24)
            }
        )

        dialog.setContentView(fullscreenView)

        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        dialog.setOnDismissListener {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        dialog.show()
    }

    private fun handleClosePlay(activity: Activity, dialog: Dialog) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            mainActivity.triggerFeatureInterAd {
                dialog.dismiss()
            }
        } else {
            dialog.dismiss()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = ScreenLightFragment()
    }
}
