package com.example.flashlight.component.main.fagment

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.CycleInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.MainActivity
import com.example.flashlight.component.main.dialog.ColorPickerDialog
import com.example.flashlight.component.main.fagment.led.LedConfig
import com.example.flashlight.component.main.fagment.led.LedConfigViewModel
import com.example.flashlight.component.main.fagment.led.LedScrollController
import com.example.flashlight.utils.SpManager
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.kt.flashlight.R
import com.kt.flashlight.databinding.FragmentLedBinding
import kotlinx.coroutines.launch

class LedFragment : BaseFragment<FragmentLedBinding>() {

    private var currentTextColor = DEFAULT_TEXT_COLOR
    private var currentShadowColor: Int? = null
    private var currentSpeed = 5
    private var currentEffectId = R.id.btnEffectGlow
    private var currentDirectionId = R.id.btnDirRight
    private lateinit var ledConfigViewModel: LedConfigViewModel
    private var currentConfig = LedConfig()
    private var isApplyingConfig = false
    private var hasAppliedConfig = false
    private var previewScroller: LedScrollController? = null
    private var fullScreenScroller: LedScrollController? = null
    private var fullScreenTextView: TextView? = null
    private var fullScreenDialog: Dialog? = null
    private var previousRequestedOrientation: Int? = null

    private val colorViews by lazy {
        listOf(
            Triple(viewBinding.colorCyan, Color.parseColor("#00E5FF"), viewBinding.colorCyan),
            Triple(viewBinding.colorWhite, Color.WHITE, viewBinding.colorWhite),
            Triple(viewBinding.colorYellow, DEFAULT_TEXT_COLOR, viewBinding.colorYellow),
            Triple(viewBinding.colorGreen, Color.parseColor("#81C784"), viewBinding.colorGreen),
            Triple(viewBinding.colorPurple, Color.parseColor("#BA68C8"), viewBinding.colorPurple),
            Triple(viewBinding.colorCoral, Color.parseColor("#E57373"), viewBinding.colorCoral)
        )
    }

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentLedBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        viewBinding.layoutBottomNav.root.isVisible = false
        NativeAdsUtils.bindHomeNative(
            activity = requireActivity(),
            lifecycleOwner = viewLifecycleOwner,
            nativeContentView = viewBinding.nativeAdContainer,
            shimmerContainerNative = viewBinding.shimmerContainerNative.root
        )
        val spManager = SpManager.getInstance(requireContext())
        ledConfigViewModel = ViewModelProvider(requireActivity())[LedConfigViewModel::class.java]
        currentConfig = ledConfigViewModel.config.value
        currentTextColor = currentConfig.textColor
        currentShadowColor = currentConfig.shadowColor
        previewScroller = LedScrollController(
            viewBinding.tvLedPreview,
            viewBinding.tvLedPreview.parent as View
        ) { currentConfig }
        updateLedConfig(currentConfig)

        viewBinding.etLedText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isApplyingConfig) {
                    val text = s?.toString().orEmpty()
                    if (text != currentConfig.text) {
                        updateLedConfig(currentConfig.copy(text = text))
                        ledConfigViewModel.setText(text)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        viewBinding.sbFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isApplyingConfig) {
                    val size = progress.coerceAtLeast(14)
                    if (size != currentConfig.fontSizeSp) {
                        val newConfig = currentConfig.copy(fontSizeSp = size)
                        updateLedConfig(newConfig)
                        if (fromUser) {
                            ledConfigViewModel.updateFontSizeInMemory(size)
                        }
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val size = (seekBar?.progress ?: currentConfig.fontSizeSp).coerceAtLeast(14)
                ledConfigViewModel.persistFontSizeSp(size)
            }
        })

        viewBinding.sbScrollSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isApplyingConfig) {
                    val speed = progress.coerceIn(1, 10)
                    if (speed != currentConfig.scrollSpeed) {
                        val newConfig = currentConfig.copy(scrollSpeed = speed)
                        updateLedConfig(newConfig)
                        if (fromUser) {
                            ledConfigViewModel.updateScrollSpeedInMemory(speed)
                        }
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val speed = (seekBar?.progress ?: currentConfig.scrollSpeed).coerceIn(1, 10)
                ledConfigViewModel.persistScrollSpeed(speed)
            }
        })

        viewBinding.cardLedPreview.setOnClickListener {
            val initialColor = currentConfig.shadowColor
                ?: if (currentConfig.effectId == R.id.btnEffectNeon) currentTextColor else DEFAULT_SHADOW_COLOR
            val dialog = ColorPickerDialog(requireContext(), initialColor) { chosenColor ->
                spManager.saveLedShadowColor(chosenColor)
                updateLedConfig(currentConfig.copy(shadowColor = chosenColor))
                ledConfigViewModel.setShadowColor(chosenColor)
            }
            dialog.show()
        }

        viewBinding.colorCyan.setOnClickListener { chooseColor(Color.parseColor("#00E5FF")) }
        viewBinding.colorWhite.setOnClickListener { chooseColor(Color.WHITE) }
        viewBinding.colorYellow.setOnClickListener { chooseColor(DEFAULT_TEXT_COLOR) }
        viewBinding.colorGreen.setOnClickListener { chooseColor(Color.parseColor("#81C784")) }
        viewBinding.colorPurple.setOnClickListener { chooseColor(Color.parseColor("#BA68C8")) }
        viewBinding.colorCoral.setOnClickListener { chooseColor(Color.parseColor("#E57373")) }

        viewBinding.btnCustomColor.setOnClickListener {
            ColorPickerDialog(requireContext(), currentTextColor) { chosenColor ->
                updateLedConfig(currentConfig.copy(textColor = chosenColor))
                ledConfigViewModel.setTextColor(chosenColor)
            }.show()
        }

        viewBinding.btnDirRight.setOnClickListener { chooseDirection(R.id.btnDirRight) }
        viewBinding.btnDirLeft.setOnClickListener { chooseDirection(R.id.btnDirLeft) }
        viewBinding.btnDirDown.setOnClickListener { chooseDirection(R.id.btnDirDown) }
        viewBinding.btnDirUp.setOnClickListener { chooseDirection(R.id.btnDirUp) }

        viewBinding.btnEffectGlow.setOnClickListener { chooseEffect(R.id.btnEffectGlow) }
        viewBinding.btnEffectBlink.setOnClickListener { chooseEffect(R.id.btnEffectBlink) }
        viewBinding.btnEffectNeon.setOnClickListener { chooseEffect(R.id.btnEffectNeon) }
        viewBinding.btnEffectfade.setOnClickListener { chooseEffect(R.id.btnEffectfade) }
        viewBinding.btnFullScreen.setOnClickListener { showFullScreenLed() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ledConfigViewModel.config.collect { config ->
                    updateLedConfig(config)
                }
            }
        }
    }

    private fun chooseColor(color: Int) {
        updateLedConfig(currentConfig.copy(textColor = color))
        ledConfigViewModel.setTextColor(color)
    }

    private fun chooseDirection(directionId: Int) {
        updateLedConfig(currentConfig.copy(directionId = directionId))
        ledConfigViewModel.setDirection(directionId)
    }

    private fun chooseEffect(effectId: Int) {
        updateLedConfig(currentConfig.copy(effectId = effectId))
        ledConfigViewModel.setEffect(effectId)
    }

    private fun updateLedConfig(config: LedConfig) {
        val previousConfig = currentConfig
        val directionChanged = !hasAppliedConfig || config.directionId != previousConfig.directionId
        val colorChanged = !hasAppliedConfig || config.textColor != previousConfig.textColor
        val shadowColorChanged = !hasAppliedConfig || config.shadowColor != previousConfig.shadowColor
        val effectChanged = !hasAppliedConfig || config.effectId != previousConfig.effectId

        currentConfig = config
        currentTextColor = config.textColor
        currentShadowColor = config.shadowColor
        currentSpeed = config.scrollSpeed.coerceIn(1, 10)
        currentEffectId = config.effectId
        currentDirectionId = config.directionId

        isApplyingConfig = true
        try {
            if (viewBinding.etLedText.text.toString() != config.text) {
                viewBinding.etLedText.setText(config.text)
                viewBinding.etLedText.setSelection(config.text.length)
            }
            if (viewBinding.sbFontSize.progress != config.fontSizeSp) {
                viewBinding.sbFontSize.progress = config.fontSizeSp
            }
            if (viewBinding.sbScrollSpeed.progress != config.scrollSpeed) {
                viewBinding.sbScrollSpeed.progress = config.scrollSpeed
            }
            viewBinding.tvFontSizeValue.text = "${config.fontSizeSp.coerceAtLeast(14)}px"
            viewBinding.tvScrollSpeedValue.text = "${currentSpeed}s"

            val previewViewport = viewBinding.tvLedPreview.parent as? View
            applyTextConfiguration(
                viewBinding.tvLedPreview,
                config,
                applyEffect = effectChanged || colorChanged || shadowColorChanged,
                viewport = previewViewport
            )

            if (colorChanged) selectColorView(config.textColor)
            if (directionChanged) selectDirection(config.directionId)
            if (effectChanged) selectEffect(config.effectId)

            previewScroller?.updateTextMetrics()
            val resetPos = directionChanged || !hasAppliedConfig
            previewScroller?.start(resetPosition = resetPos)

            fullScreenTextView?.let { textView ->
                val fullScreenViewport = textView.parent as? View
                applyTextConfiguration(
                    textView,
                    config,
                    applyEffect = effectChanged || colorChanged || shadowColorChanged,
                    viewport = fullScreenViewport
                )
                fullScreenScroller?.updateTextMetrics()
                fullScreenScroller?.start(resetPosition = resetPos)
            }
            hasAppliedConfig = true
        } finally {
            isApplyingConfig = false
        }
    }

    private fun selectColorView(color: Int) {
        val selectedView = colorViews.firstOrNull { it.second == color }?.first
        colorViews.forEach { (view, viewColor, _) ->
            view.backgroundTintList = null
            view.background = if (view == selectedView) {
                createSelectedColorDrawable(viewColor)
            } else {
                createUnselectedColorDrawable(viewColor)
            }
        }
    }

    private fun applyTextConfiguration(
        textView: TextView,
        config: LedConfig,
        applyEffect: Boolean,
        viewport: View? = null
    ) {
        val targetText = config.text.ifEmpty { "HELLO WORLD" }
        if (textView.text?.toString() != targetText) {
            textView.text = targetText
        }
        textView.maxLines = 1
        textView.isSingleLine = true
        textView.setHorizontallyScrolling(true)
        textView.ellipsize = null
        textView.setTextColor(config.textColor)

        val targetSp = config.fontSizeSp.coerceAtLeast(14).toFloat()
        val effectiveSp = calculateEffectiveFontSizeSp(textView, targetSp, viewport)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, effectiveSp)

        if (applyEffect) applyEffectToTextView(textView, config.effectId, config.shadowColor)
    }

    private fun calculateEffectiveFontSizeSp(
        textView: TextView,
        targetSp: Float,
        viewport: View?
    ): Float {
        if (viewport == null) return targetSp
        val viewportHeight = viewport.height - viewport.paddingTop - viewport.paddingBottom
        if (viewportHeight <= 0) return targetSp

        val shadowRadius = textView.shadowRadius
        val extraMargin = dpToPx(12) + (shadowRadius * 2f).toInt()
        val maxAllowedHeight = (viewportHeight - extraMargin).coerceAtLeast(30)

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, targetSp)
        textView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val measuredH = textView.measuredHeight
        if (measuredH <= maxAllowedHeight || measuredH <= 0) {
            return targetSp
        }

        val scale = maxAllowedHeight.toFloat() / measuredH.toFloat()
        return (targetSp * scale).coerceAtLeast(14f)
    }

    private fun selectDirection(directionId: Int) {
        currentDirectionId = directionId
        val dirViews = listOf(viewBinding.btnDirRight, viewBinding.btnDirLeft, viewBinding.btnDirDown, viewBinding.btnDirUp)
        dirViews.forEach { v ->
            if (v.id == directionId) {
                v.setBackgroundResource(R.drawable.bg_direction_selected)
            } else {
                v.setBackgroundResource(R.drawable.bg_option_drec_unselected)
            }
        }
    }

    private fun selectEffect(effectId: Int) {
        currentEffectId = effectId
        val effectViews = listOf(viewBinding.btnEffectGlow, viewBinding.btnEffectBlink, viewBinding.btnEffectNeon, viewBinding.btnEffectfade)
        effectViews.forEach { v ->
            if (v.id == effectId) {
                v.setBackgroundResource(R.drawable.bg_pill_selected)
                (v as? TextView)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_yellow_switch))
            } else {
                v.setBackgroundResource(R.drawable.bg_pill_unselected)
                (v as? TextView)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_no_select_btn))
            }
        }

    }

    private fun applyEffectToTextView(
        textView: TextView,
        effectId: Int,
        shadowColor: Int? = currentShadowColor
    ) {
        textView.clearAnimation()
        textView.alpha = 1.0f
        textView.setLayerType(View.LAYER_TYPE_NONE, null)

        when (effectId) {
            R.id.btnEffectGlow -> {
                applyTextShadow(textView, shadowColor ?: DEFAULT_SHADOW_COLOR)
            }
            R.id.btnEffectBlink -> {
                applyTextShadow(textView, shadowColor ?: DEFAULT_SHADOW_COLOR)
                val anim = AlphaAnimation(1f, 0f).apply {
                    duration = 200
                    repeatCount = Animation.INFINITE
                    repeatMode = Animation.REVERSE
                    interpolator = CycleInterpolator(1f)
                }
                textView.startAnimation(anim)
            }
            R.id.btnEffectNeon -> {
                val neonShadowColor = shadowColor ?: currentTextColor
                textView.setShadowLayer(40f, 0f, 0f, neonShadowColor)
            }
            R.id.btnEffectfade -> {
                applyTextShadow(textView, shadowColor ?: DEFAULT_SHADOW_COLOR)
                val anim = AlphaAnimation(0.2f, 1f).apply {
                    duration = 1000
                    repeatCount = Animation.INFINITE
                    repeatMode = Animation.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                textView.startAnimation(anim)
            }
            else -> {
                applyTextShadow(textView, shadowColor ?: DEFAULT_SHADOW_COLOR)
            }
        }
    }

    private fun createUnselectedColorDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun createSelectedColorDrawable(color: Int): LayerDrawable {
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        val selectBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_select_color) ?: GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(dpToPx(2), Color.parseColor("#FFD54F"))
            setColor(Color.TRANSPARENT)
        }
        return LayerDrawable(arrayOf(circle, selectBg))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun applyTextShadow(
        textView: TextView,
        shadowColor: Int = currentShadowColor ?: DEFAULT_SHADOW_COLOR
    ) {
        textView.setShadowLayer(20f, 0f, 0f, shadowColor)
    }



    private fun showFullScreenLed() {
        val act = activity ?: return
        if (fullScreenDialog?.isShowing == true) return

        previewScroller?.stop()
        previousRequestedOrientation = act.requestedOrientation
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val dialog = Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val viewport = FrameLayout(act).apply {
            setBackgroundColor(Color.BLACK)
            clipChildren = true
            clipToPadding = true
        }
        val textView = TextView(act).apply {
            typeface = viewBinding.tvLedPreview.typeface
            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT)
        }
        viewport.addView(
            textView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        applyTextConfiguration(textView, currentConfig, applyEffect = true, viewport = viewport)

        val closeButton = ImageView(act).apply {
            setImageResource(R.drawable.ic_close_play)
            contentDescription = "Close full screen"
            isClickable = true
            isFocusable = true
            setOnClickListener { handleClosePlay(act, dialog) }
        }
        viewport.addView(
            closeButton,
            FrameLayout.LayoutParams(dpToPx(28), dpToPx(28), Gravity.TOP or Gravity.START).apply {
                leftMargin = dpToPx(24)
                topMargin = dpToPx(24)
            }
        )

        val scroller = LedScrollController(textView, viewport) { currentConfig }
        fullScreenDialog = dialog
        fullScreenTextView = textView
        fullScreenScroller = scroller

        dialog.setContentView(viewport)
        dialog.setOnDismissListener {
            fullScreenScroller?.dispose()
            fullScreenScroller = null
            fullScreenTextView = null
            fullScreenDialog = null
            previousRequestedOrientation?.let { act.requestedOrientation = it }
            previousRequestedOrientation = null
            if (isResumed) previewScroller?.start()
        }
        dialog.show()
        viewport.post {
            applyTextConfiguration(textView, currentConfig, applyEffect = true, viewport = viewport)
            scroller.updateTextMetrics()
            scroller.start(resetPosition = true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (fullScreenDialog?.isShowing == true) {
            fullScreenScroller?.start()
        } else {
            previewScroller?.start()
        }
    }

    override fun onPause() {
        previewScroller?.stop()
        fullScreenScroller?.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        previewScroller?.dispose()
        previewScroller = null
        fullScreenScroller?.dispose()
        fullScreenScroller = null
        fullScreenDialog?.dismiss()
        fullScreenDialog = null
        fullScreenTextView = null
        super.onDestroyView()
    }

    private fun handleClosePlay(activity: android.app.Activity, dialog: Dialog) {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            mainActivity.triggerFeatureInterAd {
                dialog.dismiss()
            }
        } else {
            dialog.dismiss()
        }
    }

    companion object {
        private val DEFAULT_TEXT_COLOR = Color.parseColor("#FDE047")
        private val DEFAULT_SHADOW_COLOR = Color.parseColor("#FFD54F")
        fun newInstance() = LedFragment()
    }
}
