package com.example.flashlight.data.model

import com.kt.flashlight.R

object LanguageOptions {
    val all = listOf(
        Language("en", R.drawable.ic_eng, R.string.english, "English"),
        Language("es", R.drawable.ic_span, R.string.spanish, "Español"),
        Language("hi", R.drawable.ic_hin, R.string.hindi, "हिन्दी"),
        Language("ko", R.drawable.ic_korea, R.string.korean, "한국어"),
        Language("ja", R.drawable.ic_japan, R.string.japanese, "日本語"),
        Language("de", R.drawable.ic_german, R.string.german, "Deutsch"),
        Language("pt", R.drawable.ic_portu, R.string.portuguese, "Português"),
        Language("fr", R.drawable.ic_french, R.string.french, "Français"),
        Language("it", R.drawable.ic_italian, R.string.italian, "Italiano"),
        Language("id", R.drawable.ic_indo, R.string.indonesian, "Bahasa Indonesia"),
        Language("vi", R.drawable.ic_vn, R.string.vietnamese, "Tiếng Việt"),
        Language("ru", R.drawable.ic_russian, R.string.russian, "Русский"),
        Language("tr", R.drawable.ic_turkish, R.string.turkish, "Türkçe"),
        Language("zh", R.drawable.ic_chinese, R.string.chinese, "中文"),
    )
}
