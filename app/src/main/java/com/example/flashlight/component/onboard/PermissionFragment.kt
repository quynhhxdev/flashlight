package com.example.flashlight.component.onboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.MainActivity
import com.example.flashlight.utils.SpManager
import com.kt.flashlight.R
import com.kt.flashlight.databinding.FragmentPermissionBinding

class PermissionFragment : BaseFragment<FragmentPermissionBinding>() {

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewBinding.switchCamera.isChecked = isGranted
            updateContinueButtonState(isGranted)
            if (isGranted) {
                Toast.makeText(context, "Camera permission granted", Toast.LENGTH_SHORT).show()
            }
        }

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentPermissionBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        checkPermissionState()

        viewBinding.switchCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isCameraPermissionGranted()) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    updateContinueButtonState(true)
                }
            } else {
                updateContinueButtonState(false)
            }
        }

        viewBinding.cardCameraPermission.setOnClickListener {
            if (!isCameraPermissionGranted()) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                val newState = !viewBinding.switchCamera.isChecked
                viewBinding.switchCamera.isChecked = newState
            }
        }

        viewBinding.btnContinue.setOnClickListener {
            if (isCameraPermissionGranted()) {
                navigateToMain()
            } else {
                Toast.makeText(context, "Please grant camera permission to continue", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionState()
    }

    private fun checkPermissionState() {
        val isGranted = isCameraPermissionGranted()
        viewBinding.switchCamera.isChecked = isGranted
        updateContinueButtonState(isGranted)
    }

    private fun updateContinueButtonState(isGranted: Boolean) {
        val btn = viewBinding.btnContinue
        btn.isEnabled = isGranted
        if (isGranted) {
            btn.backgroundTintList = null
            btn.setBackgroundResource(R.drawable.bg_yelllow_btn)
            btn.alpha = 1.0f
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.bg_btn_continue))
            btn.alpha = 0.5f
        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun navigateToMain() {
        activity?.let { act ->
            SpManager.getInstance(act).setOnboardingCompleted(true)
            val intent = Intent(act, MainActivity::class.java)
            startActivity(intent)
            act.finish()
        }
    }

    companion object {
        fun newInstance() = PermissionFragment()
    }
}
