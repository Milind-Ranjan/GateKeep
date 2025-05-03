package com.example.gatekeep

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.ui.screens.AppsScreen
import com.example.gatekeep.ui.screens.HomeScreen
import com.example.gatekeep.ui.screens.PermissionScreen
import com.example.gatekeep.ui.screens.StatsScreen
import com.example.gatekeep.ui.theme.GateKeepTheme
import kotlinx.coroutines.delay
import com.example.gatekeep.service.AppMonitoringService
import com.example.gatekeep.ui.navigation.NavigationItem

class MainActivity : ComponentActivity() {
    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = AppRepository(applicationContext)
        
        setContent {
            GateKeepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasUsageAccess by remember { mutableStateOf(hasUsageStatsPermission(this)) }
                    var hasAccessibilityAccess by remember { mutableStateOf(isAccessibilityServiceEnabled(this)) }
                    var showThankYouMessage by remember { mutableStateOf(false) }
                    var permissionsJustGranted by remember { mutableStateOf(false) }
                    
                    // Periodically recheck permissions after returning from permission screens
                    LaunchedEffect(Unit) {
                        while (true) {
                            val newHasUsageAccess = hasUsageStatsPermission(this@MainActivity)
                            val newHasAccessibilityAccess = isAccessibilityServiceEnabled(this@MainActivity)
                            
                            // Check if permissions have just been granted
                            if (!hasUsageAccess || !hasAccessibilityAccess) {
                                if (newHasUsageAccess && newHasAccessibilityAccess) {
                                    // All permissions just granted
                                    permissionsJustGranted = true
                                    showThankYouMessage = true
                                }
                            }
                            
                            hasUsageAccess = newHasUsageAccess
                            hasAccessibilityAccess = newHasAccessibilityAccess
                            
                            delay(1000) // Check every second
                        }
                    }
                    
                    // Show thank you message and transition to main screen
                    LaunchedEffect(showThankYouMessage) {
                        if (showThankYouMessage) {
                            Toast.makeText(
                                this@MainActivity, 
                                "Thank you for granting all permissions! GateKeep is now ready to use.", 
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Give time for the toast to be seen
                            delay(2000)
                            showThankYouMessage = false
                        }
                    }
                    
                    if (!hasUsageAccess || !hasAccessibilityAccess) {
                        PermissionScreen(
                            hasUsageStatsPermission = hasUsageAccess,
                            hasAccessibilityPermission = hasAccessibilityAccess,
                            onRequestUsageStatsPermission = {
                                requestUsageStatsPermission()
                            },
                            onRequestAccessibilityPermission = {
                                requestAccessibilityPermission()
                            }
                        )
                    } else {
                        if (permissionsJustGranted) {
                            // Just show a loading screen while the toast is visible
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                // Empty surface while toast is showing
                            }
                            
                            // Reset the flag after a delay
                            LaunchedEffect(permissionsJustGranted) {
                                delay(2000)
                                permissionsJustGranted = false
                            }
                        } else {
                            MainScreen(repository)
                        }
                    }
                }
            }
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
}

@Composable
fun MainScreen(repository: AppRepository) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigation(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(navController)
}
            composable("apps") {
                AppsScreen(repository)
            }
            composable("stats") {
                StatsScreen(repository)
            }
        }
    }
}

@Composable
fun BottomNavigation(navController: NavHostController) {
    val items = listOf(
        NavigationItem("home", "Home", Icons.Default.Home),
        NavigationItem("apps", "Apps", Icons.Default.List),
        NavigationItem("stats", "Stats", Icons.Default.Settings)
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}