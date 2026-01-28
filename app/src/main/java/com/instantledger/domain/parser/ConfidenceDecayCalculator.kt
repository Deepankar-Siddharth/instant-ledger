package com.instantledger.domain.parser

import kotlin.math.exp
import kotlin.math.ln

/**
 * Calculates confidence decay over time.
 * Older transactions have lower effective confidence scores.
 */
object ConfidenceDecayCalculator {
    
    // Decay parameters
    private const val HALF_LIFE_DAYS = 365.0 // Confidence halves after 1 year
    // Exponential decay rate (cannot be const because it uses a function call)
    private val DECAY_RATE = ln(2.0) / HALF_LIFE_DAYS
    
    /**
     * Calculate effective confidence with time-based decay
     * @param baseConfidence Original confidence score (0.0 to 1.0)
     * @param transactionTimestamp Transaction timestamp in milliseconds
     * @return Effective confidence after decay (0.0 to 1.0)
     */
    fun calculateEffectiveConfidence(
        baseConfidence: Float,
        transactionTimestamp: Long
    ): Float {
        val now = System.currentTimeMillis()
        val daysSinceTransaction = (now - transactionTimestamp) / (1000.0 * 60 * 60 * 24)
        
        // Exponential decay: confidence decreases over time
        // Formula: effective = base * e^(-decayRate * days)
        val decayFactor = exp(-DECAY_RATE * daysSinceTransaction)
        
        // Ensure decay factor is between 0 and 1
        val clampedDecayFactor = decayFactor.coerceIn(0.0, 1.0)
        
        val effectiveConfidence = baseConfidence * clampedDecayFactor.toFloat()
        
        return effectiveConfidence.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Calculate decay factor for a given number of days
     * @param days Number of days since transaction
     * @return Decay factor (0.0 to 1.0)
     */
    fun getDecayFactor(days: Double): Float {
        val decayFactor = exp(-DECAY_RATE * days)
        return decayFactor.coerceIn(0.0, 1.0).toFloat()
    }
    
    /**
     * Get number of days since transaction
     */
    fun getDaysSince(transactionTimestamp: Long): Double {
        val now = System.currentTimeMillis()
        return (now - transactionTimestamp) / (1000.0 * 60 * 60 * 24)
    }
    
    /**
     * Check if transaction confidence has decayed significantly
     * @param threshold Minimum effective confidence to consider valid
     * @return true if effective confidence is above threshold
     */
    fun isConfidenceValid(
        baseConfidence: Float,
        transactionTimestamp: Long,
        threshold: Float = 0.5f
    ): Boolean {
        val effectiveConfidence = calculateEffectiveConfidence(baseConfidence, transactionTimestamp)
        return effectiveConfidence >= threshold
    }
}
