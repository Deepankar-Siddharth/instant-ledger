package com.instantledger.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PendingReminderScheduler {
    private const val REMINDER_INTERVAL_HOURS = 24L // Once per day maximum
    
    /**
     * Schedule daily reminder notifications
     */
    fun scheduleReminders(context: Context) {
        val reminderWork = PeriodicWorkRequestBuilder<PendingReminderWorker>(
            REMINDER_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .addTag("pending_reminder")
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PendingReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            reminderWork
        )
    }
    
    /**
     * Cancel reminder notifications
     */
    fun cancelReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PendingReminderWorker.WORK_NAME)
    }
}
