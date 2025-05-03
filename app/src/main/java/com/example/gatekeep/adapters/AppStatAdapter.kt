package com.example.gatekeep.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gatekeep.R

data class AppStatItem(
    val packageName: String,
    val appName: String,
    val openCount: Int,
    val closeCount: Int
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
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvOpenCount: TextView = itemView.findViewById(R.id.tvOpenCount)
        private val tvCloseCount: TextView = itemView.findViewById(R.id.tvCloseCount)
        
        fun bind(stat: AppStatItem) {
            tvAppName.text = stat.appName
            tvOpenCount.text = stat.openCount.toString()
            tvCloseCount.text = stat.closeCount.toString()
        }
    }
} 