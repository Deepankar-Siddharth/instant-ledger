package com.instantledger.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.instantledger.data.model.Transaction
import com.instantledger.data.model.EntryType
import com.instantledger.data.model.PaymentMode
import com.instantledger.data.model.SourceType
import com.instantledger.data.model.TransactionType
import com.instantledger.data.model.TransactionStatus

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "amount")
    val amount: Double,
    
    @ColumnInfo(name = "merchant")
    val merchant: String, // Original parsed merchant name
    
    @ColumnInfo(name = "merchant_override")
    val merchantOverride: String?, // Transaction-level override (takes priority)
    
    @ColumnInfo(name = "category")
    val category: String?, // Legacy field - kept for backward compatibility
    
    @ColumnInfo(name = "category_id")
    val categoryId: String?, // UUID of category (for versioning)
    
    @ColumnInfo(name = "category_name_snapshot")
    val categoryNameSnapshot: String?, // Snapshot of category name at time of transaction
    
    @ColumnInfo(name = "account_type")
    val accountType: String?,
    
    @ColumnInfo(name = "transaction_type")
    val transactionType: String, // DEBIT or CREDIT
    
    @ColumnInfo(name = "payment_mode")
    val paymentMode: String, // UPI, CARD, CASH, BANK
    
    @ColumnInfo(name = "raw_text_hash")
    val rawTextHash: String?,
    
    @ColumnInfo(name = "source_type")
    val sourceType: String, // SMS, NOTIFICATION, MANUAL, EMAIL
    
    @ColumnInfo(name = "entry_type")
    val entryType: String, // AUTO_CAPTURED, USER_ENTERED, USER_MODIFIED
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float,
    
    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean,
    
    @ColumnInfo(name = "project_id")
    val projectId: String?,
    
    @ColumnInfo(name = "notes")
    val notes: String?,
    
    @ColumnInfo(name = "status")
    val status: String = "CONFIRMED", // DETECTED, CONFIRMED, MODIFIED, IGNORED
    
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int = 1,
    
    @ColumnInfo(name = "parser_version")
    val parserVersion: Int = 1,
    
    @ColumnInfo(name = "sender_id")
    val senderId: String?,
    
    @ColumnInfo(name = "sender_trust_score")
    val senderTrustScore: Float?,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "is_approved")
    val isApproved: Boolean = false,

    @ColumnInfo(name = "trip_id")
    val tripId: Long? = null
) {
    fun toDomain(): Transaction {
        return Transaction(
            id = id,
            timestamp = timestamp,
            amount = amount,
            merchant = merchant,
            merchantOverride = merchantOverride,
            // Prefer snapshot if present, otherwise fall back to legacy category
            category = category ?: categoryNameSnapshot,
            categoryId = categoryId,
            categoryNameSnapshot = categoryNameSnapshot,
            accountType = accountType,
            transactionType = TransactionType.valueOf(transactionType),
            paymentMode = PaymentMode.valueOf(paymentMode),
            rawTextHash = rawTextHash,
            sourceType = SourceType.valueOf(sourceType),
            entryType = EntryType.valueOf(entryType),
            confidenceScore = confidenceScore,
            isRecurring = isRecurring,
            projectId = projectId,
            notes = notes,
            status = try { TransactionStatus.valueOf(status) } catch (e: Exception) { TransactionStatus.CONFIRMED },
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
