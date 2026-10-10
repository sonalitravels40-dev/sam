package com.example.core

data class SampleSmsItem(
    val title: String,
    val provider: String,
    val sender: String,
    val body: String,
    val isSensitiveTest: Boolean = false,
    val expectedAmount: Double = 0.0,
    val expectedTxnId: String = ""
)

object SampleSmsData {
    val samples = listOf(
        SampleSmsItem(
            title = "bKash Payment Received (English)",
            provider = "bKash",
            sender = "bKash",
            body = "You have received payment Tk 1,250.00 from 01712345678. Ref: ORDER_981. Fee Tk 0.00. Balance Tk 45,210.50. TrxID 9K34MB991 at 09/10/2026 14:30.",
            expectedAmount = 1250.00,
            expectedTxnId = "9K34MB991"
        ),
        SampleSmsItem(
            title = "bKash টাকা গ্রহণ (Bengali)",
            provider = "bKash",
            sender = "bKash",
            body = "আপনি 01898765432 হতে টাকা গ্রহণ করেছেন Tk 2,500.00। রেফারেন্স: INV-204। নতুন ব্যালেন্স Tk 18,340.00। লেনদেন আইডি: BK981X29A।",
            expectedAmount = 2500.00,
            expectedTxnId = "BK981X29A"
        ),
        SampleSmsItem(
            title = "Nagad Payment Received (English)",
            provider = "Nagad",
            sender = "NAGAD",
            body = "Payment received. Amount: Tk 3,400.00 from 01923456789. Ref: TABLE_05. TxnID: NG7710294. Balance: Tk 32,800.00. Date: 09/10/2026 15:45.",
            expectedAmount = 3400.00,
            expectedTxnId = "NG7710294"
        ),
        SampleSmsItem(
            title = "Nagad পেমেন্ট গ্রহণ (Bengali)",
            provider = "Nagad",
            sender = "NAGAD",
            body = "পেমেন্ট গ্রহণ সফল হয়েছে। গৃহীত পরিমাণ Tk 750.00 গ্রাহক 01611223344 হতে। TrxID: NG4481923। বর্তমান ব্যালেন্স Tk 12,050.00।",
            expectedAmount = 750.00,
            expectedTxnId = "NG4481923"
        ),
        SampleSmsItem(
            title = "Rocket DBBL Payment (English)",
            provider = "Rocket",
            sender = "16216",
            body = "DBBL Rocket: Payment of Tk 1,800.00 received from A/C 01755443322-1. Ref: POS_TERMINAL. Txn ID: RK88210394. Balance Tk 8,950.00.",
            expectedAmount = 1800.00,
            expectedTxnId = "RK88210394"
        ),
        SampleSmsItem(
            title = "Rocket টাকা প্রাপ্তি (Bengali)",
            provider = "Rocket",
            sender = "16216",
            body = "রকেট লেনদেন: একাউন্ট 01844556677 হতে টাকা প্রাপ্তি Tk 950.00। TrxID: RK9921447। আপনার একাউন্ট ব্যালেন্স Tk 24,110.00।",
            expectedAmount = 950.00,
            expectedTxnId = "RK9921447"
        ),
        SampleSmsItem(
            title = "Upay Merchant Alert (English)",
            provider = "Upay",
            sender = "Upay",
            body = "Payment received Tk 620.00 from 01300998877. TrxID: UP1109482. Ref: BILL_44. Balance Tk 15,400.00 at 09/10/2026 16:10.",
            expectedAmount = 620.00,
            expectedTxnId = "UP1109482"
        ),
        SampleSmsItem(
            title = "Safety Test: OTP / PIN Verification (Must Be Blocked)",
            provider = "Security Check",
            sender = "bKash",
            body = "Your bKash verification code (OTP) is 849201. Never share this PIN or OTP with anyone, even bKash officers.",
            isSensitiveTest = true,
            expectedAmount = 0.0,
            expectedTxnId = ""
        ),
        SampleSmsItem(
            title = "Safety Test: Bengali গোপন পিন কোড (Must Be Blocked)",
            provider = "Security Check",
            sender = "NAGAD",
            body = "আপনার নগদ গোপন পিন নম্বর ৪৭২৮। এই ওটিপি কোড কাউকে বলবেন না।",
            isSensitiveTest = true,
            expectedAmount = 0.0,
            expectedTxnId = ""
        )
    )
}
