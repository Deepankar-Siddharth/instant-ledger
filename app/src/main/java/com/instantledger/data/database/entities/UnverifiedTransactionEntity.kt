package com.instantledger.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Quarantine table for low-confidence transactions
 * Transactions with confidence < 0.6 are stored here for user review
 */
@Entity(tableName = "unverified_transactions")
data class UnverifiedTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "raw_text")
    val rawText: String,
    
    @ColumnInfo(name = "parsed_amount")
    val parsedAmount: Double?,
    
    @ColumnInfo(name = "parsed_merchant")
    val parsedMerchant: String?,
    
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float,
    
    @ColumnInfo(name = "sender_id")
    val senderId: String?,
    
    @ColumnInfo(name = "sender_trust_score")
    val senderTrustScore: Float?,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
