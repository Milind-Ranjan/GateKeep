package com.example.gatekeep

import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import com.example.gatekeep.service.AppMonitoringService
import com.example.gatekeep.HomeActivity
import com.google.android.material.button.MaterialButton

class PermissionsActivity : AppCompatActivity() {
    // UI Components
    private lateinit var cardUsageStats: CardView
    private lateinit var cardAccessibility: CardView
    private lateinit var btnUsageStats: MaterialButton
    private lateinit var btnAccessibility: MaterialButton
    private lateinit var tvUsageStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvPermissionsTitle: TextView
    private lateinit var tvPermissionsSubtitle: TextView
    private lateinit var permissionsBackground: View
    
    // Preferences
    private lateinit var preferences: SharedPreferences
    
    // Handler for permission checking
    private val handler = Handler(Looper.getMainLooper())
    private val permissionCheckRunnable = object : Runnable {
        override fun run() {
            checkPermissions()
            handler.postDelayed(this, 1000) // Check every second
        }
    }
    
    companion object {
        private const val PREF_NAME = "GateKeepPrefs"
        private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
        // Enable hardware acceleration for smoother animations
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
            // We are only checking here if permissions are granted - HomeActivity already checked,
            // so we assume permissions are needed if we reach this point
        setContentView(R.layout.activity_permissions)
        
        // Initialize UI components
        initViews()
        
        // Start with initial permissions check
        checkPermissions()
        
        // Set up button click listeners
        setupClickListeners()
        
        // Animate the content on entry
        animateEntrance()
        } catch (e: Exception) {
            android.util.Log.e("PermissionsActivity", "Error in onCreate: ${e.message}", e)
            // If there's a critical error, go to HomeActivity
            try {
                setPermissionsGranted(true) // Temporarily set to true to avoid loops
                navigateToMainActivity()
            } catch (e2: Exception) {
                finish()
            }
        }
    }
    
    private fun arePermissionsGranted(): Boolean {
        return preferences.getBoolean(KEY_PERMISSIONS_GRANTED, false)
    }
    
    private fun setPermissionsGranted(granted: Boolean) {
        preferences.edit().putBoolean(KEY_PERMISSIONS_GRANTED, granted).apply()
    }
    
    private fun navigateToMainActivity() {
        try {
            // Create intent for HomeActivity with proper flags to prevent loops
            val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        
        // Simple fade animation
        val fadeOutAnim = ObjectAnimator.ofFloat(
            findViewById<View>(android.R.id.content), View.ALPHA, 1f, 0f
        ).apply {
            duration = 250
            doOnEnd {
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }
        fadeOutAnim.start()
        } catch (e: Exception) {
            // If animation fails, try direct navigation
            try {
                val intent = Intent(this, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } catch (e2: Exception) {
                // Last resort, try to finish this activity
                finish()
            }
        }
    }
    
    private fun initViews() {
        permissionsBackground = findViewById(R.id.permissionsBackground)
        cardUsageStats = findViewById(R.id.cardUsageStats)
        cardAccessibility = findViewById(R.id.cardAccessibility)
        btnUsageStats = findViewById(R.id.btnUsageStats)
        btnAccessibility = findViewById(R.id.btnAccessibility)
        tvUsageStatus = findViewById(R.id.tvUsageStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)
        tvPermissionsTitle = findViewById(R.id.tvPermissionsTitle)
        tvPermissionsSubtitle = findViewById(R.id.tvPermissionsSubtitle)
        
        // Set initial states for animations
        tvPermissionsTitle.alpha = 0f
        tvPermissionsTitle.translationY = 20f
        
        tvPermissionsSubtitle.alpha = 0f
        tvPermissionsSubtitle.translationY = 15f
        
        cardUsageStats.alpha = 0f
        cardUsageStats.translationY = 30f
        
        cardAccessibility.alpha = 0f
        cardAccessibility.translationY = 30f
        
        // Flat design - remove elevation
        ViewCompat.setElevation(cardUsageStats, 0f)
        ViewCompat.setElevation(cardAccessibility, 0f)
    }
    
    private fun setupClickListeners() {
        btnUsageStats.setOnClickListener {
            // Button click animation
            animateButtonClick(btnUsageStats) {
                requestUsageStatsPermission()
            }
        }
        
        btnAccessibility.setOnClickListener {
            // Button click animation
            animateButtonClick(btnAccessibility) {
                requestAccessibilityPermission()
            }
        }
    }
    
    private fun animateButtonClick(button: MaterialButton, onClick: () -> Unit) {
        val scaleDown = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.97f)
        val scaleDownY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.97f)
        
        val buttonClickAnim = ObjectAnimator.ofPropertyValuesHolder(
            button, scaleDown, scaleDownY
        ).apply {
            duration = 120
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = 1
            interpolator = DecelerateInterpolator(1.2f)
            doOnEnd {
                onClick()
            }
            start()
        }
    }
    
    private fun animateEntrance() {
        // Start with a short delay for smoother entrance
        Handler(Looper.getMainLooper()).postDelayed({
            // Animate title with a fade in and slight slide up
            ObjectAnimator.ofFloat(tvPermissionsTitle, View.ALPHA, 0f, 1f).apply {
                duration = 800
                interpolator = DecelerateInterpolator(1.3f)
                start()
            }
            
            ObjectAnimator.ofFloat(tvPermissionsTitle, View.TRANSLATION_Y, 20f, 0f).apply {
                duration = 900
                interpolator = DecelerateInterpolator(1.4f)
                start()
            }
            
            // Animate subtitle with a slight delay
            Handler(Looper.getMainLooper()).postDelayed({
                ObjectAnimator.ofFloat(tvPermissionsSubtitle, View.ALPHA, 0f, 1f).apply {
                    duration = 700
                    interpolator = DecelerateInterpolator(1.2f)
                    start()
                }
                
                ObjectAnimator.ofFloat(tvPermissionsSubtitle, View.TRANSLATION_Y, 15f, 0f).apply {
                    duration = 800
                    interpolator = DecelerateInterpolator(1.3f)
                    start()
                }
                
                // Animate first card with a sleek entrance
                Handler(Looper.getMainLooper()).postDelayed({
                    animateCard(cardUsageStats, 0)
                    
                    // Animate second card with delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        animateCard(cardAccessibility, 0)
                    }, 150)
                }, 150)
            }, 150)
        }, 100)
    }
    
    private fun animateCard(card: CardView, delay: Long) {
        val fadeIn = ObjectAnimator.ofFloat(card, View.ALPHA, 0f, 1f)
        val slideUp = ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, 30f, 0f)
        
        AnimatorSet().apply {
            playTogether(fadeIn, slideUp)
            duration = 800
            interpolator = DecelerateInterpolator(1.3f)
            startDelay = delay
            start()
        }
    }
    
    private fun checkPermissions() {
        val hasUsageAccess = hasUsageStatsPermission()
        val hasAccessibility = isAccessibilityServiceEnabled()
        
        // Update UI based on permission status
        updateUsageStatsUI(hasUsageAccess)
        updateAccessibilityUI(hasAccessibility)
        
        // If both permissions are granted, navigate directly to HomeActivity
        if (hasUsageAccess && hasAccessibility) {
            // Remove the permission check callback to avoid multiple launches
            handler.removeCallbacks(permissionCheckRunnable)
            
            // Set permissions as granted in preferences
            setPermissionsGranted(true)
            
            // Navigate directly to HomeActivity
            navigateToMainActivity()
        }
    }
    
    private fun updateUsageStatsUI(granted: Boolean) {
        if (granted) {
            tvUsageStatus.text = "✓"
            tvUsageStatus.setTextColor(getColor(R.color.teal_200))
            btnUsageStats.text = "Permission Granted"
            btnUsageStats.alpha = 0.7f
        } else {
            tvUsageStatus.text = "⦿"
            tvUsageStatus.setTextColor(getColor(android.R.color.holo_red_light))
            btnUsageStats.text = "Grant Permission"
            btnUsageStats.alpha = 1f
        }
    }
    
    private fun updateAccessibilityUI(granted: Boolean) {
        if (granted) {
            tvAccessibilityStatus.text = "✓"
            tvAccessibilityStatus.setTextColor(getColor(R.color.teal_200))
            btnAccessibility.text = "Permission Granted"
            btnAccessibility.alpha = 0.7f
        } else {
            tvAccessibilityStatus.text = "⦿"
            tvAccessibilityStatus.setTextColor(getColor(android.R.color.holo_red_light))
            btnAccessibility.text = "Grant Permission"
            btnAccessibility.alpha = 1f
        }
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        try {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        try {
        // First check if service is registered in accessibility services
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        val packageName = packageName
        var isRegistered = false
        
        for (service in enabledServices) {
            val serviceInfo = service.resolveInfo.serviceInfo
            if (serviceInfo.packageName == packageName && 
                serviceInfo.name == "com.example.gatekeep.service.AppMonitoringService") {
                isRegistered = true
                break
            }
        }
        
        // Also check if service is actually running
            val isRunning = try {
                AppMonitoringService.isServiceRunning
            } catch (e: Exception) {
                // If there's an error, default to the registration check
                isRegistered
            }
        
        return isRegistered && isRunning
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
    
    private fun requestAccessibilityPermission() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    
    override fun onResume() {
        super.onResume()
        // Start checking permissions periodically
        handler.post(permissionCheckRunnable)
    }
    
    override fun onPause() {
        super.onPause()
        // Stop checking permissions when activity is not visible
        handler.removeCallbacks(permissionCheckRunnable)
    }
} 