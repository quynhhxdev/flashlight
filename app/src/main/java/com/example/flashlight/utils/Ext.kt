@file:Suppress("DEPRECATION")

package com.example.flashlight.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources.getSystem
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.kt.flashlight.R
import com.example.flashlight.data.model.Language
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


fun isTiramisuPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

fun Context?.isPermissionGranted(permission: String?): Boolean {
    val context = this
    return if (context == null || permission.isNullOrEmpty()) false else ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun Language.toJson(): String {
    return Gson().toJson(this)
}

fun String.toLanguageModel(): Language? {
    return kotlin.runCatching {
        Gson().fromJson(this, Language::class.java)
    }.getOrNull()
}

fun Context.setAppLanguage(languageCode: String) {
    LocaleHelper.setLocale(this, languageCode)
}

fun Context.getDeviceLanguage(): String {
    return Locale.getDefault().language
}

val Int.dp: Int get() = (this * getSystem().displayMetrics.density).toInt()

fun Long.convertTimeToDate(): String {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.timeInMillis = this
    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return format.format(utc.time)
}

fun Long.format(format: String): String {
    val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    return simpleDateFormat.format(calendar.time)
}

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = getParcelableExtra(key) as? T

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = getParcelable(key) as? T

inline fun <reified T : Parcelable> Intent.getParcelableArrayList(key: String): ArrayList<T>? = getParcelableArrayListExtra(key)


fun TextView.strikeThrough(text: String) {
    val spannable = SpannableString(text)
    spannable.setSpan(StrikethroughSpan(), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    this.text = spannable
}

fun Context.openAppInStore() {
    val uri = ("market://details?id=" + this.packageName).toUri()
    val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
    try {
        startActivity(myAppLinkToMarket)
    } catch (e: ActivityNotFoundException) {
        Logger.e(e.message)
    }
}

fun Context.share(text: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_SUBJECT, this.getString(R.string.app_name))
    intent.putExtra(Intent.EXTRA_TEXT, text)
    this.startActivity(Intent.createChooser(intent, this.getString(R.string.choose_one)))
}

fun Activity.openBrowser(url: String) {
    try {
        this.startActivity(
            Intent(
                Intent.ACTION_VIEW, url.toUri()
            )
        )
    } catch (ex: ActivityNotFoundException) {
        Logger.e(ex.message)
    }
}