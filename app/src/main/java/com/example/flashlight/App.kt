package com.example.flashlight

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ads.admob.admob.AdmobFactory
import com.ads.admob.cmp.ConsentManager
import com.ads.admob.cmp.interfaces.OnConsentResponse
import com.ads.admob.config.AfAdConfig
import com.ads.admob.config.AfAdjustConfig
import com.ads.admob.config.NetworkProvider
import com.ads.admob.listener.AdmobCallBack
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.example.flashlight.component.startup.SplashStartupProgress
import com.example.flashlight.data.model.UserAttributionType
import com.example.flashlight.utils.Constant
import com.example.flashlight.utils.Logger
import com.example.flashlight.utils.SpManager
import com.example.flashlight.utils.ads.manager.AppResumeAdManager
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.google.firebase.FirebaseApp
import com.kt.flashlight.BuildConfig
import dagger.hilt.android.HiltAndroidApp
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
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
        context = applicationContext

        // 1. Khôi phục ngôn ngữ đã lưu
        initializeAppLanguage()

        // 2. Khởi tạo AppsFlyer phân loại người dùng (Paid / Organic)
        initAppsFlyer()

        // 3. Khởi tạo AdMob
        initAdmob()

        // 4. Khởi tạo App Resume Ads
        AppResumeAdManager.init(this)

        // 5. Khởi tạo Firebase và fetch Remote Config
        if (FirebaseApp.initializeApp(this) != null) {
            //build test
            FirebaseConfigManager.instance().useHardcodedConfig(true)

            //build production
//            FirebaseConfigManager.instance().fetch(
//                onFetchProgress = { progress -> SplashStartupProgress.update(progress) }
//            )

        } else {
            SplashStartupProgress.update(100)
        }

        // Register global Activity lifecycle callbacks for Multi-Activity tracking & Ad coordination
        registerActivityLifecycleCallbacks(this)
    }

    private fun initAppsFlyer() {
        val conversionListener = object : AppsFlyerConversionListener {

            override fun onConversionDataSuccess(
                conversionData: MutableMap<String, Any>?
            ) {
                val status = conversionData?.get("af_status")?.toString()
                Logger.e("[Attribution] AppsFlyer onConversionDataSuccess af_status: $status")

                val detectedType = when (status) {
                    "Non-organic" -> UserAttributionType.PAID
                    "Organic" -> UserAttributionType.ORGANIC
                    else -> UserAttributionType.UNKNOWN
                }

                Logger.e("[Attribution] User type detected: $detectedType")

                if (detectedType != UserAttributionType.UNKNOWN) {
                    val currentType = spManager.getUserAttributionType()
                    if (currentType != detectedType) {
                        spManager.setUserAttributionType(detectedType)
                        Logger.e("[Attribution] User type saved: $detectedType")

                        FirebaseConfigManager.instance().onAttributionUpdated(detectedType)
                    } else {
                        Logger.e("[Attribution] Attribution status unchanged ($detectedType). Keeping existing configuration.")
                    }
                } else {
                    Logger.e("[Attribution] Received UNKNOWN status from AppsFlyer. Keeping existing attribution: ${spManager.getUserAttributionType()}")
                }
            }

            override fun onConversionDataFail(errorMessage: String?) {
                Logger.e("[Attribution] AppsFlyer onConversionDataFail: $errorMessage. Keeping previous attribution.")
            }

            override fun onAppOpenAttribution(
                attributionData: MutableMap<String, String>?
            ) {
                Logger.e("[Attribution] AppsFlyer onAppOpenAttribution: $attributionData")
            }

            override fun onAttributionFailure(errorMessage: String?) {
                Logger.e("[Attribution] AppsFlyer onAttributionFailure: $errorMessage. Keeping previous attribution.")
            }
        }

        try {
            val devKey = BuildConfig.APPSFLYER_DEV_KEY
            if (devKey.isNotEmpty()) {
                AppsFlyerLib.getInstance().init(devKey, conversionListener, this)
                AppsFlyerLib.getInstance().start(this)
                Logger.e("[Attribution] AppsFlyer initialized successfully with devKey.")
            } else {
                Logger.e("[Attribution] APPSFLYER_DEV_KEY is empty. Skipping AppsFlyer initialization safely.")
            }
        } catch (e: Exception) {
            Logger.e("[Attribution] initAppsFlyer error: ${e.message}")
        }
    }

    private fun initializeAppLanguage() {
        val language = SpManager.getInstance(this).getLanguage()

        val appLocales = LocaleListCompat.forLanguageTags(
            language.languageCode
        )

        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != appLocales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(appLocales)
        }
    }

    fun initConsentManager(activity: Activity, action: () -> Unit) {
        val consentManager = ConsentManager.getInstance(activity)
        consentManager.initReleaseConsent(onConsentResponse = object : OnConsentResponse {
            override fun onResponse(errorMessage: String?) {
                Logger.e("onResponse: $errorMessage -- ${consentManager.canRequestAds}")
                AppResumeAdManager.setupAppResumeAd(this@App)
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

    fun destroyAppOpenAd() {
        AppResumeAdManager.destroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
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
