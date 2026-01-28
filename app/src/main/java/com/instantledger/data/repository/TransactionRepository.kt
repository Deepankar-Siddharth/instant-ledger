package com.instantledger.data.repository

import com.instantledger.data.database.dao.TransactionDao
import com.instantledger.data.database.entities.TransactionEntity
import com.instantledger.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getApprovedTransactions(): Flow<List<Transaction>> {
        return transactionDao.getApprovedTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getApprovedTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getApprovedTransactionsByDateRange(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getTransactionsByDate(date: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(category).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun getTransactionsBySourceType(sourceType: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsBySourceType(sourceType).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    fun searchTransactionsByMerchant(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactionsByMerchant(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }
    
    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }
    
    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions.map { it.toEntity() })
    }
    
    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }
    
    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }
    
    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }
    
    suspend fun isDuplicate(hash: String): Boolean {
        return transactionDao.countByHash(hash) > 0
    }
    
    suspend fun getAllUniqueMerchants(): List<String> {
        return transactionDao.getAllUniqueMerchants()
    }
}
