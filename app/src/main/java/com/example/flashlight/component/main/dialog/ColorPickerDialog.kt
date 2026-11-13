package com.example.flashlight.component.main.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
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

        val hsv = FloatArray(3)
        Color.colorToHSV(initialColor, hsv)
        currentHue = hsv[0]
        currentSat = hsv[1]
        currentVal = hsv[2]
        currentAlpha = Color.alpha(initialColor)

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

        viewBinding.viewHueGradient.background =
            HueGradientDrawable(radius)


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

                    // Position pointer thumb
                    viewBinding.viewColorPointer.x = x - (viewBinding.viewColorPointer.width / 2f)
                    viewBinding.viewColorPointer.y = y - (viewBinding.viewColorPointer.height / 2f)

                    updateColorFromSliders()
                    true
                }
                else -> false
            }
        }
    }

    private fun create2DPaletteDrawable(hue: Float): LayerDrawable {
        val radiusPx = 16f * context.resources.displayMetrics.density

        // Layer 0: Base Hue color (solid color for current hue)
        val baseHue = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        }

        // Layer 1: White to Transparent (Horizontal Saturation)
        val satGrad = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#FFFFFFFF"), Color.parseColor("#00FFFFFF"))
        ).apply {
            cornerRadius = radiusPx
        }

        // Layer 2: Transparent to Black (Vertical Value/Brightness)
        val valGrad = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#00000000"), Color.parseColor("#FF000000"))
        ).apply {
            cornerRadius = radiusPx
        }

        return LayerDrawable(arrayOf(baseHue, satGrad, valGrad))
    }

    private fun createCheckerboardDrawable(context: Context): BitmapDrawable {
        val size = 12 // size of checker square in pixels
        val bitmap = Bitmap.createBitmap(size * 2, size * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paintLight = Paint().apply { color = Color.parseColor("#FFFFFF") }
        val paintDark = Paint().apply { color = Color.parseColor("#C4C4C4") }

        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paintLight)
        canvas.drawRect(size.toFloat(), 0f, (size * 2).toFloat(), size.toFloat(), paintDark)
        canvas.drawRect(0f, size.toFloat(), size.toFloat(), (size * 2).toFloat(), paintDark)
        canvas.drawRect(size.toFloat(), size.toFloat(), (size * 2).toFloat(), (size * 2).toFloat(), paintLight)

        return BitmapDrawable(context.resources, bitmap).apply {
            setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
    }

    private fun updateColorFromSliders() {
        val hueColor = Color.HSVToColor(floatArrayOf(currentHue, currentSat, currentVal))
        currentColor = Color.argb(
            currentAlpha,
            Color.red(hueColor),
            Color.green(hueColor),
            Color.blue(hueColor)
        )

        // Update ivColorPalette 2D gradient background dynamically using current Hue, keeping 4 rounded corners
        viewBinding.ivColorPalette.setImageDrawable(null)
        viewBinding.ivColorPalette.background = create2DPaletteDrawable(currentHue)

        // Update alpha slider gradient dynamically based on selected hue
        val solidHueColor = Color.argb(255, Color.red(hueColor), Color.green(hueColor), Color.blue(hueColor))
        val transHueColor = Color.argb(0, Color.red(hueColor), Color.green(hueColor), Color.blue(hueColor))

        val alphaDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(transHueColor, solidHueColor)
        ).apply {
            cornerRadius = 8.5f * context.resources.displayMetrics.density
        }

        val checkerboard = createCheckerboardDrawable(context)

        val bgDrawable = LayerDrawable(
            arrayOf(
                checkerboard,
                alphaDrawable
            )
        )
        viewBinding.sbAlpha.progressDrawable = bgDrawable

        // Update HEX and RGB text views
        val hex = String.format("#%08X", currentColor)
        viewBinding.etHex.setText(hex)
        viewBinding.etR.setText(Color.red(currentColor).toString())
        viewBinding.etG.setText(Color.green(currentColor).toString())
        viewBinding.etB.setText(Color.blue(currentColor).toString())
    }
}


private class HueGradientDrawable(
    private val cornerRadius: Float
) : android.graphics.drawable.Drawable() {

    private val paint = android.graphics.Paint(
        android.graphics.Paint.ANTI_ALIAS_FLAG
    )

    private val colors = intArrayOf(
        Color.RED,
        Color.parseColor("#FFFF6B00"),
        Color.parseColor("#FFFFE600"),
        Color.parseColor("#FF24FF00"),
        Color.parseColor("#FF00FFF0"),
        Color.parseColor("#FF0029FF"),
        Color.parseColor("#FF8F00FF"),
        Color.parseColor("#FFFF00E5"),
        Color.RED
    )

    private val positions = floatArrayOf(
        0f, 0.1458f, 0.25f, 0.401f,
        0.5156f, 0.6615f, 0.7344f,
        0.8698f, 1f
    )

    override fun draw(canvas: android.graphics.Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        paint.shader = android.graphics.LinearGradient(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.top.toFloat(),
            colors,
            positions,
            android.graphics.Shader.TileMode.CLAMP
        )

        canvas.drawRoundRect(
            android.graphics.RectF(bounds),
            cornerRadius,
            cornerRadius,
            paint
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(
        colorFilter: android.graphics.ColorFilter?
    ) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int =
        android.graphics.PixelFormat.TRANSLUCENT
}

