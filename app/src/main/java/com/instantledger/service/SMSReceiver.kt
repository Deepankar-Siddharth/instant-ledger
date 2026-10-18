package com.instantledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.instantledger.worker.SmsProcessWorker
import java.util.concurrent.TimeUnit

/**
 * Receives SMS and forwards raw message + timestamp to background processing only.
 * No database, no parsing, no heavy work — only extract and enqueue WorkManager.
 */
class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                Log.d(TAG, "Ignoring action=${intent?.action}")
                return
            }
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            if (messages.isEmpty()) {
                Log.d(TAG, "No message parts")
                return
            }

            // Combine multipart into one body (handle once)
            val combinedBody = messages.mapNotNull { it.messageBody }.joinToString("")
            val timestamp = messages[0].timestampMillis

            if (combinedBody.isBlank()) {
                Log.d(TAG, "Blank body after combine, skipping")
                return
            }

            val workData = workDataOf(
                SmsProcessWorker.INPUT_SMS_BODY to combinedBody,
                SmsProcessWorker.INPUT_TIMESTAMP to timestamp
            )
            val work = OneTimeWorkRequestBuilder<SmsProcessWorker>()
                .setInputData(workData)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueue(work)
            Log.d(TAG, "Enqueued SMS work (parts=${messages.size}, len=${combinedBody.length})")
        } catch (e: Exception) {
            Log.e(TAG, "onReceive failed: ${e.javaClass.simpleName}", e)
        }
    }

    companion object {
        private const val TAG = "InstantLedger.SMS"
    }
}
