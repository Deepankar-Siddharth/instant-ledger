package com.instantledger.data.repository

import com.instantledger.data.database.dao.UnverifiedTransactionDao
import com.instantledger.data.database.entities.UnverifiedTransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnverifiedTransactionRepository @Inject constructor(
    private val unverifiedTransactionDao: UnverifiedTransactionDao
) {
    fun getAllUnverifiedTransactions(): Flow<List<UnverifiedTransactionEntity>> {
        return unverifiedTransactionDao.getAllUnverifiedTransactions()
    }
    
    suspend fun getUnverifiedTransactionById(id: Long): UnverifiedTransactionEntity? {
        return unverifiedTransactionDao.getUnverifiedTransactionById(id)
    }
    
    suspend fun insertUnverifiedTransaction(transaction: UnverifiedTransactionEntity): Long {
        return unverifiedTransactionDao.insertUnverifiedTransaction(transaction)
    }
    
    suspend fun deleteUnverifiedTransaction(transaction: UnverifiedTransactionEntity) {
        unverifiedTransactionDao.deleteUnverifiedTransaction(transaction)
    }
    
    suspend fun deleteUnverifiedTransactionById(id: Long) {
        unverifiedTransactionDao.deleteUnverifiedTransactionById(id)
    }
    
    suspend fun getUnverifiedCount(): Int {
        return unverifiedTransactionDao.getUnverifiedCount()
    }
    
    suspend fun deleteAllUnverifiedTransactions() {
        unverifiedTransactionDao.deleteAllUnverifiedTransactions()
    }
}
