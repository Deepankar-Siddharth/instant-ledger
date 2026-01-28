package com.instantledger.domain.parser.stages

import com.instantledger.domain.parser.ParsingContext
import com.instantledger.domain.parser.ParsingStage

/**
 * Stage 1: Classify SMS sender and extract sender ID
 * This helps determine trust score and bank-specific parsing rules
 */
class SenderClassifierStage : ParsingStage {
    
    companion object {
        // Known bank sender IDs (partial matches)
        private val BANK_SENDER_PATTERNS = mapOf(
            "HDFCBK" to 0.95f,
            "ICICIB" to 0.95f,
            "SBIBMS" to 0.95f,
            "AXISBK" to 0.95f,
            "KOTAKB" to 0.95f,
            "PNB" to 0.90f,
            "BOI" to 0.90f,
            "AMZNIN" to 0.80f,  // Amazon Pay
            "PAYTM" to 0.85f,
            "PHONEPE" to 0.85f,
            "GPay" to 0.85f
        )
    }
    
    override fun process(context: ParsingContext): Float {
        // If sender ID is already set, use it
        if (context.senderId != null) {
            val trustScore = BANK_SENDER_PATTERNS.entries
                .firstOrNull { context.senderId!!.contains(it.key, ignoreCase = true) }
                ?.value ?: 0.5f
            
            context.stageConfidences["sender"] = trustScore
            return trustScore
        }
        
        // Try to extract sender ID from SMS content
        // This is a simplified version - in production, you'd get this from SMS metadata
        val upperSMS = context.rawSMS.uppercase()
        
        for ((pattern, trustScore) in BANK_SENDER_PATTERNS) {
            if (upperSMS.contains(pattern)) {
                context.stageConfidences["sender"] = trustScore
                return trustScore
            }
        }
        
        // Default: unknown sender
        context.stageConfidences["sender"] = 0.3f
        return 0.3f
    }
}
