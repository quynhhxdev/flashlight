package com.example.flashlight.component.onboard

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.example.flashlight.utils.Logger
import com.kt.flashlight.R
import com.kt.flashlight.databinding.ItemOnboardingNativePageBinding
import com.kt.flashlight.databinding.ItemOnboardingPageBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewPager2 Adapter hỗ trợ 2 loại trang:
 * - [VIEW_TYPE_NORMAL]: Trang Onboarding thông thường (ảnh + tiêu đề + mô tả).
 * - [VIEW_TYPE_NATIVE_FULLSCREEN]: Trang Native Fullscreen Ads chiếm toàn bộ ViewPager item.
 */
class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>,
    private val onNativeFullscreenPageBound: (pageIndex: Int, holder: NativeFullscreenViewHolder) -> Unit,
    private val onNativeFullscreenCloseClicked: (pageIndex: Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_NATIVE_FULLSCREEN = 1
    }

    // Giữ reference holders đang active theo position
    private val activeNativeHolders = mutableMapOf<Int, NativeFullscreenViewHolder>()

    // Lưu page đang cần start countdown nếu onPageSelected gọi trước khi onBindViewHolder hoàn tất
    private var pendingCountdownPageIndex: Int? = null

    override fun getItemViewType(position: Int): Int = when (pages[position]) {
        is OnboardingPage.Normal -> VIEW_TYPE_NORMAL
        is OnboardingPage.NativeFullscreen -> VIEW_TYPE_NATIVE_FULLSCREEN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NATIVE_FULLSCREEN -> {
                val binding = ItemOnboardingNativePageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                NativeFullscreenViewHolder(binding)
            }
            else -> {
                val binding = ItemOnboardingPageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                NormalPageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NormalPageViewHolder -> {
                val page = pages[position] as OnboardingPage.Normal
                holder.bind(page.data)
            }
            is NativeFullscreenViewHolder -> {
                activeNativeHolders[position] = holder
                onNativeFullscreenPageBound(position, holder)

                // Nếu có yêu cầu countdown đang chờ cho vị trí này, kích hoạt ngay
                if (pendingCountdownPageIndex == position) {
                    pendingCountdownPageIndex = null
                    holder.startCountdownIfNotStarted()
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is NativeFullscreenViewHolder) {
            val entry = activeNativeHolders.entries.find { it.value == holder }
            entry?.let { activeNativeHolders.remove(it.key) }
            holder.onRecycled()
        }
    }

    override fun getItemCount(): Int = pages.size

    /**
     * Kích hoạt countdown cho trang Native Fullscreen khi trang thực sự được chọn trên màn hình.
     */
    fun startCountdownForPage(position: Int) {
        val holder = activeNativeHolders[position]
        if (holder != null) {
            holder.startCountdownIfNotStarted()
        } else {
            // Lưu lại để chạy ngay khi holder được bind
            pendingCountdownPageIndex = position
        }
    }

    // ─── Normal Page ViewHolder ───────────────────────────────────────────────

    class NormalPageViewHolder(private val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: OnboardingData) {
            binding.imgHero.setImageResource(data.imageRes)
            binding.tvTitle.text = data.title
            binding.tvDescription.text = data.description
        }
    }

    // ─── Native Fullscreen Page ViewHolder ───────────────────────────────────

    inner class NativeFullscreenViewHolder(
        private val binding: ItemOnboardingNativePageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundPageIndex: Int = -1
        private var timerJob: Job? = null
        private var loadJob: Job? = null
        private var isCountdownRunning: Boolean = false

        private var activity: Activity? = null
        private var lifecycleOwner: LifecycleOwner? = null
        private var nativeAdHelper: NativeAdHelper? = null
        private var adEnabled: Boolean = false
        private var countdownAlreadyDone: Boolean = false
        private var onCountdownDoneCallback: (() -> Unit)? = null
        private var onAutoNavigateCallback: (() -> Unit)? = null

        fun bind(
            pageIndex: Int,
            activity: Activity,
            lifecycleOwner: LifecycleOwner,
            nativeAdHelper: NativeAdHelper?,
            adEnabled: Boolean,
            topInset: Int,
            countdownAlreadyDone: Boolean,
            onCountdownDone: () -> Unit,
            onAutoNavigate: () -> Unit,
        ) {
            // Nếu ViewHolder này được dùng lại cho một pageIndex khác, reset toàn bộ state cũ
            if (boundPageIndex != pageIndex) {
                resetState()
                boundPageIndex = pageIndex
            }

            this.activity = activity
            this.lifecycleOwner = lifecycleOwner
            this.nativeAdHelper = nativeAdHelper
            this.adEnabled = adEnabled
            this.countdownAlreadyDone = countdownAlreadyDone
            this.onCountdownDoneCallback = onCountdownDone
            this.onAutoNavigateCallback = onAutoNavigate

            applyTopInset(topInset)

            if (nativeAdHelper != null && adEnabled) {
                nativeAdHelper.updateActivity(activity, lifecycleOwner)
                nativeAdHelper.setShimmerLayoutView(binding.shimmerContainerNative.root)
                nativeAdHelper.setNativeContentView(binding.nativeAdContent)
            }

            if (countdownAlreadyDone) {
                // Đã đếm xong từ trước: hiện nút Close ngay
                showCloseButton()
            } else if (!isCountdownRunning) {
                // Chưa đếm xong và chưa chạy: chuẩn bị giao diện đếm
                initCloseButtonCountdown()
            }
        }

        /**
         * Chỉ bắt đầu đếm khi trang này thực sự hiển thị trên màn hình.
         */
        fun startCountdownIfNotStarted() {
            if (countdownAlreadyDone || isCountdownRunning) return

            val currentOwner = lifecycleOwner ?: return
            val helper = nativeAdHelper

            if (!adEnabled || helper == null) {
                Logger.e("NativeFullscreenPage ($boundPageIndex): Ad disabled or null -> skip")
                finishCountdown(currentOwner, skipImmediately = true)
                return
            }

            isCountdownRunning = true
            initCloseButtonCountdown()

            val currentState = helper.getAdNativeState().value
            if (helper.nativeAd != null || currentState is AdNativeState.Loaded) {
                Logger.e("NativeFullscreenPage ($boundPageIndex): Ad already loaded -> start timer")
                runCountdownTimer(currentOwner)
                return
            }

            // Ad chưa load xong: chờ ad load hoặc timeout 10s
            loadJob?.cancel()
            loadJob = currentOwner.lifecycleScope.launch {
                val timeoutJob = launch {
                    delay(10_000L)
                    Logger.e("NativeFullscreenPage ($boundPageIndex): Load timeout -> finish")
                    finishCountdown(currentOwner, skipImmediately = true)
                }

                helper.getAdNativeState().collect { state ->
                    when (state) {
                        is AdNativeState.Loaded -> {
                            Logger.e("NativeFullscreenPage ($boundPageIndex): Ad loaded -> start timer")
                            timeoutJob.cancel()
                            runCountdownTimer(currentOwner)
                        }
                        is AdNativeState.Fail,
                        is AdNativeState.Cancel -> {
                            Logger.e("NativeFullscreenPage ($boundPageIndex): Ad failed -> finish")
                            timeoutJob.cancel()
                            finishCountdown(currentOwner, skipImmediately = true)
                        }
                        else -> Unit
                    }
                }
            }
        }

        private fun runCountdownTimer(owner: LifecycleOwner) {
            timerJob?.cancel()
            timerJob = owner.lifecycleScope.launch {
                // Đếm ngược 3s: Close in 3 -> Close in 2 -> Close in 1
                for (sec in 2 downTo 1) {
                    delay(1_000L)
                    binding.tvClose.text = "Close in $sec"
                }
                delay(1_000L)

                // Hết 3s:
                countdownAlreadyDone = true
                showCloseButton()
                onCountdownDoneCallback?.invoke()

                // Chờ tiếp 4s (tổng cộng 3s + 4s = 7s) tự động lướt tiếp
                delay(4_000L)
                onAutoNavigateCallback?.invoke()
            }
        }

        private fun finishCountdown(owner: LifecycleOwner, skipImmediately: Boolean) {
            countdownAlreadyDone = true
            showCloseButton()
            onCountdownDoneCallback?.invoke()

            if (skipImmediately) {
                owner.lifecycleScope.launch {
                    delay(300L)
                    onAutoNavigateCallback?.invoke()
                }
            }
        }

        private fun applyTopInset(topInset: Int) {
            val params = binding.tvClose.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = topInset + 12.dpToPx(binding.root.context)
            binding.tvClose.layoutParams = params
        }

        private fun initCloseButtonCountdown() {
            binding.tvClose.apply {
                text = "Close in 3"
                background = null
                isEnabled = false
                isVisible = true
                setOnClickListener(null)
            }
        }

        private fun showCloseButton() {
            binding.tvClose.apply {
                text = ""
                setBackgroundResource(R.drawable.ic_close_button)
                isEnabled = true
                isVisible = true
                setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos >= 0) {
                        onNativeFullscreenCloseClicked(pos)
                    }
                }
            }
        }

        fun onRecycled() {
            loadJob?.cancel()
            loadJob = null
        }

        private fun resetState() {
            timerJob?.cancel()
            timerJob = null
            loadJob?.cancel()
            loadJob = null
            isCountdownRunning = false
            countdownAlreadyDone = false
            boundPageIndex = -1
        }

        private fun Int.dpToPx(context: Context): Int =
            (this * context.resources.displayMetrics.density).toInt()
    }
}
