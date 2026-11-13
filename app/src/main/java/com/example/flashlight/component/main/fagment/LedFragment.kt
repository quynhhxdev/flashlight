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
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.dialog.ColorPickerDialog
import com.example.flashlight.component.main.fagment.led.LedConfig
import com.example.flashlight.component.main.fagment.led.LedConfigViewModel
import com.example.flashlight.component.main.fagment.led.LedScrollController
import com.kt.flashlight.R
import com.kt.flashlight.databinding.FragmentLedBinding
import kotlinx.coroutines.launch

class LedFragment : BaseFragment<FragmentLedBinding>() {

    private var currentTextColor = DEFAULT_TEXT_COLOR
    private val shadowColor = SHADOW_COLOR
    private var currentSpeed = 5
    private var currentEffectId = R.id.btnEffectGlow
    private var currentDirectionId = R.id.btnDirLeft
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
        ledConfigViewModel = ViewModelProvider(requireActivity())[LedConfigViewModel::class.java]
        currentConfig = ledConfigViewModel.config.value
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
                    updateLedConfig(currentConfig.copy(fontSizeSp = size))
                    ledConfigViewModel.setFontSizeSp(size)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        viewBinding.sbScrollSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isApplyingConfig) {
                    val speed = progress.coerceIn(1, 10)
                    updateLedConfig(currentConfig.copy(scrollSpeed = speed))
                    ledConfigViewModel.setScrollSpeed(speed)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

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
        val directionChanged = config.directionId != previousConfig.directionId
        val previewEffectChanged = !hasAppliedConfig ||
            config.effectId != previousConfig.effectId || config.textColor != previousConfig.textColor
        currentConfig = config
        currentTextColor = config.textColor
        currentSpeed = config.scrollSpeed.coerceIn(1, 10)
        currentEffectId = config.effectId
        currentDirectionId = config.directionId

        isApplyingConfig = true
        try {
            if (viewBinding.etLedText.text.toString() != config.text) {
                viewBinding.etLedText.setText(config.text)
                viewBinding.etLedText.setSelection(config.text.length)
            }
            viewBinding.sbFontSize.progress = config.fontSizeSp
            viewBinding.sbScrollSpeed.progress = config.scrollSpeed
            viewBinding.tvFontSizeValue.text = "${config.fontSizeSp.coerceAtLeast(14)}px"
            viewBinding.tvScrollSpeedValue.text = "${currentSpeed}s"

            applyTextConfiguration(
                viewBinding.tvLedPreview,
                config,
                applyEffect = previewEffectChanged
            )
            selectColorView(config.textColor)
            selectDirection(config.directionId)
            selectEffect(config.effectId)
            previewScroller?.updateTextMetrics()
            if (directionChanged) previewScroller?.start(resetPosition = true)

            fullScreenTextView?.let { textView ->
                applyTextConfiguration(textView, config, applyEffect = previewEffectChanged)
                fullScreenScroller?.updateTextMetrics()
                if (directionChanged) fullScreenScroller?.start(resetPosition = true)
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

    private fun applyTextConfiguration(textView: TextView, config: LedConfig, applyEffect: Boolean) {
        textView.text = config.text.ifEmpty { "HELLO WORLD" }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.fontSizeSp.coerceAtLeast(14).toFloat())
        textView.setTextColor(config.textColor)
        textView.maxLines = 1
        textView.isSingleLine = true
        textView.setHorizontallyScrolling(true)
        textView.ellipsize = null
        if (applyEffect) applyEffectToTextView(textView, config.effectId)
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

    private fun applyEffectToTextView(textView: TextView, effectId: Int) {
        textView.clearAnimation()
        textView.alpha = 1.0f
        textView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        when (effectId) {
            R.id.btnEffectGlow -> {
                applyTextShadow(textView)
            }
            R.id.btnEffectBlink -> {
                applyTextShadow(textView)
                val anim = AlphaAnimation(1f, 0f).apply {
                    duration = 200
                    repeatCount = Animation.INFINITE
                    repeatMode = Animation.REVERSE
                    interpolator = CycleInterpolator(1f)
                }
                textView.startAnimation(anim)
            }
            R.id.btnEffectNeon -> {
                textView.setShadowLayer(40f, 0f, 0f, currentTextColor)
            }
            R.id.btnEffectfade -> {
                applyTextShadow(textView)
                val anim = AlphaAnimation(0.2f, 1f).apply {
                    duration = 1000
                    repeatCount = Animation.INFINITE
                    repeatMode = Animation.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }
                textView.startAnimation(anim)
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

    private fun applyTextShadow(textView: TextView) {
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
        applyTextConfiguration(textView, currentConfig, applyEffect = true)

        val scroller = LedScrollController(textView, viewport) { currentConfig }
        fullScreenDialog = dialog
        fullScreenTextView = textView
        fullScreenScroller = scroller

        textView.setOnClickListener { dialog.dismiss() }
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
            scroller.updateTextMetrics()
            scroller.start()
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

    companion object {
        private val DEFAULT_TEXT_COLOR = Color.parseColor("#FDE047")
        private val SHADOW_COLOR = Color.parseColor("#FFD54F")
        fun newInstance() = LedFragment()
    }
}
