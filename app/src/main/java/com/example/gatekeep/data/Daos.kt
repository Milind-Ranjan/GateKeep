package com.example.gatekeep.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface GateKeepAppDao {
    @Query("SELECT * FROM gatekeep_apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<GateKeepApp>>
    
    @Query("SELECT * FROM gatekeep_apps WHERE isEnabled = 1 ORDER BY appName ASC")
    fun getEnabledApps(): Flow<List<GateKeepApp>>
    
    @Query("SELECT * FROM gatekeep_apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): GateKeepApp?
    
    @Query("SELECT COUNT(*) FROM gatekeep_apps WHERE packageName = :packageName AND isEnabled = 1")
    suspend fun isAppEnabled(packageName: String): Int
    
    @Query("SELECT COUNT(*) FROM gatekeep_apps WHERE isEnabled = 1")
    suspend fun getEnabledAppsCount(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: GateKeepApp)
    
    @Update
    suspend fun update(app: GateKeepApp)
    
    @Delete
    suspend fun delete(app: GateKeepApp)
}

@Dao
interface AppUsageRecordDao {
    @Query("SELECT * FROM app_usage_records WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getAppUsageRecords(packageName: String): Flow<List<AppUsageRecord>>
    
    @Query("SELECT * FROM app_usage_records WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getAppUsageRecordsInTimeRange(startDate: Date, endDate: Date): Flow<List<AppUsageRecord>>
    
    @Query("SELECT COUNT(*) FROM app_usage_records WHERE packageName = :packageName AND timestamp BETWEEN :startDate AND :endDate AND action = 'OPENED'")
    suspend fun getAppOpenCount(packageName: String, startDate: Date, endDate: Date): Int
    
    @Query("SELECT packageName, COUNT(*) as count FROM app_usage_records WHERE timestamp BETWEEN :startDate AND :endDate AND action = 'OPENED' GROUP BY packageName ORDER BY count DESC")
    suspend fun getAppOpenCountsByPackage(startDate: Date, endDate: Date): List<AppOpenCount>
    
    @Query("SELECT COUNT(*) FROM app_usage_records WHERE action = :action")
    suspend fun getActionCount(action: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AppUsageRecord): Long
    
    @Transaction
    suspend fun insertWithRetry(record: AppUsageRecord): Long {
        try {
            return insert(record)
        } catch (e: Exception) {
            // Try one more time
            try {
                Thread.sleep(100) // Brief delay
                return insert(record)
            } catch (e2: Exception) {
                throw e2
            }
        }
    }
}

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getJournalEntriesForApp(packageName: String): Flow<List<JournalEntry>>
    
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentJournalEntries(limit: Int): Flow<List<JournalEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: JournalEntry): Long
}

// Helper class for app counts query
data class AppOpenCount(
    val packageName: String,
    val count: Int
) 