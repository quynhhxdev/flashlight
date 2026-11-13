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

    private val viewportLayoutChangeListener = View.OnLayoutChangeListener {
            _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
            updateTextMetrics()
        }
    }

    private val frameCallback = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (isDisposed || !textView.isAttachedToWindow || !viewport.isAttachedToWindow) {
                stop()
                return
            }

            val viewportWidth = (viewport.width - viewport.paddingLeft - viewport.paddingRight).toFloat()
            val viewportHeight = (viewport.height - viewport.paddingTop - viewport.paddingBottom).toFloat()
            if (viewportWidth <= 0f || viewportHeight <= 0f || textWidthPx <= 0 || textHeightPx <= 0) {
                viewport.postOnAnimation(this)
                return
            }

            val nowNanos = System.nanoTime()
            if (!positionInitialized) {
                resetPosition(viewportWidth, viewportHeight)
                positionInitialized = true
            } else if (lastFrameTimeNanos > 0L) {
                val elapsedSeconds = (nowNanos - lastFrameTimeNanos) / 1_000_000_000f
                advancePosition(elapsedSeconds, viewportWidth, viewportHeight)
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
        val contentWidth = ceil(Layout.getDesiredWidth(textView.text, textView.paint).toDouble())
            .toInt()
            .coerceAtLeast(0)
        val requiredWidth = (contentWidth + textView.compoundPaddingLeft + textView.compoundPaddingRight)
            .coerceAtLeast(1)

        val layoutParams = textView.layoutParams
        if (layoutParams != null && layoutParams.width != requiredWidth) {
            layoutParams.width = requiredWidth
            textView.layoutParams = layoutParams
        }

        textView.measure(
            View.MeasureSpec.makeMeasureSpec(requiredWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        textWidthPx = requiredWidth
        textHeightPx = textView.measuredHeight
    }

    fun start(resetPosition: Boolean = false) {
        if (isDisposed) return
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
        when (config().directionId) {
            R.id.btnDirLeft -> {
                translationX = viewportWidth
                translationY = 0f
            }
            R.id.btnDirRight -> {
                translationX = -textWidthPx.toFloat()
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
                translationX = viewportWidth
                translationY = 0f
            }
        }
    }

    private fun advancePosition(elapsedSeconds: Float, viewportWidth: Float, viewportHeight: Float) {
        val current = config()
        val textWidth = textWidthPx.toFloat()
        val textHeight = textHeightPx.toFloat()
        val durationSeconds = ((11 - current.scrollSpeed) * 900L)
            .coerceIn(800L, 10000L) / 1000f

        when (current.directionId) {
            R.id.btnDirRight -> {
                if (translationX >= viewportWidth) translationX = -textWidth
                val cycleDistance = viewportWidth + textWidth
                val travelDistance = cycleDistance * elapsedSeconds / durationSeconds
                val distanceToExit = (viewportWidth - translationX).coerceAtLeast(0f)
                translationX = if (travelDistance < distanceToExit) {
                    translationX + travelDistance
                } else {
                    -textWidth + ((travelDistance - distanceToExit) % cycleDistance)
                }
            }
            R.id.btnDirUp -> {
                if (translationY + textHeight <= 0f) translationY = viewportHeight
                val cycleDistance = viewportHeight + textHeight
                val travelDistance = cycleDistance * elapsedSeconds / durationSeconds
                val distanceToExit = (translationY + textHeight).coerceAtLeast(0f)
                translationY = if (travelDistance < distanceToExit) {
                    translationY - travelDistance
                } else {
                    viewportHeight - ((travelDistance - distanceToExit) % cycleDistance)
                }
            }
            R.id.btnDirDown -> {
                if (translationY >= viewportHeight) translationY = -textHeight
                val cycleDistance = viewportHeight + textHeight
                val travelDistance = cycleDistance * elapsedSeconds / durationSeconds
                val distanceToExit = (viewportHeight - translationY).coerceAtLeast(0f)
                translationY = if (travelDistance < distanceToExit) {
                    translationY + travelDistance
                } else {
                    -textHeight + ((travelDistance - distanceToExit) % cycleDistance)
                }
            }
            else -> {
                if (translationX + textWidth <= 0f) translationX = viewportWidth
                val cycleDistance = viewportWidth + textWidth
                val travelDistance = cycleDistance * elapsedSeconds / durationSeconds
                val distanceToExit = (translationX + textWidth).coerceAtLeast(0f)
                translationX = if (travelDistance < distanceToExit) {
                    translationX - travelDistance
                } else {
                    viewportWidth - ((travelDistance - distanceToExit) % cycleDistance)
                }
            }
        }
    }

    private fun applyPosition() {
        val direction = config().directionId
        val isVertical = direction == R.id.btnDirUp || direction == R.id.btnDirDown
        if (isVertical) {
            textView.translationX = 0f
            val baseTop = (textView.top - viewport.paddingTop).toFloat()
            textView.translationY = translationY - baseTop
        } else {
            val baseLeft = (textView.left - viewport.paddingLeft).toFloat()
            textView.translationX = translationX - baseLeft
            textView.translationY = 0f
        }
    }
}
