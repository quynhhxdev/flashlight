package com.example.flashlight.component.main.fagment.led

import android.text.Layout
import android.view.View
import android.widget.TextView
import com.kt.flashlight.R
import kotlin.math.ceil

class LedScrollController(
    private val textView: TextView,
    private val viewport: View,
    private val config: () -> LedConfig
) {
    private var textWidthPx = 0
    private var textHeightPx = 0
    private var translationX = 0f
    private var translationY = 0f
    private var positionInitialized = false
    private var isRunning = false
    private var isDisposed = false
    private var lastFrameTimeNanos = 0L

    private var cachedViewportWidth = 0f
    private var cachedViewportHeight = 0f
    private var cachedDirectionId = 0
    private var cachedIsVertical = false
    private var cachedIsScrollable = false
    private var cachedTargetLeft = 0f
    private var cachedTargetTop = 0f
    private var cachedStaticTargetLeft = 0f
    private var cachedStaticTargetTop = 0f
    private var cachedBaseLeft = 0f
    private var cachedBaseTop = 0f
    private var cachedDurationSeconds = 5f

    private val viewportLayoutChangeListener = View.OnLayoutChangeListener {
            _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
            updateTextMetrics()
            start(resetPosition = false)
        }
    }

    private val frameCallback = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (isDisposed || !textView.isAttachedToWindow || !viewport.isAttachedToWindow) {
                stop()
                return
            }

            if (cachedViewportWidth <= 0f || cachedViewportHeight <= 0f || textWidthPx <= 0 || textHeightPx <= 0) {
                viewport.postOnAnimation(this)
                return
            }

            if (!cachedIsScrollable) {
                applyPosition()
                viewport.postOnAnimation(this)
                return
            }

            val nowNanos = System.nanoTime()
            if (!positionInitialized) {
                resetPosition(cachedViewportWidth, cachedViewportHeight)
                positionInitialized = true
            } else if (lastFrameTimeNanos > 0L) {
                val elapsedSeconds = ((nowNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                advancePosition(elapsedSeconds, cachedViewportWidth, cachedViewportHeight)
            }
            lastFrameTimeNanos = nowNanos
            applyPosition()
            viewport.postOnAnimation(this)
        }
    }

    init {
        viewport.addOnLayoutChangeListener(viewportLayoutChangeListener)
        updateTextMetrics()
    }

    fun updateTextMetrics() {
        val paint = textView.paint
        val textCharSequence = textView.text ?: ""
        val textStr = textCharSequence.toString()

        val desiredWidth = ceil(Layout.getDesiredWidth(textCharSequence, paint).toDouble()).toInt()
        val paintWidth = ceil(paint.measureText(textStr).toDouble()).toInt()
        val contentWidth = maxOf(desiredWidth, paintWidth).coerceAtLeast(0)

        val shadowBuffer = ceil(textView.shadowRadius * 2f).toInt()
        val overhangBuffer = ceil(paint.textSize * 0.1f).toInt()
        val extraBuffer = (shadowBuffer + overhangBuffer + 16).coerceAtLeast(16)

        val paddingHorizontal = textView.compoundPaddingLeft + textView.compoundPaddingRight
        val requiredWidth = (contentWidth + paddingHorizontal + extraBuffer).coerceAtLeast(1)

        val layoutParams = textView.layoutParams
        if (layoutParams != null && layoutParams.width != requiredWidth) {
            layoutParams.width = requiredWidth
            textView.layoutParams = layoutParams
            textView.requestLayout()
        }

        textView.measure(
            View.MeasureSpec.makeMeasureSpec(requiredWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        textWidthPx = requiredWidth
        textHeightPx = textView.measuredHeight.coerceAtLeast(1)
        recalculateCache()
    }

    private fun recalculateCache() {
        val current = config()
        cachedViewportWidth = (viewport.width - viewport.paddingLeft - viewport.paddingRight).toFloat().coerceAtLeast(0f)
        cachedViewportHeight = (viewport.height - viewport.paddingTop - viewport.paddingBottom).toFloat().coerceAtLeast(0f)
        cachedDirectionId = current.directionId
        cachedIsVertical = cachedDirectionId == R.id.btnDirUp || cachedDirectionId == R.id.btnDirDown
        cachedIsScrollable = cachedIsVertical || textWidthPx > cachedViewportWidth

        cachedDurationSeconds = ((11 - current.scrollSpeed.coerceIn(1, 10)) * 900L).coerceIn(800L, 10000L) / 1000f

        cachedStaticTargetLeft = viewport.paddingLeft + (cachedViewportWidth - textWidthPx) / 2f
        cachedStaticTargetTop = viewport.paddingTop + (cachedViewportHeight - textHeightPx) / 2f

        cachedBaseLeft = (textView.left - viewport.paddingLeft).toFloat()
        cachedBaseTop = (textView.top - viewport.paddingTop).toFloat()

        if (cachedIsVertical) {
            cachedTargetLeft = if (textWidthPx > cachedViewportWidth) {
                viewport.paddingLeft.toFloat()
            } else {
                viewport.paddingLeft + (cachedViewportWidth - textWidthPx) / 2f
            }
        } else {
            cachedTargetTop = viewport.paddingTop + (cachedViewportHeight - textHeightPx) / 2f
        }
    }

    fun start(resetPosition: Boolean = false) {
        if (isDisposed) return
        recalculateCache()
        if (resetPosition) {
            positionInitialized = false
            stop()
        }
        if (isRunning) return

        isRunning = true
        lastFrameTimeNanos = 0L
        viewport.postOnAnimation(frameCallback)
    }

    fun stop() {
        isRunning = false
        lastFrameTimeNanos = 0L
        viewport.removeCallbacks(frameCallback)
    }

    fun dispose() {
        stop()
        viewport.removeOnLayoutChangeListener(viewportLayoutChangeListener)
        isDisposed = true
    }

    private fun resetPosition(viewportWidth: Float, viewportHeight: Float) {
        when (cachedDirectionId) {
            R.id.btnDirRight -> {
                translationX = -textWidthPx.toFloat()
                translationY = 0f
            }
            R.id.btnDirLeft -> {
                translationX = viewportWidth
                translationY = 0f
            }
            R.id.btnDirUp -> {
                translationX = 0f
                translationY = viewportHeight
            }
            R.id.btnDirDown -> {
                translationX = 0f
                translationY = -textHeightPx.toFloat()
            }
            else -> {
                translationX = -textWidthPx.toFloat()
                translationY = 0f
            }
        }
    }

    private fun advancePosition(elapsedSeconds: Float, viewportWidth: Float, viewportHeight: Float) {
        val textWidth = textWidthPx.toFloat()
        val textHeight = textHeightPx.toFloat()
        val durationSeconds = cachedDurationSeconds

        when (cachedDirectionId) {
            R.id.btnDirRight -> {
                val cycleDistance = viewportWidth + textWidth
                val travelDistance = cycleDistance * (elapsedSeconds / durationSeconds)
                translationX += travelDistance
                if (translationX >= viewportWidth) {
                    val overflow = (translationX - viewportWidth) % cycleDistance
                    translationX = -textWidth + overflow
                }
            }
            R.id.btnDirLeft -> {
                val cycleDistance = viewportWidth + textWidth
                val travelDistance = cycleDistance * (elapsedSeconds / durationSeconds)
                translationX -= travelDistance
                if (translationX + textWidth <= 0f) {
                    val overflow = (-textWidth - translationX) % cycleDistance
                    translationX = viewportWidth - overflow
                }
            }
            R.id.btnDirUp -> {
                val cycleDistance = viewportHeight + textHeight
                val travelDistance = cycleDistance * (elapsedSeconds / durationSeconds)
                translationY -= travelDistance
                if (translationY + textHeight <= 0f) {
                    val overflow = (-textHeight - translationY) % cycleDistance
                    translationY = viewportHeight - overflow
                }
            }
            R.id.btnDirDown -> {
                val cycleDistance = viewportHeight + textHeight
                val travelDistance = cycleDistance * (elapsedSeconds / durationSeconds)
                translationY += travelDistance
                if (translationY >= viewportHeight) {
                    val overflow = (translationY - viewportHeight) % cycleDistance
                    translationY = -textHeight + overflow
                }
            }
            else -> {
                val cycleDistance = viewportWidth + textWidth
                val travelDistance = cycleDistance * (elapsedSeconds / durationSeconds)
                translationX += travelDistance
                if (translationX >= viewportWidth) {
                    val overflow = (translationX - viewportWidth) % cycleDistance
                    translationX = -textWidth + overflow
                }
            }
        }
    }

    private fun applyPosition() {
        if (cachedViewportWidth <= 0f || cachedViewportHeight <= 0f) return

        if (!cachedIsScrollable) {
            textView.translationX = cachedStaticTargetLeft - textView.left
            textView.translationY = cachedStaticTargetTop - textView.top
            return
        }

        if (cachedIsVertical) {
            textView.translationX = cachedTargetLeft - textView.left
            textView.translationY = translationY - cachedBaseTop
        } else {
            textView.translationY = cachedTargetTop - textView.top
            textView.translationX = translationX - cachedBaseLeft
        }
    }
}
