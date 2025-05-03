package com.example.gatekeep

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    
    private lateinit var bottomNavigation: BottomNavigationView
    
    // Child activities must implement these
    abstract fun getContentViewId(): Int
    abstract fun getNavigationMenuItemId(): Int
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getContentViewId())
        
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnNavigationItemSelectedListener(this)
    }
    
    override fun onStart() {
        super.onStart()
        updateNavigationBarState()
    }
    
    // Set the menu item checked
    private fun updateNavigationBarState() {
        val actionId = getNavigationMenuItemId()
        selectBottomNavigationBarItem(actionId)
    }
    
    private fun selectBottomNavigationBarItem(itemId: Int) {
        val item = bottomNavigation.menu.findItem(itemId)
        item?.isChecked = true
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == getNavigationMenuItemId()) {
            // Already on this screen
            return true
        }
        
        when (item.itemId) {
            R.id.nav_home -> {
                startActivity(Intent(this, HomeActivity::class.java))
                return true
            }
            R.id.nav_apps -> {
                startActivity(Intent(this, AppsActivity::class.java))
                return true
            }
            R.id.nav_stats -> {
                startActivity(Intent(this, StatsActivity::class.java))
                return true
            }
        }
        return false
    }
    
    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }
} 