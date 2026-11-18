package com.example.flashlight.component.onboard

import androidx.lifecycle.ViewModel
import com.kt.flashlight.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class OnboardingNextAction {
    GO_TO_NEXT_PAGE,
    FINISH
}

/**
 * Phân biệt loại trang trong ViewPager Onboarding.
 */
sealed class OnboardingPage {
    data class Normal(val data: OnboardingData) : OnboardingPage()
    data class NativeFullscreen(val placementKey: String) : OnboardingPage()
}

data class OnboardingUiState(
    val pages: List<OnboardingPage> = emptyList(),
    val currentPage: Int = 0,
    // Trạng thái countdown cho NativeFullscreen page 1 (index 1)
    val nativeFullscreen1CountdownDone: Boolean = false,
    // Trạng thái countdown cho NativeFullscreen page 2 (index 3)
    val nativeFullscreen2CountdownDone: Boolean = false,
)

// Giữ tương thích ngược với các nơi còn dùng OnboardingData
data class OnboardingData(
    val imageRes: Int,
    val title: String,
    val description: String
)

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            pages = listOf(
                // Index 0: Onboarding page 1
                OnboardingPage.Normal(
                    OnboardingData(
                        imageRes = R.drawable.bg_onb1,
                        title = "Bright Flashlight",
                        description = "Turn your phone into a powerful\nflashlight anytime."
                    )
                ),
                // Index 1: Native Fullscreen 1 (giữa trang 1 và 2)
                OnboardingPage.NativeFullscreen(placementKey = "native_fullscreen"),
                // Index 2: Onboarding page 2
                OnboardingPage.Normal(
                    OnboardingData(
                        imageRes = R.drawable.bg_onb2,
                        title = "SOS & DJ Mode",
                        description = "Use SOS mode for emergencies for \nDJ mode for parties."
                    )
                ),
                // Index 3: Onboarding page 3
                OnboardingPage.Normal(
                    OnboardingData(
                        imageRes = R.drawable.bg_onb3,
                        title = "LED Banner",
                        description = "Display scrolling LED text with customizable\ncolors and effects."
                    )
                ),
                // Index 4: Native Fullscreen 2 (giữa trang 3 và 4)
                OnboardingPage.NativeFullscreen(placementKey = "native_fullscreen_2"),
                // Index 5: Onboarding page 4
                OnboardingPage.Normal(
                    OnboardingData(
                        imageRes = R.drawable.bg_onb4,
                        title = "Screen Light",
                        description = "Use your display as a bright colored\nlight source"
                    )
                )
            )
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun setCurrentPage(position: Int) {
        _uiState.update { it.copy(currentPage = position) }
    }

    /**
     * Đánh dấu countdown của NativeFullscreen page đã hoàn thành (chỉ set 1 lần).
     * @param pageIndex vị trí page trong ViewPager (1 hoặc 4).
     */
    fun markCountdownDone(pageIndex: Int) {
        _uiState.update {
            when (pageIndex) {
                1 -> it.copy(nativeFullscreen1CountdownDone = true)
                4 -> it.copy(nativeFullscreen2CountdownDone = true)
                else -> it
            }
        }
    }

    /**
     * Kiểm tra countdown của page tại vị trí [pageIndex] đã hoàn thành chưa.
     */
    fun isCountdownDone(pageIndex: Int): Boolean {
        return when (pageIndex) {
            1 -> _uiState.value.nativeFullscreen1CountdownDone
            4 -> _uiState.value.nativeFullscreen2CountdownDone
            else -> false
        }
    }

    /**
     * Xử lý khi người dùng nhấn Next trên trang Onboarding thông thường.
     */
    fun onNextClicked(currentItem: Int): OnboardingNextAction {
        val pages = _uiState.value.pages
        return if (currentItem < pages.size - 1) {
            OnboardingNextAction.GO_TO_NEXT_PAGE
        } else {
            OnboardingNextAction.FINISH
        }
    }

    /**
     * Kiểm tra trang tại [position] có phải NativeFullscreen không.
     */
    fun isNativeFullscreenPage(position: Int): Boolean {
        val pages = _uiState.value.pages
        return position in pages.indices && pages[position] is OnboardingPage.NativeFullscreen
    }

    /**
     * Lấy placement key của NativeFullscreen page tại [position].
     */
    fun getNativeFullscreenPlacement(position: Int): String? {
        val page = _uiState.value.pages.getOrNull(position)
        return (page as? OnboardingPage.NativeFullscreen)?.placementKey
    }

    fun getFinishTargetScreen(isConnected: Boolean): Int {
        return if (isConnected) {
            IntroActivity.SCREEN_PERMISSION
        } else {
            IntroActivity.SCREEN_NO_INTERNET
        }
    }

    /**
     * Danh sách pages để truyền vào adapter.
     */
    fun getPages(): List<OnboardingPage> = _uiState.value.pages
}
