package com.instantledger.domain.parser

import com.instantledger.data.model.PaymentMode
import com.instantledger.data.model.TransactionType
import java.util.regex.Pattern

data class ParsedTransaction(
    val amount: Double?,
    val merchant: String?,
    val accountType: String?,
    val transactionType: TransactionType?,
    val paymentMode: PaymentMode?,
    val confidenceScore: Float
)

class SMSParser {
    
    companion object {
        // Common bank SMS patterns - Broader and more flexible
        private val AMOUNT_PATTERNS = listOf(
            // Rs. 500, Rs 500, Rs.500 (with optional space and comma)
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
        
        private val MERCHANT_PATTERNS = listOf(
            Pattern.compile("(?:at|from|to|via)\\s+([A-Z][A-Z\\s&]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:merchant|vendor|payee):\\s*([A-Z][A-Z\\s&]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("UPI\\s+([A-Z][A-Z\\s&]+)", Pattern.CASE_INSENSITIVE)
        )
        
        private val DEBIT_KEYWORDS = listOf(
            "debited", "spent", "paid", "withdrawn", "deducted", "charged"
        )
        
        private val CREDIT_KEYWORDS = listOf(
            "credited", "received", "deposited", "refunded", "reversed"
        )
        
        private val UPI_KEYWORDS = listOf(
            "upi", "gpay", "phonepe", "paytm", "bhim"
        )
        
        private val CARD_KEYWORDS = listOf(
            "card", "visa", "mastercard", "rupay", "debit card", "credit card"
        )
        
        private val BANK_KEYWORDS = listOf(
            "neft", "imps", "rtgs", "transfer", "bank"
        )
        
        // Bank-specific patterns
        private val HDFC_PATTERNS = listOf(
            Pattern.compile("HDFC.*?([\\d,]+(?:\\.\\d{2})?)\\s*(?:debited|credited)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A/c\\s*\\*?([\\d]+).*?([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)
        )
        
        private val ICICI_PATTERNS = listOf(
            Pattern.compile("ICICI.*?([\\d,]+(?:\\.\\d{2})?)\\s*(?:debited|credited)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A/c\\s*([\\d]+).*?([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)
        )
        
        private val SBI_PATTERNS = listOf(
            Pattern.compile("SBI.*?([\\d,]+(?:\\.\\d{2})?)\\s*(?:debited|credited)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A/c\\s*([\\d]+).*?([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)
        )
        
        private val AXIS_PATTERNS = listOf(
            Pattern.compile("AXIS.*?([\\d,]+(?:\\.\\d{2})?)\\s*(?:debited|credited)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("A/c\\s*([\\d]+).*?([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)
        )
    }
    
    fun parseSMS(messageBody: String): ParsedTransaction {
        val lowerMessage = messageBody.lowercase()
        
        // Extract amount
        val amount = extractAmount(messageBody)
        
        // Extract merchant
        val merchant = extractMerchant(messageBody)
        
        // Determine transaction type
        val transactionType = determineTransactionType(lowerMessage)
        
        // Determine payment mode
        val paymentMode = determinePaymentMode(lowerMessage)
        
        // Extract account type
        val accountType = extractAccountType(messageBody)
        
        // Calculate confidence score
        val confidenceScore = calculateConfidenceScore(
            amount != null,
            merchant != null,
            transactionType != null,
            paymentMode != null
        )
        
        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            accountType = accountType,
            transactionType = transactionType,
            paymentMode = paymentMode,
            confidenceScore = confidenceScore
        )
    }
    
    private fun extractAmount(message: String): Double? {
        // Try bank-specific patterns first
        val bankPatterns = HDFC_PATTERNS + ICICI_PATTERNS + SBI_PATTERNS + AXIS_PATTERNS
        for (pattern in bankPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(2) ?: matcher.group(1)
                return parseAmount(amountStr)
            }
        }
        
        // Try generic patterns
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return parseAmount(matcher.group(1))
            }
        }
        
        return null
    }
    
    private fun parseAmount(amountStr: String?): Double? {
        if (amountStr == null) return null
        return try {
            amountStr.replace(",", "").toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    private fun extractMerchant(message: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        
        // Try to extract from UPI transactions
        val upiPattern = Pattern.compile("UPI\\s+([A-Z][A-Z\\s&]+)", Pattern.CASE_INSENSITIVE)
        val upiMatcher = upiPattern.matcher(message)
        if (upiMatcher.find()) {
            return upiMatcher.group(1)?.trim()
        }
        
        return null
    }
    
    private fun determineTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        val debitCount = DEBIT_KEYWORDS.count { lowerMessage.contains(it) }
        val creditCount = CREDIT_KEYWORDS.count { lowerMessage.contains(it) }
        
        return when {
            debitCount > creditCount -> TransactionType.DEBIT
            creditCount > debitCount -> TransactionType.CREDIT
            else -> null
        }
    }
    
    private fun determinePaymentMode(message: String): PaymentMode? {
        val lowerMessage = message.lowercase()
        
        return when {
            UPI_KEYWORDS.any { lowerMessage.contains(it) } -> PaymentMode.UPI
            CARD_KEYWORDS.any { lowerMessage.contains(it) } -> PaymentMode.CARD
            BANK_KEYWORDS.any { lowerMessage.contains(it) } -> PaymentMode.BANK
            else -> null
        }
    }
    
    private fun extractAccountType(message: String): String? {
        val patterns = listOf(
            Pattern.compile("(?:A/c|Account)\\s*\\*?([\\d]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(Savings|Current|Credit)\\s+(?:A/c|Account)", Pattern.CASE_INSENSITIVE)
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        
        // Try to extract bank name
        val bankPattern = Pattern.compile("(HDFC|ICICI|SBI|AXIS|KOTAK|PNB|BOI)", Pattern.CASE_INSENSITIVE)
        val bankMatcher = bankPattern.matcher(message)
        if (bankMatcher.find()) {
            return bankMatcher.group(1)
        }
        
        return null
    }
    
    private fun calculateConfidenceScore(
        hasAmount: Boolean,
        hasMerchant: Boolean,
        hasTransactionType: Boolean,
        hasPaymentMode: Boolean
    ): Float {
        var score = 0.0f
        
        if (hasAmount) score += 0.4f
        if (hasMerchant) score += 0.3f
        if (hasTransactionType) score += 0.2f
        if (hasPaymentMode) score += 0.1f
        
        return score.coerceIn(0f, 1f)
    }
}
