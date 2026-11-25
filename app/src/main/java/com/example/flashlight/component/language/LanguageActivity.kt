package com.example.flashlight.component.language

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.component.onboard.OnboardingActivity
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.example.flashlight.utils.ads.AdPlacement
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.kt.flashlight.databinding.ActivityLanguageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {

    private val TAG = "LanguageNativeAd"

    private val viewModel: LanguageViewModel by viewModels()
    private lateinit var adapter: LanguageAdapter
    private var isFromSettings = false

    override fun provideViewBinding() = ActivityLanguageBinding.inflate(layoutInflater)

    // ── Generation ID ──────────────────────────────────────────────────────
    // Incremented every time we start a new native_language_select request.
    // Callbacks that arrive with a stale generationId are discarded.
    private var currentGenerationId = 0

    // ── Shimmer reference ──────────────────────────────────────────────────
    // NativeAdsUtils.setupShimmerLayout() may replace the included shimmer view
    // in the parent. We keep a reference to the ACTUAL shimmer in the layout
    // so showShimmer/hideShimmer always control the live view.
    private var actualShimmerView: com.facebook.shimmer.ShimmerFrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        setupHeader()
        initLanguageList()
        observeViewModel()
        setupListeners()

        if (!isFromSettings) {
            setupNativeAd()
        } else {
            binding.flNativeAdContainer.isGone = true
            hideShimmer()
        }

        // Load data after observers are ready (once, not duplicated)
        viewModel.loadLanguage(isFromSettings)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "LanguageNativeAd: onStart")
    }

    // ── Header ─────────────────────────────────────────────────────────────

    private fun setupHeader() {
        if (isFromSettings) {
            binding.btnBack.isVisible = true
            binding.tvTitle.text = "Choose Language"
        } else {
            binding.btnBack.isVisible = false
            binding.tvTitle.text = "Language"
        }
    }

    // ── Language list ──────────────────────────────────────────────────────

    private fun initLanguageList() {
        adapter = LanguageAdapter(emptyList()) { language ->
            val changed = viewModel.selectLanguage(language)
            if (changed && !isFromSettings) {
                onLanguageSelectedTriggerAdReplacement()
            }
        }
        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
        binding.rvLanguage.adapter = adapter
    }

    // ── Observers ──────────────────────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(
                        state.languages,
                        state.selectedLanguage?.languageCode.orEmpty()
                    )
                    binding.btnSave.isVisible = state.isSaveVisible
                }
            }
        }
    }

    // ── Native Ad: Initial Setup (Luồng 1) ──────────────────────────────────

    /**
     * Called once in [onCreate].
     *
     * Flow:
     *  1. Resolve native_language placement.
     *  2. If disabled → hide container.
     *  3. If enabled:
     *     - Show Shimmer.
     *     - Preload (or reuse existing) native_language.
     *     - Bind views to the active helper.
     *     - Preload native_language_select in background so it sits in Pending.
     *  4. Also trigger preloads for onboarding and fullscreen placements.
     */
    private fun setupNativeAd() {
        FirebaseConfigManager.instance().doAfterFetch {
            if (isFinishing || isDestroyed) return@doAfterFetch

            val resolved = AdsIdResolver.resolve(AdPlacement.NATIVE_LANGUAGE)
            if (resolved == null) {
                binding.flNativeAdContainer.isGone = true
                Log.d(TAG, "setupNativeAd: native_language disabled → hide container")
            } else {
                binding.flNativeAdContainer.isVisible = true
                showShimmer()

                Log.d(TAG, "LanguageNativeAd: Request Priority")
                NativeAdsUtils.preloadNativeLanguage(this, forceReload = isFromSettings)

                val helper = NativeAdsUtils.nativeLanguage
                if (helper != null) {
                    observeActiveHelperState(helper)
                    val shimmer = NativeAdsUtils.setupShimmerLayout(binding.shimmerContainerNative.root)
                    actualShimmerView = shimmer
                    helper.updateActivity(this, this)
                    helper.setNativeContentView(binding.flNativeAdContainer)
                    helper.setShimmerLayoutView(shimmer)
                } else {
                    hideShimmer()
                    binding.flNativeAdContainer.isGone = true
                }
            }

            // Preload native_language_select in advance so it sits in Pending state
            // ready for instant commit when the user picks a language (Luồng 3 / STT 5)
            ensureSelectAdPreloading(currentGenerationId)
        }
    }

    /**
     * Observes the state of the active (initial) native_language helper.
     */
    private fun observeActiveHelperState(helper: NativeAdHelper) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                helper.getAdNativeState().collect { state ->
                    when (state) {
                        is AdNativeState.Loading -> {
                            showShimmer()
                        }
                        is AdNativeState.Loaded -> {
                            Log.d(TAG, "LanguageNativeAd: ActiveAd found")
                        }
                        is AdNativeState.Fail -> {
                            if (!viewModel.isSelectAdDisplayed && !viewModel.isReplacementPending) {
                                hideShimmer()
                                binding.flNativeAdContainer.isGone = true
                            }
                        }
                        is AdNativeState.Cancel -> {
                            // Luồng 2: Khi Native Ads hiện tại bị Cancel
                            Log.d(TAG, "LanguageNativeAd: Cancel current ad")
                            showShimmer()
                            if (!viewModel.isSelectAdDisplayed) {
                                viewModel.requestReplacement()
                                if (viewModel.isSelectAdReady && NativeAdsUtils.nativeLanguageSelect?.nativeAd != null) {
                                    commitSelectAd(currentGenerationId)
                                } else {
                                    ensureSelectAdPreloading(currentGenerationId)
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    // ── Preload native_language_select ──────────────────────────────────────

    private fun ensureSelectAdPreloading(generationId: Int) {
        if (isFinishing || isDestroyed) return

        NativeAdsUtils.preloadNativeLanguageSelect(
            appActivity = this,
            forceReload = false,
            onHelperReady = { selectHelper ->
                observeSelectHelperState(selectHelper, generationId)
            }
        )
    }

    /**
     * Observes the state of the replacement (native_language_select) helper.
     */
    private fun observeSelectHelperState(
        selectHelper: NativeAdHelper,
        generationId: Int
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                selectHelper.getAdNativeState().collect { state ->
                    if (!isGenerationValid(generationId)) return@collect
                    when (state) {
                        is AdNativeState.Loading -> {
                            viewModel.onSelectAdLoading()
                            if (viewModel.isReplacementPending) {
                                showShimmer()
                            }
                        }
                        is AdNativeState.Loaded -> {
                            Log.d(TAG, "LanguageNativeAd: Pending ad loaded")
                            viewModel.onSelectAdLoaded()
                            if (viewModel.isReplacementPending && !viewModel.isSelectAdDisplayed) {
                                commitSelectAd(generationId)
                            } else {
                                Log.d(TAG, "LanguageNativeAd: Waiting for language selection")
                            }
                        }
                        is AdNativeState.Fail -> {
                            viewModel.onSelectAdFailed()
                            if (viewModel.isReplacementPending && !viewModel.isSelectAdDisplayed) {
                                hideShimmer()
                                binding.flNativeAdContainer.isGone = true
                            }
                        }
                        is AdNativeState.Cancel -> {
                            if (viewModel.isReplacementPending) {
                                showShimmer()
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    // ── Native Ad: Language Selection Trigger (Luồng 4) ─────────────────────

    /**
     * Triggered when the user taps a different language item.
     *
     * Flow:
     *  1. If select ad is already committed and displayed, do nothing (prevent duplicate swap).
     *  2. Show Shimmer immediately.
     *  3. Cancel old native_language ad.
     *  4. If Pending ad is already loaded (Trường hợp A): commit immediately.
     *  5. If Pending ad is not ready yet (Trường hợp B): mark replacementRequested = true,
     *     wait for Loaded callback which will commit automatically.
     */
    private fun onLanguageSelectedTriggerAdReplacement() {
        if (isFinishing || isDestroyed) return
        Log.d(TAG, "LanguageNativeAd: Language selected")

        if (viewModel.isSelectAdDisplayed) {
            return
        }

        val myGen = ++currentGenerationId
        Log.d(TAG, "LanguageNativeAd: Replacement requested")

        // Bước 2: Hiển thị Shimmer ngay lập tức, ẩn ad cũ
        showShimmer()
        clearOldNativeViews(binding.flNativeAdContainer, binding.shimmerContainerNative.root)

        Log.d(TAG, "LanguageNativeAd: Cancel current ad")
        NativeAdsUtils.nativeLanguage?.cancel()
        showShimmer()

        // Bước 3: Kiểm tra Pending NativeAd
        if (viewModel.isSelectAdReady && NativeAdsUtils.nativeLanguageSelect?.nativeAd != null) {
            // Trường hợp A: Pending NativeAd đã Loaded
            commitSelectAd(myGen)
        } else {
            // Trường hợp B: Pending NativeAd chưa Loaded
            viewModel.requestReplacement()
            ensureSelectAdPreloading(myGen)
        }
    }

    private fun clearOldNativeViews(container: android.view.ViewGroup, shimmerView: android.view.View) {
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            if (child != shimmerView) {
                container.removeView(child)
            }
        }
    }

    /**
     * Commits the pending select ad: binds views, renders, hides shimmer.
     */
    private fun commitSelectAd(generationId: Int) {
        if (!isGenerationValid(generationId)) return
        if (isFinishing || isDestroyed) return

        val helper = NativeAdsUtils.nativeLanguageSelect
        if (helper == null || helper.nativeAd == null) return

        Log.d(TAG, "LanguageNativeAd: Start rendering")
        showShimmer()
        clearOldNativeViews(binding.flNativeAdContainer, binding.shimmerContainerNative.root)

        val shimmerForCommit = actualShimmerView
            ?: NativeAdsUtils.setupShimmerLayout(binding.shimmerContainerNative.root).also { actualShimmerView = it }

        helper.updateActivity(this, this)
        helper.setNativeContentView(binding.flNativeAdContainer)
        helper.setShimmerLayoutView(shimmerForCommit)

        Log.d(TAG, "LanguageNativeAd: NativeAdView attached")
        viewModel.onSelectAdCommitted()
        Log.d(TAG, "LanguageNativeAd: Commit success")
        Log.d(TAG, "LanguageNativeAd: Hide shimmer")
    }

    // ── Shimmer helpers ────────────────────────────────────────────────────

    private fun showShimmer() {
        if (isFinishing || isDestroyed) return
        Log.d(TAG, "LanguageNativeAd: Show shimmer")
        val shimmer = actualShimmerView ?: binding.shimmerContainerNative.root
        shimmer.isVisible = true
    }

    private fun hideShimmer() {
        if (isFinishing || isDestroyed) return
        Log.d(TAG, "LanguageNativeAd: Hide shimmer")
        val shimmer = actualShimmerView ?: binding.shimmerContainerNative.root
        shimmer.isGone = true
    }

    // ── Generation guard ───────────────────────────────────────────────────

    private fun isGenerationValid(generationId: Int): Boolean {
        return generationId == currentGenerationId
    }

    // ── Listeners ──────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnSave.setOnClickListener {
            applySelectedLanguage()
        }
    }

    private fun applySelectedLanguage() {
        val language = viewModel.saveLanguage() ?: return

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageCode)
        )

        if (isFromSettings) {
            setResult(RESULT_OK)
            finish()
        } else {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }
    }

    // ── Lifecycle cleanup (Luồng 11) ────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LanguageNativeAd: Cleanup")
        currentGenerationId = Int.MAX_VALUE
        NativeAdsUtils.destroyNativeLanguageSelect()
    }

    companion object {
        const val EXTRA_FROM_SETTINGS = "extra_from_settings"
    }
}
