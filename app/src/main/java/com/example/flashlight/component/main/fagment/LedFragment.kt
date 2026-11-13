package com.example.flashlight.component.main.fagment

import android.animation.ValueAnimator
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
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.dialog.ColorPickerDialog
import com.example.flashlight.utils.SpManager
import com.kt.flashlight.R
import com.kt.flashlight.databinding.FragmentLedBinding

class LedFragment : BaseFragment<FragmentLedBinding>() {

    private var currentTextColor = DEFAULT_TEXT_COLOR
    private val shadowColor = SHADOW_COLOR
    private var currentSpeed = 5
    private var currentEffectId = R.id.btnEffectGlow
    private var currentDirectionId = R.id.btnDirLeft

    private var previewAnimator: ValueAnimator? = null
    private var fullScreenAnimator: ValueAnimator? = null

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

        val spManager = SpManager.getInstance(requireContext())

        // Load persisted settings
        val savedText = spManager.getLedText("HELLO WORLD")
        val savedFontSize = spManager.getLedFontSize(32)
        val savedScrollSpeed = spManager.getLedScrollSpeed(5)
        val savedColor = spManager.getLedColor(DEFAULT_TEXT_COLOR)
        val savedDirection = spManager.getLedDirection(R.id.btnDirLeft)
        val savedEffect = spManager.getLedEffect(R.id.btnEffectGlow)

        currentSpeed = savedScrollSpeed
        currentEffectId = savedEffect
        currentDirectionId = savedDirection

        viewBinding.etLedText.setText(savedText)
        viewBinding.tvLedPreview.text = savedText
        viewBinding.sbFontSize.progress = savedFontSize
        viewBinding.sbScrollSpeed.progress = savedScrollSpeed

        val size = savedFontSize.coerceAtLeast(14).toFloat()
        viewBinding.tvFontSizeValue.text = "${size.toInt()}px"
        viewBinding.tvLedPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)

        viewBinding.tvScrollSpeedValue.text = "${savedScrollSpeed}s"

        currentTextColor = savedColor
        applyTextShadow(viewBinding.tvLedPreview)

        // Find and select corresponding color view or custom color
        var matchedColorView: View? = null
        colorViews.forEach { (view, color, _) ->
            if (color == savedColor) {
                matchedColorView = view
            }
        }
        if (matchedColorView != null) {
            selectColor(matchedColorView!!, savedColor)
        } else {
            setLedTextColor(savedColor)
            colorViews.forEach { (view, viewColor, _) ->
                view.backgroundTintList = null
                view.background = createUnselectedColorDrawable(viewColor)
            }
        }

        // Apply saved direction & effect
        selectDirection(savedDirection)
        selectEffect(savedEffect)
        startScrollingAnimation(viewBinding.tvLedPreview, currentSpeed)

        // Text input binding with persistence
        viewBinding.etLedText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val t = if (s.isNullOrEmpty()) "HELLO WORLD" else s.toString()
                viewBinding.tvLedPreview.text = t
                startScrollingAnimation(viewBinding.tvLedPreview, currentSpeed)
                spManager.saveLedText(t)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Font size seekbar with persistence
        viewBinding.sbFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress.coerceAtLeast(14).toFloat()
                viewBinding.tvFontSizeValue.text = "${size.toInt()}px"
                viewBinding.tvLedPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
                startScrollingAnimation(viewBinding.tvLedPreview, currentSpeed)
                spManager.saveLedFontSize(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Scroll speed seekbar with persistence
        viewBinding.sbScrollSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress.coerceIn(1, 10)
                currentSpeed = speed
                viewBinding.tvScrollSpeedValue.text = "${speed}s"
                startScrollingAnimation(viewBinding.tvLedPreview, currentSpeed)
                spManager.saveLedScrollSpeed(speed)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Color selection listeners with persistence
        viewBinding.colorCyan.setOnClickListener { selectColor(it, Color.parseColor("#00E5FF")); spManager.saveLedColor(Color.parseColor("#00E5FF")) }
        viewBinding.colorWhite.setOnClickListener { selectColor(it, Color.WHITE); spManager.saveLedColor(Color.WHITE) }
        viewBinding.colorYellow.setOnClickListener { selectColor(it, DEFAULT_TEXT_COLOR); spManager.saveLedColor(DEFAULT_TEXT_COLOR) }
        viewBinding.colorGreen.setOnClickListener { selectColor(it, Color.parseColor("#81C784")); spManager.saveLedColor(Color.parseColor("#81C784")) }
        viewBinding.colorPurple.setOnClickListener { selectColor(it, Color.parseColor("#BA68C8")); spManager.saveLedColor(Color.parseColor("#BA68C8")) }
        viewBinding.colorCoral.setOnClickListener { selectColor(it, Color.parseColor("#E57373")); spManager.saveLedColor(Color.parseColor("#E57373")) }

        // Custom color picker icon
        viewBinding.btnCustomColor.setOnClickListener {
            val dialog = ColorPickerDialog(requireContext(), currentTextColor) { chosenColor ->
                setLedTextColor(chosenColor)
                spManager.saveLedColor(chosenColor)
                colorViews.forEach { (view, viewColor, _) ->
                    view.backgroundTintList = null
                    view.background = createUnselectedColorDrawable(viewColor)
                }
            }
            dialog.show()
        }

        // Direction listeners with persistence
        viewBinding.btnDirRight.setOnClickListener { selectDirection(R.id.btnDirRight); spManager.saveLedDirection(R.id.btnDirRight) }
        viewBinding.btnDirLeft.setOnClickListener { selectDirection(R.id.btnDirLeft); spManager.saveLedDirection(R.id.btnDirLeft) }
        viewBinding.btnDirDown.setOnClickListener { selectDirection(R.id.btnDirDown); spManager.saveLedDirection(R.id.btnDirDown) }
        viewBinding.btnDirUp.setOnClickListener { selectDirection(R.id.btnDirUp); spManager.saveLedDirection(R.id.btnDirUp) }

        // Visual Effect listeners with persistence
        viewBinding.btnEffectGlow.setOnClickListener { selectEffect(R.id.btnEffectGlow); spManager.saveLedEffect(R.id.btnEffectGlow) }
        viewBinding.btnEffectBlink.setOnClickListener { selectEffect(R.id.btnEffectBlink); spManager.saveLedEffect(R.id.btnEffectBlink) }
        viewBinding.btnEffectNeon.setOnClickListener { selectEffect(R.id.btnEffectNeon); spManager.saveLedEffect(R.id.btnEffectNeon) }
        viewBinding.btnEffectfade.setOnClickListener { selectEffect(R.id.btnEffectfade); spManager.saveLedEffect(R.id.btnEffectfade) }

        // Full screen LED button
        viewBinding.btnFullScreen.setOnClickListener {
            showFullScreenLed()
        }
    }

    private fun startScrollingAnimation(textView: TextView, speed: Int) {
        textView.post {
            val parentWidth = (textView.parent as? View)?.width ?: 1000
            val parentHeight = (textView.parent as? View)?.height ?: 500
            val textWidth = textView.paint.measureText(textView.text.toString()).coerceAtLeast(100f) + 100f
            val textHeight = textView.textSize * 1.5f

            textView.translationX = 0f
            textView.translationY = 0f

            previewAnimator?.cancel()
            previewAnimator = when (currentDirectionId) {
                R.id.btnDirLeft -> {
                    ValueAnimator.ofFloat(parentWidth.toFloat(), -textWidth).apply {
                        duration = ((11 - speed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = animator.animatedValue as Float
                            textView.translationY = 0f
                        }
                    }
                }
                R.id.btnDirRight -> {
                    ValueAnimator.ofFloat(-textWidth, parentWidth.toFloat()).apply {
                        duration = ((11 - speed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = animator.animatedValue as Float
                            textView.translationY = 0f
                        }
                    }
                }
                R.id.btnDirUp -> {
                    ValueAnimator.ofFloat(parentHeight.toFloat(), -textHeight).apply {
                        duration = ((11 - speed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = 0f
                            textView.translationY = animator.animatedValue as Float
                        }
                    }
                }
                R.id.btnDirDown -> {
                    ValueAnimator.ofFloat(-textHeight, parentHeight.toFloat()).apply {
                        duration = ((11 - speed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = 0f
                            textView.translationY = animator.animatedValue as Float
                        }
                    }
                }
                else -> {
                    ValueAnimator.ofFloat(parentWidth.toFloat(), -textWidth).apply {
                        duration = ((11 - speed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = animator.animatedValue as Float
                            textView.translationY = 0f
                        }
                    }
                }
            }
            previewAnimator?.start()
        }
    }

    private fun setLedTextColor(color: Int) {
        currentTextColor = color
        viewBinding.tvLedPreview.setTextColor(color)
        applyTextShadow(viewBinding.tvLedPreview)
    }

    private fun selectColor(targetView: View, color: Int) {
        setLedTextColor(color)

        colorViews.forEach { (view, viewColor, _) ->
            view.backgroundTintList = null
            if (view == targetView) {
                view.background = createSelectedColorDrawable(viewColor)
            } else {
                view.background = createUnselectedColorDrawable(viewColor)
            }
        }
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
        startScrollingAnimation(viewBinding.tvLedPreview, currentSpeed)
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

        applyEffectToTextView(viewBinding.tvLedPreview, effectId)
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
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val dialog = android.app.Dialog(act, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val previewSp = viewBinding.tvLedPreview.textSize / resources.displayMetrics.scaledDensity

        val textView = TextView(act).apply {
            text = viewBinding.tvLedPreview.text
            setTextColor(currentTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, previewSp * 1.5f)
            typeface = viewBinding.tvLedPreview.typeface
            isSingleLine = true
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
        }

        applyEffectToTextView(textView, currentEffectId)

        // Start animation on full screen text view
        textView.post {
            val parentWidth = act.resources.displayMetrics.widthPixels.toFloat()
            val parentHeight = act.resources.displayMetrics.heightPixels.toFloat()
            val textWidth = textView.paint.measureText(textView.text.toString()).coerceAtLeast(100f) + 100f
            val textHeight = textView.textSize * 1.5f

            textView.translationX = 0f
            textView.translationY = 0f

            fullScreenAnimator?.cancel()
            fullScreenAnimator = when (currentDirectionId) {
                R.id.btnDirLeft -> {
                    ValueAnimator.ofFloat(parentWidth, -textWidth).apply {
                        duration = ((11 - currentSpeed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = animator.animatedValue as Float
                            textView.translationY = 0f
                        }
                    }
                }
                R.id.btnDirRight -> {
                    ValueAnimator.ofFloat(-textWidth, parentWidth).apply {
                        duration = ((11 - currentSpeed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = animator.animatedValue as Float
                            textView.translationY = 0f
                        }
                    }
                }
                R.id.btnDirUp -> {
                    ValueAnimator.ofFloat(parentHeight, -textHeight).apply {
                        duration = ((11 - currentSpeed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = 0f
                            textView.translationY = animator.animatedValue as Float
                        }
                    }
                }
                R.id.btnDirDown -> {
                    ValueAnimator.ofFloat(-textHeight, parentHeight).apply {
                        duration = ((11 - currentSpeed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = 0f
                            textView.translationY = animator.animatedValue as Float
                        }
                    }
                }
                else -> {
                    ValueAnimator.ofFloat(parentWidth, -textWidth).apply {
                        duration = ((11 - currentSpeed) * 900L).coerceIn(800L, 10000L)
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.RESTART
                        interpolator = LinearInterpolator()
                        addUpdateListener { animator ->
                            textView.translationX = animator.animatedValue as Float
                            textView.translationY = animator.animatedValue as Float
                        }
                    }
                }
            }
            fullScreenAnimator?.start()
        }

        textView.setOnClickListener {
            fullScreenAnimator?.cancel()
            dialog.dismiss()
        }

        dialog.setContentView(textView)
        dialog.setOnDismissListener {
            fullScreenAnimator?.cancel()
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        dialog.show()
    }

    override fun onDestroyView() {
        previewAnimator?.cancel()
        fullScreenAnimator?.cancel()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onDestroyView()
    }

    companion object {
        private val DEFAULT_TEXT_COLOR = Color.parseColor("#FDE047")
        private val SHADOW_COLOR = Color.parseColor("#FFD54F")
        fun newInstance() = LedFragment()
    }
}
