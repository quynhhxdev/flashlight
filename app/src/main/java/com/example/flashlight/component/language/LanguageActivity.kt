package com.example.flashlight.component.language

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import arrow.core.const
import com.example.flashlight.base.activity.BaseActivity
import com.example.flashlight.component.onboard.OnboardingActivity
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.example.flashlight.data.model.Language
import com.example.flashlight.data.model.LanguageOptions
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.example.flashlight.utils.LocaleHelper
import com.example.flashlight.utils.SpManager
import com.example.flashlight.utils.ads.AdPlacement
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.kt.flashlight.databinding.ActivityLanguageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {

    private val viewModel: LanguageViewModel by viewModels()
    private lateinit var adapter: LanguageAdapter
    private var selectedLanguage: Language? = null
    private var isFromSettings = false

    override fun provideViewBinding() = ActivityLanguageBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        setupHeader()
        initLanguageList()
        observeViewModel()
        setupListeners()
        setupNativeAd()

        // Load dữ liệu sau khi đã thiết lập Observer
        viewModel.loadLanguage(isFromSettings)
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

        adapter = LanguageAdapter(emptyList()) { language ->
            viewModel.selectLanguage(language)
        }
        binding.rvLanguage.layoutManager = LinearLayoutManager(this)

        binding.rvLanguage.adapter = adapter

        viewModel.loadLanguage(isFromSettings)

//        val spManager = SpManager.getInstance(this)
//        val hasSaved = spManager.hasLanguageSaved()
//        val currentLang = if (hasSaved || isFromSettings) spManager.getLanguage() else null
//        selectedLanguage = currentLang
//
//        binding.btnSave.isVisible = hasSaved || isFromSettings
//
//        val languages = LanguageOptions.all
//
//        adapter = LanguageAdapter(languages) { selected ->
//            selectedLanguage = selected
//            binding.btnSave.isVisible = true
//        }
//
//        binding.rvLanguage.layoutManager = LinearLayoutManager(this)
//        binding.rvLanguage.adapter = adapter
//        adapter.submitList(languages, currentLang?.languageCode ?: "")
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(
                        state.languages,
                        state.selectedLanguage?.languageCode.orEmpty()
                    )

                    binding.btnSave.isVisible = state.isSaveVisible
                }
            }
        }
    }

    private fun setupNativeAd() {
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve(AdPlacement.NATIVE_LANGUAGE)
            if (resolved == null) {
                binding.flNativeAdContainer.isGone = true
            } else {
                binding.flNativeAdContainer.isVisible = true
                NativeAdsUtils.preloadNativeLanguage(this)
                NativeAdsUtils.nativeLanguage?.setNativeContentView(binding.flNativeAdContainer)
                NativeAdsUtils.nativeLanguage?.setShimmerLayoutView(binding.shimmerContainerNative.root)
            }
            NativeAdsUtils.preloadNativeOnboarding(this)
            NativeAdsUtils.preloadNativeFullscreen(this)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            applySelectedLanguage()
        }

    }

    private fun applySelectedLanguage() {
        val languaguage = viewModel.saveLanguage() ?: return

        // Áp dụng ngôn ngữ cho toàn bộ ứng dụng.
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languaguage.languageCode)
        )

        if (isFromSettings) {
            // Trả kết quả về Fragment đã mở LanguageActivity
            setResult(RESULT_OK)
            finish()
        } else {
            // Luồng lần đầu chọn ngôn ngữ
            startActivity(
                Intent(this, OnboardingActivity::class.java)
            )
            finish()
        }
    }

    companion object {
        const val EXTRA_FROM_SETTINGS = "extra_from_settings"
    }
}
