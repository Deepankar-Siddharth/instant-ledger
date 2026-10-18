package com.instantledger.domain.parser.stages

import com.instantledger.domain.parser.ParsingContext
import com.instantledger.domain.parser.ParsingStage
import java.util.regex.Pattern

/**
 * Stage 4: Resolve merchant name from SMS
 * This stage will be enhanced with merchant learning engine
 */
class MerchantResolverStage : ParsingStage {
    
    companion object {
        private val MERCHANT_PATTERNS = listOf(
            Pattern.compile("(?:at|from|to|via)\\s+([A-Z][A-Z\\s&]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:merchant|vendor|payee):\\s*([A-Z][A-Z\\s&]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("UPI\\s+([A-Z][A-Z\\s&]+)", Pattern.CASE_INSENSITIVE)
        )
    }
    
    override fun process(context: ParsingContext): Float {
        // Try patterns first
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(context.rawSMS)
            if (matcher.find()) {
                val merchant = matcher.group(1)?.trim()
                if (merchant != null && merchant.isNotBlank()) {
                    context.merchant = merchant
                    context.stageConfidences["merchant"] = 0.8f
                    return 0.8f
                }
            }
        }
        
        // Try UPI-specific pattern
        val upiPattern = Pattern.compile("UPI\\s+([A-Z][A-Z\\s&]+)", Pattern.CASE_INSENSITIVE)
        val upiMatcher = upiPattern.matcher(context.rawSMS)
        if (upiMatcher.find()) {
            val merchant = upiMatcher.group(1)?.trim()
            if (merchant != null && merchant.isNotBlank()) {
                context.merchant = merchant
                context.stageConfidences["merchant"] = 0.7f
                return 0.7f
            }
        }
        
        // Merchant not found - will be set to "Unknown" later
        context.stageConfidences["merchant"] = 0.0f
        return 0.0f
    }
}
