package com.instantledger.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object DailySummaryScheduler {
    private const val WORK_NAME = "daily_summary_work"
    
    fun scheduleDailySummary(context: Context) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21) // 9 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // If 9 PM has already passed today, schedule for tomorrow
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()
        
        val workRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    fun cancelDailySummary(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
