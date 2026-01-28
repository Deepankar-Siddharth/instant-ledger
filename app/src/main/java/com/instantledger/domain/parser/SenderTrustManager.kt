package com.instantledger.domain.parser

/**
 * Manages sender ID trust scores for SMS transactions
 * High trust = known bank sender IDs
 * Low trust = unknown or suspicious sender IDs
 */
object SenderTrustManager {
    
    // Known bank sender IDs with high trust scores
    private val trustedSenders = mapOf(
        // HDFC Bank
        "HDFCBK" to 0.95f,
        "HDFCB" to 0.95f,
        "HDFC" to 0.95f,
        
        // ICICI Bank
        "ICICIB" to 0.95f,
        "ICICIBK" to 0.95f,
        "ICICI" to 0.95f,
        
        // SBI
        "SBIIN" to 0.95f,
        "SBINB" to 0.95f,
        "SBI" to 0.95f,
        
        // Axis Bank
        "AXISBK" to 0.95f,
        "AXIS" to 0.95f,
        
        // Kotak Bank
        "KOTAKB" to 0.95f,
        "KOTAK" to 0.95f,
        
        // PNB
        "PNB" to 0.95f,
        "PNBIN" to 0.95f,
        
        // BOI
        "BOI" to 0.95f,
        "BOIIN" to 0.95f,
        
        // UPI Providers
        "UPI" to 0.90f,
        "PAYTM" to 0.90f,
        "GPay" to 0.90f,
        "PHONEPE" to 0.90f,
        
        // E-commerce (medium trust)
        "AMZNIN" to 0.80f,
        "FLIPKART" to 0.80f,
        "ZOMATO" to 0.80f,
        "SWIGGY" to 0.80f,
        
        // Suspicious/Test (low trust)
        "VK-TEST" to 0.10f,
        "TEST" to 0.10f,
        "DEMO" to 0.10f
    )
    
    /**
     * Get trust score for a sender ID
     * @param senderId The sender ID from SMS
     * @return Trust score between 0.0 and 1.0
     */
    fun getTrustScore(senderId: String?): Float {
        if (senderId == null) return 0.5f
        
        val normalizedId = senderId.uppercase().trim()
        
        // Exact match
        trustedSenders[normalizedId]?.let { return it }
        
        // Partial match (contains known bank code)
        trustedSenders.keys.forEach { key ->
            if (normalizedId.contains(key)) {
                return trustedSenders[key] ?: 0.5f
            }
        }
        
        // Unknown sender - default to medium-low trust
        return 0.5f
    }
    
    /**
     * Calculate final confidence score combining content and sender trust
     * @param contentScore Confidence from SMS parsing (0.0-1.0)
     * @param senderId Sender ID from SMS
     * @return Final confidence score (0.0-1.0)
     */
    fun calculateFinalConfidence(contentScore: Float, senderId: String?): Float {
        val senderTrust = getTrustScore(senderId)
        // Weighted: 70% content, 30% sender trust
        return (contentScore * 0.7f) + (senderTrust * 0.3f)
    }
    
    /**
     * Check if sender is highly trusted
     */
    fun isHighlyTrusted(senderId: String?): Boolean {
        return getTrustScore(senderId) >= 0.9f
    }
}
