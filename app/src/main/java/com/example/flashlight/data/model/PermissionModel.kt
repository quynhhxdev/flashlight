package com.example.flashlight.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.example.flashlight.utils.Permission

@Keep
data class PermissionModel(
    @DrawableRes
    val iconRes: Int,
    @StringRes
    val title: Int,
    @StringRes
    val content: Int,
    val permissions: List<Permission>,
    var isAllowed: Boolean = false,
)