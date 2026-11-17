package com.example.flashlight.utils.ads.manager

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.helper.interstitial.InterstitialAdsHelper
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.ads.AdPlacement
import com.example.flashlight.utils.ads.InterAdsUtils
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Quản lý tập trung bộ đếm Interstitial cho 4 tính năng:
 * Flashlight, ScreenLight, LED, FlashAlert.
 *
 * Logic:
 * - Dùng 1 bộ đếm chung (không tách riêng theo feature).
 * - Hiển thị inter khi usageCount % frequency == 0 (vd: lần 2, 4, 6, 8...).
 * - Kiểm tra khoảng cách thời gian tối thiểu giữa 2 lần show (interval_between_interstitial, ms).
 * - Cấu hình được qua Firebase Remote Config:
 *     "ads_inter_feature_frequency"      → số lần dùng giữa 2 lần show (number, default = 2)
 *     "interval_between_interstitial"    → ms tối thiểu giữa 2 lần show (number, default = 0)
 */
object FeatureInterAdsManager {

    // Bộ đếm chung cho TẤT CẢ 4 tính năng
    private val usageCount = AtomicInteger(0)

    // Thời điểm close ads lần cuối (epoch ms), 0 = chưa từng show
    private val lastAdClosedAt = AtomicLong(0L)

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Preload interstitial ngay khi MainActivity khởi động.
     * Gọi 1 lần duy nhất từ MainActivity.
     */
    fun preload(activity: Activity) {
        AdsDisplayManager.prepareInterstitial(
            activity = activity,
            placement = AdPlacement.INTER_FEATURE
        )
        Logger.e("FeatureInterAds: preload requested")
    }

    /**
     * Gọi từ mỗi Fragment khi user KÍCH HOẠT tính năng (bật đèn, bật LED…).
     * - Tăng counter.
     * - Kiểm tra có đủ điều kiện show inter hay không.
     * - Nếu có: show inter rồi gọi [onAdFinished]; nếu không: gọi [onAdFinished] ngay.
     *
     * @param activity      Activity hiện tại (MainActivity)
     * @param lifecycleOwner LifecycleOwner của Fragment
     * @param onAdFinished  Callback sau khi ads đóng hoặc bị bỏ qua
     */
    fun onFeatureUsed(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        onAdFinished: () -> Unit = {}
    ) {
        val count = usageCount.incrementAndGet()
        val frequency = getFrequency()
        val intervalMs = getIntervalMs()

        Logger.e("FeatureInterAds: onFeatureUsed count=$count, frequency=$frequency, interval=${intervalMs}ms")

        if (!shouldShowAd(count, frequency, intervalMs)) {
            Logger.e("FeatureInterAds: skip – conditions not met")
            onAdFinished()
            return
        }

        showAd(activity, lifecycleOwner, onAdFinished)
    }

    /**
     * Reset counter về 0 (dùng khi cần, vd: sau purchase).
     */
    fun resetCounter() {
        usageCount.set(0)
        Logger.e("FeatureInterAds: counter reset")
    }

    // -------------------------------------------------------------------------
    // Internal logic
    // -------------------------------------------------------------------------

    private fun shouldShowAd(count: Int, frequency: Long, intervalMs: Long): Boolean {
        // Điều kiện 1: đúng bội số của frequency
        if (frequency <= 0L || count % frequency != 0L) return false

        // Điều kiện 2: đủ khoảng cách thời gian kể từ lần close ads trước
        if (intervalMs > 0L) {
            val last = lastAdClosedAt.get()
            if (last > 0L) {
                val elapsed = System.currentTimeMillis() - last
                if (elapsed < intervalMs) {
                    Logger.e("FeatureInterAds: skip – interval not reached (${elapsed}ms < ${intervalMs}ms)")
                    return false
                }
            }
        }

        return true
    }

    private fun showAd(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        onAdFinished: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onAdFinished()
            return
        }

        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve(AdPlacement.INTER_FEATURE)
            if (resolved == null) {
                Logger.e("FeatureInterAds: placement disabled or not resolved → skip")
                onAdFinished()
                return@doAfterFetch
            }

            val helper = InterstitialAdsHelper
                .getInstance(resolved.placement)

            if (helper.interstitialAdValue == null) {
                // Không có ad sẵn → load và show luôn
                Logger.e("FeatureInterAds: no cached ad → loadAndShow")
                InterAdsUtils.loadAndShowInterAds(
                    activity = activity,
                    lifecycleOwner = lifecycleOwner,
                    idAds = resolved.adUnitId,
                    adPlacement = resolved.placement,
                    isEnable = true,
                    action = {
                        recordAdClosed()
                        reloadAfterShow(activity, resolved.adUnitId, resolved.placement)
                        onAdFinished()
                    }
                )
            } else {
                // Có ad sẵn → show ngay
                Logger.e("FeatureInterAds: cached ad available → show")
                InterAdsUtils.showInterAds(
                    activity = activity,
                    lifecycleOwner = lifecycleOwner,
                    adPlacement = resolved.placement,
                    isReloadWhenClose = false,
                    action = {
                        recordAdClosed()
                        reloadAfterShow(activity, resolved.adUnitId, resolved.placement)
                        onAdFinished()
                    }
                )
            }
        }
    }

    /** Ghi lại thời điểm đóng ads để tính interval */
    private fun recordAdClosed() {
        lastAdClosedAt.set(System.currentTimeMillis())
        Logger.e("FeatureInterAds: ad closed, timestamp recorded")
    }

    /** Preload lại sau khi show để lần sau sẵn sàng ngay */
    private fun reloadAfterShow(activity: Activity, adUnitId: String, placement: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        InterAdsUtils.loadInterAds(
            activity = activity,
            idAds = adUnitId,
            adPlacement = placement,
            isEnable = true,
            isLoadAndShow = false
        )
        Logger.e("FeatureInterAds: reload triggered after show")
    }

    // -------------------------------------------------------------------------
    // Firebase Remote Config helpers
    // -------------------------------------------------------------------------

    private fun getFrequency(): Long {
        return FirebaseConfigManager.instance().getLong(
            key = AdPlacement.REMOTE_ADS_INTER_FEATURE_FREQUENCY,
            defaultValue = AdPlacement.DEFAULT_INTER_FEATURE_FREQUENCY
        ).coerceAtLeast(1L)
    }

    private fun getIntervalMs(): Long {
        return FirebaseConfigManager.instance().getLong(
            key = AdPlacement.REMOTE_INTERVAL_BETWEEN_INTERSTITIAL,
            defaultValue = AdPlacement.DEFAULT_INTERVAL_BETWEEN_INTER_MS
        ).coerceAtLeast(0L)
    }
}
