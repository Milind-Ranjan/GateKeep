package com.example.gatekeep.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gatekeep.R
import com.example.gatekeep.service.AppMonitoringService
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PermissionScreen(
    hasUsageStatsPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestUsageStatsPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit
) {
    val context = LocalContext.current
    var showAccessibilityInstructions by remember { mutableStateOf(false) }
    
    // Animation values
    var animationStarted by remember { mutableStateOf(false) }
    var showSuccessScreen by remember { mutableStateOf(false) }
    var successAnimationStarted by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = Unit) {
        delay(100) // Small initial delay
        animationStarted = true
    }
    
    // Check if all permissions are granted and show success screen
    LaunchedEffect(hasUsageStatsPermission, hasAccessibilityPermission) {
        if (hasUsageStatsPermission && hasAccessibilityPermission) {
            delay(500) // Small delay before showing success
            showSuccessScreen = true
            delay(200)
            successAnimationStarted = true
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0E10) // Darker background for contrast
    ) {
        if (showSuccessScreen) {
            // Success Screen with animations
            SuccessScreen(
                successAnimationStarted = successAnimationStarted,
                onAnimationComplete = {
                    // Navigate after animation is complete
                    // This would be handled by the parent component
                }
            )
        } else {
            // Normal permissions screen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = animationStarted,
                    enter = fadeIn(tween(800, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                initialOffsetY = { -50 },
                                animationSpec = tween(900, easing = FastOutSlowInEasing)
                            )
                ) {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Thin,
                            letterSpacing = 0.5.sp,
                            fontSize = 28.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AnimatedVisibility(
                    visible = animationStarted,
                    enter = fadeIn(tween(700, delayMillis = 150, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                initialOffsetY = { -30 },
                                animationSpec = tween(800, delayMillis = 150, easing = FastOutSlowInEasing)
                            )
    ) {
        Text(
                        text = "For a mindful digital experience",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.3.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = Color(0xFFADBBC4)
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
        
        // Usage Stats Permission
                AnimatedVisibility(
                    visible = animationStarted,
                    enter = fadeIn(tween(800, delayMillis = 300, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(900, delayMillis = 300, easing = FastOutSlowInEasing)
                            )
                ) {
        PermissionItem(
                        title = "Usage Statistics",
                        description = "Monitor app usage to help you build mindful digital habits",
            isGranted = hasUsageStatsPermission,
            onRequestPermission = onRequestUsageStatsPermission
        )
                }
        
                Spacer(modifier = Modifier.height(24.dp))
        
        // Accessibility Permission
                AnimatedVisibility(
                    visible = animationStarted,
                    enter = fadeIn(tween(800, delayMillis = 450, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(900, delayMillis = 450, easing = FastOutSlowInEasing)
                            )
                ) {
        PermissionItem(
            title = "Accessibility Service",
                        description = "Intercept app launches to provide mindfulness prompts",
            isGranted = hasAccessibilityPermission,
            onRequestPermission = {
                // Show instructions dialog instead of directly opening settings
                showAccessibilityInstructions = true
            }
        )
                }
        
        Spacer(modifier = Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = animationStarted && hasAccessibilityPermission,
                    enter = fadeIn(tween(600, delayMillis = 550))
                ) {
            OutlinedButton(
                onClick = { 
                    // Force restart the accessibility service by toggling settings
                    onRequestAccessibilityPermission()
                },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Restart Accessibility Service",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Light
                            )
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = animationStarted && hasAccessibilityPermission,
                    enter = fadeIn(tween(600, delayMillis = 600))
                ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                        text = "If interception isn't working, try restarting the service",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Light
                        ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
                AnimatedVisibility(
                    visible = animationStarted && (!hasUsageStatsPermission || !hasAccessibilityPermission),
                    enter = fadeIn(tween(600, delayMillis = 650))
                ) {
            Text(
                text = "You must grant all permissions to use GateKeep",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Light
                        ),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
                }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1518)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Row for title and status icon
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
        
        Text(
                    text = if (isGranted) "✓" else "⦿",
                    color = if (isGranted) MaterialTheme.colorScheme.primary else Color(0xFFFF5252),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Light
                    )
                )
            }
        
        Spacer(modifier = Modifier.height(16.dp))
        
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Light,
                    lineHeight = 24.sp
                ),
                color = Color(0xFFADBBC4)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!isGranted) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Grant Permission",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Light
                        ),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
            // Show additional status about the service
            val isServiceActive = AppMonitoringService.isServiceRunning
            
            if (isServiceActive) {
                Text(
                        text = "Service is running",
                    color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Light
                        )
                )
            } else {
                Text(
                        text = "Service is not running. Tap below to fix.",
                    color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Light
                        )
                )
                
                    Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "Restart Service",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Light
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessScreen(
    successAnimationStarted: Boolean,
    onAnimationComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050708))  // Darker, more opaque background
    ) {
        // Animated success circle and checkmark
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Circle background animation with minimal scale
            AnimatedVisibility(
                visible = successAnimationStarted,
                enter = fadeIn(tween(300)) +
                        scaleIn(
                            initialScale = 0.9f,
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        )
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
            
            // Checkmark animation
            AnimatedVisibility(
                visible = successAnimationStarted,
                enter = fadeIn(tween(250, delayMillis = 150))
            ) {
                Text(
                    text = "✓",
                    fontSize = 75.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Light
                )
            }
        }
        
        // Success text container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .offset(y = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title text animation
            AnimatedVisibility(
                visible = successAnimationStarted,
                enter = fadeIn(tween(250, delayMillis = 250))
            ) {
                Text(
                    text = "You're all set!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 28.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description text animation
            AnimatedVisibility(
                visible = successAnimationStarted,
                enter = fadeIn(tween(250, delayMillis = 300))
            ) {
                Text(
                    text = "GateKeep is ready to help you with your digital mindfulness journey",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.2.sp
                    ),
                    color = Color(0xFFADBBC4),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
    
    // Trigger onAnimationComplete after the animation sequence is done
    LaunchedEffect(successAnimationStarted) {
        if (successAnimationStarted) {
            // Allow time for the animation sequence, but keep it shorter
            delay(1200)
            onAnimationComplete()
        }
    }
} 