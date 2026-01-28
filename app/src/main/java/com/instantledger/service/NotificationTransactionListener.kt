package com.instantledger.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.domain.parser.SMSParser
import com.instantledger.domain.parser.TransactionParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Intent

/**
 * Listens to notification text (e.g., from Google Messages) and treats
 * bank-style notifications as incoming transactions.
 *
 * This allows auto-capture even when Instant Ledger is NOT the default SMS app.
 */
@AndroidEntryPoint
class NotificationTransactionListener : NotificationListenerService() {

    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    @Inject
    lateinit var notificationManager: com.instantledger.notification.TransactionNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val transactionParser = TransactionParser(SMSParser())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // Only react to notifications from messaging apps
        val packageName = sbn.packageName ?: return
        if (packageName != "com.google.android.apps.messaging") {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        // Combine text parts into a single body for parsing
        val body = listOf(title, text, bigText)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (body.isBlank()) return

        Log.d("InstantLedger", "NotificationTransactionListener: Received notification from $packageName, length=${body.length}")

        scope.launch {
            try {
                val timestamp = sbn.postTime
                val tx = transactionParser.parseSMSMessage(body, timestamp)
                if (tx == null) {
                    Log.d("InstantLedger", "NotificationTransactionListener: Parser returned null (low confidence or no amount)")
                    return@launch
                }

                // Check duplicates
                val isDuplicate = tx.rawTextHash?.let { transactionRepository.isDuplicate(it) } ?: false
                if (isDuplicate) {
                    Log.d("InstantLedger", "NotificationTransactionListener: Duplicate transaction detected, skipping")
                    return@launch
                }

                // Save as pending approval; user will review inside the app (no overlay)
                val pendingTx = tx.copy(isApproved = false)
                val id = transactionRepository.insertTransaction(pendingTx)
                Log.d("InstantLedger", "NotificationTransactionListener: Saved transaction from notification with ID=$id (pending approval)")
                
                // Show instant notification for new pending transaction
                notificationManager.showInstantNotification(pendingTx)
            } catch (e: Exception) {
                Log.e("InstantLedger", "NotificationTransactionListener: Error processing notification", e)
            }
        }
    }
}

