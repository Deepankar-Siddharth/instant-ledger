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
        // Amount patterns: currency (₹, Rs, INR), amounts, and transaction keywords (credited, debited, received, paid, refund, transfer, txn)
        private val AMOUNT_PATTERNS = listOf(
            Pattern.compile("Rs\\.?\\s?([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("INR\\s?([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("₹\\s?([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,.]+)\\s*(?:Rs\\.?|INR|₹)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:debited|credited|paid|spent|received|refund|transfer)\\s*(?:of|for)?\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,.]+)\\s*(?:debited|credited|paid|spent|received|refund)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:transfer|txn|txns?)\\s*(?:of|for)?\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([0-9,.]+)\\s*(?:transfer|txn)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([\\d,]+(?:\\.\\d{2})?)\\s*(?:Rs\\.?|INR|₹)", Pattern.CASE_INSENSITIVE)
        )
        
        private val MERCHANT_PATTERNS = listOf(
            Pattern.compile("(?:at|from|to|via)\\s+([A-Za-z0-9][A-Za-z0-9\\s&.-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:merchant|vendor|payee):\\s*([A-Za-z0-9][A-Za-z0-9\\s&.-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("UPI\\s+([A-Za-z0-9][A-Za-z0-9\\s&.-]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:to|from)\\s+([A-Za-z0-9][A-Za-z0-9\\s&.-]+)\\s+(?:for|of)", Pattern.CASE_INSENSITIVE)
        )
        
        private val DEBIT_KEYWORDS = listOf(
            "debited", "spent", "paid", "withdrawn", "deducted", "charged", "purchase"
        )
        
        private val CREDIT_KEYWORDS = listOf(
            "credited", "received", "deposited", "refunded", "reversed", "refund"
        )
        
        /** Any message containing amount + one of these is treated as money-related for capture */
        private val MONEY_KEYWORDS = listOf(
            "credited", "debited", "received", "paid", "refund", "transfer", "txn", "transaction",
            "neft", "imps", "rtgs", "upi", "balance", "account"
        )
        
        private val UPI_KEYWORDS = listOf(
            "upi", "gpay", "phonepe", "paytm", "bhim", "wallet"
        )
        
        private val CARD_KEYWORDS = listOf(
            "card", "visa", "mastercard", "rupay", "debit card", "credit card"
        )
        
        private val BANK_KEYWORDS = listOf(
            "neft", "imps", "rtgs", "transfer", "bank", "a/c", "account"
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
        
        val confidenceScore = calculateConfidenceScore(
            hasAmount = amount != null,
            hasMerchant = merchant != null,
            hasTransactionType = transactionType != null,
            hasPaymentMode = paymentMode != null,
            hasMoneyKeyword = hasMoneyKeyword(messageBody)
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
    
    /** True if message contains any generic money-movement keyword (for broader capture). */
    private fun hasMoneyKeyword(message: String): Boolean {
        val lower = message.lowercase()
        return MONEY_KEYWORDS.any { lower.contains(it) }
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
        hasPaymentMode: Boolean,
        hasMoneyKeyword: Boolean = false
    ): Float {
        var score = 0.0f
        if (hasAmount) score += 0.4f
        if (hasMerchant) score += 0.3f
        if (hasTransactionType) score += 0.2f
        if (hasPaymentMode) score += 0.1f
        // Broader capture: amount + any money keyword still counts as likely transaction
        if (hasAmount && hasMoneyKeyword && score < 0.5f) score = 0.5f
        return score.coerceIn(0f, 1f)
    }
}
