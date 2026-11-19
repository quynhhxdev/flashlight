package com.example.flashlight.component.onboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.flashlight.base.fragment.BaseFragment
import com.example.flashlight.component.main.MainActivity
import com.example.flashlight.utils.ads.NativeAdsUtils
import com.example.flashlight.utils.ads.manager.FirebaseConfigManager
import com.example.flashlight.utils.ads.resolver.AdsIdResolver
import com.kt.flashlight.R
import com.kt.flashlight.databinding.FragmentPermissionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PermissionFragment : BaseFragment<FragmentPermissionBinding>() {

    private val viewModel: PermissionViewModel by viewModels()

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.updateCameraPermission(isGranted)
            if (isGranted) {
                Log.d("Permission", getString(R.string.permission_camera_granted))
            }
        }

    override fun provideViewBinding(container: ViewGroup?) =
        FragmentPermissionBinding.inflate(layoutInflater, container, false)

    override fun initViews() {
        checkPermissionState()
        NativeAdsUtils.preloadHomeNative(requireContext())
        FirebaseConfigManager.instance().doAfterFetch {
            val resolved = AdsIdResolver.resolve("native_permission")
            if (resolved == null) {
                viewBinding.nativeAdContainer.isGone = true
            } else {
                viewBinding.nativeAdContainer.isVisible = true
                if (NativeAdsUtils.nativePermission != null) {
                    NativeAdsUtils.nativePermission?.let { nativeAdHelper ->
                        nativeAdHelper.updateActivity(requireActivity(), viewLifecycleOwner)
                        nativeAdHelper.setNativeContentView(viewBinding.nativeAdContent)
                        nativeAdHelper.setShimmerLayoutView(viewBinding.shimmerContainerNative.root)
                    }
                } else {
                    NativeAdsUtils.loadAndShowNative(
                        activity = requireActivity(),
                        lifecycleOwner = viewLifecycleOwner,
                        placement = "native_permission",
                        nativeContentView = viewBinding.nativeAdContent,
                        shimmerContainerNative = viewBinding.shimmerContainerNative.root
                    )
                }
            }
        }

        viewBinding.switchCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isCameraPermissionGranted()) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    viewModel.updateCameraPermission(true)
                }
            } else {
                viewModel.updateCameraPermission(false)
            }
        }

        viewBinding.cardCameraPermission.setOnClickListener {
            if (!isCameraPermissionGranted()) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                val newState = !viewBinding.switchCamera.isChecked
                viewModel.updateCameraPermission(newState)
            }
        }

        viewBinding.btnContinue.setOnClickListener {
            if (isCameraPermissionGranted()) {
                navigateToMain()
            } else {
                Toast.makeText(context, R.string.permission_camera_needed, Toast.LENGTH_SHORT).show()
            }
        }

        observeUiState()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    viewBinding.switchCamera.isChecked = state.isCameraGranted
                    updateContinueButtonState(state.isContinueEnabled)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionState()
    }

    private fun checkPermissionState() {
        val isGranted = isCameraPermissionGranted()
        viewModel.updateCameraPermission(isGranted)
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
            viewModel.completePermissionAndMarkOnboarding()
            val intent = Intent(act, MainActivity::class.java)
            startActivity(intent)
            act.finish()
        }
    }

    companion object {
        fun newInstance() = PermissionFragment()
    }
}
