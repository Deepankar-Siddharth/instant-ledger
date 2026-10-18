package com.instantledger.data.database.dao

import androidx.room.*
import com.instantledger.data.database.entities.TransactionChangeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionChangeLogDao {
    
    /**
     * Get all change logs for a specific transaction
     * Ordered by most recent first
     */
    @Query("SELECT * FROM transaction_change_logs WHERE transaction_id = :transactionId ORDER BY changed_at DESC")
    fun getChangeLogsForTransaction(transactionId: Long): Flow<List<TransactionChangeLogEntity>>
    
    /**
     * Get the last N change logs for a transaction
     */
    @Query("SELECT * FROM transaction_change_logs WHERE transaction_id = :transactionId ORDER BY changed_at DESC LIMIT :limit")
    suspend fun getRecentChangeLogs(transactionId: Long, limit: Int = 100): List<TransactionChangeLogEntity>
    
    /**
     * Get all change logs (for debugging/admin purposes)
     */
    @Query("SELECT * FROM transaction_change_logs ORDER BY changed_at DESC LIMIT :limit")
    suspend fun getAllChangeLogs(limit: Int = 1000): List<TransactionChangeLogEntity>
    
    /**
     * Insert a change log entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangeLog(changeLog: TransactionChangeLogEntity): Long
    
    /**
     * Insert multiple change log entries
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangeLogs(changeLogs: List<TransactionChangeLogEntity>)
    
    /**
     * Delete change logs older than specified timestamp
     * Used for cleanup - keep only recent N changes per transaction
     */
    @Query("DELETE FROM transaction_change_logs WHERE changed_at < :beforeTimestamp")
    suspend fun deleteOldChangeLogs(beforeTimestamp: Long)
    
    /**
     * Delete all change logs for a specific transaction
     */
    @Query("DELETE FROM transaction_change_logs WHERE transaction_id = :transactionId")
    suspend fun deleteChangeLogsForTransaction(transactionId: Long)
    
    /**
     * Keep only the last N change logs per transaction
     * This is called periodically to prevent unbounded growth
     */
    @Query("""
        DELETE FROM transaction_change_logs
        WHERE id NOT IN (
            SELECT id FROM transaction_change_logs
            WHERE transaction_id = transaction_change_logs.transaction_id
            ORDER BY changed_at DESC
            LIMIT :keepCount
        )
    """)
    suspend fun pruneChangeLogs(keepCount: Int = 100)
}
