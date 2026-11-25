package com.example.flashlight.utils.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ads.admob.R
import com.ads.admob.helper.AdOptionVisibility
import com.ads.admob.helper.adnative.NativeAdConfig
import com.ads.admob.helper.adnative.NativeAdHelper
import com.ads.admob.helper.adnative.params.AdNativeState
import com.ads.admob.helper.adnative.params.NativeAdParam
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.example.flashlight.utils.Logger
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object NativeAdsUtils {

    private val processLifecycleOwner = ProcessLifecycleOwner.get()

    var nativeLanguage: NativeAdHelper? = null
        private set

    var nativeOnboarding: NativeAdHelper? = null
        private set

    var nativeOnboarding3: NativeAdHelper? = null
        private set

    var nativeFullscreen: NativeAdHelper? = null
        private set

    var nativeFullscreen2: NativeAdHelper? = null
        private set

    var nativePermission: NativeAdHelper? = null
        private set

    var homeNative: NativeAdHelper? = null
        private set

    /**
     * Tracks the native_language_select ad helper (used after the user taps a language).
     * Kept separate from [nativeLanguage] so both helpers can coexist during the transition,
     * and so we have a stable reference to call cancel/destroy at the right time.
     */
    var nativeLanguageSelect: NativeAdHelper? = null
        private set

    fun destroyAll() {
        nativeLanguage?.cancel()
        nativeOnboarding?.cancel()
        nativeOnboarding3?.cancel()
        nativeFullscreen?.cancel()
        nativeFullscreen2?.cancel()
        nativePermission?.cancel()
        homeNative?.cancel()
        nativeLanguageSelect?.cancel()

        nativeLanguage = null
        nativeOnboarding = null
        nativeOnboarding3 = null
        nativeFullscreen = null
        nativeFullscreen2 = null
        nativePermission = null
        homeNative = null
        nativeLanguageSelect = null
    }

    fun getCustomNativeLayoutId(): Int {
        return if (FirebaseConfigManager.instance().isPaidUser()) {
            R.layout.item_native_ad_custom_paid
        } else {
            R.layout.item_native_ad_custom
        }
    }

    fun getType2NativeLayoutId(): Int {
        return if (FirebaseConfigManager.instance().isPaidUser()) {
            R.layout.layout_native_ad_view_type2_paid
        } else {
            R.layout.layout_native_ad_view_type2
        }
    }

    fun getCustomNativeShimmerLayoutId(): Int {
        return if (FirebaseConfigManager.instance().isPaidUser()) {
            R.layout.loading_native_ad_view_collapsible_paid
        } else {
            R.layout.loading_native_ad_view_collapsible
        }
    }

    fun getType2NativeShimmerLayoutId(): Int {
        return if (FirebaseConfigManager.instance().isPaidUser()) {
            R.layout.loading_native_ad_view_type2_paid
        } else {
            R.layout.loading_native_ad_view_type2
        }
    }

    fun setupShimmerLayout(
        shimmerContainer: ShimmerFrameLayout,
        isType2: Boolean = false
    ): ShimmerFrameLayout {
        val shimmerLayoutId = if (isType2) {
            getType2NativeShimmerLayoutId()
        } else {
            getCustomNativeShimmerLayoutId()
        }

        val context = shimmerContainer.context
        val parent = shimmerContainer.parent as? android.view.ViewGroup

        if (parent != null) {
            val index = parent.indexOfChild(shimmerContainer)
            val layoutParams = shimmerContainer.layoutParams
            parent.removeViewAt(index)

            val newShimmer = android.view.LayoutInflater.from(context)
                .inflate(shimmerLayoutId, parent, false) as ShimmerFrameLayout
            newShimmer.layoutParams = layoutParams
            parent.addView(newShimmer, index)
            return newShimmer
        } else {
            shimmerContainer.removeAllViews()
            android.view.LayoutInflater.from(context).inflate(shimmerLayoutId, shimmerContainer, true)
            return shimmerContainer
        }
    }

    fun preloadNativeLanguage(appActivity: AppCompatActivity, forceReload: Boolean = false) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("native_language")
            if (resolved == null) return@doAfterFetch

            val currentHelper = nativeLanguage
            if (currentHelper != null && !forceReload) {
                currentHelper.updateActivity(appActivity, processLifecycleOwner)
                if (currentHelper.nativeAd != null) return@doAfterFetch
                val state = currentHelper.getAdNativeState().value
                if (state is AdNativeState.Loading) return@doAfterFetch
            }

            val config = NativeAdConfig(
                idAdsPriority = resolved.adUnitIdPriority,
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                layoutId = getCustomNativeLayoutId(),
                adPlacement = resolved.placement,
                reloadIfFirstFail = true
            )

            nativeLanguage = NativeAdHelper(
                activity = appActivity,
                lifecycleOwner = processLifecycleOwner,
                config = config
            ).apply {
                adVisibility = AdOptionVisibility.GONE
            }

            nativeLanguage?.requestAds(NativeAdParam.Request)

            Logger.e("preloadNativeLanguage()")
        }
    }

    /**
     * Preloads the native_language_select ad.
     *
     * Creates or reuses a [NativeAdHelper] and stores it in [nativeLanguageSelect].
     * Priority / Normal fallback is handled automatically via [NativeAdConfig.idAdsPriority].
     *
     * @param appActivity   the current [AppCompatActivity]
     * @param forceReload   if true, cancels any existing helper and starts fresh
     * @param onHelperReady invoked when the helper is available to observe its state
     */
    fun preloadNativeLanguageSelect(
        appActivity: AppCompatActivity,
        forceReload: Boolean = false,
        onHelperReady: ((NativeAdHelper) -> Unit)? = null
    ) {
        FirebaseConfigManager.instance().doAfterFetch {
            if (appActivity.isFinishing || appActivity.isDestroyed) return@doAfterFetch

            val resolved = AdsIdResolver.resolve("native_language_select")
            if (resolved == null) return@doAfterFetch

            val currentHelper = nativeLanguageSelect
            if (currentHelper != null && !forceReload) {
                currentHelper.updateActivity(appActivity, processLifecycleOwner)
                if (currentHelper.nativeAd != null) {
                    onHelperReady?.invoke(currentHelper)
                    return@doAfterFetch
                }
                val state = currentHelper.getAdNativeState().value
                if (state is AdNativeState.Loading) {
                    onHelperReady?.invoke(currentHelper)
                    return@doAfterFetch
                }
            }

            if (forceReload && currentHelper != null) {
                currentHelper.cancel()
            }

            val config = NativeAdConfig(
                idAdsPriority = resolved.adUnitIdPriority,
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                layoutId = getCustomNativeLayoutId(),
                adPlacement = resolved.placement,
                reloadIfFirstFail = true
            )

            val newHelper = NativeAdHelper(
                activity = appActivity,
                lifecycleOwner = processLifecycleOwner,
                config = config
            ).apply {
                adVisibility = AdOptionVisibility.GONE
            }

            nativeLanguageSelect = newHelper
            newHelper.requestAds(NativeAdParam.Request)
            onHelperReady?.invoke(newHelper)
            Logger.e("LanguageNativeAd: preloadNativeLanguageSelect() started (placement=${resolved.placement})")
        }
    }

    /**
     * Cancels and destroys the [nativeLanguageSelect] helper, releasing all resources.
     * Safe to call even if the helper is null.
     */
    fun destroyNativeLanguageSelect() {
        nativeLanguageSelect?.cancel()
        nativeLanguageSelect = null
        Logger.e("LanguageNativeAd: destroyNativeLanguageSelect")
    }

    fun preloadNativeOnboarding(appActivity: AppCompatActivity, forceReload: Boolean = false) {
        val resolved = AdsIdResolver.resolve("native_onboarding") ?: return

        val currentHelper = nativeOnboarding
        if (currentHelper != null && !forceReload) {
            currentHelper.updateActivity(appActivity, processLifecycleOwner)
            if (currentHelper.nativeAd != null) {
                Logger.e("[NATIVE_PRELOAD][OBD1] cache hit (already loaded)")
                return
            }
            val state = currentHelper.getAdNativeState().value
            if (state is AdNativeState.Loading) {
                Logger.e("[NATIVE_PRELOAD][OBD1] already loading - skip duplicate")
                return
            }
        }

        Logger.e("[NATIVE_PRELOAD][OBD1] preload started")

        val config = NativeAdConfig(
            idAdsPriority = resolved.adUnitIdPriority,
            idAds = resolved.adUnitId,
            canShowAds = true,
            canReloadAds = false,
            layoutId = getCustomNativeLayoutId(),
            adPlacement = resolved.placement,
            reloadIfFirstFail = true
        )

        nativeOnboarding = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeOnboarding?.requestAds(NativeAdParam.Request)

        Logger.e("[NATIVE_PRELOAD][OBD1] cache stored")
    }

    fun preloadNativeOnboarding3(appActivity: AppCompatActivity, forceReload: Boolean = false) {
        val resolved = AdsIdResolver.resolve("native_onboarding_3") ?: return

        val currentHelper = nativeOnboarding3
        if (currentHelper != null && !forceReload) {
            currentHelper.updateActivity(appActivity, processLifecycleOwner)
            if (currentHelper.nativeAd != null) {
                Logger.e("[NATIVE_PRELOAD][OBD3] cache hit (already loaded)")
                return
            }
            val state = currentHelper.getAdNativeState().value
            if (state is AdNativeState.Loading) {
                Logger.e("[NATIVE_PRELOAD][OBD3] already loading - skip duplicate")
                return
            }
        }

        Logger.e("[NATIVE_PRELOAD][OBD3] preload started")

        val config = NativeAdConfig(
            idAdsPriority = resolved.adUnitIdPriority,
            idAds = resolved.adUnitId,
            canShowAds = true,
            canReloadAds = false,
            layoutId = getCustomNativeLayoutId(),
            adPlacement = resolved.placement,
            reloadIfFirstFail = true
        )

        nativeOnboarding3 = NativeAdHelper(
            activity = appActivity,
            lifecycleOwner = processLifecycleOwner,
            config = config
        ).apply {
            adVisibility = AdOptionVisibility.GONE
        }

        nativeOnboarding3?.requestAds(NativeAdParam.Request)

        Logger.e("[NATIVE_PRELOAD][OBD3] cache stored")
    }

    fun preloadNativeFullscreen(appActivity: AppCompatActivity, forceReload: Boolean = false) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("native_fullscreen")
            if (resolved == null) return@doAfterFetch

            val currentHelper = nativeFullscreen
            if (currentHelper != null && !forceReload) {
                currentHelper.updateActivity(appActivity, processLifecycleOwner)
                if (currentHelper.nativeAd != null) return@doAfterFetch
                val state = currentHelper.getAdNativeState().value
                if (state is AdNativeState.Loading) return@doAfterFetch
            }

            val config = NativeAdConfig(
                idAdsPriority = resolved.adUnitIdPriority,
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                layoutId = R.layout.layout_native_fullscreen_view,
                adPlacement = resolved.placement,
                reloadIfFirstFail = true
            )

            nativeFullscreen = NativeAdHelper(
                activity = appActivity,
                lifecycleOwner = processLifecycleOwner,
                config = config
            ).apply {
                adVisibility = AdOptionVisibility.GONE
            }

            nativeFullscreen?.requestAds(NativeAdParam.Request)

            Logger.e("preloadNativeFullscreen()")
        }
    }

    fun preloadNativeFullscreen2(appActivity: AppCompatActivity, forceReload: Boolean = false) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("native_fullscreen_2")
            if (resolved == null) return@doAfterFetch

            val currentHelper = nativeFullscreen2
            if (currentHelper != null && !forceReload) {
                currentHelper.updateActivity(appActivity, processLifecycleOwner)
                if (currentHelper.nativeAd != null) return@doAfterFetch
                val state = currentHelper.getAdNativeState().value
                if (state is AdNativeState.Loading) return@doAfterFetch
            }

            val config = NativeAdConfig(
                idAdsPriority = resolved.adUnitIdPriority,
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                layoutId = R.layout.layout_native_fullscreen_view,
                adPlacement = resolved.placement,
                reloadIfFirstFail = true
            )

            nativeFullscreen2 = NativeAdHelper(
                activity = appActivity,
                lifecycleOwner = processLifecycleOwner,
                config = config
            ).apply {
                adVisibility = AdOptionVisibility.GONE
            }

            nativeFullscreen2?.requestAds(NativeAdParam.Request)

            Logger.e("preloadNativeFullscreen2()")
        }
    }

    fun preloadNativePermission(appActivity: AppCompatActivity, forceReload: Boolean = false) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("native_permission")
            if (resolved == null) return@doAfterFetch

            val currentHelper = nativePermission
            if (currentHelper != null && !forceReload) {
                currentHelper.updateActivity(appActivity, processLifecycleOwner)
                if (currentHelper.nativeAd != null) return@doAfterFetch
                val state = currentHelper.getAdNativeState().value
                if (state is AdNativeState.Loading) return@doAfterFetch
            }

            val config = NativeAdConfig(
                idAdsPriority = resolved.adUnitIdPriority,
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                layoutId = getCustomNativeLayoutId(),
                adPlacement = resolved.placement,
                reloadIfFirstFail = true
            )

            nativePermission = NativeAdHelper(
                activity = appActivity,
                lifecycleOwner = processLifecycleOwner,
                config = config
            ).apply {
                adVisibility = AdOptionVisibility.GONE
            }

            nativePermission?.requestAds(NativeAdParam.Request)

            Logger.e("preloadNativePermission()")
        }
    }

    fun preloadHomeNative(context: android.content.Context) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("native_main")
            if (resolved == null) return@doAfterFetch

            val currentHelper = homeNative
            if (currentHelper != null) {
                if (currentHelper.nativeAd != null) return@doAfterFetch
                val state = currentHelper.getAdNativeState().value
                if (state is AdNativeState.Loading) return@doAfterFetch
            }

            val act = context as? Activity ?: return@doAfterFetch

            val config = NativeAdConfig(
                idAdsPriority = resolved.adUnitIdPriority,
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                layoutId = getType2NativeLayoutId(),
                adPlacement = resolved.placement,
                reloadIfFirstFail = true
            )

            homeNative = NativeAdHelper(
                activity = act,
                lifecycleOwner = processLifecycleOwner,
                config = config
            ).apply {
                adVisibility = AdOptionVisibility.GONE
            }

            homeNative?.requestAds(NativeAdParam.Request)
            Logger.e("preloadHomeNative()")
        }
    }

    fun bindHomeNative(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout
    ) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("native_main") ?: return@doAfterFetch
            val actualShimmer = setupShimmerLayout(shimmerContainerNative, isType2 = true)
            if (homeNative == null) {
                preloadHomeNative(activity)
            } else {
                val state = homeNative?.getAdNativeState()?.value
                if (homeNative?.nativeAd == null && state !is AdNativeState.Loading) {
                    homeNative?.updateActivity(activity, lifecycleOwner)
                    homeNative?.requestAds(NativeAdParam.Request)
                    Logger.e("bindHomeNative: Retrying request for unloaded native ad")
                }
            }
            nativeContentView.isVisible = true
            homeNative?.updateActivity(activity, lifecycleOwner)
            homeNative?.setNativeContentView(nativeContentView)
            homeNative?.setShimmerLayoutView(actualShimmer)
        }
    }

    fun bindPermissionNative(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout
    ) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("native_permission")
            val container = nativeContentView.parent as? ViewGroup
            if (resolved == null) {
                container?.isVisible = false
                return@doAfterFetch
            }
            container?.isVisible = true
            val actualShimmer = setupShimmerLayout(shimmerContainerNative, isType2 = false)
            nativeContentView.isVisible = true

            val currentHelper = nativePermission
            if (currentHelper == null) {
                val config = NativeAdConfig(
                    idAdsPriority = resolved.adUnitIdPriority,
                    idAds = resolved.adUnitId,
                    canShowAds = true,
                    canReloadAds = false,
                    layoutId = getCustomNativeLayoutId(),
                    adPlacement = resolved.placement,
                    reloadIfFirstFail = true
                )
                val helper = NativeAdHelper(
                    activity = activity,
                    lifecycleOwner = processLifecycleOwner,
                    config = config
                ).apply {
                    adVisibility = AdOptionVisibility.GONE
                }
                nativePermission = helper
                helper.setNativeContentView(nativeContentView)
                helper.setShimmerLayoutView(actualShimmer)
                helper.requestAds(NativeAdParam.Request)
                Logger.e("bindPermissionNative: Created new helper and requested ad")
            } else {
                currentHelper.updateActivity(activity, lifecycleOwner)
                currentHelper.setNativeContentView(nativeContentView)
                currentHelper.setShimmerLayoutView(actualShimmer)

                val state = currentHelper.getAdNativeState().value
                if (currentHelper.nativeAd == null && state !is AdNativeState.Loading) {
                    currentHelper.requestAds(NativeAdParam.Request)
                    Logger.e("bindPermissionNative: Retrying request for unloaded native ad")
                }
            }
        }
    }

    private fun setNative(
        activity: FragmentActivity,
        config: NativeAdConfig,
        lifecycleOwner: LifecycleOwner,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout,
        isType2: Boolean = false
    ) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            nativeContentView.isVisible = true
            val actualShimmer = setupShimmerLayout(shimmerContainerNative, isType2 = isType2)
            val nativeAdHelper = NativeAdHelper(
                activity, lifecycleOwner, config
            ).apply {
                adVisibility = AdOptionVisibility.GONE
            }

            nativeAdHelper.setNativeContentView(nativeContentView)
            nativeAdHelper.setShimmerLayoutView(actualShimmer)
            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                nativeAdHelper.requestAds(NativeAdParam.Request)
            }
        }
    }

    fun loadAndShowNative(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        placement: String,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout,
        @LayoutRes layoutId: Int = 0
    ) {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve(placement) ?: return@doAfterFetch
            val isType2 = (layoutId == com.ads.admob.R.layout.layout_native_ad_view_type2 || layoutId == com.ads.admob.R.layout.layout_native_ad_view_type2_paid)
            val finalLayoutId = if (layoutId == 0 || layoutId == com.ads.admob.R.layout.item_native_ad_custom) {
                getCustomNativeLayoutId()
            } else if (layoutId == com.ads.admob.R.layout.layout_native_ad_view_type2) {
                getType2NativeLayoutId()
            } else {
                layoutId
            }
            val config = NativeAdConfig(
                idAdsPriority = resolved.adUnitIdPriority,
                idAds = resolved.adUnitId,
                canShowAds = true,
                canReloadAds = false,
                layoutId = finalLayoutId,
                adPlacement = resolved.placement
            )
            setNative(activity, config, lifecycleOwner, nativeContentView, shimmerContainerNative, isType2 = isType2)
        }
    }

    fun loadAndShowNativeMain(
        activity: FragmentActivity,
        lifecycleOwner: LifecycleOwner,
        nativeContentView: FrameLayout,
        shimmerContainerNative: ShimmerFrameLayout
    ) = loadAndShowNative(
        activity = activity,
        lifecycleOwner = lifecycleOwner,
        placement = "native_main",
        nativeContentView = nativeContentView,
        shimmerContainerNative = shimmerContainerNative,
        layoutId = getType2NativeLayoutId()
    )

}
