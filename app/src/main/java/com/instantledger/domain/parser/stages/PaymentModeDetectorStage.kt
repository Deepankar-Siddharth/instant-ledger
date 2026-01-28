package com.instantledger.domain.parser.stages

import com.instantledger.domain.parser.ParsingContext
import com.instantledger.domain.parser.ParsingStage
import com.instantledger.data.model.PaymentMode

/**
 * Stage 5: Detect payment mode (UPI, Card, Bank, Cash)
 */
class PaymentModeDetectorStage : ParsingStage {
    
    companion object {
        private val UPI_KEYWORDS = listOf(
            "upi", "gpay", "phonepe", "paytm", "bhim", "google pay"
        )
        
        private val CARD_KEYWORDS = listOf(
            "card", "visa", "mastercard", "rupay", "debit card", "credit card"
        )
        
        private val BANK_KEYWORDS = listOf(
            "neft", "imps", "rtgs", "transfer", "bank", "ach"
        )
    }
    
    override fun process(context: ParsingContext): Float {
        val lowerSMS = context.rawSMS.lowercase()
        
        when {
            UPI_KEYWORDS.any { lowerSMS.contains(it) } -> {
                context.paymentMode = PaymentMode.UPI
                context.stageConfidences["paymentMode"] = 0.9f
                return 0.9f
            }
            CARD_KEYWORDS.any { lowerSMS.contains(it) } -> {
                context.paymentMode = PaymentMode.CARD
                context.stageConfidences["paymentMode"] = 0.9f
                return 0.9f
            }
            BANK_KEYWORDS.any { lowerSMS.contains(it) } -> {
                context.paymentMode = PaymentMode.BANK
                context.stageConfidences["paymentMode"] = 0.9f
                return 0.9f
            }
            else -> {
                // Default to UPI if not specified (most common in India)
                context.paymentMode = PaymentMode.UPI
                context.stageConfidences["paymentMode"] = 0.5f
                return 0.5f
            }
        }
    }
}
