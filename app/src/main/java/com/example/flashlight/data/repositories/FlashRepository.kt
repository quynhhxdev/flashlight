package com.example.flashlight.data.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager


@SuppressLint("ServiceCast")
class FlashRepository(context: Context) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val cameraId: String? by lazy {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    val isFlashAvailable: Boolean
        get() = cameraId != null

    fun setFlash(enabled: Boolean) {
        val id = cameraId ?: throw IllegalStateException(
            "Thiết bị không hỗ trợ đèn flash"
        )

        cameraManager.setTorchMode(id, enabled)
    }
}
