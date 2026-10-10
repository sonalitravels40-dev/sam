package com.example

import com.example.core.ApiSettings
import com.example.core.PaymentSmsProcessor
import com.example.core.SampleSmsData
import com.example.core.SketchwareHelperUtils
import com.example.data.ApprovedSenderEntity
import com.example.data.TransactionEntity
import org.junit.Assert.*
import org.junit.Test

class PaymentSmsProcessorTest {

    @Test
    fun testApprovedSenderDetection() {
        val approvedSenders = listOf(
            ApprovedSenderEntity(senderPattern = "bKash", providerName = "bKash"),
            ApprovedSenderEntity(senderPattern = "NAGAD", providerName = "Nagad"),
            ApprovedSenderEntity(senderPattern = "16216", providerName = "Rocket")
        )

        assertTrue(PaymentSmsProcessor.isApprovedSender("bKash", approvedSenders))
        assertTrue(PaymentSmsProcessor.isApprovedSender("nagad", approvedSenders))
        assertTrue(PaymentSmsProcessor.isApprovedSender("16216", approvedSenders))
        assertFalse(PaymentSmsProcessor.isApprovedSender("SpamBank", approvedSenders))
        assertFalse(PaymentSmsProcessor.isApprovedSender("", approvedSenders))
    }

    @Test
    fun testSensitiveMessageRejection() {
        // Must reject English and Bengali OTPs/PINs
        assertTrue(PaymentSmsProcessor.isSensitiveMessage("Your OTP is 1234. Never share it."))
        assertTrue(PaymentSmsProcessor.isSensitiveMessage("bKash verification code (OTP) 987654"))
        assertTrue(PaymentSmsProcessor.isSensitiveMessage("আপনার গোপন পিন নম্বর ৪৭২৮। কাউকে বলবেন না।"))
        assertTrue(PaymentSmsProcessor.isSensitiveMessage("Do not share your password or pin."))

        // Legitimate payment SMS must NOT be flagged as sensitive
        val validSms = "You have received payment Tk 1,250.00 from 01712345678. Ref: ORDER_981. Fee Tk 0.00. Balance Tk 45,210.50. TrxID 9K34MB991"
        assertFalse(PaymentSmsProcessor.isSensitiveMessage(validSms))
    }

    @Test
    fun testBkashEnglishParsing() {
        val sms = "You have received payment Tk 1,250.00 from 01712345678. Ref: ORDER_981. Fee Tk 0.00. Balance Tk 45,210.50. TrxID 9K34MB991 at 09/10/2026 14:30."
        val result = PaymentSmsProcessor.parseTransactionSms("bKash", sms)

        assertTrue(result.isValid)
        assertEquals("bKash", result.provider)
        assertEquals("9K34MB991", result.transactionId)
        assertEquals(1250.00, result.amount, 0.01)
        assertEquals(45210.50, result.balance ?: 0.0, 0.01)
        assertTrue(result.confidenceScore >= 80)
    }

    @Test
    fun testNagadBengaliParsing() {
        val sms = "পেমেন্ট গ্রহণ সফল হয়েছে। গৃহীত পরিমাণ Tk 750.00 গ্রাহক 01611223344 হতে। TrxID: NG4481923। বর্তমান ব্যালেন্স Tk 12,050.00।"
        val result = PaymentSmsProcessor.parseTransactionSms("NAGAD", sms)

        assertTrue(result.isValid)
        assertEquals("Nagad", result.provider)
        assertEquals("NG4481923", result.transactionId)
        assertEquals(750.00, result.amount, 0.01)
        assertEquals(12050.00, result.balance ?: 0.0, 0.01)
    }

    @Test
    fun testRocketParsing() {
        val sms = "DBBL Rocket: Payment of Tk 1,800.00 received from A/C 01755443322-1. Ref: POS_TERMINAL. Txn ID: RK88210394. Balance Tk 8,950.00."
        val result = PaymentSmsProcessor.parseTransactionSms("16216", sms)

        assertTrue(result.isValid)
        assertEquals("Rocket", result.provider)
        assertEquals("RK88210394", result.transactionId)
        assertEquals(1800.00, result.amount, 0.01)
    }

    @Test
    fun testDuplicateDetection() {
        val existing = listOf(
            TransactionEntity(
                transactionId = "TXN_EXISTING_1",
                provider = "bKash",
                senderId = "bKash",
                amount = 500.0,
                transactionHash = "hash1"
            )
        )

        assertTrue(PaymentSmsProcessor.isDuplicateTransaction("TXN_EXISTING_1", "hash2", existing))
        assertTrue(PaymentSmsProcessor.isDuplicateTransaction("TXN_OTHER", "hash1", existing))
        assertFalse(PaymentSmsProcessor.isDuplicateTransaction("TXN_NEW", "hash_new", existing))
    }

    @Test
    fun testSketchwareHelperParity() {
        val sms = "You have received payment Tk 500.00 from 01712345678. Ref: BILL1. Balance Tk 5,000.00. TrxID TXN789"
        val data = SketchwareHelperUtils.parseTransactionSms("bKash", sms)

        assertTrue(data.isValid)
        assertEquals("TXN789", data.transactionId)
        assertEquals(500.0, data.amount, 0.01)
        assertEquals(5000.0, data.balance, 0.01)
    }

    @Test
    fun testHttpRejection() {
        val tx = TransactionEntity(
            transactionId = "TXN_1",
            provider = "bKash",
            senderId = "bKash",
            amount = 100.0
        )
        val insecureSettings = ApiSettings(apiUrl = "http://insecure-api.com/reconcile")
        val result = PaymentSmsProcessor.sendTransactionToApi(tx, insecureSettings, "test-secret")

        assertFalse(result.isSuccess)
        assertTrue(result.errorMessage?.contains("HTTPS") == true)
    }
}
