package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.core.PaymentSmsProcessor
import com.example.data.AppDatabase
import com.example.data.AuditLogEntity
import com.example.data.TransactionEntity
import com.example.service.NotificationHelper
import com.example.service.SmsMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        // Only process if merchant has explicitly activated monitoring
        if (!SmsMonitoringService.isMonitoringActive(context)) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages: Array<SmsMessage>? = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) {
                    return@launch
                }

                // Group multi-part SMS by originating address
                val messagesBySender = mutableMapOf<String, StringBuilder>()
                var smsTimestamp = System.currentTimeMillis()

                for (msg in messages) {
                    val sender = msg.originatingAddress ?: msg.displayOriginatingAddress ?: continue
                    val body = msg.messageBody ?: msg.displayMessageBody ?: ""
                    smsTimestamp = msg.timestampMillis

                    if (!messagesBySender.containsKey(sender)) {
                        messagesBySender[sender] = StringBuilder()
                    }
                    messagesBySender[sender]?.append(body)
                }

                val db = AppDatabase.getInstance(context)
                val enabledSenders = db.approvedSenderDao().getEnabledSenders()

                for ((sender, bodyBuilder) in messagesBySender) {
                    val fullMessage = bodyBuilder.toString()
                    if (fullMessage.isBlank()) continue

                    // 1. Verify approved sender
                    if (!PaymentSmsProcessor.isApprovedSender(sender, enabledSenders)) {
                        continue
                    }

                    // 2. Filter out sensitive OTP/PIN messages
                    if (PaymentSmsProcessor.isSensitiveMessage(fullMessage)) {
                        db.auditLogDao().insertLog(
                            AuditLogEntity(
                                action = "SENSITIVE_SMS_IGNORED",
                                details = "Ignored sensitive OTP/PIN/Authentication message from $sender."
                            )
                        )
                        continue
                    }

                    // 3. Parse transaction
                    val parsed = PaymentSmsProcessor.parseTransactionSms(sender, fullMessage)
                    if (!parsed.isValid && parsed.rejectReason != null) {
                        db.auditLogDao().insertLog(
                            AuditLogEntity(
                                action = "SMS_PARSE_UNSUPPORTED",
                                details = "SMS from $sender did not contain valid payment fields: ${parsed.rejectReason}"
                            )
                        )
                        continue
                    }

                    // 4. Check duplicate transaction
                    val hash = PaymentSmsProcessor.generateTransactionHash(
                        parsed.provider,
                        parsed.transactionId,
                        parsed.amount
                    )
                    val existingTxns = db.transactionDao().getAllTransactions().first()
                    val isDuplicate = PaymentSmsProcessor.isDuplicateTransaction(
                        parsed.transactionId,
                        hash,
                        existingTxns
                    )

                    val status = if (isDuplicate) "DUPLICATE" else "PENDING"

                    val entity = TransactionEntity(
                        transactionId = parsed.transactionId,
                        provider = parsed.provider,
                        senderId = parsed.senderId,
                        amount = parsed.amount,
                        currency = parsed.currency,
                        customerReference = parsed.customerReference,
                        balance = parsed.balance,
                        originalSmsMasked = parsed.maskedOriginalSms,
                        transactionHash = hash,
                        confidenceScore = parsed.confidenceScore,
                        timestamp = smsTimestamp,
                        status = status,
                        needsReview = parsed.needsReview || isDuplicate
                    )

                    db.transactionDao().insertTransaction(entity)
                    db.auditLogDao().insertLog(
                        AuditLogEntity(
                            action = "SMS_TRANSACTION_CAPTURED",
                            details = "Captured ${parsed.provider} payment: Txn ${parsed.transactionId}, Tk ${parsed.amount} ($status)"
                        )
                    )

                    // 5. Fire alert notification without sensitive content
                    if (!isDuplicate) {
                        NotificationHelper.showPaymentAlertNotification(
                            context,
                            parsed.provider,
                            parsed.amount,
                            entity.transactionId.hashCode()
                        )
                    }
                }
            } catch (e: Exception) {
                // Safeguard against unexpected crash
            } finally {
                pendingResult.finish()
            }
        }
    }
}
