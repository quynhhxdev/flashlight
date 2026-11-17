package com.example.flashlight.component.main.fagment

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.ViewGroup
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.MainActivity
import com.example.flashlight.component.main.dialog.ColorPickerDialog
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.utils.SpManager
import com.example.flashlight.utils.ads.AdPlacement
import com.example.flashlight.utils.ads.InterAdsUtils
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.example.flashlight.utils.ads.manager.FeatureInterAdsManager
import com.kt.flashlight.databinding.FragmentScreenLightBinding

class ScreenLightFragment : BaseFragment<FragmentScreenLightBinding>() {

    private var baseColor = Color.parseColor("#4FC3F7") // Blue default

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentScreenLightBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        // Hide internal bottom nav if present
        viewBinding.layoutBottomNav.root.isVisible = false

        NativeAdsUtils.bindHomeNative(
            activity = requireActivity(),
            lifecycleOwner = viewLifecycleOwner,
            nativeContentView = viewBinding.nativeAdContainer,
            shimmerContainerNative = viewBinding.shimmerContainerNative.root
        )

        val spManager = SpManager.getInstance(requireContext())
        baseColor = spManager.getScreenLightColor(Color.parseColor("#4FC3F7"))
        val savedBrightness = spManager.getScreenLightBrightness(80)

        viewBinding.sbBrightness.progress = savedBrightness
        viewBinding.tvBrightnessValue.text = "$savedBrightness%"

        updateColorPreview(baseColor)

        // Brightness slider listener
        viewBinding.sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewBinding.tvBrightnessValue.text = "$progress%"
                updateColorPreview(baseColor)
                spManager.saveScreenLightBrightness(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Click color preview box to open ColorPickerDialog
        viewBinding.cardColorPreview.setOnClickListener {
            val dialog = ColorPickerDialog(requireContext(), baseColor) { newColor ->
                baseColor = newColor
                updateColorPreview(baseColor)
                spManager.saveScreenLightColor(baseColor)
            }
            dialog.show()
        }

        // Fullscreen / Zoom button – trigger feature inter trước khi mở
        viewBinding.btnFullScreen.setOnClickListener {
            triggerFeatureAdThenFullScreen()
        }
    }

    /**
     * Kích hoạt bộ đếm inter chung qua MainActivity.
     * Nếu đủ điều kiện → show inter → sau khi đóng ads mới mở full screen.
     * Nếu không đủ → mở full screen ngay.
     */
    private fun triggerFeatureAdThenFullScreen() {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            mainActivity.triggerFeatureInterAd {
                toggleFullScreenPreview()
            }
        } else {
            toggleFullScreenPreview()
        }
    }

    private fun updateColorPreview(color: Int) {
        baseColor = color
        val brightness = viewBinding.sbBrightness.progress
        val adjustedColor = adjustColorBrightness(baseColor, brightness)
        viewBinding.cardColorPreview.setCardBackgroundColor(adjustedColor)
    }

    private fun adjustColorBrightness(color: Int, brightnessPercent: Int): Int {
        val factor = brightnessPercent / 100f
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    private fun toggleFullScreenPreview() {
        val act = activity ?: return

        // Rotate to landscape
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val dialog = android.app.Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val brightness = viewBinding.sbBrightness.progress
        val adjustedColor = adjustColorBrightness(baseColor, brightness)

        val fullscreenView = FrameLayout(act).apply {
            setBackgroundColor(adjustedColor)
        }
        val closeButton = ImageView(act).apply {
            setImageResource(com.kt.flashlight.R.drawable.ic_close_play)
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

        // Make dialog window immersive full screen
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onDestroyView()
    }

    private fun handleClosePlay(activity: android.app.Activity, dialog: android.app.Dialog) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            mainActivity.triggerFeatureInterAd {
                dialog.dismiss()
            }
        } else {
            FeatureInterAdsManager.onFeatureUsed(
                activity = activity,
                lifecycleOwner = viewLifecycleOwner,
                onAdFinished = {
                    dialog.dismiss()
                }
            )
        }
    }

    companion object {
        fun newInstance() = ScreenLightFragment()
    }
}
