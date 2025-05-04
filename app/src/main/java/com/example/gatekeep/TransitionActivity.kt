package com.example.gatekeep

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.button.MaterialButton

/**
 * A dedicated transition activity for the "You're all set" screen
 * This provides a clean, simple transition between the permissions screen
 * and the main app without any lag or animation issues.
 */
class TransitionActivity : AppCompatActivity() {

    private lateinit var successCircleCard: CardView
    private lateinit var checkmarkImage: ImageView
    private lateinit var successTitle: TextView
    private lateinit var successDescription: TextView
    private lateinit var continueButton: MaterialButton
    
    private val TAG = "TransitionActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            
            Log.d(TAG, "Starting TransitionActivity")
            
            // Enable hardware acceleration for smoother animations
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            
            // Hide system UI for immersive experience
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            
            Log.d(TAG, "Setting content view")
            setContentView(R.layout.transition_success)
            
            // Initialize UI elements
            try {
                Log.d(TAG, "Initializing views")
                successCircleCard = findViewById(R.id.successCircleCard)
                checkmarkImage = findViewById(R.id.checkmarkImage)
                successTitle = findViewById(R.id.successTitle)
                successDescription = findViewById(R.id.successDescription)
                continueButton = findViewById(R.id.continueButton)
            } catch (e: Exception) {
                Log.e(TAG, "Error finding views: ${e.message}", e)
                Toast.makeText(this, "Error initializing UI: ${e.message}", Toast.LENGTH_LONG).show()
                // Continue despite errors
            }
            
            try {
                Log.d(TAG, "Setting up animations")
                // Set initial states for animations
                successCircleCard.alpha = 0f
                successCircleCard.scaleX = 0.9f
                successCircleCard.scaleY = 0.9f
                
                checkmarkImage.alpha = 0f
                checkmarkImage.scaleX = 0.5f
                checkmarkImage.scaleY = 0.5f
                
                successTitle.alpha = 0f
                successDescription.alpha = 0f
                continueButton.alpha = 0f
                continueButton.translationY = 50f
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up animations: ${e.message}", e)
                // Continue despite errors
            }
            
            // Set up the continue button click listener
            continueButton.setOnClickListener {
                Log.d(TAG, "Continue button clicked")
                navigateToHomeScreen()
            }
            
            // Start animations after a tiny delay
            Log.d(TAG, "Scheduling animations")
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    animateTransition()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in animation: ${e.message}", e)
                }
            }, 100)
            
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            
            // In case of critical error, try to navigate to home screen anyway
            try {
                navigateToHomeScreen()
            } catch (e2: Exception) {
                Log.e(TAG, "Could not navigate to home: ${e2.message}", e2)
                finish()
            }
        }
    }
    
    private fun animateTransition() {
        try {
            Log.d(TAG, "Starting animations")
            // Create a single animator set for synchronized animations
            val animSet = AnimatorSet()
            
            // Card animations
            val circleAlpha = ObjectAnimator.ofFloat(successCircleCard, View.ALPHA, 0f, 1f).apply {
                duration = 400
            }
            
            val circleScaleX = ObjectAnimator.ofFloat(successCircleCard, View.SCALE_X, 0.9f, 1f).apply {
                duration = 400
            }
            
            val circleScaleY = ObjectAnimator.ofFloat(successCircleCard, View.SCALE_Y, 0.9f, 1f).apply {
                duration = 400
            }
            
            // Checkmark animations
            val checkmarkAlpha = ObjectAnimator.ofFloat(checkmarkImage, View.ALPHA, 0f, 1f).apply {
                duration = 300
                startDelay = 200
            }
            
            val checkmarkScaleX = ObjectAnimator.ofFloat(checkmarkImage, View.SCALE_X, 0.5f, 1f).apply {
                duration = 350
                startDelay = 200
            }
            
            val checkmarkScaleY = ObjectAnimator.ofFloat(checkmarkImage, View.SCALE_Y, 0.5f, 1f).apply {
                duration = 350
                startDelay = 200
            }
            
            // Text animations
            val textAlpha = ObjectAnimator.ofFloat(successTitle, View.ALPHA, 0f, 1f).apply {
                duration = 300
                startDelay = 250
            }
            
            val descAlpha = ObjectAnimator.ofFloat(successDescription, View.ALPHA, 0f, 1f).apply {
                duration = 300
                startDelay = 350
            }
            
            // Button animations
            val buttonAlpha = ObjectAnimator.ofFloat(continueButton, View.ALPHA, 0f, 1f).apply {
                duration = 300
                startDelay = 450
            }
            
            val buttonTranslate = ObjectAnimator.ofFloat(continueButton, View.TRANSLATION_Y, 50f, 0f).apply {
                duration = 300
                startDelay = 450
            }
            
            // Play all animations together with their individual timing
            animSet.playTogether(
                circleAlpha, circleScaleX, circleScaleY,
                checkmarkAlpha, checkmarkScaleX, checkmarkScaleY,
                textAlpha, descAlpha, buttonAlpha, buttonTranslate
            )
            
            // Use clean interpolator for smooth motion
            animSet.interpolator = DecelerateInterpolator(1.0f)
            animSet.start()
            
            Log.d(TAG, "Animations started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Animation error: ${e.message}", e)
            // If animation fails, try to proceed to home screen after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToHomeScreen()
            }, 1000)
        }
    }
    
    private fun navigateToHomeScreen() {
        try {
            Log.d(TAG, "Navigating to HomeActivity")
            // Direct navigation to traditional HomeActivity to avoid Compose loading delay
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            
            // Use our custom fade animations for a smoother transition
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            
            finish()
            Log.d(TAG, "Navigation completed")
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
            Toast.makeText(this, "Error navigating to home screen: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onPause() {
        try {
            super.onPause()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        try {
            super.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
    }
} 