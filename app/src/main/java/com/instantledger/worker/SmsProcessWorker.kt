package com.instantledger.worker

import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.instantledger.data.model.EntryType
import com.instantledger.data.model.SourceType
import com.instantledger.data.model.TransactionStatus
import com.instantledger.data.preferences.IgnoredHashesStore
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.domain.parser.TransactionParser
import com.instantledger.domain.parser.SMSParser
import com.instantledger.notification.TransactionNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single source of truth for SMS → transaction: runs on background thread,
 * checks duplicate by hash before insert, validates via gate, never crashes.
 */
@HiltWorker
class SmsProcessWorker @AssistedInject constructor(
    @Assisted private val context: android.content.Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val notificationManager: TransactionNotificationManager
) : CoroutineWorker(context, params) {

    private val transactionParser = TransactionParser(SMSParser())

    companion object {
        const val INPUT_SMS_BODY = "sms_body"
        const val INPUT_TIMESTAMP = "sms_timestamp"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val body = inputData.getString(INPUT_SMS_BODY) ?: run {
                android.util.Log.w("SmsProcessWorker", "Missing body, skipping")
                return@withContext Result.success()
            }
            val timestamp = inputData.getLong(INPUT_TIMESTAMP, 0L)
            if (timestamp <= 0L) {
                android.util.Log.w("SmsProcessWorker", "Invalid timestamp, skipping")
                return@withContext Result.success()
            }

            // 1) Duplicate + user-ignored check BEFORE parsing (normalized hash)
            val normalizedBody = TransactionParser.normalizeForHash(body)
            val hash = TransactionParser.hashForDuplicateCheck(normalizedBody)
            if (transactionRepository.isDuplicate(hash)) {
                android.util.Log.d("SmsProcessWorker", "Duplicate hash, skipping")
                return@withContext Result.success()
            }
            val ignoredStore = IgnoredHashesStore(context.applicationContext)
            if (ignoredStore.contains(hash)) {
                android.util.Log.d("SmsProcessWorker", "User previously ignored this transaction, skipping")
                return@withContext Result.success()
            }

            // 2) Parse + validation gate (parser returns null if invalid or gate fails)
            val transaction = transactionParser.parseSMSMessage(body, timestamp)
            if (transaction == null) {
                android.util.Log.d("SmsProcessWorker", "Parse or validation gate rejected, discarding")
                return@withContext Result.success()
            }

            // 3) Guard nulls (parser should not return null amount; defensive)
            val amount = transaction.amount
            if (amount == null || !amount.isFinite() || amount <= 0) {
                android.util.Log.w("SmsProcessWorker", "Invalid amount, skipping")
                return@withContext Result.success()
            }
            val merchant = transaction.merchant?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown"

            val toInsert = transaction.copy(
                amount = amount,
                merchant = merchant,
                isApproved = false
            )

            // 4) Insert (duplicate check already done; hash is set in transaction)
            transactionRepository.insertTransaction(toInsert)
            android.util.Log.d("SmsProcessWorker", "Saved transaction (pending approval)")

            // 5) Notify user
            if (toInsert.entryType == EntryType.AUTO_CAPTURED) {
                notificationManager.showInstantNotification(toInsert)
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SmsProcessWorker", "Processing failed: ${e.javaClass.simpleName}", e)
            Result.success() // Do not retry; discard to avoid crash loop
        }
    }
}
