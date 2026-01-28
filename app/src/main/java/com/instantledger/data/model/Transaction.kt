package com.instantledger.data.model

import com.instantledger.data.database.entities.TransactionEntity

data class Transaction(
    val id: Long = 0,
    val timestamp: Long,
    val amount: Double,
    val merchant: String, // Original parsed merchant name
    val merchantOverride: String? = null, // Transaction-level override (takes priority)
    // Legacy category name (will be kept for backward compatibility in UI)
    val category: String?,
    // Versioned category reference
    val categoryId: String? = null,
    val categoryNameSnapshot: String? = null,
    val accountType: String?,
    val transactionType: TransactionType,
    val paymentMode: PaymentMode,
    val rawTextHash: String?,
    val sourceType: SourceType,
    val entryType: EntryType,
    val confidenceScore: Float,
    val isRecurring: Boolean,
    val projectId: String?,
    val notes: String?,
    val status: TransactionStatus = TransactionStatus.CONFIRMED,
    val schemaVersion: Int = 1,
    val parserVersion: Int = 1,
    val senderId: String? = null,
    val senderTrustScore: Float? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isApproved: Boolean = false,
    val tripId: Long? = null
) {
    fun toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            timestamp = timestamp,
            amount = amount,
            merchant = merchant,
            merchantOverride = merchantOverride,
            // Persist both legacy category and versioned category fields
            category = category ?: categoryNameSnapshot,
            categoryId = categoryId,
            categoryNameSnapshot = categoryNameSnapshot ?: category,
            accountType = accountType,
            transactionType = transactionType.name,
            paymentMode = paymentMode.name,
            rawTextHash = rawTextHash,
            sourceType = sourceType.name,
            entryType = entryType.name,
            confidenceScore = confidenceScore,
            isRecurring = isRecurring,
            projectId = projectId,
            notes = notes,
            status = status.name,
            schemaVersion = schemaVersion,
            parserVersion = parserVersion,
            senderId = senderId,
            senderTrustScore = senderTrustScore,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isApproved = isApproved,
            tripId = tripId
        )
    }
}

enum class TransactionType {
    DEBIT, CREDIT
}

enum class PaymentMode {
    UPI, CARD, CASH, BANK
}

enum class SourceType {
    SMS, NOTIFICATION, MANUAL, EMAIL
}

enum class EntryType {
    AUTO_CAPTURED, USER_ENTERED, USER_MODIFIED
}
