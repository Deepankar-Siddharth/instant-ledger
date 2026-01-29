package com.instantledger.domain.parser

import com.instantledger.data.model.*
import java.security.MessageDigest

class TransactionParser(
    private val smsParser: SMSParser
) {

    companion object {
        /** Normalize SMS text for consistent hashing (multipart, trim, whitespace). */
        fun normalizeForHash(text: String): String {
            return text.trim()
                .replace(Regex("\\s+"), " ")
                .replace(Regex("[\r\n]+"), " ")
        }

        /** Compute duplicate-check hash from normalized text. */
        fun hashForDuplicateCheck(normalizedText: String): String {
            if (normalizedText.isBlank()) return ""
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(normalizedText.toByteArray())
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }

    fun parseSMSMessage(
        messageBody: String,
        timestamp: Long
    ): Transaction? {
        if (messageBody.isBlank()) return null
        if (timestamp <= 0L) return null

        // Validation gate: must imply money movement; exclude OTP, balance-only, failed, promo
        if (!SMSValidationGate.shouldAcceptAsTransaction(messageBody)) return null

        val parsed = try {
            smsParser.parseSMS(messageBody)
        } catch (e: Exception) {
            return null
        }

        if (parsed.amount == null || !parsed.amount.isFinite() || parsed.amount <= 0) return null
        if (parsed.confidenceScore < 0.4f) return null

        val normalizedBody = normalizeForHash(messageBody)
        val hash = hashForDuplicateCheck(normalizedBody)
        val merchant = parsed.merchant?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown"
        val transactionType = parsed.transactionType ?: TransactionType.DEBIT
        val paymentMode = parsed.paymentMode ?: PaymentMode.UPI
        val now = System.currentTimeMillis()

        return Transaction(
            id = 0,
            timestamp = timestamp,
            amount = parsed.amount,
            merchant = merchant,
            category = null,
            accountType = parsed.accountType,
            transactionType = transactionType,
            paymentMode = paymentMode,
            rawTextHash = hash,
            sourceType = SourceType.SMS,
            entryType = EntryType.AUTO_CAPTURED,
            confidenceScore = parsed.confidenceScore,
            isRecurring = false,
            projectId = null,
            notes = null,
            status = TransactionStatus.DETECTED,
            createdAt = now,
            updatedAt = now,
            isApproved = false
        )
    }
    
    fun parseManualEntry(
        amount: Double,
        merchant: String,
        category: String?,
        paymentMode: PaymentMode,
        timestamp: Long,
        notes: String?,
        transactionType: TransactionType = TransactionType.DEBIT
    ): Transaction {
        val now = System.currentTimeMillis()
        
        return Transaction(
            id = 0,
            timestamp = timestamp,
            amount = amount,
            merchant = merchant.trim(),
            category = category,
            accountType = null,
            transactionType = transactionType,
            paymentMode = paymentMode,
            rawTextHash = null,
            sourceType = SourceType.MANUAL,
            entryType = EntryType.USER_ENTERED,
            confidenceScore = 1.0f, // Manual entries are always 100% confident
            isRecurring = false,
            projectId = null,
            notes = notes,
            createdAt = now,
            updatedAt = now
        )
    }
    
}
