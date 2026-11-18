package com.example.flashlight.base.activity

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.example.flashlight.data.model.Language
import com.example.flashlight.utils.SpManager

abstract class BaseActivity<V : ViewBinding> : AppCompatActivity() {

    private var _binding: V? = null
    val binding: V
        get() = _binding ?: throw IllegalStateException("ViewBinding cannot be accessed before onCreate or after onDestroy")

    // protected lateinit var currentLanguage: Language

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // 1. Setup Edge-to-Edge UI for Multi-Activity architecture
        setupEdgeToEdge()

        // 2. Inflate ViewBinding automatically
        _binding = provideViewBinding()
        setContentView(binding.root)

        // 3. Handle Back press via OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBack()
            }
        })

        // 4. Lifecycle hooks
        initViews()
        initData()
        initObserver()
    }

    /**
     * Configure Edge-to-Edge immersive window insets
     */
    protected open fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }


    /**
     * Default back action: finish current activity
     */
    open fun onBack() {
        finish()
    }

    abstract fun provideViewBinding(): V

    open fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

            val bottomSpacing = view.resources.getDimensionPixelSize(com.kt.flashlight.R.dimen._4dp)
            view.updatePadding(top = insets.top, bottom = insets.bottom + bottomSpacing)

            WindowInsetsCompat.CONSUMED
        }
    }
    open fun initData() {}
    open fun initObserver() {}

    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    fun addFragment(id: Int, fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction().add(id, fragment).commit()
    }

    fun replaceFragment(id: Int, fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction().replace(id, fragment).commit()
    }

    fun showLoading() {}
    fun hideLoading() {}

    fun showInterAd(action: () -> Unit) {
        action.invoke()
    }

    fun showRewardAd(action: () -> Unit) {
        action.invoke()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null // Prevent memory leak
    }
}
