package com.example.flashlight.utils.ads

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.reward.RewardAdConfig
import com.ads.admob.helper.reward.RewardAdHelper
import com.ads.admob.listener.RewardAdRequestCallBack
import com.ads.admob.listener.RewardAdShowCallBack
import com.example.flashlight.utils.Logger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

object RewardAdsUtils {

    fun loadRewardAds(
        activity: Activity,
        idAds: String,
        adPlacement: String,
        isEnable: Boolean,
        isLoadAndShow: Boolean = false,
        onAdLoaded: (() -> Unit)? = null
    ) {
        if (ConsentManager.getInstance(activity).getConsentResult(activity)) {
            RewardAdHelper.getInstance(adPlacement).setRewardAdConfig(
                RewardAdConfig(
                    idAds = idAds,
                    canShowAds = isEnable,
                    canReloadAds = false,
                    adPlacement = adPlacement,
                    reloadIfFirstFail = true
                )
            )
            loadAds(activity, adPlacement, isLoadAndShow, onAdLoaded)
        }
    }


    fun showRewardAds(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        adPlacement: String,
        isReloadWhenClose: Boolean = false,
        action: () -> Unit
    ) {
        RewardAdHelper.getInstance(adPlacement).forceShowRewardAd(activity, lifecycleOwner, object : RewardAdShowCallBack {

            override fun onAdClose() {
                checkReload(activity, adPlacement, isReloadWhenClose)
                action.invoke()
            }

            override fun onAdFailedToShow(adError: AdError) {
                checkReload(activity, adPlacement, isReloadWhenClose)
                action.invoke()
            }

        })
    }

    fun loadAndShowRewardAds(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        idAds: String,
        adPlacement: String,
        isEnable: Boolean,
        action: () -> Unit
    ) {
        loadRewardAds(activity, idAds, adPlacement, isEnable, true, onAdLoaded = {
            showRewardAds(activity, lifecycleOwner, adPlacement, action = action)
        })
    }

    private fun loadAds(activity: Activity, adPlacement: String, isLoadAndShow: Boolean = false, onAdLoaded: (() -> Unit)? = null) {
        RewardAdHelper.getInstance(adPlacement).requestRewardAds(activity, isLoadAndShow, object : RewardAdRequestCallBack {
            override fun onAdLoaded(data: ContentAd) {
                Logger.e("$adPlacement : onAdLoaded")
                onAdLoaded?.invoke()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e("$adPlacement : onAdFailedToLoad: ${loadAdError.message}")
            }

        })
    }

    private fun checkReload(activity: Activity, adPlacement: String, isReloadWhenClose: Boolean) {
        if (isReloadWhenClose) {
            loadAds(activity, adPlacement)
        }
    }

    fun cancelAllAds(context: Context, isPurchased: Boolean) {
        RewardAdHelper.getInstance(AdPlacement.REWARD_UNLOCK).cancelRequestAndShowAllAds(isPurchased)
    }
}