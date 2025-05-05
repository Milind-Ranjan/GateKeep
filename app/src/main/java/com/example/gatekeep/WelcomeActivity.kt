package com.example.gatekeep

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import com.example.gatekeep.HomeActivity
import com.example.gatekeep.service.AppMonitoringService

class WelcomeActivity : AppCompatActivity() {
    // UI Components
    private lateinit var logoFrame: FrameLayout
    private lateinit var logoImage: ImageView
    private lateinit var logoForeground: ImageView
    private lateinit var appNameText: TextView
    private lateinit var taglineText: TextView
    private lateinit var btnGetStarted: Button
    private lateinit var logoContainer: ConstraintLayout
    private lateinit var welcomeBackground: View
    
    // Preferences
    private lateinit var preferences: SharedPreferences
    
    // Animation durations - refined for premium feel
    private val logoAnimDuration = 1500L
    private val textAnimDuration = 700L
    private val buttonAnimDuration = 650L
    
    companion object {
        private const val PREF_NAME = "GateKeepPrefs"
        private const val KEY_FIRST_TIME = "first_time_launch"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Check if this is the first launch
            preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            
            // We are only checking here - HomeActivity already checked, so we assume
            // we are in the welcome flow if we reach this point
            setContentView(R.layout.activity_welcome)
            
            // Initialize UI components
            initViews()
            
            // Start entrance animations
            startAnimations()
            
            // Set up the Get Started button click listener
            btnGetStarted.setOnClickListener {
                // Mark as not first launch anymore - this is critical
                setFirstTimeLaunch(false)
                
                // Premium button click effect
                animateButtonClick()
            }
        } catch (e: Exception) {
            android.util.Log.e("WelcomeActivity", "Error in onCreate: ${e.message}", e)
            // If there's an error, mark first time as false to avoid loops
            setFirstTimeLaunch(false)
            navigateToNextScreen()
        }
    }
    
    private fun animateButtonClick() {
        // Subtle scale down with slight color darkening
        val scaleDown = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.97f)
        val scaleDownY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.97f)
        
        val buttonClickAnim = ObjectAnimator.ofPropertyValuesHolder(
            btnGetStarted, scaleDown, scaleDownY
        ).apply {
            duration = 80
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = 1
            doOnEnd {
                // Create sophisticated exit animations
                performExitAnimation()
            }
            start()
        }
    }
    
    private fun performExitAnimation() {
        // Create coordinated premium exit animations
        val fadeOutElements = AnimatorSet()
        
        // Logo fade and subtle scale out
        val logoFadeOut = ObjectAnimator.ofFloat(logoContainer, View.ALPHA, 1f, 0f)
        logoFadeOut.duration = 450
        
        val logoScaleX = ObjectAnimator.ofFloat(logoContainer, View.SCALE_X, 1f, 0.96f)
        logoScaleX.duration = 450
        
        val logoScaleY = ObjectAnimator.ofFloat(logoContainer, View.SCALE_Y, 1f, 0.96f) 
        logoScaleY.duration = 450
        
        // Staggered text elements fade out
        val titleFadeOut = ObjectAnimator.ofFloat(appNameText, View.ALPHA, 1f, 0f)
        titleFadeOut.duration = 380
        titleFadeOut.startDelay = 50
        
        val taglineFadeOut = ObjectAnimator.ofFloat(taglineText, View.ALPHA, 1f, 0f)
        taglineFadeOut.duration = 350
        taglineFadeOut.startDelay = 80
        
        // Button elegant fade/scale out
        val buttonFadeOut = ObjectAnimator.ofFloat(btnGetStarted, View.ALPHA, 1f, 0f)
        buttonFadeOut.duration = 350
        
        val buttonScaleX = ObjectAnimator.ofFloat(btnGetStarted, View.SCALE_X, 1f, 0.97f)
        buttonScaleX.duration = 450
        
        val buttonScaleY = ObjectAnimator.ofFloat(btnGetStarted, View.SCALE_Y, 1f, 0.97f)
        buttonScaleY.duration = 450
        
        // Play all fade out animations with careful orchestration
        fadeOutElements.playTogether(
            logoFadeOut, logoScaleX, logoScaleY,
            titleFadeOut, taglineFadeOut,
            buttonFadeOut, buttonScaleX, buttonScaleY
        )
        
        fadeOutElements.interpolator = DecelerateInterpolator(1.2f)
        
        fadeOutElements.doOnEnd {
            // Start PermissionsActivity when animation completes
            val intent = Intent(this, PermissionsActivity::class.java)
            startActivity(intent)
            
            // Apply fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            
            // Finish this activity
            finish()
        }
        
        fadeOutElements.start()
    }
    
    private fun isFirstTimeLaunch(): Boolean {
        return preferences.getBoolean(KEY_FIRST_TIME, true)
    }
    
    private fun setFirstTimeLaunch(isFirstTime: Boolean) {
        try {
            preferences.edit().putBoolean(KEY_FIRST_TIME, isFirstTime).apply()
        } catch (e: Exception) {
            // Add fallback for any potential issues
            try {
                preferences.edit().putBoolean(KEY_FIRST_TIME, isFirstTime).commit()
            } catch (e2: Exception) {
                // If both attempts fail, log the error
                android.util.Log.e("WelcomeActivity", "Failed to set first time launch flag: ${e2.message}")
            }
        }
    }
    
    private fun navigateToNextScreen() {
        try {
            // Check if permissions have been granted
            val permissionsActivity = Intent(this, PermissionsActivity::class.java)
            val homeActivity = Intent(this, HomeActivity::class.java)
            
            // Check if app has necessary permissions
            val hasAllPermissions = hasAllRequiredPermissions()
            
            // Navigate to appropriate screen
            startActivity(if (hasAllPermissions) homeActivity else permissionsActivity)
            finish()
        } catch (e: Exception) {
            // If there's an error, try to start PermissionsActivity as a fallback
            try {
                startActivity(Intent(this, PermissionsActivity::class.java))
                finish()
            } catch (e2: Exception) {
                // If that also fails, just finish this activity
                finish()
            }
        }
    }
    
    private fun hasAllRequiredPermissions(): Boolean {
        try {
            // Check for Usage Stats permission
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            val hasUsageStats = mode == android.app.AppOpsManager.MODE_ALLOWED
            
            // Check for Accessibility Service
            val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            
            var hasAccessibility = false
            for (service in enabledServices) {
                val serviceInfo = service.resolveInfo.serviceInfo
                if (serviceInfo.packageName == packageName && 
                    serviceInfo.name == "com.example.gatekeep.service.AppMonitoringService") {
                    hasAccessibility = true
                    break
                }
            }
            
            // We also need to check if the service is running, but handle potential errors
            val isServiceRunning = try {
                AppMonitoringService.isServiceRunning
            } catch (e: Exception) {
                // If we can't access the service class static property, default to the accessibility check
                hasAccessibility
            }
            
            return hasUsageStats && hasAccessibility && isServiceRunning
        } catch (e: Exception) {
            // If any exceptions occur, return false to be safe
            return false
        }
    }
    
    private fun initViews() {
        welcomeBackground = findViewById(R.id.welcomeBackground)
        logoFrame = findViewById(R.id.logoFrame)
        logoImage = findViewById(R.id.logoImage)
        logoForeground = findViewById(R.id.logoForeground)
        appNameText = findViewById(R.id.appNameText)
        taglineText = findViewById(R.id.taglineText)
        btnGetStarted = findViewById(R.id.btnGetStarted)
        logoContainer = findViewById(R.id.logoContainer)
        
        // Set initial states for animation
        logoContainer.alpha = 0f
        logoContainer.scaleX = 0.92f
        logoContainer.scaleY = 0.92f
        logoContainer.translationY = 20f
        
        // Set initial states for logo layers
        logoImage.alpha = 0f
        logoForeground.alpha = 0f
        
        // Disable button elevation for flat design
        ViewCompat.setElevation(btnGetStarted, 0f)
    }
    
    private fun startAnimations() {
        // Initial slight delay for premium experience
        Handler(Looper.getMainLooper()).postDelayed({
            // Animation for logo container with refined timing
            val logoContainerFadeIn = ObjectAnimator.ofFloat(logoContainer, View.ALPHA, 0f, 1f)
            logoContainerFadeIn.duration = logoAnimDuration
            
            val logoContainerScaleX = ObjectAnimator.ofFloat(logoContainer, View.SCALE_X, 0.92f, 1f)
            logoContainerScaleX.duration = logoAnimDuration + 100
            
            val logoContainerScaleY = ObjectAnimator.ofFloat(logoContainer, View.SCALE_Y, 0.92f, 1f)
            logoContainerScaleY.duration = logoAnimDuration + 100
            
            val logoContainerTranslateY = ObjectAnimator.ofFloat(logoContainer, View.TRANSLATION_Y, 20f, 0f)
            logoContainerTranslateY.duration = logoAnimDuration
            
            val logoContainerAnimSet = AnimatorSet().apply {
                playTogether(logoContainerFadeIn, logoContainerScaleX, logoContainerScaleY, logoContainerTranslateY)
                interpolator = DecelerateInterpolator(1.3f)
            }
            
            // Animate the logo components with refined timing
            val logoFadeIn = ObjectAnimator.ofFloat(logoImage, View.ALPHA, 0f, 1f).apply {
                duration = 800
                startDelay = 200
                interpolator = DecelerateInterpolator(1.4f)
            }
            
            // Foreground with elegant fade-in
            val foregroundFadeIn = ObjectAnimator.ofFloat(logoForeground, View.ALPHA, 0f, 1f).apply {
                duration = 900
                startDelay = 400
                interpolator = DecelerateInterpolator(1.2f)
            }
            
            // Logo layers animation set
            val logoLayersAnimSet = AnimatorSet().apply {
                playTogether(logoFadeIn, foregroundFadeIn)
            }
            
            // Start both animation sets together
            val combinedLogoAnimSet = AnimatorSet().apply {
                playTogether(logoContainerAnimSet, logoLayersAnimSet)
            }
            combinedLogoAnimSet.start()
            
            // Animate app name with minimalist subtlety
            Handler(Looper.getMainLooper()).postDelayed({
                val appNameFadeIn = ObjectAnimator.ofFloat(appNameText, View.ALPHA, 0f, 1f)
                appNameFadeIn.duration = textAnimDuration
                
                val appNameTranslateY = ObjectAnimator.ofFloat(
                    appNameText, 
                    View.TRANSLATION_Y, 
                    30f, 
                    0f
                )
                appNameTranslateY.duration = textAnimDuration + 100
                
                val appNameAnimSet = AnimatorSet().apply {
                    playTogether(appNameFadeIn, appNameTranslateY)
                    interpolator = DecelerateInterpolator(1.3f)
                    start()
                }
                
                // Animate tagline with careful timing
                Handler(Looper.getMainLooper()).postDelayed({
                    val taglineFadeIn = ObjectAnimator.ofFloat(taglineText, View.ALPHA, 0f, 1f)
                    taglineFadeIn.duration = textAnimDuration - 100
                    
                    val taglineTranslateY = ObjectAnimator.ofFloat(
                        taglineText, 
                        View.TRANSLATION_Y, 
                        15f, 
                        0f
                    )
                    taglineTranslateY.duration = textAnimDuration
                    
                    val taglineAnimSet = AnimatorSet().apply {
                        playTogether(taglineFadeIn, taglineTranslateY)
                        interpolator = DecelerateInterpolator(1.2f)
                        start()
                    }
                    
                    // Animate button with premium subtle appearance
                    Handler(Looper.getMainLooper()).postDelayed({
                        val buttonFadeIn = ObjectAnimator.ofFloat(btnGetStarted, View.ALPHA, 0f, 1f)
                        buttonFadeIn.duration = buttonAnimDuration
                        
                        val buttonTranslateY = ObjectAnimator.ofFloat(
                            btnGetStarted, 
                            View.TRANSLATION_Y, 
                            25f, 
                            0f
                        )
                        buttonTranslateY.duration = buttonAnimDuration + 50
                        
                        // Subtle scale for button
                        val buttonScaleX = ObjectAnimator.ofFloat(btnGetStarted, View.SCALE_X, 0.98f, 1f)
                        buttonScaleX.duration = buttonAnimDuration + 100
                        
                        val buttonScaleY = ObjectAnimator.ofFloat(btnGetStarted, View.SCALE_Y, 0.98f, 1f)
                        buttonScaleY.duration = buttonAnimDuration + 100
                        
                        val buttonAnimSet = AnimatorSet().apply {
                            playTogether(buttonFadeIn, buttonTranslateY, buttonScaleX, buttonScaleY)
                            interpolator = DecelerateInterpolator(1.1f)
                            start()
                        }
                    }, 120)
                    
                }, 100)
                
            }, logoAnimDuration - 150)
        }, 200) // Initial delay for premium feel
    }
    
    // Refined subtle continuous animation with premium experience
    private fun animateLogoForeground() {
        // Ultra-subtle pulse effect
        val pulseX = ObjectAnimator.ofFloat(logoForeground, View.SCALE_X, 1f, 1.015f).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val pulseY = ObjectAnimator.ofFloat(logoForeground, View.SCALE_Y, 1f, 1.015f).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Very slow rotation - more subtle
        val rotateAnimation = ObjectAnimator.ofFloat(logoForeground, View.ROTATION, 0f, 360f).apply {
            duration = 65000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Combine animations for premium experience
        AnimatorSet().apply {
            playTogether(pulseX, pulseY, rotateAnimation)
            start()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Start the subtle continuous animation when activity is visible
        animateLogoForeground()
    }
} 