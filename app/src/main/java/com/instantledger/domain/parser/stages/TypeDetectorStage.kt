package com.instantledger.domain.parser.stages

import com.instantledger.domain.parser.ParsingContext
import com.instantledger.domain.parser.ParsingStage
import com.instantledger.data.model.TransactionType

/**
 * Stage 3: Detect transaction type (Debit or Credit)
 */
class TypeDetectorStage : ParsingStage {
    
    companion object {
        private val DEBIT_KEYWORDS = listOf(
            "debited", "spent", "paid", "withdrawn", "deducted", "charged", "purchase"
        )
        
        private val CREDIT_KEYWORDS = listOf(
            "credited", "received", "deposited", "refunded", "reversed", "salary"
        )
    }
    
    override fun process(context: ParsingContext): Float {
        val lowerSMS = context.rawSMS.lowercase()
        
        val debitCount = DEBIT_KEYWORDS.count { lowerSMS.contains(it) }
        val creditCount = CREDIT_KEYWORDS.count { lowerSMS.contains(it) }
        
        when {
            debitCount > creditCount -> {
                context.transactionType = TransactionType.DEBIT
                context.stageConfidences["type"] = 0.9f
                return 0.9f
            }
            creditCount > debitCount -> {
                context.transactionType = TransactionType.CREDIT
                context.stageConfidences["type"] = 0.9f
                return 0.9f
            }
            debitCount > 0 || creditCount > 0 -> {
                // Ambiguous - default to debit
                context.transactionType = TransactionType.DEBIT
                context.stageConfidences["type"] = 0.6f
                return 0.6f
            }
            else -> {
                // No keywords found - cannot determine
                context.stageConfidences["type"] = 0.0f
                return 0.0f
            }
        }
    }
}
