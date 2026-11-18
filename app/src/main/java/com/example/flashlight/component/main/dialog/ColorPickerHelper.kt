package com.example.flashlight.component.main.dialog

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable

object ColorPickerHelper {

    fun colorToHsvAlpha(color: Int): FloatArray {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return floatArrayOf(hsv[0], hsv[1], hsv[2], Color.alpha(color).toFloat())
    }

    fun calculateColor(hue: Float, sat: Float, valVal: Float, alpha: Int): Int {
        val hueColor = Color.HSVToColor(floatArrayOf(hue, sat, valVal))
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(hueColor),
            Color.green(hueColor),
            Color.blue(hueColor)
        )
    }

    fun formatHex(color: Int): String {
        return String.format("#%08X", color)
    }

    fun getRed(color: Int): Int = Color.red(color)
    fun getGreen(color: Int): Int = Color.green(color)
    fun getBlue(color: Int): Int = Color.blue(color)

    fun create2DPaletteDrawable(hue: Float, radiusPx: Float): LayerDrawable {
        val baseHue = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        }

        val satGrad = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor("#FFFFFFFF"), Color.parseColor("#00FFFFFF"))
        ).apply {
            cornerRadius = radiusPx
        }

        val valGrad = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#00000000"), Color.parseColor("#FF000000"))
        ).apply {
            cornerRadius = radiusPx
        }

        return LayerDrawable(arrayOf(baseHue, satGrad, valGrad))
    }

    fun createAlphaSliderDrawable(context: Context, hue: Float, sat: Float, valVal: Float, radiusPx: Float): LayerDrawable {
        val hueColor = Color.HSVToColor(floatArrayOf(hue, sat, valVal))
        val solidHueColor = Color.argb(255, Color.red(hueColor), Color.green(hueColor), Color.blue(hueColor))
        val transHueColor = Color.argb(0, Color.red(hueColor), Color.green(hueColor), Color.blue(hueColor))

        val alphaDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(transHueColor, solidHueColor)
        ).apply {
            cornerRadius = radiusPx
        }

        val checkerboard = createCheckerboardDrawable(context)
        return LayerDrawable(arrayOf(checkerboard, alphaDrawable))
    }

    fun createCheckerboardDrawable(context: Context): BitmapDrawable {
        val size = 12
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
}

class HueGradientDrawable(
    private val cornerRadius: Float
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

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

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        paint.shader = android.graphics.LinearGradient(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.top.toFloat(),
            colors,
            positions,
            Shader.TileMode.CLAMP
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

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}
