package com.example.gatekeep

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Base activity for all traditional XML-based activities
 * with optimizations for smooth transitions
 */
abstract class BaseActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    
    abstract fun getContentViewId(): Int
    abstract fun getNavigationMenuItemId(): Int
    
    private lateinit var navigationView: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable hardware acceleration for better performance
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        super.onCreate(savedInstanceState)
        setContentView(getContentViewId())
        
        // Initialize the bottom navigation view
        navigationView = findViewById(R.id.bottomNavigation)
        navigationView.setOnNavigationItemSelectedListener(this)
    }
    
    override fun onResume() {
        super.onResume()
        // Update the selected menu item
        navigationView.menu.findItem(getNavigationMenuItemId()).isChecked = true
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Don't do anything if the current item is selected
        if (item.itemId == getNavigationMenuItemId()) {
            return true
        }
        
        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, HomeActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
                return true
            }
            R.id.nav_apps -> {
                startActivity(Intent(this, AppsActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
                return true
            }
            R.id.nav_stats -> {
                startActivity(Intent(this, StatsActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
                return true
            }
        }
        return false
    }
    
    override fun finish() {
        super.finish()
        // Apply fade animation when finishing the activity
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
} 