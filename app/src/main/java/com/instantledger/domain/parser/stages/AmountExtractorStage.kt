package com.instantledger.domain.parser.stages

import com.instantledger.domain.parser.ParsingContext
import com.instantledger.domain.parser.ParsingStage
import java.util.regex.Pattern

/**
 * Stage 2: Extract transaction amount from SMS
 * This is the most critical stage - amount must be found
 */
class AmountExtractorStage : ParsingStage {
    
    companion object {
        private val AMOUNT_PATTERNS = listOf(
            // Rs. 500, Rs 500, Rs.500
            Pattern.compile("Rs\\.?\\s?([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            // INR 500, INR500
            Pattern.compile("INR\\s?([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            // ₹ 500, ₹500
            Pattern.compile("₹\\s?([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            // Amount before currency: 500 Rs, 500 INR
            Pattern.compile("([0-9,.]+)\\s*(?:Rs\\.?|INR|₹)", Pattern.CASE_INSENSITIVE),
            // debited/credited with amount
            Pattern.compile("(?:debited|credited|paid|spent)\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            // Amount before debited/credited
            Pattern.compile("([0-9,.]+)\\s*(?:debited|credited|paid|spent)", Pattern.CASE_INSENSITIVE),
            // Generic number patterns (fallback)
            Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+(?:\\.\\d{2})?)\\s*(?:Rs\\.?|INR|₹)", Pattern.CASE_INSENSITIVE)
        )
    }
    
    override fun process(context: ParsingContext): Float {
        // Try each pattern in order
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(context.rawSMS)
            if (matcher.find()) {
                val amountStr = matcher.group(1)
                val amount = parseAmount(amountStr)
                
                if (amount != null && amount > 0 && amount.isFinite()) {
                    context.amount = amount
                    context.stageConfidences["amount"] = 1.0f
                    return 1.0f
                }
            }
        }
        
        // Amount not found
        context.stageConfidences["amount"] = 0.0f
        return 0.0f
    }
    
    private fun parseAmount(amountStr: String?): Double? {
        if (amountStr == null) return null
        return try {
            amountStr.replace(",", "").toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }
}
