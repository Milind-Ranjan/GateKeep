package com.example.gatekeep.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gatekeep.R

data class AppStatItem(
    val packageName: String,
    val appName: String,
    val openCount: Int,
    val mindfulCloses: Int = 0 // Changed from timeUsed to mindfulCloses
)

class AppStatAdapter(
    private var stats: List<AppStatItem>
) : RecyclerView.Adapter<AppStatAdapter.AppStatViewHolder>() {
    
    fun updateStats(newStats: List<AppStatItem>) {
        this.stats = newStats
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppStatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_stat, parent, false)
        return AppStatViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AppStatViewHolder, position: Int) {
        holder.bind(stats[position])
    }
    
    override fun getItemCount(): Int = stats.size
    
    inner class AppStatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvOpenCount: TextView = itemView.findViewById(R.id.tvOpenCount)
        private val tvMindfulCloses: TextView = itemView.findViewById(R.id.tvTimeUsed) // Reusing the tvTimeUsed for mindful closes
        
        fun bind(stat: AppStatItem) {
            tvAppName.text = stat.appName
            tvOpenCount.text = "Opened ${stat.openCount} times today"
            tvMindfulCloses.text = "${stat.mindfulCloses}"
            
            // Load app icon using the same method as AppAdapter
            try {
                val packageManager = itemView.context.packageManager
                val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                ivAppIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
            } catch (e: Exception) {
                // If we can't load the icon, use a default one
                ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }
    }
} 