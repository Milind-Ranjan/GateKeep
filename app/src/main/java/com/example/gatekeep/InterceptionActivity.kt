package com.example.gatekeep

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
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
            // Set window flags before super.onCreate for immediate effect
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN

            // Important: Set the window to appear immediately
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )

            super.onCreate(savedInstanceState)

            // Terminate any task that might be starting the target app
            killTargetAppTask()
            
            setContentView(R.layout.activity_interception)
            
            // Ensure this activity is properly set up with the right flags
            if (intent != null) {
                if ((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) == 0) {
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
        try {
            // Disable buttons to prevent double-clicks
            btnContinue.isEnabled = false
            btnClose.isEnabled = false
            
            // Save the journal entry if there's text
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
            
            // Let the service know we're launching this app to avoid re-interception
            AppMonitoringService.getInstance()?.markAppAsLaunching(packageName)
            
            // Show a completion animation before launching the app
            showCompletionAnimation {
                // Launch the app
                try {
                    // Get package manager
                    val pm = packageManager
                    // Get the launch intent for the app
                    val launchIntent = pm.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                                 Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                 Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    
                    if (launchIntent != null) {
                        // Record the "CONTINUED" action
                        activityScope.launch {
                            repository.recordAppAction(packageName, "CONTINUED")
                        }
                        
                        startActivity(launchIntent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    } else {
                        // If we can't get the launch intent, just finish
                        Toast.makeText(this, "Unable to launch app", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("GateKeepInterception", "Error launching app: ${e.message}", e)
                    Toast.makeText(this, "Error launching app", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } catch (e: Exception) {
            Log.e("GateKeepInterception", "Error in continueToApp: ${e.message}", e)
            finish()
        }
    }
    
    private fun showCompletionAnimation(onComplete: () -> Unit) {
        try {
            // Create subtle animations to show completion
            val breathingCircleFadeOut = ObjectAnimator.ofFloat(breathingCircle, View.ALPHA, 1f, 0f).apply {
                duration = 500
                interpolator = DecelerateInterpolator()
            }
            
            val mindfulnessContainerScale = ObjectAnimator.ofPropertyValuesHolder(
                mindfulnessContainer,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.9f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.9f)
            ).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
            }
            
            val mindfulnessContainerFadeOut = ObjectAnimator.ofFloat(mindfulnessContainer, View.ALPHA, 1f, 0f).apply {
                duration = 500
                startDelay = 100
                interpolator = DecelerateInterpolator()
            }
            
            val bottomActionContainerSlideDown = ObjectAnimator.ofFloat(
                bottomActionContainer,
                View.TRANSLATION_Y,
                0f,
                resources.displayMetrics.heightPixels.toFloat()
            ).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
            }
            
            // Create animation set
            val animSet = AnimatorSet()
            animSet.playTogether(
                breathingCircleFadeOut,
                mindfulnessContainerScale,
                mindfulnessContainerFadeOut,
                bottomActionContainerSlideDown
            )
            
            // Run completion callback when animation ends
            animSet.doOnEnd {
                onComplete()
            }
            
            // Start animations
            animSet.start()
        } catch (e: Exception) {
            Log.e("GateKeepInterception", "Error in completion animation: ${e.message}", e)
            onComplete() // Still run the completion callback if there's an error
        }
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

    // Kill any task for the target app to prevent it from launching
    private fun killTargetAppTask() {
        if (packageName.isNullOrEmpty()) {
            packageName = intent.getStringExtra("packageName") ?: ""
            if (packageName.isEmpty()) return
        }
        
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // Try to find and kill any task associated with the target app
            activityManager.appTasks.forEach { appTask ->
                val taskInfo = appTask.taskInfo
                if (taskInfo != null && taskInfo.baseActivity != null && 
                    taskInfo.baseActivity!!.packageName == packageName) {
                    // We found the task for the target app, try to finish it
                    appTask.finishAndRemoveTask()
                }
            }
            
            // Alternative approach for older Android versions
            @Suppress("DEPRECATION")
            activityManager.runningAppProcesses?.forEach { processInfo ->
                if (processInfo.processName == packageName) {
                    android.os.Process.killProcess(processInfo.pid)
                }
            }
        } catch (e: Exception) {
            Log.e("GateKeepInterception", "Error killing target app task: ${e.message}", e)
        }
    }
} 