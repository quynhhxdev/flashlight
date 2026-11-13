package com.example.flashlight.component.main.dialog

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import com.kt.flashlight.R
import com.kt.flashlight.databinding.DialogRatingCartBinding
import com.example.flashlight.base.dialog.BaseDialog


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

        when (rating) {
            1 -> {
                viewBinding.ivEmotion.setImageResource(R.drawable.ic_1star)
                viewBinding.tvTitle.setText(R.string.rating_title_1)
                viewBinding.tvDescription.setText(R.string.rating_desc_1)
            }
            2 -> {
                viewBinding.ivEmotion.setImageResource(R.drawable.ic_2star)
                viewBinding.tvTitle.setText(R.string.rating_title_2)
                viewBinding.tvDescription.setText(R.string.rating_desc_2)
            }
            3 -> {
                viewBinding.ivEmotion.setImageResource(R.drawable.ic_3star)
                viewBinding.tvTitle.setText(R.string.rating_title_3)
                viewBinding.tvDescription.setText(R.string.rating_desc_3)
            }
            4 -> {
                viewBinding.ivEmotion.setImageResource(R.drawable.ic_4star)
                viewBinding.tvTitle.setText(R.string.rating_title_4)
                viewBinding.tvDescription.setText(R.string.rating_desc_4)
            }
            5 -> {
                viewBinding.ivEmotion.setImageResource(R.drawable.ic_5star)
                viewBinding.tvTitle.setText(R.string.rating_title_5)
                viewBinding.tvDescription.setText(R.string.rating_desc_5)
            }
            else -> {
                viewBinding.ivEmotion.setImageResource(R.drawable.ic_like_name_app)
                val displayAppName = appName ?: context.getString(R.string.app_name)
                viewBinding.tvTitle.text = context.getString(R.string.rating_title_default, displayAppName)
                viewBinding.tvDescription.setText(R.string.rating_desc_default)
            }
        }
    }
}
