package com.example.flashlight.component.onboard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.utils.Utils
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.utils.dp
import com.kt.flashlight.R
import com.kt.flashlight.databinding.ActivityOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {

    private val viewModel: OnboardingViewModel by viewModels()

    private var topSystemBarInset = 0
    private var currentPagePosition = 0
    private var defaultLightStatusBarAppearance = false
    private var defaultLayoutInDisplayCutoutMode: Int? = null

    private var isNativeOnboardingEnabled = false
    private var isNativeFullscreenEnabled = false
    private var isNativeFullscreen2Enabled = false

    private lateinit var pagerAdapter: OnboardingPagerAdapter

    override fun provideViewBinding() = ActivityOnboardingBinding.inflate(layoutInflater)

    override fun initViews() {
        defaultLightStatusBarAppearance =
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            defaultLayoutInDisplayCutoutMode = window.attributes.layoutInDisplayCutoutMode
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.onboardingContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            topSystemBarInset = insets.top
            val bottomSpacing = view.resources.getDimensionPixelSize(R.dimen._8dp)
            view.updatePadding(bottom = insets.bottom + bottomSpacing)
            updatePageInsets(currentPagePosition)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewPager()
        setupListeners()
        preloadAds()
        observeViewModel()
    }

    private fun setupViewPager() {
        pagerAdapter = OnboardingPagerAdapter(
            pages = viewModel.getPages(),
            onNativeFullscreenPageBound = { pageIndex, holder ->
                bindNativeFullscreenHolder(pageIndex, holder)
            },
            onNativeFullscreenCloseClicked = { pageIndex ->
                binding.viewPagerOnboarding.isUserInputEnabled = true
                navigateToNextPage(pageIndex)
            }
        )
        binding.viewPagerOnboarding.adapter = pagerAdapter

        binding.viewPagerOnboarding.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPagePosition = position
                viewModel.setCurrentPage(position)

                val isNativePage = viewModel.isNativeFullscreenPage(position)

                // 1. Ẩn/hiện nút Next và Indicator trên Native Fullscreen page
                binding.onboardingControls.isVisible = !isNativePage

                // 2. Native Ad Banner ở dưới: chỉ hiển thị ở Màn Onboarding 1 (index 0) và Màn Onboarding 3 (index 3).
                // Màn Onboarding 2 (index 2) và 4 (index 5) KHÔNG CÓ native ads banner.
                val hasBottomNativeAd = (position == 0 || position == 3)
                binding.nativeAdContainer.isVisible = hasBottomNativeAd && isNativeOnboardingEnabled

                // 3. Khóa/mở vuốt:
                if (isNativePage) {
                    val isCountdownDone = viewModel.isCountdownDone(position)
                    if (isCountdownDone) {
                        // Đã đếm xong trước đó (người dùng vuốt lại): cho phép vuốt bình thường
                        binding.viewPagerOnboarding.isUserInputEnabled = true
                    } else {
                        // Chưa đếm xong: khóa vuốt không cho lướt qua màn tiếp theo trong 3s
                        binding.viewPagerOnboarding.isUserInputEnabled = false
                    }
                    // Kích hoạt bộ đếm riêng cho trang Native Fullscreen này
                    pagerAdapter.startCountdownForPage(position)
                } else {
                    // Màn onboarding thông thường: luôn cho phép vuốt bình thường
                    binding.viewPagerOnboarding.isUserInputEnabled = true
                }

                updateIndicators(position)
                updatePageInsets(position)

                // Preload các quảng cáo tiếp theo nếu chưa load được
                when (position) {
                    2 -> {
                        if (NativeAdsUtils.nativeFullscreen2?.nativeAd == null) {
                            NativeAdsUtils.preloadNativeFullscreen2(this@OnboardingActivity)
                        }
                    }
                    5 -> NativeAdsUtils.preloadNativePermission(this@OnboardingActivity)
                }
            }
        })
    }

    private fun bindNativeFullscreenHolder(
        pageIndex: Int,
        holder: OnboardingPagerAdapter.NativeFullscreenViewHolder
    ) {
        val placement = viewModel.getNativeFullscreenPlacement(pageIndex) ?: return

        val (nativeAdHelper, isEnabled) = when (placement) {
            "native_fullscreen" -> NativeAdsUtils.nativeFullscreen to isNativeFullscreenEnabled
            "native_fullscreen_2" -> NativeAdsUtils.nativeFullscreen2 to isNativeFullscreen2Enabled
            else -> return
        }

        val countdownAlreadyDone = viewModel.isCountdownDone(pageIndex)

        holder.bind(
            pageIndex = pageIndex,
            activity = this,
            lifecycleOwner = this,
            nativeAdHelper = nativeAdHelper,
            adEnabled = isEnabled,
            topInset = topSystemBarInset,
            countdownAlreadyDone = countdownAlreadyDone,
            onCountdownDone = {
                // Đếm xong 3s: lưu state và MỞ KHÓA vuốt (người dùng có thể vuốt hoặc ấn X)
                viewModel.markCountdownDone(pageIndex)
                if (currentPagePosition == pageIndex) {
                    binding.viewPagerOnboarding.isUserInputEnabled = true
                }
            },
            onAutoNavigate = {
                // Sau 7s: tự động lướt qua màn tiếp theo nếu người dùng vẫn chưa chuyển trang
                if (currentPagePosition == pageIndex) {
                    binding.viewPagerOnboarding.isUserInputEnabled = true
                    navigateToNextPage(pageIndex)
                }
            }
        )
    }

    private fun navigateToNextPage(fromPageIndex: Int) {
        val nextPage = fromPageIndex + 1
        if (nextPage < viewModel.getPages().size) {
            binding.viewPagerOnboarding.currentItem = nextPage
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateIndicators(state.currentPage)
                }
            }
        }
    }

    private fun updatePageInsets(position: Int) {
        val edgeToEdgeImage = isEdgeToEdgeImagePage(position)
        val layoutParams = binding.viewPagerOnboarding.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = if (edgeToEdgeImage) 0 else topSystemBarInset
        binding.viewPagerOnboarding.layoutParams = layoutParams

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
            defaultLightStatusBarAppearance && !edgeToEdgeImage

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowLayoutParams = window.attributes
            val cutoutMode = if (edgeToEdgeImage) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                defaultLayoutInDisplayCutoutMode
                    ?: WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            if (windowLayoutParams.layoutInDisplayCutoutMode != cutoutMode) {
                windowLayoutParams.layoutInDisplayCutoutMode = cutoutMode
                window.attributes = windowLayoutParams
            }
        }
    }

    /**
     * Màn hình tràn viền status bar:
     * - Onboarding 1 (index 0) và Onboarding 3 (index 3)
     * - Native Fullscreen 1 (index 1) và Native Fullscreen 2 (index 4)
     * Màn Onboarding 2 (index 2) và Onboarding 4 (index 5) không tràn viền.
     */
    private fun isEdgeToEdgeImagePage(position: Int): Boolean {
        return position == 0 || position == 3 || viewModel.isNativeFullscreenPage(position)
    }

    private fun preloadAds() {
        FirebaseConfigManager.instance().doAfterFetch {
            if (isFinishing || isDestroyed) return@doAfterFetch

            // Native Onboarding banner (chỉ hiện ở index 0 và 3)
            isNativeOnboardingEnabled = AdsIdResolver.resolve("native_onboarding") != null
            val hasBottomNativeAd = (currentPagePosition == 0 || currentPagePosition == 3)
            binding.nativeAdContainer.isVisible = hasBottomNativeAd && isNativeOnboardingEnabled
            if (isNativeOnboardingEnabled) {
                NativeAdsUtils.preloadNativeOnboarding(this)
                NativeAdsUtils.nativeOnboarding?.let { nativeAdHelper ->
                    nativeAdHelper.setNativeContentView(binding.nativeAdContent)
                    nativeAdHelper.setShimmerLayoutView(binding.shimmerContainerNative.root)
                }
            }

            // Native Fullscreen 1 (page index 1)
            isNativeFullscreenEnabled = AdsIdResolver.resolve("native_fullscreen") != null
            if (isNativeFullscreenEnabled) NativeAdsUtils.preloadNativeFullscreen(this)

            // Native Fullscreen 2 (page index 4)
            isNativeFullscreen2Enabled = AdsIdResolver.resolve("native_fullscreen_2") != null
            if (isNativeFullscreen2Enabled) NativeAdsUtils.preloadNativeFullscreen2(this)

            // Nếu đang đứng tại Native Fullscreen, kích hoạt countdown nếu chưa chạy
            if (viewModel.isNativeFullscreenPage(currentPagePosition)) {
                pagerAdapter.startCountdownForPage(currentPagePosition)
            }
        }
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            val current = binding.viewPagerOnboarding.currentItem
            when (viewModel.onNextClicked(current)) {
                OnboardingNextAction.GO_TO_NEXT_PAGE -> {
                    binding.viewPagerOnboarding.currentItem = current + 1
                }
                OnboardingNextAction.FINISH -> {
                    onOnboardingFinished()
                }
            }
        }
    }

    private fun updateIndicators(position: Int) {
        val normalPages = viewModel.getPages()
            .withIndex()
            .filter { it.value is OnboardingPage.Normal }
            .map { it.index }

        val indicators = listOf(
            binding.indicator1,
            binding.indicator2,
            binding.indicator3,
            binding.indicator4
        )

        val currentNormalRank = normalPages.indexOfFirst { it == position }

        for (i in indicators.indices) {
            val indicator = indicators[i]
            val params = indicator.layoutParams
            if (i == currentNormalRank && currentNormalRank >= 0) {
                params.width = 20.dp
                indicator.layoutParams = params
                indicator.setBackgroundResource(R.drawable.bg_indicator_selected)
            } else {
                params.width = 8.dp
                indicator.layoutParams = params
                indicator.setBackgroundResource(R.drawable.bg_indicator_unselected)
            }
        }
    }

    private fun onOnboardingFinished() {
        NativeAdsUtils.preloadHomeNative(this)
        val isConnected = Utils.isConnected(this)
        val targetScreen = viewModel.getFinishTargetScreen(isConnected)

        val intent = Intent(this, IntroActivity::class.java).apply {
            putExtra(IntroActivity.EXTRA_START_SCREEN, targetScreen)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
