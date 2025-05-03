package com.example.gatekeep

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.example.gatekeep.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GateKeepApplication : Application() {

    private lateinit var repository: AppRepository
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        repository = AppRepository(applicationContext)
        
        // Check both permissions on startup and show clear notifications
        val hasUsageStats = hasUsageStatsPermission()
        val hasAccessibility = isAccessibilityServiceEnabled()
        
        if (!hasUsageStats || !hasAccessibility) {
            val missingPermissions = mutableListOf<String>().apply {
                if (!hasUsageStats) add("Usage Stats")
                if (!hasAccessibility) add("Accessibility Service")
            }
            
            Toast.makeText(
                this,
                "GateKeep requires permissions: ${missingPermissions.joinToString(", ")}. Please grant them to use the app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        for (service in enabledServices) {
            val serviceInfo = service.resolveInfo.serviceInfo
            if (serviceInfo.packageName == packageName && 
                serviceInfo.name == "com.example.gatekeep.service.AppMonitoringService") {
                return true
            }
        }
        return false
    }
} 