package com.instantledger.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.instantledger.worker.SmsProcessWorker
import java.util.concurrent.TimeUnit

/**
 * Legacy entry point: if started with SMS body + timestamp, forwards to WorkManager
 * and stops. No database or parsing in this process — single source of truth in SmsProcessWorker.
 */
class TransactionCaptureService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val body = intent?.getStringExtra(EXTRA_SMS_BODY)
            val timestamp = intent?.getLongExtra(EXTRA_TIMESTAMP, 0L) ?: 0L
            if (!body.isNullOrBlank() && timestamp > 0L) {
                val workData = workDataOf(
                    SmsProcessWorker.INPUT_SMS_BODY to body,
                    SmsProcessWorker.INPUT_TIMESTAMP to timestamp
                )
                val work = OneTimeWorkRequestBuilder<SmsProcessWorker>()
                    .setInputData(workData)
                    .setInitialDelay(0, TimeUnit.MILLISECONDS)
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(work)
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionCaptureService", "Enqueue failed: ${e.javaClass.simpleName}", e)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_SMS_BODY = "sms_body"
        const val EXTRA_TIMESTAMP = "timestamp"
    }
}
