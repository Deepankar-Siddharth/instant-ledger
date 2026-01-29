package com.instantledger.domain.parser

/**
 * Validation gate before saving an SMS as a transaction.
 * A message is accepted ONLY if it has amount + transaction verb and is not OTP/balance-only/failed/promo.
 */
object SMSValidationGate {

    /** Transaction verbs that imply money movement (required for acceptance). */
    private val TRANSACTION_VERBS = listOf(
        "credited", "debited", "paid", "received", "spent", "refunded", "transferred",
        "transfer", "withdrawn", "deducted", "deposited", "reversed"
    )

    /** OTP / verification patterns — exclude. */
    private val OTP_PATTERNS = listOf(
        Regex("\\b(?:otp|verification code|one time password)\\s*[.:]?\\s*\\d{4,8}\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\d{4,8}\\s*(?:is|as)\\s*(?:your|the)\\s*(?:otp|code)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:use|enter)\\s*(?:otp|code)\\s*\\d{4,8}\\b", RegexOption.IGNORE_CASE),
        Regex("\\bvalid\\s*(?:for|only)\\s*\\d+\\s*(?:min|minutes)\\b", RegexOption.IGNORE_CASE)
    )

    /** Failed / declined — exclude. */
    private val FAILED_PATTERNS = listOf(
        Regex("\\b(?:transaction|payment|transfer)\\s*(?:failed|declined|unsuccessful|could not)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:failed|declined|unsuccessful)\\s*(?:transaction|payment)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:insufficient|not enough)\\s*(?:balance|funds)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:invalid|incorrect)\\s*(?:otp|pin|password)\\b", RegexOption.IGNORE_CASE)
    )

    /** Promotional / marketing — exclude. */
    private val PROMO_PATTERNS = listOf(
        Regex("\\b(?:offer|discount|promo|cashback|reward)\\s*(?:on|for|get)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:click here|register now|act now|limited time)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:unsubscribe|stop|reply)\\s*(?:to\\s*)?(?:opt\\s*out|stop)\\b", RegexOption.IGNORE_CASE)
    )

    /** Card limit / generic alerts that are not a single transaction. */
    private val ALERT_EXCLUDE = listOf(
        Regex("\\b(?:credit\\s*)?limit\\s*(?:increased|revised|alert)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:spend|usage)\\s*(?:limit|alert)\\b", RegexOption.IGNORE_CASE)
    )

    /**
     * Returns true only if the message should be saved as a transaction:
     * - Has a valid numeric amount (caller checks)
     * - Contains at least one transaction verb
     * - Is not OTP, balance-only, failed/declined, or promotional
     */
    fun shouldAcceptAsTransaction(messageBody: String): Boolean {
        if (messageBody.isBlank()) return false
        val lower = messageBody.lowercase().trim()

        // Must contain at least one transaction verb (context implies money movement)
        if (!TRANSACTION_VERBS.any { lower.contains(it) }) return false

        // Exclude OTP
        if (OTP_PATTERNS.any { it.containsMatchIn(lower) }) return false

        // Exclude failed/declined
        if (FAILED_PATTERNS.any { it.containsMatchIn(lower) }) return false

        // Exclude promotional
        if (PROMO_PATTERNS.any { it.containsMatchIn(lower) }) return false

        // Exclude limit alerts
        if (ALERT_EXCLUDE.any { it.containsMatchIn(lower) }) return false

        return true
    }
}
