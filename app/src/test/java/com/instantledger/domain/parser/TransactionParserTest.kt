package com.instantledger.domain.parser

import com.instantledger.data.model.EntryType
import com.instantledger.data.model.PaymentMode
import com.instantledger.data.model.SourceType
import com.instantledger.data.model.TransactionStatus
import com.instantledger.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionParserTest {

    @Test
    fun `parseSMSMessage returns null for blank body`() {
        val parser = TransactionParser(SMSParser())
        assertNull(parser.parseSMSMessage("", 1000L))
        assertNull(parser.parseSMSMessage("   ", 1000L))
    }

    @Test
    fun `parseSMSMessage returns null for invalid timestamp`() {
        val parser = TransactionParser(SMSParser())
        assertNull(parser.parseSMSMessage("Rs. 100 debited", 0L))
        assertNull(parser.parseSMSMessage("Rs. 100 debited", -1L))
    }

    @Test
    fun `parseSMSMessage returns null when validation gate rejects`() {
        val parser = TransactionParser(SMSParser())
        // OTP message should be rejected by the gate
        assertNull(parser.parseSMSMessage("Your OTP is 123456", 1000L))
    }

    @Test
    fun `parseSMSMessage creates transaction with hash`() {
        val parser = TransactionParser(SMSParser())
        val txn = parser.parseSMSMessage("Rs. 500 debited from your account at Amazon", 1000L)
        assertNotNull(txn)
        assertEquals(500.0, txn.amount, 0.001)
        assertEquals("Amazon", txn.merchant)
        assertEquals(TransactionType.DEBIT, txn.transactionType)
        assertEquals(EntryType.AUTO_CAPTURED, txn.entryType)
        assertEquals(SourceType.SMS, txn.sourceType)
        assertFalse(txn.isApproved)
        assertNotNull(txn.rawTextHash)
        assertTrue(txn.rawTextHash!!.isNotEmpty())
    }

    @Test
    fun `duplicate hash is deterministic`() {
        val body = "Rs. 500 debited from your account at Amazon"
        val normalized = TransactionParser.normalizeForHash(body)
        val hash1 = TransactionParser.hashForDuplicateCheck(normalized)
        val hash2 = TransactionParser.hashForDuplicateCheck(normalized)
        assertEquals(hash1, hash2)
        assertTrue(hash1.length == 64) // SHA-256 hex
    }

    @Test
    fun `normalizeForHash collapses whitespace`() {
        assertEquals("a b c", TransactionParser.normalizeForHash("  a   b\n\tc  "))
    }

    @Test
    fun `hashForDuplicateCheck returns empty for blank`() {
        assertEquals("", TransactionParser.hashForDuplicateCheck(""))
        assertEquals("", TransactionParser.hashForDuplicateCheck("   "))
    }

    @Test
    fun `parseManualEntry creates approved manual transaction`() {
        val parser = TransactionParser(SMSParser())
        val txn = parser.parseManualEntry(
            amount = 250.0,
            merchant = "  Manual Store  ",
            category = "Food",
            paymentMode = PaymentMode.CASH,
            timestamp = 2000L,
            notes = "lunch"
        )
        assertEquals(250.0, txn.amount, 0.001)
        assertEquals("Manual Store", txn.merchant)
        assertEquals("Food", txn.category)
        assertEquals(PaymentMode.CASH, txn.paymentMode)
        assertEquals(EntryType.USER_ENTERED, txn.entryType)
        assertEquals(SourceType.MANUAL, txn.sourceType)
        assertEquals(1.0f, txn.confidenceScore, 0.001f)
        assertNull(txn.rawTextHash)
    }
}
