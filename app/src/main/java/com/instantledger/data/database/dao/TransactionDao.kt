package com.instantledger.data.database.dao

import androidx.room.*
import com.instantledger.data.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE is_approved = 1 ORDER BY timestamp DESC")
    fun getApprovedTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND is_approved = 1 ORDER BY timestamp DESC")
    fun getApprovedTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch') ORDER BY timestamp DESC")
    fun getTransactionsByDate(date: Long): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE source_type = :sourceType ORDER BY timestamp DESC")
    fun getTransactionsBySourceType(sourceType: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTransactionsByMerchant(query: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)
    
    @Query("SELECT COUNT(*) FROM transactions WHERE raw_text_hash = :hash")
    suspend fun countByHash(hash: String): Int
    
    @Query("SELECT DISTINCT merchant FROM transactions WHERE merchant IS NOT NULL AND merchant != '' ORDER BY merchant ASC")
    suspend fun getAllUniqueMerchants(): List<String>
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
