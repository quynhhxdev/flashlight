package com.example.flashlight.component.main.fagment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.FlashUiState
import com.example.flashlight.data.model.FlashMode
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.kt.flashlight.R
import com.kt.flashlight.databinding.FragmentFlashlightBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FlashlightFragment : BaseFragment<FragmentFlashlightBinding>() {

    private val viewModel: FlashlightViewModel by viewModels()

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.toggleFlashlight()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.camera_permission_required_flash,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentFlashlightBinding.inflate(layoutInflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NativeAdsUtils.bindHomeNative(
            activity = requireActivity(),
            lifecycleOwner = viewLifecycleOwner,
            nativeContentView = viewBinding.nativeAdContainer,
            shimmerContainerNative = viewBinding.shimmerContainerNative.root
        )
        setupListeners()
        observeUiState()
    }

    private fun setupListeners() = with(viewBinding) {
        btnPower.setOnClickListener {
            checkCameraPermission()
        }

        btnModeFlash.setOnClickListener {
            viewModel.setFlashMode(FlashMode.FLASH)
        }

        btnModeSos.setOnClickListener {
            viewModel.setFlashMode(FlashMode.SOS)
        }

        btnModeDj.setOnClickListener {
            viewModel.setFlashMode(FlashMode.DJ)
        }
    }

    private fun checkCameraPermission() {
        val context = context ?: return
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.toggleFlashlight()
            }
            else -> {
                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderPowerButton(state)
                    renderModeButtons(state)

                    state.error?.let { message ->
                        Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun renderPowerButton(state: FlashUiState) = with(viewBinding) {
        tvPowerStatus.text = if (state.isEnabled) getString(R.string.power_on) else getString(R.string.power_off)

        val iconRes = when (state.selectedMode) {
            FlashMode.FLASH -> R.drawable.ic_flash_off
            FlashMode.SOS -> R.drawable.ic_sos
            FlashMode.DJ -> R.drawable.ic_dj_mode
        }

        ivPowerIcon.setImageResource(iconRes)

        powerBackground.setBackgroundResource(
            if (state.isEnabled) {
                R.drawable.bg_flash_light
            } else {
                R.color.power_off
            }
        )

        val contentColor = ContextCompat.getColor(
            requireContext(),
            if (state.isEnabled) {
                R.color.white
            } else {
                R.color.bg_no_select_btn
            }
        )

        ivPowerIcon.imageTintList = ColorStateList.valueOf(contentColor)
        tvPowerStatus.setTextColor(contentColor)
    }

    private fun renderModeButtons(state: FlashUiState) = with(viewBinding) {
        renderMode(
            selected = state.selectedMode == FlashMode.FLASH,
            button = btnModeFlash,
            icon = ivModeFlash,
            title = tvModeFlash
        )

        renderMode(
            selected = state.selectedMode == FlashMode.SOS,
            button = btnModeSos,
            icon = ivModeSos,
            title = tvModeSos
        )

        renderMode(
            selected = state.selectedMode == FlashMode.DJ,
            button = btnModeDj,
            icon = ivModeDj,
            title = tvModeDj
        )
    }

    private fun renderMode(
        selected: Boolean,
        button: View,
        icon: ImageView,
        title: TextView
    ) {
        button.setBackgroundResource(
            if (selected) {
                R.drawable.bg_btn_flash
            } else {
                R.drawable.bg_no_internet_card
            }
        )

        val color = ContextCompat.getColor(
            requireContext(),
            if (selected) {
                R.color.bg_yellow_switch
            } else {
                R.color.bg_no_select_btn
            }
        )

        icon.imageTintList = ColorStateList.valueOf(color)
        title.setTextColor(color)
    }

    companion object {
        fun newInstance() = FlashlightFragment()
    }
}
