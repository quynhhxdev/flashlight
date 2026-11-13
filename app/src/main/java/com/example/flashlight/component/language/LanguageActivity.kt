package com.example.flashlight.component.language

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.component.onboard.OnboardingActivity
import com.example.flashlight.data.model.Language
import com.example.flashlight.data.model.LanguageOptions
import com.example.flashlight.utils.LocaleHelper
import com.example.flashlight.utils.SpManager
import com.kt.flashlight.R
import com.kt.flashlight.databinding.ActivityLanguageBinding

class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {

    private lateinit var adapter: LanguageAdapter
    private var selectedLanguage: Language? = null
    private var isFromSettings = false

    override fun provideViewBinding() = ActivityLanguageBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        setupHeader()
        initLanguageList()
        setupListeners()
    }

    private fun setupHeader() {
        if (isFromSettings) {
            binding.btnBack.isVisible = true
            binding.tvTitle.text = "Choose Language"
        } else {
            binding.btnBack.isVisible = false
            binding.tvTitle.text = "Language"
        }
    }

    private fun initLanguageList() {
        val spManager = SpManager.getInstance(this)
        val currentLang = spManager.getLanguage()
        selectedLanguage = currentLang

        val languages = LanguageOptions.all

        adapter = LanguageAdapter(languages) { selected ->
            selectedLanguage = selected
        }

        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
        binding.rvLanguage.adapter = adapter
        adapter.submitList(languages, currentLang.languageCode)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            selectedLanguage?.let { lang ->
                val spManager = SpManager.getInstance(this)
                spManager.saveLanguage(lang)
                spManager.setLanguageChosen()
                LocaleHelper.setLocale(this, lang.languageCode)

                // Apply locale using AppCompatDelegate without manual restarting MainActivity
                val appLocale = LocaleListCompat.forLanguageTags(lang.languageCode)
                AppCompatDelegate.setApplicationLocales(appLocale)

                if (isFromSettings) {
                    setResult(RESULT_OK)
                } else {
                    val intent = Intent(this, OnboardingActivity::class.java)
                    startActivity(intent)
                }
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_FROM_SETTINGS = "extra_from_settings"
    }
}
