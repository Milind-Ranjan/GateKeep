package com.example.gatekeep

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gatekeep.adapters.AppAdapter
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.data.GateKeepApp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppsActivity : BaseActivity() {
    private lateinit var repository: AppRepository
    private lateinit var appsAdapter: AppAdapter
    private lateinit var rvApps: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvSelectedCount: TextView
    private lateinit var noResultsContainer: View
    private lateinit var loadingContainer: View
    
    // Track active jobs to manage cancellation properly
    private var loadAppsJob: Job? = null
    
    override fun getContentViewId(): Int = R.layout.screen_apps
    
    override fun getNavigationMenuItemId(): Int = R.id.nav_apps
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            
            repository = AppRepository(applicationContext)
            
            // Set header title
            val headerTitle = findViewById<TextView>(R.id.headerTitle)
            headerTitle?.text = "Apps"
            
            // Initialize views
            rvApps = findViewById(R.id.rvApps)
            etSearch = findViewById(R.id.etSearch)
            tvSelectedCount = findViewById(R.id.tvSelectedCount)
            noResultsContainer = findViewById(R.id.tvNoResults) // This is now a LinearLayout
            loadingContainer = findViewById(R.id.loadingContainer)
            
            // Set up RecyclerView
            rvApps.layoutManager = LinearLayoutManager(this)
            appsAdapter = AppAdapter(emptyList()) { app, isEnabled ->
                lifecycleScope.launch {
                    try {
                        repository.saveGateKeepApp(app.copy(isEnabled = isEnabled))
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        android.util.Log.e("AppsActivity", "Error updating app: ${e.message}", e)
                    }
                }
            }
            rvApps.adapter = appsAdapter
            
            // Set up search functionality
            etSearch.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    filterApps(s.toString())
                }
                
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
            
            // Load apps
            loadApps()
        } catch (e: Exception) {
            // Log error and show a toast
            android.util.Log.e("AppsActivity", "Error in onCreate: ${e.message}", e)
            android.widget.Toast.makeText(this, "Error loading apps: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onDestroy() {
        // Cancel any ongoing jobs when activity is destroyed
        loadAppsJob?.cancel()
        super.onDestroy()
    }
    
    private fun loadApps() {
        try {
            // Cancel any existing job before starting a new one
            loadAppsJob?.cancel()
            
            loadingContainer.visibility = View.VISIBLE
            rvApps.visibility = View.GONE
            
            loadAppsJob = lifecycleScope.launch {
                try {
                    val installedApps = withContext(Dispatchers.IO) {
                        repository.getInstalledApps()
                    }
                    
                    // Check if coroutine is still active before proceeding
                    if (!isActive) return@launch
                    
                    repository.getAllApps().collect { savedAppsList ->
                        // Check if coroutine is still active
                        if (!isActive) return@collect
                        
                        // Merge installed apps with saved state
                        val mergedApps = installedApps.map { app ->
                            savedAppsList.find { it.packageName == app.packageName } ?: app
                        }
                        
                        // Update UI
                        withContext(Dispatchers.Main) {
                            if (!isActive) return@withContext
                            
                            loadingContainer.visibility = View.GONE
                            rvApps.visibility = View.VISIBLE
                            appsAdapter.updateApps(mergedApps)
                            updateSelectedCount(mergedApps)
                        }
                    }
                } catch (e: Exception) {
                    // Specifically handle cancellation
                    if (e is CancellationException) {
                        android.util.Log.d("AppsActivity", "Load apps job was cancelled")
                        return@launch
                    }
                    
                    android.util.Log.e("AppsActivity", "Error loading apps: ${e.message}", e)
                    if (isActive) {
                        withContext(Dispatchers.Main) {
                            loadingContainer.visibility = View.GONE
                            android.widget.Toast.makeText(
                                this@AppsActivity, 
                                "Error loading apps: ${e.message}", 
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppsActivity", "Error in loadApps: ${e.message}", e)
            loadingContainer.visibility = View.GONE
            android.widget.Toast.makeText(this, "Error loading apps: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    private fun filterApps(query: String) {
        try {
            val filteredApps = appsAdapter.getOriginalApps().filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
            
            appsAdapter.updateApps(filteredApps)
            
            if (filteredApps.isEmpty() && query.isNotEmpty()) {
                noResultsContainer.visibility = View.VISIBLE
                rvApps.visibility = View.GONE
            } else {
                noResultsContainer.visibility = View.GONE
                rvApps.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            android.util.Log.e("AppsActivity", "Error filtering apps: ${e.message}", e)
        }
    }
    
    private fun updateSelectedCount(apps: List<GateKeepApp>) {
        try {
            val enabledCount = apps.count { it.isEnabled }
            tvSelectedCount.text = "$enabledCount apps selected for mindfulness"
        } catch (e: Exception) {
            android.util.Log.e("AppsActivity", "Error updating count: ${e.message}", e)
        }
    }
} 