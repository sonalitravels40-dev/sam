package com.example.data

import kotlinx.coroutines.flow.Flow

interface TransactionDao {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsByStatus(status: String): Flow<List<TransactionEntity>>
    suspend fun getTransactionById(id: Long): TransactionEntity?
    suspend fun findByTransactionId(txnId: String): TransactionEntity?
    suspend fun findByHash(hash: String): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
    fun getCount(): Flow<Int>
    fun getPendingCount(): Flow<Int>
    fun getSentCount(): Flow<Int>
    fun getFailedCount(): Flow<Int>
}

interface SubmissionHistoryDao {
    fun getAllHistory(): Flow<List<SubmissionHistoryEntity>>
    suspend fun insertHistory(history: SubmissionHistoryEntity): Long
    suspend fun clearHistory()
}

interface ApprovedSenderDao {
    fun getAllSenders(): Flow<List<ApprovedSenderEntity>>
    suspend fun getEnabledSenders(): List<ApprovedSenderEntity>
    suspend fun insertSender(sender: ApprovedSenderEntity): Long
    suspend fun insertSenders(senders: List<ApprovedSenderEntity>)
    suspend fun updateSender(sender: ApprovedSenderEntity)
    suspend fun deleteSender(sender: ApprovedSenderEntity)
    suspend fun getSenderCount(): Int
}

interface AuditLogDao {
    fun getRecentLogs(): Flow<List<AuditLogEntity>>
    suspend fun insertLog(log: AuditLogEntity): Long
    suspend fun clearLogs()
}
