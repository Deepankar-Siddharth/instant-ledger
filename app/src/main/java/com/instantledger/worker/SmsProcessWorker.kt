package com.instantledger.worker

import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Result
import androidx.work.WorkerParameters
import com.instantledger.data.model.EntryType
import com.instantledger.data.repository.TransactionRepository
import com.instantledger.domain.parser.SMSParser
import com.instantledger.domain.parser.TransactionParser
import com.instantledger.notification.TransactionNotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Single source of truth for SMS → transaction: runs on background thread,
 * checks duplicate by hash before insert, validates via gate, never crashes.
 *
 * Result contract:
 * - Result.success()  -> SMS processed, skipped (duplicate/invalid), or inserted; no error.
 * - Result.retry()    -> transient error (e.g. database locked); WorkManager will retry.
 * - Result.failure()  -> permanent error; no retry, no crash loop.
 *
 * All logging is operation-level only; no SMS body, amounts, or merchants are logged.
 */
@HiltWorker
class SmsProcessWorker @AssistedInject constructor(
    @Assisted private val context: android.content.Context,
    @Assisted params: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val notificationManager: TransactionNotificationManager,
    private val transactionParser: TransactionParser,
    private val smsParser: SMSParser
) : CoroutineWorker(context, params) {

    private val ignoredStore = IgnoredHashesStore(context.applicationContext)

    /** Duplicate time window in minutes for the time-window fallback check. */
    private const val DUPLICATE_WINDOW_MINUTES = 30L

    companion object {
        const val INPUT_SMS_BODY = "sms_body"
        const val INPUT_TIMESTAMP = "sms_timestamp"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return try {
            processSms()
        } catch (e: Exception) {
            logE("SMS processing failed", e)
            if (isTransient(e)) Result.retry() else Result.failure()
        }
    }

    private suspend fun processSms(): Result {
        val body = inputData.getString(INPUT_SMS_BODY) ?: return Result.success()
        val timestamp = inputData.getLong(INPUT_TIMESTAMP, 0L)
        if (timestamp <= 0L) return Result.success()

        // 1) Duplicate + user-ignored check BEFORE parsing (normalized hash)
        val normalizedBody = TransactionParser.normalizeForHash(body)
        val hash = TransactionParser.hashForDuplicateCheck(normalizedBody)
        if (transactionRepository.isDuplicate(hash)) {
            logD("Duplicate hash, skipping")
            return Result.success()
        }
        if (ignoredStore.contains(hash)) {
            logD("User previously ignored this transaction, skipping")
            return Result.success()
        }

        // 2) Parse + validation gate (parser returns null if invalid or gate fails)
        val transaction = transactionParser.parseSMSMessage(body, timestamp)
        if (transaction == null) {
            logD("Parse or validation gate rejected, discarding")
            return Result.success()
        }

        // 3) Guard nulls (parser should not return null amount; defensive)
        val amount = transaction.amount
        if (amount == null || !amount.isFinite() || amount <= 0) {
            logW("Invalid amount, skipping")
            return Result.success()
        }
        val merchant = transaction.merchant?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown"

        // 4) Time-window duplicate fallback (same amount + merchant + date within N minutes)
        if (transactionRepository.isRecentDuplicate(amount, merchant, timestamp, DUPLICATE_WINDOW_MINUTES)) {
            logD("Time-window duplicate, skipping")
            return Result.success()
        }

        // 5) Insert (duplicate check already done; hash is set in transaction)
        val toInsert = transaction.copy(
            amount = amount,
            merchant = merchant,
            isApproved = false
        )
        transactionRepository.insertTransaction(toInsert)
        logD("Saved transaction (pending approval)")

        // 6) Notify user (best-effort; notification failure must not fail the worker)
        if (toInsert.entryType == EntryType.AUTO_CAPTURED) {
            try {
                notificationManager.showInstantNotification(toInsert)
            } catch (e: Exception) {
                logW("Notification failed", e)
            }
        }

        Result.success()
    }

    /** Classifies exceptions: transient (retryable) vs permanent. */
    private fun isTransient(e: Throwable): Boolean = when (e) {
        is SQLiteDatabaseLockedException,
        is SQLiteFullException,
        is SQLiteException -> true
        else -> false
    }

    // ---------- operation-level logging only (no SMS body, amounts, or merchants) ----------
    private fun logD(msg: String) {
        android.util.Log.d(TAG, msg)
    }

    private fun logW(msg: String, t: Throwable? = null) {
        android.util.Log.w(TAG, msg, t)
    }

    private fun logE(msg: String, t: Throwable? = null) {
        android.util.Log.e(TAG, msg, t)
    }

    private companion object {
        private const val TAG = "SmsProcessWorker"
    }
}
