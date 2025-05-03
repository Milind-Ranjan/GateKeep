package com.example.gatekeep

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gatekeep.adapters.AppAdapter
import com.example.gatekeep.data.AppRepository
import com.example.gatekeep.data.GateKeepApp
import kotlinx.coroutines.launch

class AppsActivity : BaseActivity() {
    private lateinit var repository: AppRepository
    private lateinit var appsAdapter: AppAdapter
    private lateinit var rvApps: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvSelectedCount: TextView
    private lateinit var tvNoResults: TextView
    private lateinit var loadingContainer: View
    
    override fun getContentViewId(): Int = R.layout.screen_apps
    
    override fun getNavigationMenuItemId(): Int = R.id.nav_apps
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = AppRepository(applicationContext)
        
        // Initialize views
        rvApps = findViewById(R.id.rvApps)
        etSearch = findViewById(R.id.etSearch)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        tvNoResults = findViewById(R.id.tvNoResults)
        loadingContainer = findViewById(R.id.loadingContainer)
        
        // Set up RecyclerView
        rvApps.layoutManager = LinearLayoutManager(this)
        appsAdapter = AppAdapter(emptyList()) { app, isEnabled ->
            lifecycleScope.launch {
                repository.saveGateKeepApp(app.copy(isEnabled = isEnabled))
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
    }
    
    private fun loadApps() {
        loadingContainer.visibility = View.VISIBLE
        rvApps.visibility = View.GONE
        
        lifecycleScope.launch {
            val installedApps = repository.getInstalledApps()
            val savedApps = repository.getAllApps().collect { savedAppsList ->
                // Merge installed apps with saved state
                val mergedApps = installedApps.map { app ->
                    savedAppsList.find { it.packageName == app.packageName } ?: app
                }
                
                // Update UI
                runOnUiThread {
                    loadingContainer.visibility = View.GONE
                    rvApps.visibility = View.VISIBLE
                    appsAdapter.updateApps(mergedApps)
                    updateSelectedCount(mergedApps)
                }
            }
        }
    }
    
    private fun filterApps(query: String) {
        val filteredApps = appsAdapter.getOriginalApps().filter {
            it.appName.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
        }
        
        appsAdapter.updateApps(filteredApps)
        
        if (filteredApps.isEmpty() && query.isNotEmpty()) {
            tvNoResults.visibility = View.VISIBLE
            rvApps.visibility = View.GONE
        } else {
            tvNoResults.visibility = View.GONE
            rvApps.visibility = View.VISIBLE
        }
    }
    
    private fun updateSelectedCount(apps: List<GateKeepApp>) {
        val enabledCount = apps.count { it.isEnabled }
        tvSelectedCount.text = "$enabledCount apps selected for mindfulness"
    }
} 