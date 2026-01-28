package com.instantledger.domain.parser

import com.instantledger.data.model.*
import java.security.MessageDigest

class TransactionParser(
    private val smsParser: SMSParser
) {
    
    fun parseSMSMessage(
        messageBody: String,
        timestamp: Long
    ): Transaction? {
        val parsed = smsParser.parseSMS(messageBody)
        
        // Validate that we have minimum required data
        if (parsed.amount == null || parsed.confidenceScore < 0.4f) {
            return null
        }
        
        // Generate hash for duplicate detection
        val hash = generateHash(messageBody)
        
        // Normalize merchant name
        val merchant = parsed.merchant?.trim() ?: "Unknown"
        
        // Default values
        val transactionType = parsed.transactionType ?: TransactionType.DEBIT
        val paymentMode = parsed.paymentMode ?: PaymentMode.UPI
        
        val now = System.currentTimeMillis()
        
        return Transaction(
            id = 0,
            timestamp = timestamp,
            amount = parsed.amount,
            merchant = merchant,
            category = null, // Will be set by user classification
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
            status = TransactionStatus.DETECTED, // Auto-captured transactions start as DETECTED
            createdAt = now,
            updatedAt = now,
            isApproved = false // Auto-captured transactions require approval
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
    
    private fun generateHash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(text.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
