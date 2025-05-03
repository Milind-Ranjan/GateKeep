package com.example.gatekeep

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class HomeActivity : BaseActivity() {
    
    override fun getContentViewId(): Int = R.layout.screen_home
    
    override fun getNavigationMenuItemId(): Int = R.id.nav_home
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up button click listeners
        val btnApps = findViewById<Button>(R.id.btnApps)
        val btnStats = findViewById<Button>(R.id.btnStats)
        val btnShare = findViewById<Button>(R.id.btnShare)
        
        btnApps.setOnClickListener {
            startActivity(Intent(this, AppsActivity::class.java))
        }
        
        btnStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        
        btnShare.setOnClickListener {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, 
                    "Check out GateKeep! It helps me be more mindful about my smartphone usage: " +
                    "https://play.google.com/store/apps/details?id=com.example.gatekeep")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Share GateKeep"))
        }
    }
} 