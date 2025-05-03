package com.example.gatekeep.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        private val tvPackageName: TextView = itemView.findViewById(R.id.tvPackageName)
        private val switchApp: SwitchMaterial = itemView.findViewById(R.id.switchApp)
        
        fun bind(app: GateKeepApp) {
            tvAppName.text = app.appName
            tvPackageName.text = app.packageName
            
            // Set switch state without triggering listener
            switchApp.setOnCheckedChangeListener(null)
            switchApp.isChecked = app.isEnabled
            
            // Set listener for changes
            switchApp.setOnCheckedChangeListener { _, isChecked ->
                onToggle(app, isChecked)
            }
        }
    }
} 