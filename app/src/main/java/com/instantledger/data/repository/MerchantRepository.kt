package com.instantledger.data.repository

import com.instantledger.data.database.dao.MerchantDao
import com.instantledger.data.database.entities.MerchantEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantRepository @Inject constructor(
    private val merchantDao: MerchantDao
) {
    fun getAllMerchants(): Flow<List<MerchantEntity>> {
        return merchantDao.getAllMerchants()
    }
    
    suspend fun getMerchantByOriginalName(originalName: String): MerchantEntity? {
        return merchantDao.getMerchantByOriginalName(originalName)
    }
    
    suspend fun getMerchantByDisplayName(displayName: String): MerchantEntity? {
        return merchantDao.getMerchantByDisplayName(displayName)
    }
    
    suspend fun insertMerchant(merchant: MerchantEntity) {
        merchantDao.insertMerchant(merchant)
    }
    
    suspend fun updateMerchant(merchant: MerchantEntity) {
        // Use insert with REPLACE strategy to ensure update works correctly
        merchantDao.insertMerchant(merchant.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteMerchant(merchant: MerchantEntity) {
        merchantDao.deleteMerchant(merchant)
    }
    
    suspend fun deleteMerchantByOriginalName(originalName: String) {
        merchantDao.deleteMerchantByOriginalName(originalName)
    }
    
    suspend fun getUsageCount(originalName: String): Int {
        return merchantDao.getUsageCount(originalName)
    }
}
