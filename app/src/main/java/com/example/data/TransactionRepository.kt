package com.example.data

import android.content.Context
import com.example.core.ApiResult
import com.example.core.ApiSettings
import com.example.core.PaymentSmsProcessor
import com.example.security.KeystoreSecretManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    val allTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactions()
    val pendingCount: Flow<Int> = database.transactionDao().getPendingCount()
    val sentCount: Flow<Int> = database.transactionDao().getSentCount()
    val failedCount: Flow<Int> = database.transactionDao().getFailedCount()
    val totalCount: Flow<Int> = database.transactionDao().getCount()

    val submissionHistory: Flow<List<SubmissionHistoryEntity>> = database.submissionHistoryDao().getAllHistory()
    val approvedSenders: Flow<List<ApprovedSenderEntity>> = database.approvedSenderDao().getAllSenders()
    val auditLogs: Flow<List<AuditLogEntity>> = database.auditLogDao().getRecentLogs()

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return database.transactionDao().getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return database.transactionDao().insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        database.transactionDao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        database.transactionDao().deleteById(id)
        database.auditLogDao().insertLog(
            AuditLogEntity(
                action = "TRANSACTION_DELETED",
                details = "Deleted transaction record ID: $id"
            )
        )
    }

    suspend fun deleteAllTransactions() {
        database.transactionDao().deleteAll()
        database.auditLogDao().insertLog(
            AuditLogEntity(
                action = "ALL_TRANSACTIONS_DELETED",
                details = "User wiped all transaction records locally."
            )
        )
    }

    suspend fun clearHistory() {
        database.submissionHistoryDao().clearHistory()
        database.auditLogDao().insertLog(
            AuditLogEntity(
                action = "SUBMISSION_HISTORY_CLEARED",
                details = "User cleared all API submission history logs."
            )
        )
    }

    suspend fun wipeAllData() {
        database.transactionDao().deleteAll()
        database.submissionHistoryDao().clearHistory()
        database.auditLogDao().clearLogs()
        KeystoreSecretManager.clearAllSecrets(context)
    }

    suspend fun addApprovedSender(pattern: String, provider: String, notes: String = "") {
        database.approvedSenderDao().insertSender(
            ApprovedSenderEntity(
                senderPattern = pattern.trim(),
                providerName = provider.trim(),
                isEnabled = true,
                notes = notes
            )
        )
        database.auditLogDao().insertLog(
            AuditLogEntity(
                action = "APPROVED_SENDER_ADDED",
                details = "Added approved sender: $pattern ($provider)"
            )
        )
    }

    suspend fun updateApprovedSender(sender: ApprovedSenderEntity) {
        database.approvedSenderDao().updateSender(sender)
    }

    suspend fun deleteApprovedSender(sender: ApprovedSenderEntity) {
        database.approvedSenderDao().deleteSender(sender)
    }

    /**
     * Executes HTTPS transmission of a single verified transaction to the gateway.
     */
    suspend fun sendToGateway(
        transaction: TransactionEntity,
        settings: ApiSettings
    ): ApiResult = withContext(Dispatchers.IO) {
        val secret = KeystoreSecretManager.getApiSecret(context)
        val result = PaymentSmsProcessor.sendTransactionToApi(transaction, settings, secret)

        val updatedStatus = if (result.isSuccess) "SENT" else "FAILED"
        val updatedTx = transaction.copy(
            status = updatedStatus,
            apiResponseCode = result.httpCode,
            apiResponseMessage = result.errorMessage ?: "Success HTTP ${result.httpCode}",
            needsReview = false
        )
        database.transactionDao().updateTransaction(updatedTx)

        database.submissionHistoryDao().insertHistory(
            SubmissionHistoryEntity(
                transactionId = transaction.transactionId,
                provider = transaction.provider,
                amount = transaction.amount,
                destinationDomain = result.destinationDomain,
                httpStatus = result.httpCode,
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage
            )
        )

        database.auditLogDao().insertLog(
            AuditLogEntity(
                action = if (result.isSuccess) "GATEWAY_SUBMIT_SUCCESS" else "GATEWAY_SUBMIT_FAILED",
                details = "Txn ${transaction.transactionId} (${transaction.provider}) HTTP ${result.httpCode}: ${result.errorMessage ?: "OK"}"
            )
        )

        result
    }

    /**
     * Sends a mock test transaction payload to test endpoint connectivity.
     */
    suspend fun testApiEndpoint(settings: ApiSettings): ApiResult = withContext(Dispatchers.IO) {
        val dummyTx = TransactionEntity(
            transactionId = "TXN_TEST_" + System.currentTimeMillis() % 100000,
            provider = "bKash",
            senderId = "TEST_SENDER",
            amount = 100.00,
            currency = "BDT",
            customerReference = "TEST_CUSTOMER_REF",
            timestamp = System.currentTimeMillis()
        )
        val secret = KeystoreSecretManager.getApiSecret(context)
        val result = PaymentSmsProcessor.sendTransactionToApi(dummyTx, settings, secret)

        database.auditLogDao().insertLog(
            AuditLogEntity(
                action = "API_ENDPOINT_TESTED",
                details = "Tested endpoint: ${result.destinationDomain}, HTTP ${result.httpCode}: ${result.errorMessage ?: "Success"}"
            )
        )
        result
    }

    /**
     * Exports transactions locally to a CSV report.
     */
    suspend fun exportTransactionsToCsv(transactions: List<TransactionEntity>): File = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val fileName = "sms_reader_report_${dateFormat.format(Date())}.csv"
        val file = File(context.cacheDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("ID,TransactionID,Provider,Sender,Amount,Currency,CustomerReference,Status,Confidence,Timestamp\n")
            val recordDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            for (tx in transactions) {
                writer.append("${tx.id},")
                writer.append("\"${tx.transactionId}\",")
                writer.append("\"${tx.provider}\",")
                writer.append("\"${tx.senderId}\",")
                writer.append("${tx.amount},")
                writer.append("\"${tx.currency}\",")
                writer.append("\"${tx.customerReference}\",")
                writer.append("\"${tx.status}\",")
                writer.append("${tx.confidenceScore},")
                writer.append("\"${recordDateFormat.format(Date(tx.timestamp))}\"\n")
            }
        }
        file
    }
}
