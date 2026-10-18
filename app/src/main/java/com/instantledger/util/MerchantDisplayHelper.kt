package com.instantledger.util

import com.instantledger.data.model.Transaction
import com.instantledger.data.repository.MerchantRepository
import kotlinx.coroutines.flow.first

/**
 * Helper utility for resolving merchant display names with priority:
 * 1. Transaction-level override (merchantOverride)
 * 2. Managed merchant name (from merchants table)
 * 3. Parsed SMS merchant (merchant field)
 * 4. "Unknown"
 */
object MerchantDisplayHelper {
    
    /**
     * Get the display name for a transaction based on priority rules
     * This is a suspend function because it may need to query the merchant repository
     */
    suspend fun getDisplayName(
        transaction: Transaction,
        merchantRepository: MerchantRepository
    ): String {
        // Priority 1: Transaction-level override
        if (!transaction.merchantOverride.isNullOrBlank()) {
            return transaction.merchantOverride
        }
        
        // Priority 2: Managed merchant name
        val managedMerchant = merchantRepository.getMerchantByOriginalName(transaction.merchant)
        if (managedMerchant != null) {
            return managedMerchant.displayName
        }
        
        // Priority 3: Parsed SMS merchant
        if (transaction.merchant.isNotBlank() && transaction.merchant != "Unknown") {
            return transaction.merchant
        }
        
        // Priority 4: "Unknown"
        return "Unknown"
    }
    
    /**
     * Get display names for a list of transactions
     * More efficient as it batches merchant lookups
     */
    suspend fun getDisplayNames(
        transactions: List<Transaction>,
        merchantRepository: MerchantRepository
    ): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        val allMerchants = merchantRepository.getAllMerchants().first()
        val merchantMap = allMerchants.associateBy { it.originalName }
        
        transactions.forEach { transaction ->
            result[transaction.id] = when {
                // Priority 1: Transaction-level override
                !transaction.merchantOverride.isNullOrBlank() -> transaction.merchantOverride
                
                // Priority 2: Managed merchant name
                merchantMap.containsKey(transaction.merchant) -> merchantMap[transaction.merchant]!!.displayName
                
                // Priority 3: Parsed SMS merchant
                transaction.merchant.isNotBlank() && transaction.merchant != "Unknown" -> transaction.merchant
                
                // Priority 4: "Unknown"
                else -> "Unknown"
            }
        }
        
        return result
    }
}
