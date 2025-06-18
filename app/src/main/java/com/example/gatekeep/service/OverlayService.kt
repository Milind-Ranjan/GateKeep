package com.example.gatekeep.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import com.example.gatekeep.R
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.views.OrbitalView
import com.example.gatekeep.views.ParticleView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var repository: AppRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    
    private var currentPackageName: String = ""
    private var isOverlayShowing = false
    
    companion object {
        private const val NOTIFICATION_ID = 12345
        private const val CHANNEL_ID = "overlay_service_channel"
        private var serviceInstance: OverlayService? = null
        
        fun showOverlay(context: Context, packageName: String) {
            val intent = Intent(context, OverlayService::class.java)
            intent.action = "SHOW_OVERLAY"
            intent.putExtra("packageName", packageName)
            context.startForegroundService(intent)
        }
        
        fun hideOverlay(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            intent.action = "HIDE_OVERLAY"
            context.startForegroundService(intent)
        }
        
        fun isOverlayActive(): Boolean {
            return serviceInstance?.isOverlayShowing == true
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        repository = AppRepository(applicationContext)
        
        createNotificationChannel()
        startForegroundService()
        
        Log.d("OverlayService", "OverlayService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW_OVERLAY" -> {
                val packageName = intent.getStringExtra("packageName") ?: ""
                if (packageName.isNotEmpty()) {
                    showSystemOverlay(packageName)
                }
            }
            "HIDE_OVERLAY" -> {
                hideSystemOverlay()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GateKeep Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Manages app interception overlays"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GateKeep Active")
            .setContentText("Monitoring selected apps")
            .setSmallIcon(R.drawable.ic_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun showSystemOverlay(packageName: String) {
        if (isOverlayShowing) {
            Log.d("OverlayService", "Overlay already showing, updating for package: $packageName")
            updateOverlayContent(packageName)
            return
        }
        
        currentPackageName = packageName
        
        try {
            // Create overlay view using the simplified layout
            val inflater = LayoutInflater.from(this)
            overlayView = inflater.inflate(R.layout.overlay_interception, null)
            
            // Set up the overlay view
            setupOverlayView(overlayView!!, packageName)
            
            // Create layout parameters for system overlay
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Initially not focusable
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            
            // Add the overlay to window manager
            windowManager.addView(overlayView, params)
            isOverlayShowing = true
            
            // Make focusable after a short delay to allow user interaction
            handler.postDelayed({
                try {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                    windowManager.updateViewLayout(overlayView, params)
                } catch (e: Exception) {
                    Log.e("OverlayService", "Error making overlay focusable: ${e.message}", e)
                }
            }, 500)
            
            Log.d("OverlayService", "System overlay shown for package: $packageName")
            
            // Monitor for attempts to switch away
            startAppSwitchMonitoring()
            
        } catch (e: Exception) {
            Log.e("OverlayService", "Error showing system overlay: ${e.message}", e)
            isOverlayShowing = false
        }
    }
    
    private fun setupOverlayView(view: View, packageName: String) {
        // Initialize UI elements similar to InterceptionActivity
        val outerRipple = view.findViewById<View>(R.id.outerRipple)
        val middleRipple = view.findViewById<View>(R.id.middleRipple)
        val particleContainer = view.findViewById<FrameLayout>(R.id.particleSystemContainer)
        val orbitalContainer = view.findViewById<FrameLayout>(R.id.orbitalContainer)
        val tvBreathingInstruction = view.findViewById<TextView>(R.id.tvBreathingInstruction)
        val mindfulnessContainer = view.findViewById<CardView>(R.id.mindfulnessCard)
        val etJournal = view.findViewById<EditText>(R.id.etMindfulnessReflection)
        val bottomActionContainer = view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.actionBar)
        val btnContinue = view.findViewById<Button>(R.id.btnContinueApp)
        val btnClose = view.findViewById<Button>(R.id.btnSkip)
        
        // Add particle and orbital views
        try {
            particleContainer.addView(ParticleView(this))
            orbitalContainer.addView(OrbitalView(this))
        } catch (e: Exception) {
            Log.e("OverlayService", "Error adding custom views: ${e.message}", e)
        }
        
        // Set up initial UI state
        tvBreathingInstruction.text = "BREATHE IN"
        outerRipple.alpha = 0f
        middleRipple.alpha = 0f
        mindfulnessContainer.alpha = 0f
        mindfulnessContainer.visibility = View.INVISIBLE
        
        // Hide bottom action container initially - only show after breathing session
        bottomActionContainer.alpha = 0f
        bottomActionContainer.visibility = View.INVISIBLE
        
        // Set up button listeners
        btnContinue.setOnClickListener {
            continueToApp(packageName, etJournal.text.toString())
        }
        
        btnClose.setOnClickListener {
            closeApp(packageName)
        }
        
        // Start animations
        startOverlayAnimations(view)
        
        // Transition to journal phase after breathing (8 seconds)
        handler.postDelayed({
            showJournalPhase(view)
        }, 8000)
    }
    
    private fun startOverlayAnimations(view: View) {
        val breathingCircle = view.findViewById<CardView>(R.id.breathingCircle)
        val outerRipple = view.findViewById<View>(R.id.outerRipple)
        val middleRipple = view.findViewById<View>(R.id.middleRipple)
        
        // Start breathing animation
        val breathingAnimation = breathingCircle.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(4000)
            .withEndAction {
                breathingCircle.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(4000)
                    .withEndAction {
                        // Repeat
                        if (isOverlayShowing) {
                            startOverlayAnimations(view)
                        }
                    }
                    .start()
            }
        
        breathingAnimation.start()
        
        // Start pulse animations
        handler.postDelayed({
            if (isOverlayShowing) {
                startPulseAnimations(outerRipple, middleRipple)
            }
        }, 1000)
    }
    
    private fun startPulseAnimations(outerRipple: View, middleRipple: View) {
        outerRipple.alpha = 0.3f
        outerRipple.scaleX = 1f
        outerRipple.scaleY = 1f
        
        outerRipple.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .alpha(0f)
            .setDuration(4000)
            .withEndAction {
                if (isOverlayShowing) {
                    startPulseAnimations(outerRipple, middleRipple)
                }
            }
            .start()
        
        // Second wave with delay
        handler.postDelayed({
            if (isOverlayShowing) {
                middleRipple.alpha = 0.5f
                middleRipple.scaleX = 1f
                middleRipple.scaleY = 1f
                
                middleRipple.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .alpha(0f)
                    .setDuration(4000)
                    .start()
            }
        }, 2000)
    }
    
    private fun showJournalPhase(view: View) {
        val tvBreathingInstruction = view.findViewById<TextView>(R.id.tvBreathingInstruction)
        val mindfulnessContainer = view.findViewById<CardView>(R.id.mindfulnessCard)
        val bottomActionContainer = view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.actionBar)
        
        tvBreathingInstruction.text = "REFLECT"
        
        // Show mindfulness container first
        mindfulnessContainer.visibility = View.VISIBLE
        mindfulnessContainer.alpha = 0f
        mindfulnessContainer.scaleX = 0.8f
        mindfulnessContainer.scaleY = 0.8f
        
        mindfulnessContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .withEndAction {
                // Only show bottom actions after mindfulness container is fully visible
                showBottomActions(bottomActionContainer)
            }
            .start()
    }
    
    private fun showBottomActions(bottomActionContainer: androidx.constraintlayout.widget.ConstraintLayout) {
        // Make the bottom action container visible and animate it up
        bottomActionContainer.visibility = View.VISIBLE
        bottomActionContainer.alpha = 0f
        
        // Animate the container fading in
        bottomActionContainer.animate()
            .alpha(1f)
            .setDuration(600)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
    
    private fun continueToApp(packageName: String, journalText: String) {
        // Save journal entry if provided
        if (journalText.isNotBlank()) {
            serviceScope.launch {
                try {
                    repository.saveJournalEntry(
                        packageName = packageName,
                        prompt = "Journal entry",
                        content = journalText
                    )
                    repository.recordAppAction(packageName, "CONTINUED")
                } catch (e: Exception) {
                    Log.e("OverlayService", "Error saving journal entry: ${e.message}", e)
                }
            }
        }
        
        // Mark app as being launched to avoid re-interception
        AppMonitoringService.getInstance()?.markAppAsLaunching(packageName)
        
        // Hide overlay
        hideSystemOverlay()
        
        // Launch the app
        handler.postDelayed({
            try {
                val pm = packageManager
                val launchIntent = pm.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                             Intent.FLAG_ACTIVITY_CLEAR_TOP or
                             Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                
                if (launchIntent != null) {
                    startActivity(launchIntent)
                    Log.d("OverlayService", "Successfully launched app: $packageName")
                } else {
                    Log.e("OverlayService", "Could not get launch intent for: $packageName")
                }
            } catch (e: Exception) {
                Log.e("OverlayService", "Error launching app: ${e.message}", e)
            }
        }, 300)
    }
    
    private fun closeApp(packageName: String) {
        // Record close action
        serviceScope.launch {
            try {
                repository.recordAppAction(packageName, "CLOSED")
            } catch (e: Exception) {
                Log.e("OverlayService", "Error recording close action: ${e.message}", e)
            }
        }
        
        // Hide overlay and go to home
        hideSystemOverlay()
        
        handler.postDelayed({
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("OverlayService", "Error returning to home: ${e.message}", e)
            }
        }, 300)
    }
    
    private fun updateOverlayContent(packageName: String) {
        // Update the overlay for a different package if needed
        currentPackageName = packageName
        overlayView?.let { view ->
            setupOverlayView(view, packageName)
        }
    }
    
    private fun hideSystemOverlay() {
        try {
            overlayView?.let { view ->
                windowManager.removeView(view)
            }
            overlayView = null
            isOverlayShowing = false
            currentPackageName = ""
            
            Log.d("OverlayService", "System overlay hidden")
        } catch (e: Exception) {
            Log.e("OverlayService", "Error hiding system overlay: ${e.message}", e)
        }
    }
    
    private fun startAppSwitchMonitoring() {
        // Monitor for app switching attempts and re-show overlay if user tries to access the intercepted app
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isOverlayShowing && currentPackageName.isNotEmpty()) {
                    // Check if the user might be trying to switch to the intercepted app
                    checkForAppSwitch()
                    
                    // Schedule next check
                    handler.postDelayed(this, 500) // Check every 500ms
                }
            }
        }, 500)
    }
    
    private fun checkForAppSwitch() {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            
            // Get running tasks - use traditional for loop instead of forEach to allow break
            val appTasks = activityManager.appTasks
            for (appTask in appTasks) {
                try {
                    val taskInfo = appTask.taskInfo
                    if (taskInfo != null && taskInfo.baseActivity != null) {
                        val taskPackage = taskInfo.baseActivity!!.packageName
                        
                        // If user is trying to access the intercepted app, bring overlay back to front
                        if (taskPackage == currentPackageName) {
                            Log.d("OverlayService", "Detected attempt to switch to intercepted app: $taskPackage")
                            bringOverlayToFront()
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.w("OverlayService", "Error checking app task: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("OverlayService", "Error in checkForAppSwitch: ${e.message}", e)
        }
    }
    
    private fun bringOverlayToFront() {
        try {
            overlayView?.let { view ->
                // Update overlay to ensure it's on top
                val params = view.layoutParams as WindowManager.LayoutParams
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                windowManager.updateViewLayout(view, params)
                
                // Make sure overlay is visible and focused
                view.visibility = View.VISIBLE
                view.bringToFront()
                
                Log.d("OverlayService", "Brought overlay to front")
            }
        } catch (e: Exception) {
            Log.e("OverlayService", "Error bringing overlay to front: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideSystemOverlay()
        serviceInstance = null
        Log.d("OverlayService", "OverlayService destroyed")
    }
} 