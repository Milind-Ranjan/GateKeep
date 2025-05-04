package com.example.gatekeep

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply system UI flags for smoother appearance
        window.setDecorFitsSystemWindows(false)
        
        // Enable hardware acceleration for better performance
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        repository = AppRepository(applicationContext)
        
        setContent {
            GateKeepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasUsageAccess by remember { mutableStateOf(hasUsageStatsPermission(this)) }
                    var hasAccessibilityAccess by remember { mutableStateOf(isAccessibilityServiceEnabled(this)) }
                    var permissionsJustGranted by remember { mutableStateOf(false) }
                    
                    // Main content
                    MainContent()
                    
                    // Check permissions
                    LaunchedEffect(Unit) {
                        // Single check at startup is sufficient
                        hasUsageAccess = hasUsageStatsPermission(this@MainActivity)
                        hasAccessibilityAccess = isAccessibilityServiceEnabled(this@MainActivity)
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
fun MainContent() {
    val context = LocalContext.current
    val repository = remember { AppRepository(context) }
    val navController = rememberNavController()
    
    // No permission check here - we assume permissions are already granted 
    // since PermissionsActivity would redirect here only when permissions are granted
    
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