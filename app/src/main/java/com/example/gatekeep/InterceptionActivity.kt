package com.example.gatekeep

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.service.AppMonitoringService
import com.example.gatekeep.views.OrbitalView
import com.example.gatekeep.views.ParticleView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InterceptionActivity : AppCompatActivity() {
    private lateinit var repository: AppRepository
    private var packageName: String = ""
    private val activityScope = CoroutineScope(Dispatchers.IO)
    
    // UI Elements
    private lateinit var breathingCircle: CardView
    private lateinit var pulseWave1: View
    private lateinit var pulseWave2: View
    private lateinit var particleContainer: FrameLayout
    private lateinit var orbitalContainer: FrameLayout
    private lateinit var tvPhaseIndicator: TextView
    private lateinit var mindfulnessContainer: CardView
    private lateinit var etJournal: EditText
    private lateinit var bottomActionContainer: CardView
    private lateinit var btnContinue: Button
    private lateinit var btnClose: Button
    
    // Animation related
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var breathingAnimator: AnimatorSet
    private lateinit var pulseAnimator1: ViewPropertyAnimator
    private lateinit var pulseAnimator2: ViewPropertyAnimator
    private var currentPhase = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_interception)
            
            // Ensure this activity is properly set up with the right flags
            if (intent != null) {
                val flags = intent.flags
                if ((flags and Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            
            packageName = intent.getStringExtra("packageName") ?: ""
            
            if (packageName.isEmpty()) {
                Log.e("GateKeepInterception", "Package name is empty, cannot continue")
                finish()
                return
            }
            
            repository = AppRepository(applicationContext)
            
            // Initialize UI elements
            initializeViews()
            
            // Set up animations
            setupAnimations()
            
            // Start the interception experience flow
            startInterceptionFlow()
        } catch (e: Exception) {
            Log.e("GateKeepInterception", "Error in onCreate: ${e.message}", e)
            finish() // Make sure we finish even if there's an error
        }
    }
    
    private fun initializeViews() {
        breathingCircle = findViewById(R.id.breathingCircle)
        pulseWave1 = findViewById(R.id.pulseWave1)
        pulseWave2 = findViewById(R.id.pulseWave2)
        particleContainer = findViewById(R.id.particleContainer)
        orbitalContainer = findViewById(R.id.orbitalContainer)
        tvPhaseIndicator = findViewById(R.id.tvPhaseIndicator)
        mindfulnessContainer = findViewById(R.id.mindfulnessContainer)
        etJournal = findViewById(R.id.etJournal)
        bottomActionContainer = findViewById(R.id.bottomActionContainer)
        btnContinue = findViewById(R.id.btnContinue)
        btnClose = findViewById(R.id.btnClose)
        
        // Add particle and orbital views
        try {
            particleContainer.addView(ParticleView(this))
            orbitalContainer.addView(OrbitalView(this))
        } catch (e: Exception) {
            Log.e("GateKeepInterception", "Error adding custom views: ${e.message}", e)
        }
        
        // Set up button click listeners
        btnContinue.setOnClickListener {
            continueToApp(etJournal.text.toString())
        }
        
        btnClose.setOnClickListener {
            closeApp()
        }
        
        // Set initial state of UI elements
        pulseWave1.alpha = 0f
        pulseWave2.alpha = 0f
        mindfulnessContainer.alpha = 0f
        mindfulnessContainer.visibility = View.INVISIBLE
        
        // Position the bottom action container below the screen initially
        bottomActionContainer.translationY = resources.displayMetrics.heightPixels.toFloat()
    }
    
    private fun setupAnimations() {
        // Create breathing animation
        breathingAnimator = createBreathingAnimation()
        
        // Set up pulse animations
        setupPulseAnimations()
    }
    
    private fun startInterceptionFlow() {
        // Start with the breathing phase
        startBreathingPhase()
    }
    
    private fun startBreathingPhase() {
        currentPhase = 1
        tvPhaseIndicator.text = "BREATHE"
        
        // Start the breathing animation
        breathingAnimator.start()
        
        // Start pulse animations with delay
        handler.postDelayed({
            startPulseAnimations()
        }, 1000)
        
        // Move to mindfulness phase after breathing
        handler.postDelayed({
            startMindfulnessPhase()
        }, 8000)
    }
    
    private fun startMindfulnessPhase() {
        currentPhase = 2
        tvPhaseIndicator.text = "JOURNAL"
        
        // Animate the mindfulness container to appear
        mindfulnessContainer.visibility = View.VISIBLE
        mindfulnessContainer.alpha = 0f
        mindfulnessContainer.scaleX = 0.8f
        mindfulnessContainer.scaleY = 0.8f
        
        mindfulnessContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(0.8f))
            .start()
        
        // Enable continue button regardless of text input
        btnContinue.isEnabled = true
        
        // Animate the bottom action container to slide up
        bottomActionContainer.animate()
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    private fun createBreathingAnimation(): AnimatorSet {
        // Create breathing animation values
        val scaleXUp = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f)
        val scaleYUp = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f)
        val scaleXDown = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.5f, 1f)
        val scaleYDown = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.5f, 1f)
        
        // Create animators
        val breatheIn = ObjectAnimator.ofPropertyValuesHolder(breathingCircle, scaleXUp, scaleYUp).apply {
            duration = 4000
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val breatheOut = ObjectAnimator.ofPropertyValuesHolder(breathingCircle, scaleXDown, scaleYDown).apply {
            duration = 4000
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Create sequence
        return AnimatorSet().apply {
            playSequentially(breatheIn, breatheOut)
            // Repeat animation
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    animation.start()
                }
            })
        }
    }
    
    private fun setupPulseAnimations() {
        // Set initial state
        pulseWave1.scaleX = 1f
        pulseWave1.scaleY = 1f
        pulseWave1.alpha = 0.7f
        
        pulseWave2.scaleX = 1f
        pulseWave2.scaleY = 1f
        pulseWave2.alpha = 0.5f
    }
    
    private fun startPulseAnimations() {
        // Pulse wave 1 animation
        pulseAnimator1 = pulseWave1.animate()
            .scaleX(3f)
            .scaleY(3f)
            .alpha(0f)
            .setDuration(3000)
            .setInterpolator(AccelerateDecelerateInterpolator())
        
        pulseAnimator1.withEndAction {
            pulseWave1.scaleX = 1f
            pulseWave1.scaleY = 1f
            pulseWave1.alpha = 0.7f
            startPulseAnimations()
        }
        
        // Pulse wave 2 animation with delay
        handler.postDelayed({
            pulseAnimator2 = pulseWave2.animate()
                .scaleX(3f)
                .scaleY(3f)
                .alpha(0f)
                .setDuration(3000)
                .setInterpolator(AccelerateDecelerateInterpolator())
            
            pulseAnimator2.withEndAction {
                pulseWave2.scaleX = 1f
                pulseWave2.scaleY = 1f
                pulseWave2.alpha = 0.5f
                // Don't restart this one, it will be restarted by the first one
            }
            
            pulseAnimator2.start()
        }, 1500)
        
        pulseAnimator1.start()
    }
    
    private fun continueToApp(journalText: String) {
        // Save the journal entry first
        if (journalText.isNotBlank()) {
            activityScope.launch {
                try {
                    repository.saveJournalEntry(
                        packageName = packageName,
                        prompt = "Journal entry",
                        content = journalText
                    )
                    repository.recordAppAction(packageName, "CONTINUED")
                } catch (e: Exception) {
                    Log.e("GateKeepInterception", "Error saving journal entry: ${e.message}", e)
                }
            }
        }
        
        // Show a completion animation
        showCompletionAnimation {
            // Mark this app as being launched to prevent re-interception
            try {
                AppMonitoringService.markAppLaunch(applicationContext, packageName)
            } catch (e: Exception) {
                Log.e("GateKeepInterception", "Error marking app as launching: ${e.message}", e)
            }
            
            // First finish this activity to ensure we're out of the way
            // We need to do this BEFORE launching the target app
            finish()
            
            // Small delay to ensure our activity is fully finished
            try {
                Thread.sleep(100)
            } catch (e: Exception) {
                // Ignore
            }
            
            try {
                // Now launch the app
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    // Set up the intent for a clean launch directly to the app
                    launchIntent.apply {
                        // Clear all flags first
                        flags = 0
                        // Add necessary flags
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        // Add main/launcher categories to ensure we start at the app's main activity
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        addCategory(Intent.CATEGORY_DEFAULT)
                        // Add component to be more specific - this forces the intent to go to the app's main activity
                        val componentName = launchIntent.component
                        if (componentName != null) {
                            component = componentName
                        }
                    }
                    
                    // Launch from the application context to avoid activity reference issues
                    applicationContext.startActivity(launchIntent)
                } else {
                    // If we can't launch the app, go home
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(homeIntent)
                }
            } catch (e: Exception) {
                // If there's an error, go home
                try {
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(homeIntent)
                } catch (e2: Exception) {
                    Log.e("GateKeepInterception", "Error going home: ${e2.message}", e2)
                }
            }
        }
    }
    
    private fun showCompletionAnimation(onComplete: () -> Unit) {
        // Scale up the breathing circle fast
        val scaleUp = ObjectAnimator.ofFloat(breathingCircle, "scaleX", breathingCircle.scaleX, 30f).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        val scaleUpY = ObjectAnimator.ofFloat(breathingCircle, "scaleY", breathingCircle.scaleY, 30f).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Create full screen flash effect
        val animSet = AnimatorSet()
        animSet.playTogether(scaleUp, scaleUpY)
        animSet.doOnEnd {
            onComplete()
        }
        animSet.start()
    }
    
    private fun closeApp() {
        // Record that the user decided to close the app
        activityScope.launch {
            try {
                repository.recordAppAction(packageName, "CLOSED")
            } catch (e: Exception) {
                Log.e("GateKeepInterception", "Error recording close action: ${e.message}", e)
            }
        }
        
        // Create closing animation
        // Scale down and fade out the entire layout
        val rootView = findViewById<ConstraintLayout>(android.R.id.content)
        
        rootView.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                // Show message to user
                Toast.makeText(this, "Mindfully closed app", Toast.LENGTH_SHORT).show()
                
                // Go back to home screen
                try {
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("GateKeepInterception", "Error returning to home: ${e.message}", e)
                }
                
                // Finish the activity
                finish()
            }
            .start()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        if (intent.getBooleanExtra("should_finish", false)) {
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        
        // Cleanup animations
        if (::breathingAnimator.isInitialized) {
            breathingAnimator.cancel()
        }
        
        if (::pulseAnimator1.isInitialized) {
            pulseAnimator1.cancel()
        }
        
        if (::pulseAnimator2.isInitialized) {
            pulseAnimator2.cancel()
        }
    }
} 