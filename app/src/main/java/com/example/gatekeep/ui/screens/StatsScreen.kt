package com.example.gatekeep.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.gatekeep.R
import com.example.gatekeep.StatsActivity
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.data.GateKeepApp
import com.patrykandpatryk.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatryk.vico.compose.axis.vertical.startAxis
import com.patrykandpatryk.vico.compose.chart.Chart
import com.patrykandpatryk.vico.compose.chart.column.columnChart
import com.patrykandpatryk.vico.core.entry.entryModelOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(repository: AppRepository) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Launch the traditional XML-based StatsActivity
    LaunchedEffect(Unit) {
        context.startActivity(Intent(context, StatsActivity::class.java))
    }
    
    val enabledApps by repository.getEnabledApps().collectAsState(initial = emptyList())
    var isLoading by remember { mutableStateOf(true) }
    var appStats by remember { mutableStateOf<List<AppStat>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }
    
    // Observe lifecycle to refresh data when screen is shown
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Auto-refresh data every 30 seconds
    LaunchedEffect(key1 = refreshKey) {
        while (true) {
            loadStats(repository, enabledApps, { isLoading = it }, { appStats = it })
            delay(30000) // Refresh every 30 seconds
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_usage_today),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Text(
                text = "Last refreshed: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (appStats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No gatekept apps yet. Go to the Apps tab to select apps to monitor.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                // Daily usage chart
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Daily App Opens",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (appStats.isNotEmpty()) {
                            val chartEntries = appStats.take(5)
                                .mapIndexed { index, stat -> index.toFloat() to stat.openCount.toFloat() }
                                .toTypedArray()
                            
                            val chartModel = entryModelOf(*chartEntries)
                            val xLabels = appStats.take(5).map { it.app.appName }
                            
                            Chart(
                                chart = columnChart(),
                                model = chartModel,
                                startAxis = startAxis(),
                                bottomAxis = bottomAxis(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Legend
                            xLabels.forEachIndexed { index, appName ->
                                Text(
                                    text = "${index + 1}. $appName",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // List of apps with stats
                LazyColumn(
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(appStats) { stat ->
                        AppStatItem(stat)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        
        // Refresh button
        FloatingActionButton(
            onClick = { refreshKey++ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Refresh stats"
            )
        }
    }
}

private suspend fun loadStats(
    repository: AppRepository,
    enabledApps: List<GateKeepApp>,
    setLoading: (Boolean) -> Unit,
    setStats: (List<AppStat>) -> Unit
) {
    setLoading(true)
    try {
        // Try to get all stats at once first
        val allAppCounts = repository.getAllAppOpenCountsToday()
        
        // If that doesn't work, fall back to individual queries
        if (allAppCounts.isEmpty() && enabledApps.isNotEmpty()) {
            val stats = enabledApps.map { app ->
                val openCount = repository.getAppOpenCountToday(app.packageName)
                AppStat(app, openCount)
            }
            setStats(stats.sortedByDescending { it.openCount })
        } else {
            // Convert the map to stats objects
            val stats = enabledApps.map { app ->
                val count = allAppCounts[app.packageName] ?: 0
                AppStat(app, count)
            }
            setStats(stats.sortedByDescending { it.openCount })
        }
    } catch (e: Exception) {
        // Handle errors
    } finally {
        setLoading(false)
    }
}

@Composable
fun AppStatItem(stat: AppStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stat.app.appName,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Text(
                text = "Opened: ${stat.openCount}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (stat.openCount > 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

data class AppStat(
    val app: GateKeepApp,
    val openCount: Int
) 