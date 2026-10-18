package com.instantledger.domain.parser

import com.instantledger.data.model.PaymentMode
import com.instantledger.data.model.TransactionType
import com.instantledger.domain.parser.stages.*

/**
 * Pipeline-based SMS parser that processes SMS through multiple stages.
 * Each stage can fail independently and reports its own confidence.
 */
class PipelineSMSParser {
    
    private val stages = listOf(
        SenderClassifierStage(),
        AmountExtractorStage(),
        TypeDetectorStage(),
        MerchantResolverStage(),
        PaymentModeDetectorStage()
    )
    
    /**
     * Parse SMS through the pipeline
     */
    fun parseSMS(messageBody: String, senderId: String? = null): ParsedTransaction {
        val context = ParsingContext(
            rawSMS = messageBody,
            senderId = senderId
        )
        
        // Process through each stage
        stages.forEach { stage ->
            try {
                stage.process(context)
            } catch (e: Exception) {
                // Stage failed - log but continue
                android.util.Log.w("PipelineSMSParser", "Stage ${stage::class.simpleName} failed", e)
            }
        }
        
        // Set defaults if not found
        if (context.merchant == null) {
            context.merchant = "Unknown"
        }
        
        if (context.transactionType == null) {
            context.transactionType = TransactionType.DEBIT
        }
        
        if (context.paymentMode == null) {
            context.paymentMode = PaymentMode.UPI
        }
        
        // Calculate overall confidence
        val overallConfidence = context.calculateOverallConfidence()
        
        return ParsedTransaction(
            amount = context.amount,
            merchant = context.merchant,
            accountType = context.accountType,
            transactionType = context.transactionType,
            paymentMode = context.paymentMode,
            confidenceScore = overallConfidence
        )
    }
}
