package com.example.gatekeep.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gatekeep.R
import com.example.gatekeep.data.GateKeepApp
import com.google.android.material.switchmaterial.SwitchMaterial

class AppAdapter(
    private var apps: List<GateKeepApp>,
    private val onToggle: (GateKeepApp, Boolean) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {
    
    private var originalApps: List<GateKeepApp> = apps
    
    fun getOriginalApps(): List<GateKeepApp> = originalApps
    
    fun updateApps(newApps: List<GateKeepApp>) {
        this.originalApps = newApps
        this.apps = newApps
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }
    
    override fun getItemCount(): Int = apps.size
    
    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvMindfulLabel: TextView = itemView.findViewById(R.id.tvMindfulLabel)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val switchApp: SwitchMaterial = itemView.findViewById(R.id.switchApp)
        
        fun bind(app: GateKeepApp) {
            tvAppName.text = app.appName
            
            // Load app icon
            try {
                val packageManager = itemView.context.packageManager
                val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                ivAppIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
            } catch (e: Exception) {
                // If we can't load the icon, use a default one
                ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            
            // Show/hide "Added for mindfulness" label based on enablement
            tvMindfulLabel.visibility = if (app.isEnabled) View.VISIBLE else View.GONE
            
            // Set switch state without triggering listener
            switchApp.setOnCheckedChangeListener(null)
            switchApp.isChecked = app.isEnabled
            
            // Set listener for changes
            switchApp.setOnCheckedChangeListener { _, isChecked ->
                // Update the mindfulness label immediately for better UX
                tvMindfulLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
                onToggle(app, isChecked)
            }
            
            // Make the entire item clickable to toggle the switch
            itemView.setOnClickListener {
                switchApp.isChecked = !switchApp.isChecked
            }
        }
    }
} 