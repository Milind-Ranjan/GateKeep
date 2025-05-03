package com.example.gatekeep.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.gatekeep.InterceptionActivity
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.data.GateKeepApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AppMonitoringService : AccessibilityService() {
    private lateinit var repository: AppRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var lastDetectedPackage: String? = null
    private var lastDetectionTime: Long = 0
    private val TAG = "GateKeepService"
    private var enabledApps: List<GateKeepApp> = emptyList()
    private var enabledPackages: Set<String> = emptySet()
    
    // Maps package name to last interception time
    private val packageInterceptionTimes = ConcurrentHashMap<String, Long>()
    
    // Maps package name to launch time for apps we're launching
    private val appsBeingLaunched = ConcurrentHashMap<String, Long>()
    
    // Handler for delayed tasks
    private val handler = Handler(Looper.getMainLooper())
    
    // Time thresholds
    private val REINTERCEPTION_THRESHOLD = 10000L  // 10 seconds
    private val LAUNCH_TIMEOUT = 5000L  // 5 seconds
    
    // Periodic task to clean up tracking maps
    private val cleanupTask = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            
            // Clean up interception times
            packageInterceptionTimes.entries.removeIf { 
                currentTime - it.value > REINTERCEPTION_THRESHOLD 
            }
            
            // Clean up launch tracking
            appsBeingLaunched.entries.removeIf { 
                currentTime - it.value > LAUNCH_TIMEOUT 
            }
            
            // Reschedule the task
            handler.postDelayed(this, 5000) // Run every 5 seconds
        }
    }

    companion object {
        // Static variable to track if service is running
        var isServiceRunning = false
            private set
        
        // Static reference to the current service instance
        private var serviceInstance: AppMonitoringService? = null
        
        // Method to mark an app as being launched - callable from anywhere
        fun markAppLaunch(context: Context, packageName: String) {
            serviceInstance?.markAppAsLaunching(packageName)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(applicationContext)
        isServiceRunning = true
        serviceInstance = this
        
        // Load the initial list of enabled apps
        serviceScope.launch {
            try {
                loadEnabledApps()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading enabled apps: ${e.message}", e)
            }
        }
        
        // Start the periodic cleanup task
        handler.postDelayed(cleanupTask, 5000)
        
        Toast.makeText(applicationContext, "GateKeep service started", Toast.LENGTH_SHORT).show()
    }
    
    private suspend fun loadEnabledApps() {
        try {
            repository.getEnabledApps().collect { apps ->
                enabledApps = apps
                enabledPackages = apps.filter { it.isEnabled }.map { it.packageName }.toSet()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting enabled apps: ${e.message}", e)
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Configure the service with optimal settings
        val info = serviceInfo
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                          AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                     AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        info.notificationTimeout = 50
        info.packageNames = null // Monitor all packages
        
        serviceInfo = info
        
        isServiceRunning = true
        
        // Load the enabled apps list when service connects
        serviceScope.launch {
            try {
                loadEnabledApps()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading enabled apps after connect: ${e.message}", e)
            }
        }
        
        // Setup task removal detection
        setupTaskRemovedDetection()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            val packageName = event.packageName?.toString() ?: return
            
            // Only process window state changes - this is when an app comes to the foreground
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // Ignore our own app and system UI
                if (packageName == "com.example.gatekeep" || 
                    packageName.contains("com.android.systemui") || 
                    packageName == "android" ||
                    packageName.startsWith("com.android.launcher")) {
                    return
                }
                
                // Check if this package is in our enabled set
                if (!enabledPackages.contains(packageName)) {
                    return
                }
                
                // Check if this app is in the "being launched" list
                val launchTime = appsBeingLaunched[packageName]
                val currentTime = System.currentTimeMillis()
                
                if (launchTime != null) {
                    val timeSinceLaunch = currentTime - launchTime
                    if (timeSinceLaunch < LAUNCH_TIMEOUT) {
                        // Skip interception for apps we're launching ourselves
                        return
                    } else {
                        // Remove from the list if timeout exceeded
                        appsBeingLaunched.remove(packageName)
                    }
                }
                
                // Check if we've recently intercepted this app
                val lastInterceptionTime = packageInterceptionTimes[packageName]
                
                if (lastInterceptionTime != null) {
                    val timeSinceLastInterception = currentTime - lastInterceptionTime
                    if (timeSinceLastInterception < REINTERCEPTION_THRESHOLD) {
                        return
                    }
                }
                
                // This app is enabled and eligible for interception
                // Update the interception time
                packageInterceptionTimes[packageName] = currentTime
                
                // Record the app opening action
                serviceScope.launch {
                    try {
                        repository.recordAppAction(packageName, "OPENED")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error recording action: ${e.message}", e)
                    }
                }
                
                // Launch interception activity
                val intent = Intent(this, InterceptionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("packageName", packageName)
                    putExtra("timestamp", System.currentTimeMillis())
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch interception activity", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onAccessibilityEvent: ${e.message}", e)
        }
    }

    override fun onInterrupt() {
        // Not used
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Stop the periodic task
        handler.removeCallbacks(cleanupTask)
        isServiceRunning = false
        
        // Clear self reference
        if (serviceInstance == this) {
            serviceInstance = null
        }
    }
    
    // Method to mark an app as being launched
    fun markAppAsLaunching(packageName: String) {
        appsBeingLaunched[packageName] = System.currentTimeMillis()
    }
    
    // Setup detection for app clearing and screen events
    private fun setupTaskRemovedDetection() {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> {
                            val reason = intent.getStringExtra("reason")
                            if (reason == "recentapps") {
                                packageInterceptionTimes.clear()
                            }
                        }
                        Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> {
                            packageInterceptionTimes.clear()
                        }
                    }
                }
            }
            
            registerReceiver(receiver, filter)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up task removal detection: ${e.message}")
        }
    }
} 