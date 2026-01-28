package com.instantledger.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.instantledger.R
import com.instantledger.data.model.Transaction
import com.instantledger.data.model.TransactionType
import com.instantledger.data.model.PaymentMode
import com.instantledger.data.model.SourceType
import com.instantledger.data.model.EntryType
import com.instantledger.data.model.TransactionStatus
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.data.repository.UnverifiedTransactionRepository
import com.instantledger.data.database.entities.UnverifiedTransactionEntity
import com.instantledger.domain.parser.TransactionParser
import com.instantledger.domain.parser.SMSParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransactionCaptureService : Service() {
    
    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    @Inject
    lateinit var unverifiedTransactionRepository: UnverifiedTransactionRepository
    
    @Inject
    lateinit var notificationManager: com.instantledger.notification.TransactionNotificationManager
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val transactionParser = TransactionParser(SMSParser())
    
    companion object {
        const val EXTRA_SMS_BODY = "sms_body"
        const val EXTRA_TIMESTAMP = "timestamp"
        // Confidence threshold for quarantine: transactions below this go to quarantine table
        private const val QUARANTINE_THRESHOLD = 0.6f
    }
    
    override fun onCreate() {
        super.onCreate()
        // No-op: service runs briefly to parse and save; no user-visible UI
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val smsBody = intent?.getStringExtra(EXTRA_SMS_BODY)
        val timestamp = intent?.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        
        android.util.Log.d("InstantLedger", "TransactionCaptureService: onStartCommand called")
        
        if (smsBody != null && timestamp != null) {
            android.util.Log.d("InstantLedger", "TransactionCaptureService: Processing SMS [Length: ${smsBody?.length ?: 0} chars]")
            processTransaction(smsBody, timestamp)
        } else {
            android.util.Log.e("InstantLedger", "TransactionCaptureService: Missing SMS body or timestamp")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun processTransaction(smsBody: String, timestamp: Long) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("InstantLedger", "TransactionCaptureService: Parsing SMS")
                // Parse the SMS
                val parsed = transactionParser.parseSMSMessage(smsBody, timestamp)
                
                val transactionToSave: Transaction = if (parsed != null) {
                    // Log transaction parsed without sensitive details
                    android.util.Log.d("InstantLedger", "TransactionCaptureService: Parsed transaction - isApproved: ${parsed.isApproved}")
                    parsed.copy(isApproved = false)
                } else {
                    android.util.Log.w("InstantLedger", "TransactionCaptureService: Parser returned null, creating fallback debug transaction")
                    // Fallback "debug" transaction for ANY SMS so pipeline can be verified end-to-end
                    val now = System.currentTimeMillis()
                    val preview = smsBody.take(80)
                    Transaction(
                        id = 0,
                        timestamp = timestamp,
                        amount = 0.0,
                        merchant = "Unknown",
                        category = null,
                        categoryId = null,
                        categoryNameSnapshot = null,
                        accountType = null,
                        transactionType = TransactionType.DEBIT,
                        paymentMode = PaymentMode.CASH,
                        rawTextHash = smsBody.hashCode().toString(),
                        sourceType = SourceType.SMS,
                        entryType = EntryType.AUTO_CAPTURED,
                        confidenceScore = 0f,
                        isRecurring = false,
                        projectId = null,
                        notes = preview,
                        status = TransactionStatus.DETECTED,
                        schemaVersion = 1,
                        parserVersion = 0,
                        senderId = null,
                        senderTrustScore = null,
                        createdAt = now,
                        updatedAt = now,
                        isApproved = false,
                        tripId = null
                    )
                }

                // Check for duplicates using rawTextHash when available
                val isDuplicate = transactionToSave.rawTextHash?.let { hash ->
                    transactionRepository.isDuplicate(hash)
                } ?: false
                
                if (!isDuplicate) {
                    try {
                        // Quarantine logic: confidence < 0.6 → quarantine table, ≥ 0.6 → main table
                        if (transactionToSave.confidenceScore < QUARANTINE_THRESHOLD) {
                            // Route to quarantine table
                            val unverifiedEntity = UnverifiedTransactionEntity(
                                id = 0,
                                rawText = smsBody,
                                parsedAmount = transactionToSave.amount,
                                parsedMerchant = transactionToSave.merchant,
                                confidenceScore = transactionToSave.confidenceScore,
                                senderId = transactionToSave.senderId,
                                senderTrustScore = transactionToSave.senderTrustScore,
                                timestamp = transactionToSave.timestamp,
                                createdAt = transactionToSave.createdAt
                            )
                            val quarantineId = unverifiedTransactionRepository.insertUnverifiedTransaction(unverifiedEntity)
                            android.util.Log.d(
                                "InstantLedger",
                                "TransactionCaptureService: Low-confidence transaction routed to quarantine (ID=$quarantineId, confidence=${transactionToSave.confidenceScore})"
                            )
                        } else {
                            // Route to main transactions table
                            val insertedId = transactionRepository.insertTransaction(transactionToSave)
                            android.util.Log.d(
                                "InstantLedger",
                                "TransactionCaptureService: Transaction saved with ID=$insertedId (approved=${transactionToSave.isApproved}, confidence=${transactionToSave.confidenceScore}, debugFallback=${parsed == null})"
                            )
                            
                            // Show instant notification for new pending transaction
                            if (!transactionToSave.isApproved && transactionToSave.entryType == EntryType.AUTO_CAPTURED) {
                                notificationManager.showInstantNotification(transactionToSave)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("InstantLedger", "TransactionCaptureService: Failed to save transaction to database", e)
                        android.util.Log.e("InstantLedger", "TransactionCaptureService: Exception: ${e.javaClass.simpleName}: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    android.util.Log.d("InstantLedger", "TransactionCaptureService: Duplicate transaction detected, skipping")
                }
            } catch (e: Exception) {
                android.util.Log.e("InstantLedger", "TransactionCaptureService: Error processing transaction", e)
                e.printStackTrace()
            } finally {
                stopSelf()
            }
        }
    }
}
