package com.example.flashlight.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun onAttach(context: Context?, defaultLanguage: String): Context? {
        return setLocale(context, defaultLanguage)
    }

    fun setLocale(context: Context?, language: String): Context? {
        if (context == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language)
        } else {
            updateResourcesLegacy(context, language)
        }
    }

    fun getLocale(language: String): Locale {
        return if (language.contains("-")) {
            val parts = language.split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale(language)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResources(context: Context, language: String): Context {
        val locale = getLocale(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, language: String): Context {
        val locale = getLocale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.locale = locale
        configuration.setLayoutDirection(locale)

        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }
}
