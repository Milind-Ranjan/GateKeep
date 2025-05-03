package com.example.gatekeep.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gatekeep.R
import com.example.gatekeep.data.JournalEntry
import java.text.SimpleDateFormat
import java.util.Locale

class JournalEntryAdapter(
    private var entries: List<JournalEntry>
) : RecyclerView.Adapter<JournalEntryAdapter.JournalEntryViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    private var appNameCache = mutableMapOf<String, String>()
    
    fun updateEntries(newEntries: List<JournalEntry>) {
        this.entries = newEntries
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal_entry, parent, false)
        return JournalEntryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: JournalEntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }
    
    override fun getItemCount(): Int = entries.size
    
    inner class JournalEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvTimeStamp: TextView = itemView.findViewById(R.id.tvTimeStamp)
        private val tvPrompt: TextView = itemView.findViewById(R.id.tvPrompt)
        private val tvJournalContent: TextView = itemView.findViewById(R.id.tvJournalContent)
        private val context: Context = itemView.context
        
        fun bind(entry: JournalEntry) {
            // Get app name from package name
            val appName = getAppName(entry.packageName)
            
            tvAppName.text = appName
            tvTimeStamp.text = dateFormat.format(entry.timestamp)
            tvPrompt.text = entry.prompt
            tvJournalContent.text = entry.content
        }
        
        private fun getAppName(packageName: String): String {
            // Check cache first
            if (appNameCache.containsKey(packageName)) {
                return appNameCache[packageName]!!
            }
            
            // Try to get app name from package manager
            try {
                val packageManager = context.packageManager
                val info = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(info).toString()
                
                // Cache the result
                appNameCache[packageName] = appName
                
                return appName
            } catch (e: PackageManager.NameNotFoundException) {
                // If we can't find the app, just use the package name
                appNameCache[packageName] = packageName
                return packageName
            }
        }
    }
} 