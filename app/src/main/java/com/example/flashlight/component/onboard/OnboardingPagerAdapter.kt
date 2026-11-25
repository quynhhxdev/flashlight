package com.example.flashlight.component.onboard

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.example.flashlight.utils.Logger
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
        private const val COUNTDOWN_SECONDS = 3
        private const val AUTO_NAVIGATE_DELAY_MS = 4_000L
    }

    // Giữ reference holders đang active theo position
    private val activeNativeHolders = mutableMapOf<Int, NativeFullscreenViewHolder>()

    // Generation counter đảm bảo mỗi visitId là duy nhất kể cả khi vuốt nhanh liên tiếp
    private var visitSequence = 0L

    // Lưu page đang cần start visit session nếu onPageSelected gọi trước khi onBindViewHolder hoàn tất
    private var pendingVisitPageIndex: Int? = null
    private var pendingVisitId: Long? = null

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
                activeNativeHolders.entries.removeAll { it.value == holder && it.key != position }
                activeNativeHolders[position] = holder
                onNativeFullscreenPageBound(position, holder)

                // Nếu có yêu cầu visit session đang chờ cho vị trí này, kích hoạt ngay
                if (pendingVisitPageIndex == position && pendingVisitId != null) {
                    val visitId = pendingVisitId!!
                    pendingVisitPageIndex = null
                    pendingVisitId = null
                    holder.startNewVisitSession(visitId)
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
     * Bắt đầu một lượt truy cập (visit session) mới cho trang Native Fullscreen khi được chọn.
     */
    fun startNewVisitForPage(position: Int): Long {
        val newVisitId = ++visitSequence
        val holder = activeNativeHolders[position]
        if (holder != null) {
            if (pendingVisitPageIndex == position) {
                pendingVisitPageIndex = null
                pendingVisitId = null
            }
            holder.startNewVisitSession(newVisitId)
        } else {
            pendingVisitPageIndex = position
            pendingVisitId = newVisitId
        }
        return newVisitId
    }

    /**
     * Dừng lượt truy cập hiện tại của trang Native Fullscreen khi rời khỏi trang đó.
     */
    fun stopVisitForPage(position: Int) {
        if (pendingVisitPageIndex == position) {
            pendingVisitPageIndex = null
            pendingVisitId = null
        }
        val holder = activeNativeHolders[position]
        holder?.stopVisitSession()
    }

    fun hasActiveVisitForPage(position: Int): Boolean {
        return pendingVisitPageIndex == position || activeNativeHolders[position]?.hasActiveVisit() == true
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
        private var currentVisitId: Long = 0L
        private var isCloseButtonReady: Boolean = false
        private var lastTopInset: Int = 0
        private var timerJob: Job? = null
        private var loadJob: Job? = null

        private var activity: Activity? = null
        private var lifecycleOwner: LifecycleOwner? = null
        private var nativeAdHelper: NativeAdHelper? = null
        private var adEnabled: Boolean = false
        private var onCountdownDoneCallback: (() -> Unit)? = null
        private var onAutoNavigateCallback: (() -> Unit)? = null

        init {
            // Đảm bảo khi NativeAdHelper inflate layout_native_fullscreen_view vào nativeAdContent,
            // nút X luôn bị ẩn đúng theo trạng thái hiện tại của visit session (tránh XML mặc định visible).
            binding.nativeAdContent.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    applyTopInset(lastTopInset)
                    if (!isCloseButtonReady) {
                        initCloseButtonCountdown()
                    }
                }

                override fun onChildViewRemoved(parent: View?, child: View?) = Unit
            })
        }

        fun bind(
            pageIndex: Int,
            activity: Activity,
            lifecycleOwner: LifecycleOwner,
            nativeAdHelper: NativeAdHelper?,
            adEnabled: Boolean,
            topInset: Int,
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
            this.lastTopInset = topInset
            this.onCountdownDoneCallback = onCountdownDone
            this.onAutoNavigateCallback = onAutoNavigate

            applyTopInset(topInset)
            if (!isCloseButtonReady) {
                initCloseButtonCountdown()
            }

            if (nativeAdHelper != null && adEnabled) {
                nativeAdHelper.updateActivity(activity, lifecycleOwner)
                nativeAdHelper.setShimmerLayoutView(binding.shimmerContainerNative.root)
                nativeAdHelper.setNativeContentView(binding.nativeAdContent)
            }
        }

        /**
         * Bắt đầu một visit session mới mỗi lần vào trang Native Fullscreen.
         */
        fun startNewVisitSession(visitId: Long) {
            currentVisitId = visitId
            isCloseButtonReady = false
            timerJob?.cancel()
            timerJob = null
            loadJob?.cancel()
            loadJob = null

            val currentOwner = lifecycleOwner ?: return
            val helper = nativeAdHelper

            if (!adEnabled || helper == null) {
                Logger.e("NativeFullscreenPage ($boundPageIndex, visit $visitId): Ad disabled or null -> finish session")
                finishVisitSession(currentOwner, visitId, skipImmediately = true)
                return
            }

            initCloseButtonCountdown()

            val currentState = helper.getAdNativeState().value
            if (helper.nativeAd != null || currentState is AdNativeState.Loaded) {
                Logger.e("NativeFullscreenPage ($boundPageIndex, visit $visitId): Ad already loaded -> start countdown")
                runCountdownTimer(currentOwner, visitId)
                return
            }

            // Ad chưa load xong: chờ ad load hoặc timeout 10s
            loadJob = currentOwner.lifecycleScope.launch {
                val timeoutJob = launch {
                    delay(10_000L)
                    if (currentVisitId == visitId) {
                        Logger.e("NativeFullscreenPage ($boundPageIndex, visit $visitId): Load timeout -> finish session")
                        finishVisitSession(currentOwner, visitId, skipImmediately = true)
                        loadJob?.cancel()
                    }
                }

                helper.getAdNativeState().collect { state ->
                    if (currentVisitId != visitId) return@collect
                    when (state) {
                        is AdNativeState.Loaded -> {
                            Logger.e("NativeFullscreenPage ($boundPageIndex, visit $visitId): Ad loaded -> start timer")
                            timeoutJob.cancel()
                            runCountdownTimer(currentOwner, visitId)
                            loadJob?.cancel()
                        }
                        is AdNativeState.Fail,
                        is AdNativeState.Cancel -> {
                            Logger.e("NativeFullscreenPage ($boundPageIndex, visit $visitId): Ad failed -> finish session")
                            timeoutJob.cancel()
                            finishVisitSession(currentOwner, visitId, skipImmediately = true)
                            loadJob?.cancel()
                        }
                        else -> Unit
                    }
                }
            }
        }

        private fun runCountdownTimer(owner: LifecycleOwner, visitId: Long) {
            timerJob?.cancel()
            timerJob = owner.lifecycleScope.launch {
                if (currentVisitId != visitId) return@launch
                applyTopInset(lastTopInset)
                initCloseButtonCountdown()

                // Đếm ngược 3s: Close in 3 -> Close in 2 -> Close in 1
                for (sec in COUNTDOWN_SECONDS downTo 1) {
                    if (currentVisitId != visitId) return@launch
                    val tvCountdown = binding.nativeAdContent.findViewById<TextView>(com.ads.admob.R.id.tvCountdown)
                    val btnClose = binding.nativeAdContent.findViewById<View>(com.ads.admob.R.id.btnClose)
                    tvCountdown?.text = "Close in $sec"
                    tvCountdown?.isVisible = true
                    btnClose?.isVisible = false
                    delay(1_000L)
                }
                if (currentVisitId != visitId) return@launch

                // Hết 3s: Hiện nút X
                showCloseButton(visitId)
                onCountdownDoneCallback?.invoke()

                // Chờ tiếp 4s sau khi nút X xuất hiện mới tự động chuyển màn (tổng 7s)
                delay(AUTO_NAVIGATE_DELAY_MS)
                if (currentVisitId != visitId) return@launch
                onAutoNavigateCallback?.invoke()
            }
        }

        private fun finishVisitSession(owner: LifecycleOwner, visitId: Long, skipImmediately: Boolean) {
            if (currentVisitId != visitId) return
            showCloseButton(visitId)
            onCountdownDoneCallback?.invoke()

            if (skipImmediately) {
                timerJob?.cancel()
                timerJob = owner.lifecycleScope.launch {
                    delay(300L)
                    if (currentVisitId == visitId) {
                        onAutoNavigateCallback?.invoke()
                    }
                }
            }
        }

        private fun applyTopInset(topInset: Int) {
            val tvCountdown = binding.nativeAdContent.findViewById<TextView>(com.ads.admob.R.id.tvCountdown)
            val btnClose = binding.nativeAdContent.findViewById<View>(com.ads.admob.R.id.btnClose)

            val params1 = tvCountdown?.layoutParams as? ViewGroup.MarginLayoutParams
            if (params1 != null) {
                params1.topMargin = topInset + 12.dpToPx(binding.root.context)
                tvCountdown.layoutParams = params1
            }
            val params2 = btnClose?.layoutParams as? ViewGroup.MarginLayoutParams
            if (params2 != null) {
                params2.topMargin = topInset + 12.dpToPx(binding.root.context)
                btnClose.layoutParams = params2
            }
        }

        private fun initCloseButtonCountdown() {
            isCloseButtonReady = false
            val tvCountdown = binding.nativeAdContent.findViewById<TextView>(com.ads.admob.R.id.tvCountdown)
            val btnClose = binding.nativeAdContent.findViewById<View>(com.ads.admob.R.id.btnClose)
            tvCountdown?.text = "Close in $COUNTDOWN_SECONDS"
            tvCountdown?.isVisible = true
            btnClose?.isVisible = false
            btnClose?.setOnClickListener(null)
        }

        private fun showCloseButton(visitId: Long) {
            if (currentVisitId != visitId) return
            isCloseButtonReady = true
            val tvCountdown = binding.nativeAdContent.findViewById<TextView>(com.ads.admob.R.id.tvCountdown)
            val btnClose = binding.nativeAdContent.findViewById<View>(com.ads.admob.R.id.btnClose)
            tvCountdown?.isVisible = false
            btnClose?.isVisible = true
            btnClose?.setOnClickListener {
                if (currentVisitId == visitId) {
                    timerJob?.cancel()
                    timerJob = null
                    val pos = bindingAdapterPosition
                    if (pos >= 0) {
                        onNativeFullscreenCloseClicked(pos)
                    }
                }
            }
        }

        fun hasActiveVisit(): Boolean = currentVisitId != 0L

        fun stopVisitSession() {
            currentVisitId = 0L
            isCloseButtonReady = false
            timerJob?.cancel()
            timerJob = null
            loadJob?.cancel()
            loadJob = null
            // Ẩn lại nút X ngay khi rời trang để lần kéo/quay lại không thấy nút X cũ
            initCloseButtonCountdown()
        }

        fun onRecycled() {
            stopVisitSession()
        }

        private fun resetState() {
            stopVisitSession()
            boundPageIndex = -1
        }

        private fun Int.dpToPx(context: Context): Int =
            (this * context.resources.displayMetrics.density).toInt()
    }
}
