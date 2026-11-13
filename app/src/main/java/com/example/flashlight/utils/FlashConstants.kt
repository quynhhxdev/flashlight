package com.example.flashlight.utils

object FlashConstants {
    // SOS timing (milliseconds)
    const val SOS_SHORT_ON = 200L
    const val SOS_SHORT_OFF = 200L
    const val SOS_LONG_ON = 600L
    const val SOS_LONG_OFF = 200L
    const val SOS_PAUSE = 400L
    const val SOS_CYCLE_PAUSE = 1200L

    // DJ timing
    const val DJ_ON_DURATION = 250L
    const val DJ_OFF_DURATION = 250L

    // Error messages
    const val ERROR_NO_FLASH =
        "Thiết bị không hỗ trợ đèn flash"

    const val ERROR_FLASH_FAILED =
        "Không thể điều khiển đèn flash"
}