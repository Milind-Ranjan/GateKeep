package com.example.gatekeep

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gatekeep.service.AppMonitoringService

class TraditionalMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check for required permissions
        val hasUsageAccess = hasUsageStatsPermission(this)
        val hasAccessibilityAccess = isAccessibilityServiceEnabled(this)
        
        if (!hasUsageAccess || !hasAccessibilityAccess) {
            // Show permission screen
            setContentView(R.layout.screen_permissions)
            
            // Set up permission request buttons
            val btnUsageStats = findViewById<Button>(R.id.btnUsageStats)
            val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
            val tvUsageStatus = findViewById<TextView>(R.id.tvUsageStatus)
            val tvAccessibilityStatus = findViewById<TextView>(R.id.tvAccessibilityStatus)
            
            // Update status texts
            tvUsageStatus.text = if (hasUsageAccess) "✓ Granted" else "✗ Required"
            tvAccessibilityStatus.text = if (hasAccessibilityAccess) "✓ Granted" else "✗ Required"
            
            // Set button states
            btnUsageStats.isEnabled = !hasUsageAccess
            btnAccessibility.isEnabled = !hasAccessibilityAccess
            
            // Set up click listeners
            btnUsageStats.setOnClickListener {
                requestUsageStatsPermission()
            }
            
            btnAccessibility.setOnClickListener {
                requestAccessibilityPermission()
            }
            
            // Set up continue button
            val btnContinue = findViewById<Button>(R.id.btnContinue)
            btnContinue.isEnabled = hasUsageAccess && hasAccessibilityAccess
            
            btnContinue.setOnClickListener {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        } else {
            // All permissions granted, proceed to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
            finish() // Close this activity so it's not in the back stack
        }
    }
    
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // First check if service is registered in accessibility services
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        val packageName = context.packageName
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
        val isRunning = AppMonitoringService.isServiceRunning
        
        return isRegistered && isRunning
    }
    
    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
    
    private fun requestAccessibilityPermission() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check permission status again when returning to the app
        val hasUsageAccess = hasUsageStatsPermission(this)
        val hasAccessibilityAccess = isAccessibilityServiceEnabled(this)
        
        // If all permissions are granted, proceed to HomeActivity
        if (hasUsageAccess && hasAccessibilityAccess) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        } else {
            // Update UI if we're on the permission screen
            try {
                val tvUsageStatus = findViewById<TextView>(R.id.tvUsageStatus)
                val tvAccessibilityStatus = findViewById<TextView>(R.id.tvAccessibilityStatus)
                val btnUsageStats = findViewById<Button>(R.id.btnUsageStats)
                val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
                val btnContinue = findViewById<Button>(R.id.btnContinue)
                
                tvUsageStatus.text = if (hasUsageAccess) "✓ Granted" else "✗ Required"
                tvAccessibilityStatus.text = if (hasAccessibilityAccess) "✓ Granted" else "✗ Required"
                
                btnUsageStats.isEnabled = !hasUsageAccess
                btnAccessibility.isEnabled = !hasAccessibilityAccess
                btnContinue.isEnabled = hasUsageAccess && hasAccessibilityAccess
            } catch (e: Exception) {
                // View might not be inflated yet, ignore
            }
        }
    }
} 