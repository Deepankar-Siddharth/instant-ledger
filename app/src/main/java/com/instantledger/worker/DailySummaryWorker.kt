package com.instantledger.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.instantledger.R
import com.instantledger.data.model.TransactionType
import com.instantledger.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "daily_summary_channel"
        private const val NOTIFICATION_ID = 2000
    }

    override suspend fun doWork(): Result {
        return try {
            // Create notification channel
            createNotificationChannel()
            
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1
            
            val allTransactions = transactionRepository.getAllTransactions().first()
            val todayTransactions = allTransactions.filter {
                it.timestamp >= startOfDay && it.timestamp <= endOfDay
            }
            
            if (todayTransactions.isEmpty()) {
                // No transactions today, don't show notification
                return Result.success()
            }
            
            val totalDebit = todayTransactions
                .filter { it.transactionType == TransactionType.DEBIT }
                .sumOf { it.amount }
            
            val totalCredit = todayTransactions
                .filter { it.transactionType == TransactionType.CREDIT }
                .sumOf { it.amount }
            
            val netAmount = totalCredit - totalDebit
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            
            val summaryText = buildString {
                append("Today's Summary:\n")
                if (totalDebit > 0) {
                    append("Spent: ${currencyFormat.format(totalDebit)}\n")
                }
                if (totalCredit > 0) {
                    append("Received: ${currencyFormat.format(totalCredit)}\n")
                }
                append("Net: ${currencyFormat.format(netAmount)}")
            }
            
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Daily Transaction Summary")
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Daily Summary",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily transaction summary notifications"
            }
            val notificationManager = applicationContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
