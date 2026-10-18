package com.instantledger.data.database.dao

import androidx.room.*
import com.instantledger.data.database.entities.MerchantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {
    
    @Query("SELECT * FROM merchants ORDER BY display_name ASC")
    fun getAllMerchants(): Flow<List<MerchantEntity>>
    
    @Query("SELECT * FROM merchants WHERE original_name = :originalName LIMIT 1")
    suspend fun getMerchantByOriginalName(originalName: String): MerchantEntity?
    
    @Query("SELECT * FROM merchants WHERE display_name = :displayName LIMIT 1")
    suspend fun getMerchantByDisplayName(displayName: String): MerchantEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: MerchantEntity)
    
    @Update
    suspend fun updateMerchant(merchant: MerchantEntity)
    
    @Delete
    suspend fun deleteMerchant(merchant: MerchantEntity)
    
    @Query("DELETE FROM merchants WHERE original_name = :originalName")
    suspend fun deleteMerchantByOriginalName(originalName: String)
    
    @Query("SELECT COUNT(*) FROM transactions WHERE merchant = :originalName")
    suspend fun getUsageCount(originalName: String): Int
}
