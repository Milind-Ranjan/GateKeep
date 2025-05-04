package com.example.gatekeep

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.example.gatekeep.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : BaseActivity() {
    private lateinit var repository: AppRepository
    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun getContentViewId(): Int = R.layout.screen_home

    override fun getNavigationMenuItemId(): Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Enable hardware acceleration for better performance
            window.setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )

            // Pre-load layout before super.onCreate for faster rendering
            setTheme(R.style.Theme_GateKeep_AppCompat)

            super.onCreate(savedInstanceState)
            
            // Initialize repository
            repository = AppRepository(applicationContext)

            // Set header title
            findViewById<TextView>(R.id.headerTitle)?.apply {
                text = "GateKeep"
            }

            // Load actual stats from repository
            loadStatistics()

            // Set greeting based on time of day
            findViewById<TextView>(R.id.tvGreeting)?.apply {
                val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                text = when {
                    hourOfDay < 12 -> "Good morning"
                    hourOfDay < 18 -> "Good afternoon"
                    else -> "Good evening"
                }
            }

            // Set up card click listeners for navigation
            findViewById<CardView>(R.id.cardManageApps)?.apply {
                setOnClickListener {
                    startActivity(Intent(this@HomeActivity, AppsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }

            findViewById<CardView>(R.id.cardViewStats)?.apply {
                setOnClickListener {
                    startActivity(Intent(this@HomeActivity, StatsActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }

            // Set up share functionality
            val shareAction = {
                try {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT,
                            "Check out GateKeep! It helps me be more mindful about my smartphone usage: " +
                                    "https://play.google.com/store/apps/details?id=com.example.gatekeep")
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent, "Share GateKeep"))
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to share: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            // Both the card and button can trigger sharing
            findViewById<CardView>(R.id.cardShare)?.apply {
                setOnClickListener { shareAction() }
            }
            
            findViewById<Button>(R.id.btnShare)?.apply {
                setOnClickListener { shareAction() }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing home screen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadStatistics() {
        activityScope.launch {
            try {
                // Get mindful moments (apps closed after breathing session)
                val mindfulMoments = withContext(Dispatchers.IO) {
                    repository.getMindfulMomentsCount()
                }
                
                // Get number of apps selected for mindfulness tracking
                val appsTracked = withContext(Dispatchers.IO) {
                    repository.getEnabledAppsCount()
                }
                
                // Update UI with actual stats
                findViewById<TextView>(R.id.tvMindfulMoments)?.apply {
                    text = mindfulMoments.toString()
                }
                
                findViewById<TextView>(R.id.tvAppsTracked)?.apply {
                    text = appsTracked.toString()
                }
            } catch (e: Exception) {
                // If we can't load stats, display default values
                findViewById<TextView>(R.id.tvMindfulMoments)?.apply {
                    text = "0"
                }
                
                findViewById<TextView>(R.id.tvAppsTracked)?.apply {
                    text = "0"
                }
                
                Log.e("HomeActivity", "Error loading statistics: ${e.message}", e)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload stats when returning to the activity
        loadStatistics()
    }
}