package com.example.gatekeep

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import com.example.gatekeep.views.OrbitalView
import com.example.gatekeep.views.ParticleView

class WelcomeActivity : AppCompatActivity() {
    // UI Components
    private lateinit var logoFrame: FrameLayout
    private lateinit var logoImage: ImageView
    private lateinit var logoForeground: ImageView
    private lateinit var appNameText: TextView
    private lateinit var taglineText: TextView
    private lateinit var btnGetStarted: Button
    private lateinit var logoContainer: ConstraintLayout
    private lateinit var welcomeParticleContainer: View
    private lateinit var welcomeOrbitalContainer: View
    
    // Animation durations
    private val logoAnimDuration = 1200L
    private val textAnimDuration = 800L
    private val buttonAnimDuration = 600L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        
        // Initialize UI components
        initViews()
        
        // Add particle and orbital views
        try {
            welcomeParticleContainer.also {
                if (it is android.widget.FrameLayout) {
                    it.addView(ParticleView(this))
                }
            }
            
            welcomeOrbitalContainer.also {
                if (it is android.widget.FrameLayout) {
                    it.addView(OrbitalView(this))
                }
            }
        } catch (e: Exception) {
            // Log any errors, but continue
            android.util.Log.e("WelcomeActivity", "Error adding custom views: ${e.message}", e)
        }
        
        // Start entrance animations
        startAnimations()
        
        // Set up the Get Started button click listener
        btnGetStarted.setOnClickListener {
            // Create animation for transitioning out
            val fadeOut = ObjectAnimator.ofFloat(logoContainer, View.ALPHA, 1f, 0f).apply {
                duration = 500
            }
            
            fadeOut.doOnEnd {
                // Start MainActivity when animation completes
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                
                // Apply fade transition
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                
                // Finish this activity
                finish()
            }
            
            fadeOut.start()
        }
    }
    
    private fun initViews() {
        logoFrame = findViewById(R.id.logoFrame)
        logoImage = findViewById(R.id.logoImage)
        logoForeground = findViewById(R.id.logoForeground)
        appNameText = findViewById(R.id.appNameText)
        taglineText = findViewById(R.id.taglineText)
        btnGetStarted = findViewById(R.id.btnGetStarted)
        logoContainer = findViewById(R.id.logoContainer)
        welcomeParticleContainer = findViewById(R.id.welcomeParticleContainer)
        welcomeOrbitalContainer = findViewById(R.id.welcomeOrbitalContainer)
        
        // Set initial states for animation
        logoContainer.alpha = 0f
        logoContainer.scaleX = 0.8f
        logoContainer.scaleY = 0.8f
        
        // Set initial states for logo layers
        logoImage.alpha = 0f
        logoForeground.alpha = 0f
        
        // Text and button are already set to alpha 0 in the layout
    }
    
    private fun startAnimations() {
        // Animation for logo container
        val logoContainerFadeIn = ObjectAnimator.ofFloat(logoContainer, View.ALPHA, 0f, 1f)
        val logoContainerScaleX = ObjectAnimator.ofFloat(logoContainer, View.SCALE_X, 0.8f, 1f)
        val logoContainerScaleY = ObjectAnimator.ofFloat(logoContainer, View.SCALE_Y, 0.8f, 1f)
        
        val logoContainerAnimSet = AnimatorSet().apply {
            playTogether(logoContainerFadeIn, logoContainerScaleX, logoContainerScaleY)
            duration = logoAnimDuration
            interpolator = OvershootInterpolator(0.8f)
        }
        
        // Animate the logo components sequentially
        // First main logo
        val logoFadeIn = ObjectAnimator.ofFloat(logoImage, View.ALPHA, 0f, 1f).apply {
            duration = 600
            startDelay = 300
        }
        
        // Then foreground
        val foregroundFadeIn = ObjectAnimator.ofFloat(logoForeground, View.ALPHA, 0f, 1f).apply {
            duration = 600
            startDelay = 400
        }
        
        // Create and start logo layers animation set
        val logoLayersAnimSet = AnimatorSet().apply {
            playTogether(logoFadeIn, foregroundFadeIn)
        }
        
        // Start both animation sets together
        val combinedLogoAnimSet = AnimatorSet().apply {
            playTogether(logoContainerAnimSet, logoLayersAnimSet)
        }
        combinedLogoAnimSet.start()
        
        // Animate app name after logo animation
        Handler(Looper.getMainLooper()).postDelayed({
            val appNameFadeIn = ObjectAnimator.ofFloat(appNameText, View.ALPHA, 0f, 1f)
            val appNameTranslateY = ObjectAnimator.ofFloat(
                appNameText, 
                View.TRANSLATION_Y, 
                50f, 
                0f
            )
            
            val appNameAnimSet = AnimatorSet().apply {
                playTogether(appNameFadeIn, appNameTranslateY)
                duration = textAnimDuration
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            
            // Animate tagline shortly after app name
            Handler(Looper.getMainLooper()).postDelayed({
                val taglineFadeIn = ObjectAnimator.ofFloat(taglineText, View.ALPHA, 0f, 1f)
                val taglineTranslateY = ObjectAnimator.ofFloat(
                    taglineText, 
                    View.TRANSLATION_Y, 
                    30f, 
                    0f
                )
                
                val taglineAnimSet = AnimatorSet().apply {
                    playTogether(taglineFadeIn, taglineTranslateY)
                    duration = textAnimDuration
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                
                // Animate button after all other elements
                Handler(Looper.getMainLooper()).postDelayed({
                    val buttonFadeIn = ObjectAnimator.ofFloat(btnGetStarted, View.ALPHA, 0f, 1f)
                    val buttonTranslateY = ObjectAnimator.ofFloat(
                        btnGetStarted, 
                        View.TRANSLATION_Y, 
                        50f, 
                        0f
                    )
                    
                    val buttonAnimSet = AnimatorSet().apply {
                        playTogether(buttonFadeIn, buttonTranslateY)
                        duration = buttonAnimDuration
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                }, textAnimDuration / 2)
                
            }, textAnimDuration / 2)
            
        }, logoAnimDuration)
    }
    
    // Add subtle continuous animation to the logo foreground
    private fun animateLogoForeground() {
        val rotateAnimation = ObjectAnimator.ofFloat(logoForeground, View.ROTATION, 0f, 360f).apply {
            duration = 40000 // Very slow rotation
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Start the subtle continuous animation when activity is visible
        animateLogoForeground()
    }
} 