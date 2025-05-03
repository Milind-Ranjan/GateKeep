package com.example.gatekeep.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gatekeep.R
import com.example.gatekeep.service.AppMonitoringService

@Composable
fun PermissionScreen(
    hasUsageStatsPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit
) {
    val context = LocalContext.current
    var showAccessibilityInstructions by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Usage Stats Permission
        PermissionItem(
            title = "Usage Stats Permission",
            description = "GateKeep needs usage access permission to monitor app launches. This permission is essential for detecting when you open an app that you've chosen to gatekeep.",
            isGranted = hasUsageStatsPermission,
            onRequestPermission = onRequestUsageStatsPermission
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Accessibility Permission
        PermissionItem(
            title = "Accessibility Service",
            description = "GateKeep needs accessibility service permission to intercept app launches. Without this permission, the app cannot provide the mindfulness prompts.",
            isGranted = hasAccessibilityPermission,
            onRequestPermission = {
                // Show instructions dialog instead of directly opening settings
                showAccessibilityInstructions = true
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        if (hasAccessibilityPermission) {
            OutlinedButton(
                onClick = { 
                    Toast.makeText(context, "Verifying accessibility service status...", Toast.LENGTH_SHORT).show()
                    // Force restart the accessibility service by toggling settings
                    onRequestAccessibilityPermission()
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(text = "Restart Accessibility Service")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "If interception isn't working, try restarting the accessibility service",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!hasUsageStatsPermission || !hasAccessibilityPermission) {
            Text(
                text = "You must grant all permissions to use GateKeep",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "All permissions granted! ✓",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "GateKeep is ready to use. You'll be redirected to the main screen momentarily.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    if (showAccessibilityInstructions) {
        AccessibilityInstructionsDialog(
            onDismiss = { showAccessibilityInstructions = false },
            onOpenSettings = {
                showAccessibilityInstructions = false
                onRequestAccessibilityPermission()
            }
        )
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isGranted) {
            Text(
                text = "✓ Granted",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show additional status about the service
            val isServiceActive = AppMonitoringService.isServiceRunning
            
            if (isServiceActive) {
                Text(
                    text = "✓ Service is running",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "⚠️ Service is not running. Tap below to fix.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(text = "Restart Service")
                }
            }
        } else {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(text = "Grant Permission")
            }
        }
    }
} 