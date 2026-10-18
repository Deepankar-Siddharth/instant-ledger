package com.instantledger.data.audit

import com.instantledger.data.database.dao.TransactionChangeLogDao
import com.instantledger.data.database.entities.TransactionChangeLogEntity
import com.instantledger.data.model.ChangeSource
import com.instantledger.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for logging transaction changes for audit trail.
 * Tracks what changed, when, and why.
 */
@Singleton
class TransactionAuditLogger @Inject constructor(
    private val changeLogDao: TransactionChangeLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Log changes between old and new transaction states
     */
    fun logTransactionChanges(
        oldTransaction: Transaction?,
        newTransaction: Transaction,
        source: ChangeSource
    ) {
        scope.launch {
            val changes = detectChanges(oldTransaction, newTransaction)
            if (changes.isNotEmpty()) {
                val changeLogs = changes.map { (field, oldValue, newValue) ->
                    TransactionChangeLogEntity(
                        transactionId = newTransaction.id,
                        field = field,
                        oldValue = oldValue,
                        newValue = newValue,
                        changedAt = System.currentTimeMillis(),
                        source = source.name
                    )
                }
                changeLogDao.insertChangeLogs(changeLogs)
                
                // Prune old logs periodically (keep last 100 per transaction)
                // This is done asynchronously to not block the main operation
                changeLogDao.pruneChangeLogs(keepCount = 100)
            }
        }
    }
    
    /**
     * Log a single field change
     */
    fun logFieldChange(
        transactionId: Long,
        field: String,
        oldValue: String?,
        newValue: String?,
        source: ChangeSource
    ) {
        scope.launch {
            val changeLog = TransactionChangeLogEntity(
                transactionId = transactionId,
                field = field,
                oldValue = oldValue,
                newValue = newValue,
                changedAt = System.currentTimeMillis(),
                source = source.name
            )
            changeLogDao.insertChangeLog(changeLog)
        }
    }
    
    /**
     * Detect all changes between two transaction states
     */
    private fun detectChanges(
        oldTransaction: Transaction?,
        newTransaction: Transaction
    ): List<Triple<String, String?, String?>> {
        if (oldTransaction == null) {
            // New transaction - log all initial values
            return listOf(
                Triple("amount", null, newTransaction.amount.toString()),
                Triple("merchant", null, newTransaction.merchant),
                Triple("category", null, newTransaction.category),
                Triple("transactionType", null, newTransaction.transactionType.name),
                Triple("paymentMode", null, newTransaction.paymentMode.name),
                Triple("status", null, newTransaction.status.name),
                Triple("isApproved", null, newTransaction.isApproved.toString())
            )
        }
        
        val changes = mutableListOf<Triple<String, String?, String?>>()
        
        // Compare each field
        if (oldTransaction.amount != newTransaction.amount) {
            changes.add(Triple("amount", oldTransaction.amount.toString(), newTransaction.amount.toString()))
        }
        
        if (oldTransaction.merchant != newTransaction.merchant) {
            changes.add(Triple("merchant", oldTransaction.merchant, newTransaction.merchant))
        }
        
        if (oldTransaction.category != newTransaction.category) {
            changes.add(Triple("category", oldTransaction.category, newTransaction.category))
        }
        
        if (oldTransaction.transactionType != newTransaction.transactionType) {
            changes.add(Triple("transactionType", oldTransaction.transactionType.name, newTransaction.transactionType.name))
        }
        
        if (oldTransaction.paymentMode != newTransaction.paymentMode) {
            changes.add(Triple("paymentMode", oldTransaction.paymentMode.name, newTransaction.paymentMode.name))
        }
        
        if (oldTransaction.status != newTransaction.status) {
            changes.add(Triple("status", oldTransaction.status.name, newTransaction.status.name))
        }
        
        if (oldTransaction.isApproved != newTransaction.isApproved) {
            changes.add(Triple("isApproved", oldTransaction.isApproved.toString(), newTransaction.isApproved.toString()))
        }
        
        if (oldTransaction.notes != newTransaction.notes) {
            changes.add(Triple("notes", oldTransaction.notes, newTransaction.notes))
        }
        
        if (oldTransaction.confidenceScore != newTransaction.confidenceScore) {
            changes.add(Triple("confidenceScore", oldTransaction.confidenceScore.toString(), newTransaction.confidenceScore.toString()))
        }
        
        return changes
    }
}
