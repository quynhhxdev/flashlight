package com.example.flashlight.component.main.dialog

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.example.flashlight.base.dialog.BaseDialog
import com.kt.flashlight.R
import com.kt.flashlight.databinding.DialogRatingCartBinding

class RatingDialog @JvmOverloads constructor(
    context: Context,
    private var onRatingSubmitListener: ((rating: Int) -> Unit)? = null,
    private var onCancelListener: (() -> Unit)? = null
) : BaseDialog<DialogRatingCartBinding>(context) {

    var selectedRating: Int = 0
        private set

    private var appName: String? = null

    override fun provideViewBinding(): DialogRatingCartBinding {
        return DialogRatingCartBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun initViews() {
        super.initViews()

        val stars = listOf(
            viewBinding.ivStar1,
            viewBinding.ivStar2,
            viewBinding.ivStar3,
            viewBinding.ivStar4,
            viewBinding.ivStar5
        )

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                updateRatingState(index + 1)
            }
        }

        viewBinding.ivClose.setOnClickListener {
            onCancelListener?.invoke()
            dismiss()
        }

        viewBinding.btnSubmit.setOnClickListener {
            onRatingSubmitListener?.invoke(selectedRating)
            dismiss()
        }

        updateRatingState(0)
    }

    fun setAppName(name: String): RatingDialog {
        this.appName = name
        if (selectedRating == 0) {
            val displayAppName = name.ifEmpty { context.getString(R.string.app_name) }
            viewBinding.tvTitle.text = context.getString(R.string.rating_title_default, displayAppName)
        }
        return this
    }

    fun setOnRatingSubmitListener(listener: (rating: Int) -> Unit): RatingDialog {
        this.onRatingSubmitListener = listener
        return this
    }

    fun setOnCancelListener(listener: () -> Unit): RatingDialog {
        this.onCancelListener = listener
        return this
    }

    fun updateRatingState(rating: Int) {
        selectedRating = rating

        val stars = listOf(
            viewBinding.ivStar1,
            viewBinding.ivStar2,
            viewBinding.ivStar3,
            viewBinding.ivStar4,
            viewBinding.ivStar5
        )

        stars.forEachIndexed { index, imageView ->
            if (index < rating) {
                imageView.setImageResource(R.drawable.ic_star_on)
            } else {
                imageView.setImageResource(R.drawable.ic_star_off)
            }
        }

        val displayState = RatingHelper.getRatingState(rating)
        viewBinding.ivEmotion.setImageResource(displayState.emotionDrawableRes)
        viewBinding.tvDescription.setText(displayState.descriptionRes)

        if (displayState.titleRes != null) {
            viewBinding.tvTitle.setText(displayState.titleRes)
        } else {
            val displayAppName = appName ?: context.getString(R.string.app_name)
            viewBinding.tvTitle.text = context.getString(R.string.rating_title_default, displayAppName)
        }
    }
}
