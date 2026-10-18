package com.instantledger.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.instantledger.R
import com.instantledger.data.model.Transaction
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.data.preferences.EncryptedSettingsPreferences
import com.instantledger.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

class TransactionNotificationManager(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val settingsPreferences: EncryptedSettingsPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val CHANNEL_ID = "transaction_notifications"
        private const val CHANNEL_NAME = "Transaction Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for detected transactions"
        
        // Notification IDs
        private const val INSTANT_NOTIFICATION_ID = 1000
        private const val GROUPED_NOTIFICATION_ID = 1001
        private const val GROUP_KEY = "pending_transactions_group"
        
        // Intent extras
        const val EXTRA_OPEN_PENDING = "open_pending"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT // Not high priority, no heads-up
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(false)
                enableLights(false)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Show instant notification when a new transaction is detected
     */
    fun showInstantNotification(transaction: Transaction) {
        // Check if notifications are enabled
        if (!settingsPreferences.areNewTransactionNotificationsEnabled()) {
            return
        }
        
        scope.launch {
            try {
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                val amountText = currencyFormat.format(transaction.amount)
                // Use merchant override if present, otherwise use merchant field
                val merchantText = transaction.merchantOverride?.takeIf { it.isNotBlank() }
                    ?: transaction.merchant.takeIf { it.isNotBlank() && it != "Unknown" }
                    ?: transaction.category?.takeIf { it.isNotBlank() }
                    ?: "Transaction"
                
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_OPEN_PENDING, true)
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    INSTANT_NOTIFICATION_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Transaction detected")
                    .setContentText("$amountText at $merchantText")
                    .setSubText("Select a category to approve")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Not high priority
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setGroup(GROUP_KEY)
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("$amountText at $merchantText\nSelect a category to approve"))
                    .build()
                
                NotificationManagerCompat.from(context).notify(INSTANT_NOTIFICATION_ID, notification)
                
                // Check if we should show grouped summary
                checkAndShowGroupedSummary()
            } catch (e: Exception) {
                android.util.Log.e("InstantLedger", "TransactionNotificationManager: Failed to show instant notification", e)
            }
        }
    }
    
    /**
     * Show grouped summary notification when 2+ pending transactions exist
     */
    private suspend fun checkAndShowGroupedSummary() {
        val allTransactions = transactionRepository.getAllTransactions().first()
        // Pending = transactions without category OR not approved
        val pendingCount = allTransactions.count { 
            it.category.isNullOrBlank() || !it.isApproved 
        }
        
        if (pendingCount >= 2) {
            showGroupedSummary(pendingCount)
        } else if (pendingCount == 0) {
            // Cancel all notifications when count reaches zero
            cancelAllNotifications()
        }
    }
    
    /**
     * Show grouped summary notification
     */
    private fun showGroupedSummary(pendingCount: Int) {
        if (!settingsPreferences.areNewTransactionNotificationsEnabled()) {
            return
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_PENDING, true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            GROUPED_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("You have pending transactions")
            .setContentText("$pendingCount transactions need a category")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$pendingCount transactions need your confirmation"))
            .build()
        
        NotificationManagerCompat.from(context).notify(GROUPED_NOTIFICATION_ID, notification)
    }
    
    /**
     * Cancel all pending transaction notifications
     */
    fun cancelAllNotifications() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(INSTANT_NOTIFICATION_ID)
        notificationManager.cancel(GROUPED_NOTIFICATION_ID)
    }
    
    /**
     * Update grouped summary when pending count changes
     */
    fun updateGroupedSummary() {
        scope.launch {
            checkAndShowGroupedSummary()
        }
    }
}
