package com.example.flashlight.component.main.dialog

import com.kt.flashlight.R

data class RatingDisplayState(
    val emotionDrawableRes: Int,
    val titleRes: Int?,
    val descriptionRes: Int
)

object RatingHelper {

    fun getRatingState(rating: Int): RatingDisplayState {
        return when (rating) {
            1 -> RatingDisplayState(
                emotionDrawableRes = R.drawable.ic_1star,
                titleRes = R.string.rating_title_1,
                descriptionRes = R.string.rating_desc_1
            )
            2 -> RatingDisplayState(
                emotionDrawableRes = R.drawable.ic_2star,
                titleRes = R.string.rating_title_2,
                descriptionRes = R.string.rating_desc_2
            )
            3 -> RatingDisplayState(
                emotionDrawableRes = R.drawable.ic_3star,
                titleRes = R.string.rating_title_3,
                descriptionRes = R.string.rating_desc_3
            )
            4 -> RatingDisplayState(
                emotionDrawableRes = R.drawable.ic_4star,
                titleRes = R.string.rating_title_4,
                descriptionRes = R.string.rating_desc_4
            )
            5 -> RatingDisplayState(
                emotionDrawableRes = R.drawable.ic_5star,
                titleRes = R.string.rating_title_5,
                descriptionRes = R.string.rating_desc_5
            )
            else -> RatingDisplayState(
                emotionDrawableRes = R.drawable.ic_like_name_app,
                titleRes = null,
                descriptionRes = R.string.rating_desc_default
            )
        }
    }
}
