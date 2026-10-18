package com.instantledger.domain.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SMSValidationGateTest {

    @Test
    fun `accepts debited transaction with amount context`() {
        assertTrue(SMSValidationGate.shouldAcceptAsTransaction("Rs. 500 debited from your account at Amazon"))
    }

    @Test
    fun `accepts credited transaction`() {
        assertTrue(SMSValidationGate.shouldAcceptAsTransaction("Rs. 1000 credited to your account"))
    }

    @Test
    fun `accepts UPI transaction`() {
        assertTrue(SMSValidationGate.shouldAcceptAsTransaction("UPI txn of Rs. 200 to Merchant"))
    }

    @Test
    fun `rejects OTP message`() {
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Your OTP is 123456 for login"))
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("OTP: 123456 is your verification code"))
    }

    @Test
    fun `rejects failed transaction`() {
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Transaction failed: insufficient balance"))
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Payment declined for Rs. 500"))
    }

    @Test
    fun `rejects promotional message`() {
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Get 50% cashback on your next purchase"))
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Limited time offer! Click here now"))
    }

    @Test
    fun `rejects balance-only alert`() {
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Your available balance is Rs. 10,000.00"))
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Account balance: Rs. 5000"))
    }

    @Test
    fun `rejects blank message`() {
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction(""))
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("   "))
    }

    @Test
    fun `does not reject legitimate transaction containing word offer in merchant name`() {
        // A real transaction should not be rejected merely because "offer" appears in merchant
        assertTrue(SMSValidationGate.shouldAcceptAsTransaction("Rs. 250 debited at Offer Mart store"))
    }

    @Test
    fun `rejects card limit alert`() {
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Your credit limit has been increased"))
    }

    @Test
    fun `rejects statement generated alert`() {
        assertFalse(SMSValidationGate.shouldAcceptAsTransaction("Your monthly statement is ready"))
    }
}
