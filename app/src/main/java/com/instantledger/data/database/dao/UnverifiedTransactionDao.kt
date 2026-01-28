package com.instantledger.data.database.dao

import androidx.room.*
import com.instantledger.data.database.entities.UnverifiedTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnverifiedTransactionDao {
    
    @Query("SELECT * FROM unverified_transactions ORDER BY created_at DESC")
    fun getAllUnverifiedTransactions(): Flow<List<UnverifiedTransactionEntity>>
    
    @Query("SELECT * FROM unverified_transactions WHERE id = :id")
    suspend fun getUnverifiedTransactionById(id: Long): UnverifiedTransactionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnverifiedTransaction(transaction: UnverifiedTransactionEntity): Long
    
    @Delete
    suspend fun deleteUnverifiedTransaction(transaction: UnverifiedTransactionEntity)
    
    @Query("DELETE FROM unverified_transactions WHERE id = :id")
    suspend fun deleteUnverifiedTransactionById(id: Long)
    
    @Query("SELECT COUNT(*) FROM unverified_transactions")
    suspend fun getUnverifiedCount(): Int
    
    @Query("DELETE FROM unverified_transactions")
    suspend fun deleteAllUnverifiedTransactions()
}
