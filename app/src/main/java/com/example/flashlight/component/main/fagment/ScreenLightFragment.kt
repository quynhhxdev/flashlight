package com.example.flashlight.component.main.fagment

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.dialog.ColorPickerDialog
import com.example.flashlight.utils.SpManager
import com.kt.flashlight.databinding.FragmentScreenLightBinding

class ScreenLightFragment : BaseFragment<FragmentScreenLightBinding>() {

    private var baseColor = Color.parseColor("#4FC3F7") // Blue default

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentScreenLightBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        // Hide internal bottom nav if present
        viewBinding.layoutBottomNav.root.isVisible = false

        val spManager = SpManager.getInstance(requireContext())
        baseColor = spManager.getScreenLightColor(Color.parseColor("#4FC3F7"))
        val savedBrightness = spManager.getScreenLightBrightness(80)

        viewBinding.sbBrightness.progress = savedBrightness
        viewBinding.tvBrightnessValue.text = "$savedBrightness%"

        updateColorPreview(baseColor)

        // Brightness slider listener (only adjusts cardColorPreview brightness, not system window brightness)
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

        // Fullscreen / Zoom button
        viewBinding.btnFullScreen.setOnClickListener {
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

        val view = View(act).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(adjustedColor)
        }

        // Tap screen to dismiss full screen and return to portrait
        view.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)

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

    override fun onDestroyView() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = ScreenLightFragment()
    }
}
