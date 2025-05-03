package com.example.gatekeep.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val database = AppDatabase.getDatabase(context)
    private val gateKeepAppDao = database.gateKeepAppDao()
    private val appUsageRecordDao = database.appUsageRecordDao()
    private val journalEntryDao = database.journalEntryDao()
    
    // Preference keys
    private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    
    // Preferences
    val isSetupCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SETUP_COMPLETED] ?: false
    }
    
    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SETUP_COMPLETED] = completed
        }
    }
    
    // App operations
    fun getAllApps(): Flow<List<GateKeepApp>> = gateKeepAppDao.getAllApps()
    fun getEnabledApps(): Flow<List<GateKeepApp>> = gateKeepAppDao.getEnabledApps()
    
    suspend fun getInstalledApps(): List<GateKeepApp> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        // Create a list of popular app package names that should be included even if they're system apps
        val popularApps = listOf(
            "com.google.android.youtube", // YouTube
            "com.instagram.android", // Instagram
            "com.facebook.katana", // Facebook
            "com.twitter.android", // Twitter
            "com.snapchat.android", // Snapchat
            "com.whatsapp", // WhatsApp
            "com.tiktok.android", // TikTok
            "com.netflix.mediaclient", // Netflix
            "com.spotify.music", // Spotify
            "com.amazon.app.shopping", // Amazon
            "com.pinterest" // Pinterest
        )
        
        return installedApps
            .filter { 
                // Include the app if:
                // 1. It's not a system app OR
                // 2. It's one of the popular apps we want to include regardless
                ((it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || 
                 popularApps.contains(it.packageName)) &&
                // Always exclude our own app
                it.packageName != context.packageName
            }
            .map {
                val appName = packageManager.getApplicationLabel(it).toString()
                GateKeepApp(
                    packageName = it.packageName,
                    appName = appName,
                    isEnabled = false
                )
            }
            .sortedBy { it.appName }
    }
    
    suspend fun saveGateKeepApp(app: GateKeepApp) {
        gateKeepAppDao.insert(app)
    }
    
    suspend fun updateGateKeepApp(app: GateKeepApp) {
        gateKeepAppDao.update(app)
    }
    
    suspend fun isAppGateKept(packageName: String): Boolean {
        try {
            // First try the direct count query which is faster
            val isEnabled = gateKeepAppDao.isAppEnabled(packageName) > 0
            
            if (!isEnabled) {
                // Double-check with the full app object as a fallback
                val app = gateKeepAppDao.getAppByPackageName(packageName)
                return app != null && app.isEnabled
            }
            
            return isEnabled
        } catch (e: Exception) {
            // Log error but don't crash
            Log.e("AppRepository", "Error checking if app is gatekept: ${e.message}", e)
            return false
        }
    }
    
    // Usage records
    suspend fun recordAppAction(packageName: String, action: String) {
        try {
            val record = AppUsageRecord(
                packageName = packageName,
                timestamp = Date(),
                action = action
            )
            appUsageRecordDao.insertWithRetry(record)
            Log.d("AppRepository", "Recorded action $action for $packageName")
        } catch (e: Exception) {
            // Log error but don't crash
            Log.e("AppRepository", "Error recording app action: ${e.message}", e)
        }
    }
    
    fun getAppUsageRecords(packageName: String): Flow<List<AppUsageRecord>> {
        return appUsageRecordDao.getAppUsageRecords(packageName)
    }
    
    suspend fun getAppOpenCountToday(packageName: String): Int {
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startDate = calendar.time
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endDate = calendar.time
            
            return appUsageRecordDao.getAppOpenCount(packageName, startDate, endDate)
        } catch (e: Exception) {
            // Log error but don't crash
            Log.e("AppRepository", "Error getting app open count: ${e.message}", e)
            return 0
        }
    }
    
    // Add this new method for getting stats for all apps
    suspend fun getAllAppOpenCountsToday(): Map<String, Int> {
        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startDate = calendar.time
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endDate = calendar.time
            
            val appCounts = appUsageRecordDao.getAppOpenCountsByPackage(startDate, endDate)
            return appCounts.associate { it.packageName to it.count }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error getting all app open counts: ${e.message}", e)
            return emptyMap()
        }
    }
    
    // Journal entries
    suspend fun saveJournalEntry(packageName: String, prompt: String, content: String) {
        val entry = JournalEntry(
            packageName = packageName,
            timestamp = Date(),
            prompt = prompt,
            content = content
        )
        journalEntryDao.insert(entry)
    }
    
    fun getJournalEntriesForApp(packageName: String): Flow<List<JournalEntry>> {
        return journalEntryDao.getJournalEntriesForApp(packageName)
    }
} 