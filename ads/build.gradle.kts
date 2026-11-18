plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.ads.admob"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    annotationProcessor("androidx.lifecycle:lifecycle-compiler:2.8.7")
    api("io.arrow-kt:arrow-core:1.2.4")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")

    // admob
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.2.0")

    // mediation admob
    implementation("com.google.ads.mediation:facebook:6.18.0.0")
    implementation("com.google.ads.mediation:applovin:13.1.0.0")
    implementation("com.google.ads.mediation:vungle:7.4.3.0")
    implementation("com.google.ads.mediation:pangle:6.4.0.6.0")
    implementation("com.google.ads.mediation:mintegral:16.8.61.0")
    implementation("com.unity3d.ads:unity-ads:4.12.5")
    implementation("com.google.ads.mediation:unity:4.12.5.0")

    implementation("com.google.android.gms:play-services-appset:16.1.0")
    implementation("com.google.android.gms:play-services-basement:18.5.0")

    // adjust
    implementation("com.adjust.sdk:adjust-android:5.5.0")
    implementation("com.android.installreferrer:installreferrer:2.2")

    // fb sdk
    implementation("com.facebook.android:facebook-android-sdk:18.1.3")

    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.11.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation("androidx.multidex:multidex:2.0.1")
}
