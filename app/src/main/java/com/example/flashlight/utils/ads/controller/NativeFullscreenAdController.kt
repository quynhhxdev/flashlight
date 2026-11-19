package com.example.flashlight.utils.ads.controller

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.example.flashlight.utils.Logger
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Data class đóng gói các view thành phần của màn hình Native Fullscreen Ad.
 */
data class NativeFullscreenViews(
    val overlay: View,
    val content: FrameLayout,
    val shimmer: ShimmerFrameLayout
)

/**
 * Controller tập trung quản lý logic hiển thị, đếm ngược và vòng đời của Native Fullscreen Ad.
 * Dùng chung cho SplashActivity, OnboardingActivity và bất kỳ màn hình nào cần hiển thị Native Fullscreen.
 */
class NativeFullscreenAdController {

    private var timerJob: Job? = null
    private var loadJob: Job? = null

    var isShowing: Boolean = false
        private set

    private var hasFinished = false
    private var hideOverlayOnFinish: Boolean = true
    private var currentViews: NativeFullscreenViews? = null
    private var currentOnFinished: (() -> Unit)? = null

    /**
     * Hiển thị Native Fullscreen (truyền theo từng View riêng biệt).
     */
    fun show(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        nativeAdHelper: NativeAdHelper?,
        enabled: Boolean,
        overlay: View,
        content: FrameLayout,
        shimmer: ShimmerFrameLayout,
        countdownSeconds: Int = 3,
        autoSkipDelay: Long = 4_000L,
        loadTimeout: Long = 10_000L,
        hideOverlayOnFinish: Boolean = true,
        onFinished: () -> Unit
    ) {
        show(
            activity = activity,
            lifecycleOwner = lifecycleOwner,
            nativeAdHelper = nativeAdHelper,
            enabled = enabled,
            views = NativeFullscreenViews(overlay, content, shimmer),
            countdownSeconds = countdownSeconds,
            autoSkipDelay = autoSkipDelay,
            loadTimeout = loadTimeout,
            hideOverlayOnFinish = hideOverlayOnFinish,
            onFinished = onFinished
        )
    }

    /**
     * Hiển thị Native Fullscreen (truyền qua [NativeFullscreenViews]).
     */
    fun show(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        nativeAdHelper: NativeAdHelper?,
        enabled: Boolean,
        views: NativeFullscreenViews,
        countdownSeconds: Int = 3,
        autoSkipDelay: Long = 4_000L,
        loadTimeout: Long = 10_000L,
        hideOverlayOnFinish: Boolean = true,
        onFinished: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed || isShowing) return

        // 1. Kiểm tra trạng thái bật/tắt hoặc helper null
        if (!enabled || nativeAdHelper == null) {
            Logger.e("NativeFullscreenController: Ad disabled or helper is null -> skip")
            onFinished()
            return
        }

        nativeAdHelper.updateActivity(activity, lifecycleOwner)
        val currentState = nativeAdHelper.getAdNativeState().value

        // 2. Bỏ qua nếu quảng cáo đã có trạng thái Fail hoặc Cancel
        if (nativeAdHelper.nativeAd == null &&
            (currentState is AdNativeState.Fail || currentState is AdNativeState.Cancel)
        ) {
            Logger.e("NativeFullscreenController: Ad already in Fail/Cancel state -> skip")
            onFinished()
            return
        }

        isShowing = true
        hasFinished = false
        this.hideOverlayOnFinish = hideOverlayOnFinish
        currentViews = views
        currentOnFinished = onFinished

        val (overlay, content, shimmer) = views

        // 3. Khởi tạo giao diện
        content.removeAllViews()
        overlay.isVisible = true

        nativeAdHelper.setShimmerLayoutView(shimmer)
        nativeAdHelper.setNativeContentView(content)

        fun updateCloseViews(isCountdown: Boolean, sec: Int = 3) {
            val tvCountdown = content.findViewById<TextView>(com.ads.admob.R.id.tvCountdown)
            val btnClose = content.findViewById<View>(com.ads.admob.R.id.btnClose)
            if (isCountdown) {
                tvCountdown?.text = "Close in $sec"
                tvCountdown?.isVisible = true
                btnClose?.isVisible = false
                btnClose?.setOnClickListener(null)
            } else {
                tvCountdown?.isVisible = false
                btnClose?.isVisible = true
                btnClose?.setOnClickListener {
                    if (isShowing) finish()
                }
            }
        }

        // 4. Hàm đếm ngược: "Close in 3" -> "Close in 2" -> "Close in 1" -> "X"
        fun startCloseTimer() {
            if (timerJob?.isActive == true) return

            timerJob = lifecycleOwner.lifecycleScope.launch {
                if (countdownSeconds > 0) {
                    for (sec in (countdownSeconds - 1) downTo 1) {
                        delay(1_000L)
                        if (!isShowing || !overlay.isVisible) return@launch
                        updateCloseViews(isCountdown = true, sec = sec)
                    }
                    delay(1_000L)
                    if (!isShowing || !overlay.isVisible) return@launch
                }

                updateCloseViews(isCountdown = false)

                // Tự động bỏ qua sau autoSkipDelay
                delay(autoSkipDelay)
                if (!isShowing || !overlay.isVisible) return@launch

                finish()
            }
        }

        if (countdownSeconds > 0) {
            updateCloseViews(isCountdown = true, sec = countdownSeconds)
        } else {
            updateCloseViews(isCountdown = false)
        }

        // 5. Nếu ad đã load thành công -> Bắt đầu đếm ngược ngay
        if (nativeAdHelper.nativeAd != null || currentState is AdNativeState.Loaded) {
            Logger.e("NativeFullscreenController: Ad already loaded -> start countdown")
            startCloseTimer()
            return
        }

        // 6. Chờ quảng cáo load hoặc timeout
        loadJob = lifecycleOwner.lifecycleScope.launch {
            val timeoutJob = launch {
                delay(loadTimeout)
                Logger.e("NativeFullscreenController: Load timeout reached ($loadTimeout ms) -> skip")
                finish()
            }

            nativeAdHelper.getAdNativeState().collect { state ->
                when (state) {
                    is AdNativeState.Loaded -> {
                        Logger.e("NativeFullscreenController: Ad loaded successfully -> start timer")
                        timeoutJob.cancel()
                        startCloseTimer()
                    }
                    is AdNativeState.Fail,
                    is AdNativeState.Cancel -> {
                        Logger.e("NativeFullscreenController: Ad load failed/canceled -> skip")
                        timeoutJob.cancel()
                        finish()
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Đóng Native Fullscreen và hoàn tất callback.
     */
    fun finish() {
        if (hasFinished) return
        hasFinished = true
        isShowing = false

        timerJob?.cancel()
        timerJob = null

        loadJob?.cancel()
        loadJob = null

        currentViews?.let { views ->
            // Chỉ ẩn overlay nếu được yêu cầu. Với SplashActivity, giữ overlay để không bị lộ giao diện Splash.
            if (hideOverlayOnFinish) {
                views.overlay.isGone = true
            }
        }

        val callback = currentOnFinished
        currentViews = null
        currentOnFinished = null

        callback?.invoke()
    }

    /**
     * Hủy tất cả job và giải phóng tham chiếu khi Activity bị destroy.
     * Không kích hoạt callback điều hướng.
     */
    fun release() {
        timerJob?.cancel()
        timerJob = null

        loadJob?.cancel()
        loadJob = null

        isShowing = false
        hasFinished = true
        currentViews = null
        currentOnFinished = null
    }
}
