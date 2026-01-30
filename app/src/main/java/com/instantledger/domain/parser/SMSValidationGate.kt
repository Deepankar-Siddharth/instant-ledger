package com.instantledger.domain.parser

/**
 * Validation gate before saving an SMS as a transaction.
 * A message is accepted if it has amount (caller checks) AND (strong verb OR financial rail keyword),
 * and is NOT OTP / failed / promo / limit alert.
 */
object SMSValidationGate {

    /** Transaction verbs and channel phrases that imply money movement. */
    private val TRANSACTION_VERBS = listOf(
        // Generic
        "credited", "debited", "paid", "received", "spent", "refunded",
        "transferred", "transfer", "withdrawn", "deducted", "deposited",
        "reversed", "charged", "purchase", "purchased",
        // Banking short forms
        "cr", "dr", "txn", "trxn", "transaction",
        // UPI / wallet phrasing
        "sent to", "received from", "credited to", "debited from",
        "upi", "upi txn", "upi ref", "upi transfer",
        // Card / POS
        "card", "pos", "atm", "swipe", "tap", "tap & pay",
        "card ending", "ending with",
        // Bank rails
        "imps", "neft", "rtgs", "ach", "ecs",
        // Wallets
        "wallet", "paytm", "phonepe", "gpay", "google pay",
        "amazon pay", "mobikwik", "freecharge"
    )

    /** Financial rail / channel keywords (UPI, card, bank, wallet). */
    private val FINANCIAL_RAIL_KEYWORDS = listOf(
        "upi", "imps", "neft", "rtgs", "ach", "ecs",
        "pos", "atm", "card", "debit card", "credit card",
        "wallet", "bank a/c", "account", "a/c", "acct"
    )

    /** OTP / verification patterns — exclude. */
    private val OTP_PATTERNS = listOf(
        Regex("\\b(?:otp|verification code|one time password)\\s*[.:]?\\s*\\d{4,8}\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\d{4,8}\\s*(?:is|as)\\s*(?:your|the)\\s*(?:otp|code)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:use|enter)\\s*(?:otp|code)\\s*\\d{4,8}\\b", RegexOption.IGNORE_CASE),
        Regex("\\bvalid\\s*(?:for|only)\\s*\\d+\\s*(?:min|minutes)\\b", RegexOption.IGNORE_CASE)
    )

    /** Failed / declined / reversed — exclude. */
    private val FAILED_PATTERNS = listOf(
        Regex("\\b(?:transaction|payment|transfer)\\s*(?:failed|declined|unsuccessful|could not|reversed)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:failed|declined|unsuccessful|reversed)\\s*(?:transaction|payment)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:insufficient|not enough)\\s*(?:balance|funds)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:invalid|incorrect)\\s*(?:otp|pin|password)\\b", RegexOption.IGNORE_CASE)
    )

    /** Promotional / marketing — exclude. */
    private val PROMO_PATTERNS = listOf(
        Regex("\\b(?:offer|discount|promo|cashback|reward)\\s*(?:on|for|get)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:click here|register now|act now|limited time)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:unsubscribe|stop|reply)\\s*(?:to\\s*)?(?:opt\\s*out|stop)\\b", RegexOption.IGNORE_CASE)
    )

    /** Card limit / statement / generic alerts — exclude (not a single transaction). */
    private val ALERT_EXCLUDE = listOf(
        Regex("\\b(?:credit\\s*)?limit\\s*(?:increased|revised|alert)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:spend|usage)\\s*(?:limit|alert)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:statement|e-?statement|account statement)\\s*(?:generated|available|ready)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:monthly|weekly)\\s*statement\\b", RegexOption.IGNORE_CASE)
    )

    /** Balance-only alerts (no transaction; just balance info) — exclude. */
    private val BALANCE_ONLY_PATTERNS = listOf(
        Regex("\\b(?:available balance|avl\\.?\\s*bal|balance is|your balance)\\s*[.:]?\\s*[Rr]s\\.?\\s*[\\d,]+\\.?\\d*\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:balance|bal)\\s*(?:as of|on)\\s*\\d", RegexOption.IGNORE_CASE),
        Regex("\\b(?:account balance|current balance)\\s*[.:]?", RegexOption.IGNORE_CASE)
    )

    /**
     * Returns true only if the message should be saved as a transaction:
     * - Has a valid numeric amount (caller checks)
     * - Contains at least one transaction verb OR one financial rail keyword
     * - Is not OTP, failed/declined, promotional, or limit alert
     */
    fun shouldAcceptAsTransaction(messageBody: String): Boolean {
        if (messageBody.isBlank()) return false
        val lower = messageBody.lowercase().trim()

        val hasVerb = TRANSACTION_VERBS.any { lower.contains(it) }
        val hasRail = FINANCIAL_RAIL_KEYWORDS.any { lower.contains(it) }
        // Must indicate money movement in some form (verb or rail)
        if (!hasVerb && !hasRail) return false

        // Exclude OTP
        if (OTP_PATTERNS.any { it.containsMatchIn(lower) }) return false

        // Exclude failed/declined
        if (FAILED_PATTERNS.any { it.containsMatchIn(lower) }) return false

        // Exclude promotional
        if (PROMO_PATTERNS.any { it.containsMatchIn(lower) }) return false

        // Exclude limit / statement alerts
        if (ALERT_EXCLUDE.any { it.containsMatchIn(lower) }) return false

        // Exclude balance-only alerts (message is only about balance, no transaction verb)
        val looksBalanceOnly = BALANCE_ONLY_PATTERNS.any { it.containsMatchIn(lower) }
        val hasStrongTransactionVerb = listOf(
            "credited", "debited", "paid", "received", "spent", "charged",
            "withdrawn", "deducted", "deposited", "transferred", "refunded", "purchase", "purchased"
        ).any { lower.contains(it) }
        if (looksBalanceOnly && !hasStrongTransactionVerb) return false

        return true
    }
}
