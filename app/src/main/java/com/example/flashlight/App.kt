package com.example.flashlight

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.ads.admob.config.AfAdConfig
import com.ads.admob.config.AfAdjustConfig
import com.ads.admob.config.NetworkProvider
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.appoppen.AppResumeAdHelper
import com.ads.admob.listener.AdmobCallBack
import com.ads.admob.listener.AppOpenAdCallBack
import com.example.flashlight.component.startup.SplashStartupProgress
import com.example.flashlight.utils.Constant
import com.example.flashlight.utils.FirebaseConfigManager
import com.example.flashlight.utils.LocaleHelper
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.SpManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Application.ActivityLifecycleCallbacks {

    @Inject
    lateinit var spManager: SpManager

    private var activityReferences = 0
    private var isActivityChangingConfiguration = false

    companion object {
        var context: Context? = null
            private set
        private var mInstance: App? = null
        val instance get() = mInstance

        var appResumeAdHelper: AppResumeAdHelper? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        context = applicationContext

        initAdmob()

        if (FirebaseApp.initializeApp(this) != null) {
            FirebaseConfigManager.instance().fetch(
                onFetchProgress = { progress -> SplashStartupProgress.update(progress) }
            )
        } else {
            SplashStartupProgress.update(100)
        }

        // Register global Activity lifecycle callbacks for Multi-Activity tracking & Ad coordination
        registerActivityLifecycleCallbacks(this)
    }

    fun initConsentManager(activity: Activity, action: () -> Unit) {
        val consentManager = ConsentManager.getInstance(activity)
        consentManager.initReleaseConsent(onConsentResponse = object : OnConsentResponse {
            override fun onResponse(errorMessage: String?) {
                Logger.e("onResponse: $errorMessage -- ${consentManager.canRequestAds}")
                initOpenResume()
                action.invoke()
            }

            override fun onPolicyRequired(isRequired: Boolean) {
                Logger.e("onPolicyRequired: $isRequired")
                spManager.putBoolean(Constant.KEY_SP_IS_SHOW_UMP_SETTING, isRequired)
            }
        })
    }

    private fun initAdmob() {
        Logger.e("initAdmob")
        val afAdjustConfig = AfAdjustConfig.Build(
            adjustToken = "kthfy8dfgkqo",
            adRevenueKey = "",
            environmentProduct = true
        ).build()

        val afAdConfig = AfAdConfig.Builder(afAdjustConfig = afAdjustConfig)
            .intervalBetweenInterstitial(1000)
            .buildVariantProduce(false)
            .mediationProvider(NetworkProvider.ADMOB)
            .listTestDevices(arrayListOf("45EFA44EA19BAA15F2E090E74382177F"))
            .build()

        AdmobFactory.INSTANCE.initAdmob(this, afAdConfig, adCallback = object : AdmobCallBack {
            override fun initialized() {
                Logger.e("Admob initialized")
            }
        })
    }

    private fun initOpenResume() {
        appResumeAdHelper?.registerAdListener(object : AppOpenAdCallBack {
            override fun onAppOpenAdShow() {
                Logger.e("onAppOpenAdShow")
            }

            override fun onAppOpenAdClose() {
                Logger.e("onAppOpenAdClose")
            }

            override fun onAdLoaded(data: ContentAd) {
                Logger.e("onAdLoaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Logger.e("onAdFailedToLoad: ${loadAdError.message}")
            }

            override fun onAdClicked() {
                Logger.e("onAdClicked")
            }

            override fun onAdImpression() {
                Logger.e("onAdImpression")
            }

            override fun onAdFailedToShow(adError: AdError) {
                Logger.e("onAdFailedToShow: ${adError.message}")
            }
        })
    }

    fun destroyAppOpenAd() {
        appResumeAdHelper?.unregisterAllAdListener()
        appResumeAdHelper?.destroyAds()
        appResumeAdHelper = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val language = spManager.getLanguage()
        LocaleHelper.onAttach(this, language.languageCode)
    }

    override fun attachBaseContext(base: Context?) {
        val savedLang = if (base != null) SpManager.getInstance(base).getLanguage().languageCode else Locale.getDefault().language
        val context = LocaleHelper.onAttach(base, savedLang)
        super.attachBaseContext(context)
    }

    //region Application.ActivityLifecycleCallbacks implementation
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("onActivityCreated: ${activity::class.java.simpleName}")
    }

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfiguration) {
            // App transitioned from Background to Foreground
            Logger.d("App entered Foreground (Resumed/Started)")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        Logger.d("onActivityResumed: ${activity::class.java.simpleName}")
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfiguration = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfiguration) {
            // App transitioned from Foreground to Background
            Logger.d("App entered Background")
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        Logger.d("onActivityDestroyed: ${activity::class.java.simpleName}")
    }
    //endregion
}
