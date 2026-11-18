package com.example.flashlight.component.main.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.SeekBar
import com.example.flashlight.base.dialog.BaseDialog
import com.kt.flashlight.databinding.DialogColorPickerBinding

class ColorPickerDialog @JvmOverloads constructor(
    context: Context,
    private val initialColor: Int = Color.parseColor("#9751F2"),
    private val onColorSelected: (Int) -> Unit
) : BaseDialog<DialogColorPickerBinding>(context) {

    private var currentHue = 270f
    private var currentSat = 1f
    private var currentVal = 1f
    private var currentAlpha = 255
    private var currentColor = initialColor

    override fun provideViewBinding(): DialogColorPickerBinding {
        return DialogColorPickerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initViews() {
        super.initViews()

        val hsvAlpha = ColorPickerHelper.colorToHsvAlpha(initialColor)
        currentHue = hsvAlpha[0]
        currentSat = hsvAlpha[1]
        currentVal = hsvAlpha[2]
        currentAlpha = hsvAlpha[3].toInt()

        viewBinding.sbHue.progress = currentHue.toInt()
        viewBinding.sbAlpha.progress = currentAlpha

        setupPaletteTouchListener()
        updateColorFromSliders()

        viewBinding.sbHue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentHue = progress.toFloat()
                updateColorFromSliders()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        viewBinding.sbAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentAlpha = progress
                updateColorFromSliders()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val radius = 8.5f * context.resources.displayMetrics.density
        viewBinding.viewHueGradient.background = HueGradientDrawable(radius)

        viewBinding.btnChooseColor.setOnClickListener {
            onColorSelected(currentColor)
            dismiss()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPaletteTouchListener() {
        viewBinding.layoutPaletteContainer.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val width = view.width.toFloat().coerceAtLeast(1f)
                    val height = view.height.toFloat().coerceAtLeast(1f)

                    val x = event.x.coerceIn(0f, width)
                    val y = event.y.coerceIn(0f, height)

                    currentSat = (x / width).coerceIn(0f, 1f)
                    currentVal = (1f - (y / height)).coerceIn(0f, 1f)

                    viewBinding.viewColorPointer.x = x - (viewBinding.viewColorPointer.width / 2f)
                    viewBinding.viewColorPointer.y = y - (viewBinding.viewColorPointer.height / 2f)

                    updateColorFromSliders()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateColorFromSliders() {
        currentColor = ColorPickerHelper.calculateColor(currentHue, currentSat, currentVal, currentAlpha)

        val radiusPx = 16f * context.resources.displayMetrics.density
        viewBinding.ivColorPalette.setImageDrawable(null)
        viewBinding.ivColorPalette.background = ColorPickerHelper.create2DPaletteDrawable(currentHue, radiusPx)

        val sliderRadius = 8.5f * context.resources.displayMetrics.density
        viewBinding.sbAlpha.progressDrawable = ColorPickerHelper.createAlphaSliderDrawable(
            context, currentHue, currentSat, currentVal, sliderRadius
        )

        viewBinding.etHex.setText(ColorPickerHelper.formatHex(currentColor))
        viewBinding.etR.setText(ColorPickerHelper.getRed(currentColor).toString())
        viewBinding.etG.setText(ColorPickerHelper.getGreen(currentColor).toString())
        viewBinding.etB.setText(ColorPickerHelper.getBlue(currentColor).toString())
    }
}
