package com.instantledger.data.model

/**
 * Represents a change to a transaction field.
 * Used for audit trail and debugging.
 */
data class TransactionChangeLog(
    val id: Long = 0,
    val transactionId: Long,
    val field: String,
    val oldValue: String?,
    val newValue: String?,
    val changedAt: Long,
    val source: ChangeSource
)

enum class ChangeSource {
    USER_EDIT,           // User manually edited transaction
    AUTO_CLASSIFICATION,  // Automatic in-app classification
    BULK_UPDATE,         // Bulk operation
    MIGRATION,           // Database migration
    PARSER_UPDATE,       // Parser re-processed transaction
    SYSTEM_CORRECTION    // System fixed invalid data
}
