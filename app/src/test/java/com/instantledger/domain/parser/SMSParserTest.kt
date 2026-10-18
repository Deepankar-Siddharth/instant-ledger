package com.instantledger.domain.parser

import com.instantledger.data.model.EntryType
import com.instantledger.data.model.PaymentMode
import com.instantledger.data.model.SourceType
import com.instantledger.data.model.TransactionStatus
import com.instantledger.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SMSParserTest {

    @Test
    fun `extracts amount from Rupee-prefixed message`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Rs. 1,200.00 debited from your account at Amazon.in")
        assertNotNull(parsed.amount)
        assertEquals(1200.0, parsed.amount, 0.001)
    }

    @Test
    fun `extracts amount from INR keyword`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("INR 500 credited to your account")
        assertNotNull(parsed.amount)
        assertEquals(500.0, parsed.amount, 0.001)
    }

    @Test
    fun `detects debit transaction type`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Rs. 200 debited from your account")
        assertEquals(TransactionType.DEBIT, parsed.transactionType)
    }

    @Test
    fun `detects credit transaction type`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Rs. 1000 credited to your account")
        assertEquals(TransactionType.CREDIT, parsed.transactionType)
    }

    @Test
    fun `extracts merchant from at keyword`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Rs. 300 spent at Swiggy")
        assertNotNull(parsed.merchant)
        assertEquals("Swiggy", parsed.merchant)
    }

    @Test
    fun `detects UPI payment mode`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("UPI payment of Rs. 400 to Merchant")
        assertEquals(PaymentMode.UPI, parsed.paymentMode)
    }

    @Test
    fun `bank-specific HDFC pattern extracts amount`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("HDFC Bank: A/c *1234 debited with Rs. 750.00")
        assertNotNull(parsed.amount)
        assertEquals(750.0, parsed.amount, 0.001)
    }

    @Test
    fun `message with no amount returns null amount`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Welcome to Instant Ledger")
        assertNull(parsed.amount)
    }

    @Test
    fun `confidence score is zero when nothing detected`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Hello there")
        assertEquals(0f, parsed.confidenceScore, 0.001f)
    }

    @Test
    fun `extracts reference number`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Rs. 500 debited, Ref: AXB1234567")
        assertNotNull(parsed.referenceNumber)
        assertEquals("AXB1234567", parsed.referenceNumber)
    }

    @Test
    fun `extracts currency as INR`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("₹ 1,200.00 debited")
        assertEquals("INR", parsed.currency)
    }

    @Test
    fun `confidence level is HIGH when amount merchant type mode present`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Rs. 500 debited from your account at Amazon via UPI")
        assertEquals(ConfidenceLevel.HIGH, parsed.confidenceLevel)
    }

    @Test
    fun `confidence level is MEDIUM with amount and merchant only`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Rs. 500 at Amazon")
        assertEquals(ConfidenceLevel.MEDIUM, parsed.confidenceLevel)
    }

    @Test
    fun `detects EMI as debit`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("EMI of Rs. 2000 debited")
        assertEquals(TransactionType.DEBIT, parsed.transactionType)
    }

    @Test
    fun `detects refund as credit`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("Rs. 300 refunded to your account")
        assertEquals(TransactionType.CREDIT, parsed.transactionType)
    }

    @Test
    fun `kotak bank pattern extracts amount`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("KOTAK: Rs. 800 debited from your account")
        assertNotNull(parsed.amount)
        assertEquals(800.0, parsed.amount, 0.001)
    }

    @Test
    fun `boi bank pattern extracts amount`() {
        val parser = SMSParser()
        val parsed = parser.parseSMS("BOI: Rs. 900 debited")
        assertNotNull(parsed.amount)
        assertEquals(900.0, parsed.amount, 0.001)
    }
}
