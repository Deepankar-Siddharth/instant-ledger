package com.instantledger.domain.parser

import android.content.Context
import android.util.Log
import com.instantledger.data.database.dao.TransactionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Merchant Resolution Engine that learns from user corrections
 * and applies intelligent merchant name resolution.
 */
@Singleton
class MerchantResolutionEngine @Inject constructor(
    private val transactionDao: TransactionDao
) {
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Resolve merchant name using multiple strategies:
     * 1. Exact match in alias table
     * 2. Known alias lookup
     * 3. Keyword similarity
     * 4. Last-seen merchant heuristic
     * 5. Fallback to "Unknown"
     */
    suspend fun resolveMerchant(rawMerchant: String?): String {
        if (rawMerchant.isNullOrBlank()) {
            return "Unknown"
        }
        
        val normalized = normalizeMerchant(rawMerchant)
        
        // Strategy 1: Exact match
        val exactMatch = findExactMatch(normalized)
        if (exactMatch != null) {
            return exactMatch
        }
        
        // Strategy 2: Known alias lookup
        val aliasMatch = findAliasMatch(normalized)
        if (aliasMatch != null) {
            return aliasMatch
        }
        
        // Strategy 3: Keyword similarity
        val similarMatch = findSimilarMerchant(normalized)
        if (similarMatch != null) {
            return similarMatch
        }
        
        // Strategy 4: Last-seen merchant heuristic
        val lastSeen = findLastSeenMerchant(normalized)
        if (lastSeen != null) {
            return lastSeen
        }
        
        // Strategy 5: Fallback
        return normalized
    }
    
    /**
     * Learn from user correction - store alias mapping
     */
    fun learnAlias(rawMerchant: String, correctedMerchant: String) {
        scope.launch {
            try {
                // Store in a simple in-memory cache for now
                // In production, you'd store this in a MerchantAlias table
                Log.d("MerchantResolutionEngine", "Learned alias: '$rawMerchant' -> '$correctedMerchant'")
                
                // TODO: Store in MerchantAliasEntity table when implemented
            } catch (e: Exception) {
                Log.e("MerchantResolutionEngine", "Failed to learn alias", e)
            }
        }
    }
    
    /**
     * Normalize merchant name (remove special chars, uppercase, etc.)
     */
    private fun normalizeMerchant(merchant: String): String {
        return merchant
            .uppercase()
            .replace(Regex("[*#@]"), "")  // Remove common UPI codes
            .replace(Regex("\\s+"), " ")  // Normalize whitespace
            .trim()
    }
    
    /**
     * Strategy 1: Exact match in database
     */
    private suspend fun findExactMatch(normalized: String): String? {
        return try {
            val merchants = transactionDao.getAllUniqueMerchants()
            merchants.firstOrNull { normalizeMerchant(it) == normalized }
        } catch (e: Exception) {
            Log.w("MerchantResolutionEngine", "Failed to find exact match", e)
            null
        }
    }
    
    /**
     * Strategy 2: Known alias lookup
     * Common patterns: "ZMT*ORDER" -> "Zomato", "AMZNIN" -> "Amazon"
     */
    private fun findAliasMatch(normalized: String): String? {
        val knownAliases = mapOf(
            "ZMT" to "Zomato",
            "ZMT*ORDER" to "Zomato",
            "AMZNIN" to "Amazon",
            "AMAZON PAY" to "Amazon",
            "SWIGGY" to "Swiggy",
            "UBER" to "Uber",
            "OYO" to "Oyo"
        )
        
        for ((alias, merchant) in knownAliases) {
            if (normalized.contains(alias, ignoreCase = true)) {
                return merchant
            }
        }
        
        return null
    }
    
    /**
     * Strategy 3: Keyword similarity
     * Find merchants with similar keywords
     */
    private suspend fun findSimilarMerchant(normalized: String): String? {
        return try {
            val merchants = transactionDao.getAllUniqueMerchants()
            val keywords = extractKeywords(normalized)
            
            merchants.firstOrNull { merchant ->
                val merchantKeywords = extractKeywords(merchant)
                val commonKeywords = keywords.intersect(merchantKeywords)
                commonKeywords.size >= 2  // At least 2 common keywords
            }
        } catch (e: Exception) {
            Log.w("MerchantResolutionEngine", "Failed to find similar merchant", e)
            null
        }
    }
    
    /**
     * Strategy 4: Last-seen merchant heuristic
     * If we've seen a similar merchant recently, use it
     */
    private suspend fun findLastSeenMerchant(normalized: String): String? {
        // This would query recent transactions and find similar merchant names
        // For now, return null - can be enhanced later
        return null
    }
    
    /**
     * Extract keywords from merchant name
     */
    private fun extractKeywords(merchant: String): Set<String> {
        return merchant
            .uppercase()
            .split(Regex("[\\s*#@&]+"))
            .filter { it.length >= 3 }  // Only meaningful keywords
            .toSet()
    }
}
