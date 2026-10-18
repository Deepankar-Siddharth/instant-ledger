package com.instantledger.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.instantledger.worker.SmsProcessWorker
import java.util.concurrent.TimeUnit

/**
 * Listens to notifications (e.g. from messaging app) and forwards body + timestamp
 * to the same background Worker as SMS — single source of truth, duplicate-safe.
 */
class NotificationTransactionListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            if (sbn == null) return
            val packageName = sbn.packageName ?: return
            if (packageName != "com.google.android.apps.messaging") return

            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
            val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

            val body = listOf(title, text, bigText)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            if (body.isBlank()) return

            val timestamp = sbn.postTime
            val workData = workDataOf(
                SmsProcessWorker.INPUT_SMS_BODY to body,
                SmsProcessWorker.INPUT_TIMESTAMP to timestamp
            )
            val work = OneTimeWorkRequestBuilder<SmsProcessWorker>()
                .setInputData(workData)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(work)
            Log.d(TAG, "Enqueued notification work (len=${body.length})")
        } catch (e: Exception) {
            Log.e(TAG, "onNotificationPosted failed: ${e.javaClass.simpleName}", e)
        }
    }

    companion object {
        private const val TAG = "InstantLedger.NotifListener"
    }
}
