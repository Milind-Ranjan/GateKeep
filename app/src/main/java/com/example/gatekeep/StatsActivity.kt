package com.example.gatekeep

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gatekeep.adapters.AppStatAdapter
import com.example.gatekeep.adapters.AppStatItem
import com.example.gatekeep.adapters.JournalEntryAdapter
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.data.JournalEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class StatsActivity : BaseActivity() {
    private lateinit var repository: AppRepository
    private lateinit var rvAppUsage: RecyclerView
    private lateinit var rvJournalEntries: RecyclerView
    private lateinit var tvTotalOpened: TextView
    private lateinit var tvMindfulCloses: TextView
    private lateinit var tvJournalEntries: TextView
    private lateinit var tvNoData: TextView
    private lateinit var tvNoJournalEntries: TextView
    
    // Adapters
    private lateinit var appStatAdapter: AppStatAdapter
    private lateinit var journalEntryAdapter: JournalEntryAdapter
    
    override fun getContentViewId(): Int = R.layout.screen_stats
    
    override fun getNavigationMenuItemId(): Int = R.id.nav_stats
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = AppRepository(applicationContext)
        
        // Set header title
        val headerTitle = findViewById<TextView>(R.id.headerTitle)
        headerTitle.text = "Stats"
        
        // Initialize views
        rvAppUsage = findViewById(R.id.rvAppUsage)
        rvJournalEntries = findViewById(R.id.rvJournalEntries)
        tvTotalOpened = findViewById(R.id.tvTotalOpened)
        tvMindfulCloses = findViewById(R.id.tvMindfulCloses)
        tvJournalEntries = findViewById(R.id.tvJournalEntries)
        tvNoData = findViewById(R.id.tvNoData)
        tvNoJournalEntries = findViewById(R.id.tvNoJournalEntries)
        
        // Set up RecyclerViews
        rvAppUsage.layoutManager = LinearLayoutManager(this)
        appStatAdapter = AppStatAdapter(emptyList())
        rvAppUsage.adapter = appStatAdapter
        
        rvJournalEntries.layoutManager = LinearLayoutManager(this)
        journalEntryAdapter = JournalEntryAdapter(emptyList())
        rvJournalEntries.adapter = journalEntryAdapter
        
        // Load stats
        loadStats()
    }
    
    private fun loadStats() {
        lifecycleScope.launch {
            // Get today's date range
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startDate = calendar.time
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endDate = calendar.time
            
            // Load app data
            val appOpenCounts = repository.getAllAppOpenCountsToday()
            val enabledApps = repository.getEnabledApps().first()
            var closedCounts = 0
            
            // Try to get journal entries
            val allJournalEntries = mutableListOf<JournalEntry>()
            var journalCount = 0
            
            // For each enabled app, get its journal entries
            enabledApps.forEach { app ->
                try {
                    val appJournalEntries = repository.getJournalEntriesForApp(app.packageName).first()
                    allJournalEntries.addAll(appJournalEntries)
                    journalCount += appJournalEntries.size
                    
                    // Count closed actions (simple approach)
                    val appUsageRecords = repository.getAppUsageRecords(app.packageName).first()
                    closedCounts += appUsageRecords.count { it.action == "CLOSED" }
                } catch (e: Exception) {
                    // Ignore errors for individual apps
                }
            }
            
            // Prepare app stats items
            val appStats = enabledApps.map { app ->
                AppStatItem(
                    packageName = app.packageName,
                    appName = app.appName,
                    openCount = appOpenCounts[app.packageName] ?: 0,
                    closeCount = 0 // Will be updated if we have better data
                )
            }.sortedByDescending { it.openCount }
            
            // Sort journal entries by timestamp, most recent first
            allJournalEntries.sortByDescending { it.timestamp }
            
            // Update UI on main thread
            runOnUiThread {
                // Update summary stats
                val totalOpens = appOpenCounts.values.sum()
                tvTotalOpened.text = totalOpens.toString()
                tvMindfulCloses.text = closedCounts.toString()
                tvJournalEntries.text = journalCount.toString()
                
                // Update app usage list
                if (appStats.isEmpty()) {
                    tvNoData.visibility = View.VISIBLE
                    rvAppUsage.visibility = View.GONE
                } else {
                    tvNoData.visibility = View.GONE
                    rvAppUsage.visibility = View.VISIBLE
                    appStatAdapter.updateStats(appStats)
                }
                
                // Update journal entries
                if (allJournalEntries.isEmpty()) {
                    tvNoJournalEntries.visibility = View.VISIBLE
                    rvJournalEntries.visibility = View.GONE
                } else {
                    tvNoJournalEntries.visibility = View.GONE
                    rvJournalEntries.visibility = View.VISIBLE
                    journalEntryAdapter.updateEntries(allJournalEntries)
                }
            }
        }
    }
} 