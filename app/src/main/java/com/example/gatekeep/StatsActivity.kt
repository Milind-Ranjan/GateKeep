package com.example.gatekeep

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gatekeep.adapters.AppStatAdapter
import com.example.gatekeep.adapters.AppStatItem
import com.example.gatekeep.adapters.JournalEntryAdapter
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.data.JournalEntry
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatsActivity : BaseActivity() {
    private lateinit var repository: AppRepository
    private lateinit var rvAppUsage: RecyclerView
    private lateinit var rvJournalEntries: RecyclerView
    private lateinit var tvTotalOpened: TextView
    private lateinit var tvMindfulCloses: TextView
    private lateinit var tvJournalEntries: TextView
    private lateinit var tvNoData: TextView
    private lateinit var tvNoJournalEntries: TextView
    private lateinit var tvWeeklyInsight: TextView
    private lateinit var currentDateText: TextView
    private lateinit var usageProgressContainer: FrameLayout
    private lateinit var btnViewAllApps: TextView
    private lateinit var btnNewJournalEntry: TextView
    private lateinit var btnViewWeeklySummary: TextView
    
    // Progress indicator views
    private var progressForeground: ProgressBar? = null
    private var tvUsagePercent: TextView? = null
    
    // Adapters
    private lateinit var appStatAdapter: AppStatAdapter
    private lateinit var journalEntryAdapter: JournalEntryAdapter
    
    override fun getContentViewId(): Int = R.layout.screen_stats
    
    override fun getNavigationMenuItemId(): Int = R.id.nav_stats
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = AppRepository(applicationContext)
        
        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Initialize all views
        initializeViews()
        
        // Set up RecyclerViews
        setupRecyclerViews()
        
        // Set up current date
        setupDate()
        
        // Set up button click listeners
        setupClickListeners()
        
        // Load stats
        loadStats()
    }
    
    private fun initializeViews() {
        try {
            // Main views from the original layout
            rvAppUsage = findViewById(R.id.rvAppUsage)
            rvJournalEntries = findViewById(R.id.rvJournalEntries)
            tvTotalOpened = findViewById(R.id.tvTotalOpened)
            tvMindfulCloses = findViewById(R.id.tvMindfulCloses)
            tvJournalEntries = findViewById(R.id.tvJournalEntries)
            tvNoData = findViewById(R.id.tvNoData)
            tvNoJournalEntries = findViewById(R.id.tvNoJournalEntries)
            
            // New views from our redesigned layout
            currentDateText = findViewById(R.id.currentDateText)
            usageProgressContainer = findViewById(R.id.usageProgressContainer)
            btnViewAllApps = findViewById(R.id.btnViewAllApps)
            btnNewJournalEntry = findViewById(R.id.btnNewJournalEntry)
            btnViewWeeklySummary = findViewById(R.id.btnViewWeeklySummary)
            tvWeeklyInsight = findViewById(R.id.tvWeeklyInsight)
            
            // Set header title (now using the new header)
            val headerTitle = findViewById<TextView>(R.id.headerTitle)
            headerTitle.text = "Insights"
            
            // Inflate the usage progress view into the container
            val usageProgressView = layoutInflater.inflate(R.layout.layout_usage_progress, usageProgressContainer, true)
            
            // Get references to progress views
            progressForeground = findViewById(R.id.progressForeground)
            tvUsagePercent = findViewById(R.id.tvUsagePercent)
        } catch (e: Exception) {
            // Log any errors related to finding views
            android.util.Log.e("StatsActivity", "Error initializing views: ${e.message}", e)
        }
    }
    
    private fun setupRecyclerViews() {
        // Set up app usage RecyclerView
        rvAppUsage.layoutManager = LinearLayoutManager(this)
        appStatAdapter = AppStatAdapter(emptyList())
        rvAppUsage.adapter = appStatAdapter
        
        // Set up journal entries RecyclerView
        rvJournalEntries.layoutManager = LinearLayoutManager(this)
        journalEntryAdapter = JournalEntryAdapter(emptyList())
        rvJournalEntries.adapter = journalEntryAdapter
    }
    
    private fun setupDate() {
        // Format and display current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        currentDateText.text = "Today, ${dateFormat.format(Date())}"
    }
    
    private fun setupClickListeners() {
        btnViewAllApps.setOnClickListener {
            // Handle the click to view all apps - could expand the list or navigate to a detailed view
            // For now, just show a toast message
            android.widget.Toast.makeText(this, "View all apps clicked", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        btnNewJournalEntry.setOnClickListener {
            // Handle new journal entry
            android.widget.Toast.makeText(this, "New reflection clicked", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        btnViewWeeklySummary.setOnClickListener {
            // Handle view weekly summary
            android.widget.Toast.makeText(this, "View summary clicked", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadStats() {
        lifecycleScope.launch {
            try {
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
                
                // We need to track mindful closes for each app
                val appMindfulCloses = mutableMapOf<String, Int>()
                
                // For each enabled app, get its journal entries and usage records
                enabledApps.forEach { app ->
                    try {
                        val appJournalEntries = repository.getJournalEntriesForApp(app.packageName).first()
                        allJournalEntries.addAll(appJournalEntries)
                        journalCount += appJournalEntries.size
                        
                        // Count closed actions for this specific app
                        val appUsageRecords = repository.getAppUsageRecords(app.packageName).first()
                        val thisAppClosedCount = appUsageRecords.count { it.action == "CLOSED" }
                        appMindfulCloses[app.packageName] = thisAppClosedCount
                        closedCounts += thisAppClosedCount
                    } catch (e: Exception) {
                        // Ignore errors for individual apps
                    }
                }
                
                // Prepare app stats items with mindful closes instead of time used
                val appStats = enabledApps.map { app ->
                    AppStatItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        openCount = appOpenCounts[app.packageName] ?: 0,
                        mindfulCloses = appMindfulCloses[app.packageName] ?: 0
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
                    
                    // Update the progress indicator
                    updateUsageProgress(totalOpens, closedCounts)
                    
                    // Generate a personalized weekly insight message
                    updateWeeklyInsight(totalOpens, closedCounts, journalCount)
                    
                    // Update app usage list
                    if (appStats.isEmpty()) {
                        usageProgressContainer.visibility = View.VISIBLE
                        tvNoData.visibility = View.VISIBLE
                        rvAppUsage.visibility = View.GONE
                    } else {
                        usageProgressContainer.visibility = View.VISIBLE
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
            } catch (e: Exception) {
                // Handle any errors that occur
                android.util.Log.e("StatsActivity", "Error loading stats: ${e.message}", e)
            }
        }
    }
    
    private fun updateUsageProgress(totalOpens: Int, mindfulCloses: Int) {
        try {
            // Calculate mindful usage percentage
            val percentage = if (totalOpens > 0) {
                (mindfulCloses * 100) / totalOpens
            } else {
                0
            }
            
            // Update the progress and text
            progressForeground?.progress = percentage
            tvUsagePercent?.text = "$percentage%"
        } catch (e: Exception) {
            android.util.Log.e("StatsActivity", "Error updating progress: ${e.message}", e)
        }
    }
    
    private fun updateWeeklyInsight(totalOpens: Int, mindfulCloses: Int, journalCount: Int) {
        try {
            // Generate a dynamic insight message based on the stats
            val message = when {
                totalOpens == 0 -> "No app usage tracked yet this week. Start using GateKeep with your favorite apps!"
                mindfulCloses > totalOpens / 2 -> "Great job! You've mindfully closed apps ${mindfulCloses} times this week. That's excellent self-awareness."
                journalCount > 3 -> "You've created ${journalCount} journal entries this week. Your reflections help build better digital habits!"
                totalOpens > 20 && mindfulCloses < 5 -> "You've opened apps ${totalOpens} times this week. Try adding more mindful moments to your routine."
                else -> "You're using apps mindfully this week! Keep tracking your usage to see more personalized insights."
            }
            
            tvWeeklyInsight.text = message
        } catch (e: Exception) {
            // Set a default message in case of errors
            tvWeeklyInsight.text = "Keep using GateKeep to see personalized insights about your app usage patterns."
        }
    }
} 