package com.instantledger.domain.parser

/**
 * Base interface for parsing stages in the SMS parsing pipeline.
 * Each stage processes the parsing context and reports its confidence.
 */
interface ParsingStage {
    /**
     * Process the parsing context and update it with extracted information
     * @return confidence score for this stage (0.0 to 1.0)
     */
    fun process(context: ParsingContext): Float
}

/**
 * Context object passed through the parsing pipeline
 */
data class ParsingContext(
    val rawSMS: String,
    val senderId: String? = null,
    var amount: Double? = null,
    var merchant: String? = null,
    var accountType: String? = null,
    var transactionType: com.instantledger.data.model.TransactionType? = null,
    var paymentMode: com.instantledger.data.model.PaymentMode? = null,
    val stageConfidences: MutableMap<String, Float> = mutableMapOf()
) {
    /**
     * Calculate overall confidence from all stage confidences
     */
    fun calculateOverallConfidence(): Float {
        if (stageConfidences.isEmpty()) return 0.0f
        
        // Weighted average: amount is most important
        val amountConfidence = stageConfidences["amount"] ?: 0.0f
        val merchantConfidence = stageConfidences["merchant"] ?: 0.0f
        val typeConfidence = stageConfidences["type"] ?: 0.0f
        val paymentModeConfidence = stageConfidences["paymentMode"] ?: 0.0f
        
        return (amountConfidence * 0.4f +
                merchantConfidence * 0.3f +
                typeConfidence * 0.2f +
                paymentModeConfidence * 0.1f).coerceIn(0f, 1f)
    }
}
