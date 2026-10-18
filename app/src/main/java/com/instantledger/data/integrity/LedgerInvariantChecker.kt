package com.instantledger.data.integrity

import com.instantledger.data.model.*
import android.util.Log

/**
 * Internal integrity checker that validates transaction data invariants.
 * Prevents silent corruption by catching invalid states.
 */
object LedgerInvariantChecker {
    
    private const val TAG = "LedgerInvariantChecker"
    
    /**
     * Validation result for a transaction
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )
    
    /**
     * Validate a single transaction against all invariants
     */
    fun validateTransaction(transaction: Transaction): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Invariant 1: Amount must be positive
        if (transaction.amount < 0) {
            errors.add("Transaction has negative amount: ${transaction.amount}")
        }
        
        // Invariant 2: Amount must be finite (not NaN or Infinity)
        if (!transaction.amount.isFinite()) {
            errors.add("Transaction amount is not finite: ${transaction.amount}")
        }
        
        // Invariant 3: Timestamp must be valid (positive and reasonable)
        if (transaction.timestamp <= 0) {
            errors.add("Transaction has invalid timestamp: ${transaction.timestamp}")
        }
        
        // Check if timestamp is in the future (more than 1 hour ahead)
        val now = System.currentTimeMillis()
        val oneHourFromNow = now + (60 * 60 * 1000)
        if (transaction.timestamp > oneHourFromNow) {
            warnings.add("Transaction timestamp is more than 1 hour in the future")
        }
        
        // Check if timestamp is too old (more than 10 years ago)
        val tenYearsAgo = now - (10L * 365 * 24 * 60 * 60 * 1000)
        if (transaction.timestamp < tenYearsAgo) {
            warnings.add("Transaction timestamp is more than 10 years old")
        }
        
        // Invariant 4: TransactionType must be valid enum
        try {
            TransactionType.valueOf(transaction.transactionType.name)
        } catch (e: IllegalArgumentException) {
            errors.add("Transaction has invalid transaction type: ${transaction.transactionType}")
        }
        
        // Invariant 5: PaymentMode must be valid enum
        try {
            PaymentMode.valueOf(transaction.paymentMode.name)
        } catch (e: IllegalArgumentException) {
            errors.add("Transaction has invalid payment mode: ${transaction.paymentMode}")
        }
        
        // Invariant 6: SourceType must be valid enum
        try {
            SourceType.valueOf(transaction.sourceType.name)
        } catch (e: IllegalArgumentException) {
            errors.add("Transaction has invalid source type: ${transaction.sourceType}")
        }
        
        // Invariant 7: EntryType must be valid enum
        try {
            EntryType.valueOf(transaction.entryType.name)
        } catch (e: IllegalArgumentException) {
            errors.add("Transaction has invalid entry type: ${transaction.entryType}")
        }
        
        // Invariant 8: Status must be valid enum
        try {
            TransactionStatus.valueOf(transaction.status.name)
        } catch (e: IllegalArgumentException) {
            errors.add("Transaction has invalid status: ${transaction.status}")
        }
        
        // Invariant 9: Confidence score must be in valid range [0.0, 1.0]
        if (transaction.confidenceScore < 0.0f || transaction.confidenceScore > 1.0f) {
            errors.add("Transaction confidence score out of range [0.0, 1.0]: ${transaction.confidenceScore}")
        }
        
        // Invariant 10: Cannot have isApproved=false AND status=CONFIRMED
        if (!transaction.isApproved && transaction.status == TransactionStatus.CONFIRMED) {
            errors.add("Transaction cannot be unapproved (isApproved=false) and confirmed (status=CONFIRMED) simultaneously")
        }
        
        // Invariant 11: Merchant should not be empty for non-ignored transactions
        if (transaction.merchant.isBlank() && transaction.category != "Ignore") {
            warnings.add("Transaction has empty merchant name but is not ignored")
        }
        
        // Invariant 12: Category should be set for approved transactions
        if (transaction.isApproved && transaction.category.isNullOrBlank()) {
            warnings.add("Approved transaction has no category assigned")
        }
        
        // Invariant 13: Schema and parser versions should be positive
        if (transaction.schemaVersion < 1) {
            errors.add("Transaction has invalid schema version: ${transaction.schemaVersion}")
        }
        if (transaction.parserVersion < 1) {
            errors.add("Transaction has invalid parser version: ${transaction.parserVersion}")
        }
        
        // Invariant 14: Timestamps should be consistent
        if (transaction.createdAt > transaction.updatedAt) {
            errors.add("Transaction createdAt (${transaction.createdAt}) is after updatedAt (${transaction.updatedAt})")
        }
        
        // Invariant 15: Sender trust score should be in valid range if present
        transaction.senderTrustScore?.let { score ->
            if (score < 0.0f || score > 1.0f) {
                errors.add("Transaction sender trust score out of range [0.0, 1.0]: $score")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validate a list of transactions and return aggregate results
     */
    fun validateTransactions(transactions: List<Transaction>): ValidationResult {
        val allErrors = mutableListOf<String>()
        val allWarnings = mutableListOf<String>()
        var validCount = 0
        var invalidCount = 0
        
        transactions.forEach { transaction ->
            val result = validateTransaction(transaction)
            if (result.isValid) {
                validCount++
            } else {
                invalidCount++
                allErrors.addAll(result.errors.map { "Transaction ID ${transaction.id}: $it" })
            }
            allWarnings.addAll(result.warnings.map { "Transaction ID ${transaction.id}: $it" })
        }
        
        if (invalidCount > 0) {
            allErrors.add(0, "Found $invalidCount invalid transactions out of ${transactions.size} total")
        }
        
        return ValidationResult(
            isValid = invalidCount == 0,
            errors = allErrors,
            warnings = allWarnings
        )
    }
    
    /**
     * Run integrity check and log results
     * Should be called after migrations, bulk edits, and app updates
     */
    fun runIntegrityCheck(transactions: List<Transaction>, context: String = "Unknown"): Boolean {
        Log.d(TAG, "Running integrity check for context: $context")
        
        val result = validateTransactions(transactions)
        
        if (result.isValid) {
            if (result.warnings.isNotEmpty()) {
                Log.w(TAG, "Integrity check passed with ${result.warnings.size} warnings:")
                result.warnings.forEach { Log.w(TAG, "  - $it") }
            } else {
                Log.d(TAG, "Integrity check passed: All ${transactions.size} transactions are valid")
            }
            return true
        } else {
            Log.e(TAG, "Integrity check FAILED: Found ${result.errors.size} errors:")
            result.errors.forEach { Log.e(TAG, "  - $it") }
            
            if (result.warnings.isNotEmpty()) {
                Log.w(TAG, "Also found ${result.warnings.size} warnings:")
                result.warnings.forEach { Log.w(TAG, "  - $it") }
            }
            
            return false
        }
    }
}
