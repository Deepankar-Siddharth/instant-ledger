package com.instantledger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

class SMSReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("InstantLedger", "SMSReceiver.onReceive() called with action=${intent.action}")
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d("InstantLedger", "SMSReceiver: SMS_RECEIVED_ACTION with ${messages.size} message parts")
            
            for (message in messages) {
                val sender = message.originatingAddress ?: "unknown"
                val messageBody = message.messageBody ?: ""
                val timestamp = message.timestampMillis
                
                // Debug logs for sender and body (trimmed for safety)
                Log.d(
                    "InstantLedger",
                    "SMSReceiver: From=$sender, BodyPreview=\"${messageBody.take(80)}\" (length=${messageBody.length})"
                )
                
                // Optional bank SMS heuristic (for logging only)
                val bankLike = isBankSMS(messageBody)
                Log.d("InstantLedger", "SMSReceiver: isBankSMS=$bankLike")
                
                // Always start capture service so ANY SMS creates/updates a pending transaction
                val serviceIntent = Intent(context, TransactionCaptureService::class.java).apply {
                    putExtra(TransactionCaptureService.EXTRA_SMS_BODY, messageBody)
                    putExtra(TransactionCaptureService.EXTRA_TIMESTAMP, timestamp)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d("InstantLedger", "SMSReceiver: TransactionCaptureService start requested")
                } catch (e: Exception) {
                    Log.e("InstantLedger", "SMSReceiver: Failed to start TransactionCaptureService", e)
                }
            }
        } else {
            Log.d("InstantLedger", "SMSReceiver: Ignoring non-SMS action=${intent.action}")
        }
    }
    
    private fun isBankSMS(messageBody: String): Boolean {
        val lowerMessage = messageBody.lowercase()
        
        // Check for common bank keywords
        val bankKeywords = listOf(
            "hdfc", "icici", "sbi", "axis", "kotak", "pnb", "boi",
            "debited", "credited", "balance", "account", "upi",
            "transaction", "payment", "withdrawal", "deposit"
        )
        
        // Broader amount patterns - more flexible
        val amountPatterns = listOf(
            Regex("Rs\\.?\\s?([0-9,.]+)", RegexOption.IGNORE_CASE),  // Rs. 500, Rs 500, Rs.500
            Regex("INR\\s?([0-9,.]+)", RegexOption.IGNORE_CASE),     // INR 500, INR500
            Regex("₹\\s?([0-9,.]+)", RegexOption.IGNORE_CASE),       // ₹ 500, ₹500
            Regex("([0-9,.]+)\\s?(?:Rs\\.?|INR|₹)", RegexOption.IGNORE_CASE), // 500 Rs, 500 INR
            Regex("(?:debited|credited|paid|spent)\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,.]+)", RegexOption.IGNORE_CASE),
            Regex("([0-9,.]+)\\s*(?:debited|credited|paid|spent)", RegexOption.IGNORE_CASE)
        )
        
        // Check if message contains bank keywords
        val hasBankKeyword = bankKeywords.any { lowerMessage.contains(it) }
        
        // Check if any amount pattern matches
        val hasAmount = amountPatterns.any { it.containsMatchIn(messageBody) }
        
        // Log match details
        if (hasAmount) {
            val matchedPattern = amountPatterns.firstOrNull { it.containsMatchIn(messageBody) }
            val matchResult = matchedPattern?.find(messageBody)
            val amount = matchResult?.groupValues?.getOrNull(1) ?: "unknown"
            Log.d("InstantLedger", "Match Found: $amount (pattern: ${matchedPattern?.pattern})")
        }
        
        // Return true if has bank keyword AND amount pattern
        return hasBankKeyword && hasAmount
    }
}
