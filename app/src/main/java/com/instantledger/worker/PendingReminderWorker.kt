package com.instantledger.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.instantledger.R
import com.instantledger.data.model.EntryType
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.data.preferences.EncryptedSettingsPreferences
import com.instantledger.ui.main.MainActivity
import com.instantledger.notification.TransactionNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class PendingReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val settingsPreferences: EncryptedSettingsPreferences
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val CHANNEL_ID = "pending_reminder_notifications"
        private const val CHANNEL_NAME = "Pending Reminders"
        private const val NOTIFICATION_ID = 2000
        const val WORK_NAME = "pending_reminder_work"
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Check if reminder notifications are enabled
            if (!settingsPreferences.areReminderNotificationsEnabled()) {
                return Result.success()
            }
            
            // Get pending transactions count
            // Pending = transactions without category OR not approved
            val allTransactions = transactionRepository.getAllTransactions().first()
            val pendingCount = allTransactions.count { 
                it.category.isNullOrBlank() || !it.isApproved 
            }
            
            if (pendingCount == 0) {
                // No pending transactions, cancel any existing notifications
                NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
                return Result.success()
            }
            
            // Create notification channel
            createNotificationChannel()
            
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TransactionNotificationManager.EXTRA_OPEN_PENDING, true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Reminder: transactions need review")
                .setContentText("You have $pendingCount transaction${if (pendingCount > 1) "s" else ""} without categories")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Not high priority
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("You have $pendingCount transaction${if (pendingCount > 1) "s" else ""} that need a category to be approved."))
                .build()
            
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("InstantLedger", "PendingReminderWorker: Error showing reminder", e)
            Result.retry()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // Not high priority
            ).apply {
                description = "Reminders for pending transactions"
                enableVibration(false)
                enableLights(false)
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
