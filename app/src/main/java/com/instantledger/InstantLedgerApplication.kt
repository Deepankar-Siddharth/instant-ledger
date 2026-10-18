package com.instantledger

import android.app.Application
import androidx.work.WorkManager
import com.instantledger.worker.DailySummaryScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InstantLedgerApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("InstantLedger", "InstantLedgerApplication: onCreate called")
        
        // Initialize database early to catch any initialization errors
        try {
            com.instantledger.data.database.AppDatabase.getDatabase(this)
            android.util.Log.d("InstantLedger", "InstantLedgerApplication: Database initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("InstantLedger", "InstantLedgerApplication: Failed to initialize database", e)
            android.util.Log.e("InstantLedger", "InstantLedgerApplication: Exception: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
        
        // Schedule daily summary notification at 9 PM
        DailySummaryScheduler.scheduleDailySummary(this)
        
        // Schedule pending reminder notifications
        com.instantledger.worker.PendingReminderScheduler.scheduleReminders(this)
    }
}
