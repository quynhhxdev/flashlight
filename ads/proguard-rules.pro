# AdMob / Google Play Services
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.mediation.** { *; }
-dontwarn com.google.android.gms.ads.**

# Adjust SDK
-keep class com.adjust.sdk.** { *; }
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep public class com.android.installreferrer.** { *; }

# Pangle / ByteDance SDK
-keep class com.bytedance.sdk.** { *; }
-keep class com.bytedance.adsdk.** { *; }
-keep class com.bytedance.component.** { *; }
-dontwarn com.bytedance.**

# Facebook Audience Network & SDK
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.infer.annotation.**

# AppLovin SDK
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**

# Vungle SDK
-keep class com.vungle.** { *; }
-dontwarn com.vungle.**

# Mintegral SDK
-keep class com.mbridge.** { *; }
-keep interface com.mbridge.** { *; }
-dontwarn com.mbridge.**
-keep class **.R$* { public static final int mbridge*; }

# Unity Ads SDK
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.**

# Google Billing
-keep class com.android.billingclient.api.** { *; }

# Custom Ads Module
-keep class com.ads.admob.** { *; }
